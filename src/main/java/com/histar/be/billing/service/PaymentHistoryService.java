package com.histar.be.billing.service;

import com.histar.be.billing.dto.*;
import com.histar.be.common.exception.*;
import com.histar.be.config.SepayProperties;
import java.sql.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;

/** Account-scoped receipt history. GET operations never run entitlement activation. */
@Service
@RequiredArgsConstructor
public class PaymentHistoryService {
    private static final ZoneId VN=ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<String> TYPES=Set.of("ALL","PERSONAL","ORGANIZATION");
    private static final Set<String> STATUSES=Set.of("ALL","PENDING","UNDERPAID","PAID","EXPIRED","FAILED","REFUNDED","VOIDED");
    private final NamedParameterJdbcTemplate jdbc;
    private final SepayProperties properties;
    private final SepayB2cPaymentService personal;
    private final SepayOrgPaymentService organization;

    public PaymentHistoryPage list(UUID user,String type,String status,String q,LocalDate from,LocalDate to,int page,int size){
        type=choice(type,TYPES);status=choice(status,STATUSES);
        if(page<0||size<1||size>100||from!=null&&to!=null&&from.isAfter(to))throw new BusinessRuleException("Bộ lọc lịch sử thanh toán không hợp lệ");
        String search=q==null?"":q.trim().toLowerCase(Locale.ROOT);
        if(search.length()>200)throw new BusinessRuleException("Từ khóa tìm kiếm tối đa 200 ký tự");
        var params=params(user).addValue("type",type).addValue("status",status).addValue("q",search);
        String where=" where (:type='ALL' or kind=:type) and (:status='ALL' or normalized_status=:status)";
        if(!search.isEmpty())where+=" and position(:q in lower(concat(order_code,' ',coalesce(plan_type,''),' ',coalesce(org_name,''),' ',coalesce(site_code,''),' ',coalesce(transfer_content,''))))>0";
        if(from!=null){where+=" and created_at>=:from";params.addValue("from",Timestamp.from(from.atStartOfDay(VN).toInstant()));}
        if(to!=null){where+=" and created_at<:to";params.addValue("to",Timestamp.from(to.plusDays(1).atStartOfDay(VN).toInstant()));}
        String query=base()+where;
        var summary=jdbc.queryForObject("select count(*) total, coalesce(sum(case when normalized_status='PAID' then 1 else 0 end),0) paid, coalesce(sum(case when normalized_status not in ('REFUNDED','VOIDED') then received_amount else 0 end),0) received, count(distinct case when normalized_status='PAID' and "+active()+" then concat(kind,':',coalesce(cast(sub_id as varchar),order_code)) else null end) active from ("+query+") filtered",params,(rs,n)->new PaymentHistoryPage.Summary(rs.getLong("total"),rs.getLong("paid"),rs.getLong("received"),rs.getLong("active")));
        params.addValue("limit",size).addValue("offset",(long)page*size);
        var items=jdbc.query(query+" order by created_at desc nulls last,kind,id desc limit :limit offset :offset",params,this::map);
        return new PaymentHistoryPage(items,page,size,summary.totalTransactions(),(int)Math.ceil((double)summary.totalTransactions()/size),summary);
    }

    public PaymentHistoryItem detail(UUID user,String kind,UUID id){
        kind=choice(kind,Set.of("PERSONAL","ORGANIZATION"));
        var items=jdbc.query(base()+" where kind=:kind and id=:id",params(user).addValue("kind",kind).addValue("id",id),this::map);
        if(items.isEmpty())throw new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán");
        return items.get(0);
    }

    public PaymentHistoryItem refresh(UUID user,String kind,UUID id){
        var receipt=detail(user,kind,id); // Authenticate ownership before any mutation/provider operation.
        if("ORGANIZATION".equals(receipt.kind()))organization.getPaymentStatus(user,receipt.orderCode());
        else personal.getPaymentStatus(user,receipt.orderCode());
        return detail(user,kind,id);
    }

    private MapSqlParameterSource params(UUID user){
        Instant now=Instant.now();
        return new MapSqlParameterSource("user",user).addValue("now",Timestamp.from(now))
                .addValue("expiryCutoff",Timestamp.from(now.minusSeconds(Math.max(0,properties.getExpiryGraceMinutes())*60L)))
                .addValue("today",java.sql.Date.valueOf(LocalDate.now(VN)));
    }
    private static String choice(String value,Set<String> allowed){
        String result=value==null?"ALL":value.toUpperCase(Locale.ROOT);
        if(!allowed.contains(result))throw new BusinessRuleException("Loại giao dịch hoặc trạng thái không hợp lệ");
        return result;
    }

    private static String active(){return "((ent_start is not null and ent_start<=:now and ent_end>:now) or (ent_start is null and sub_active=true and start_date<=:today and end_date>=:today))";}

    private String base(){
        // PostgreSQL/H2 compatible extraction of the server-recorded cumulative receipt only.
        // Never expose provider_payload, which can contain banking and webhook metadata.
        String received="cast(nullif(regexp_replace(regexp_substr(provider_payload,'\"receivedAmount\"[[:space:]]*:[[:space:]]*[0-9]{1,18}'),'[^0-9]','','g'),'') as bigint)";
        return "select normalized.*, case when recorded_received is not null then recorded_received when normalized_status='PAID' then amount_vnd else null end received_amount from (select raw.*, "+received+" recorded_received, case when upper(status) in ('REFUND','REFUNDED') then 'REFUNDED' when upper(status) in ('VOID','VOIDED','CANCELLED','CANCELED') then 'VOIDED' when upper(status) in ('PENDING','UNDERPAID') and expires_at<=:expiryCutoff then 'EXPIRED' when upper(status) in ('PENDING','UNDERPAID','PAID','EXPIRED','FAILED') then upper(status) else 'FAILED' end normalized_status from ("+union()+") raw) normalized";
    }

    private String union(){
        String fields="p.id,p.provider,p.order_code,p.transfer_content,p.amount_vnd,p.status,p.provider_payload,p.provider_reference_code,p.plan_type,p.expires_at,p.paid_at,p.created_at,p.updated_at,s.start_date,s.end_date,s.is_active sub_active,s.id sub_id";
        String entitlement="select e.%s from b2c_visit_entitlements e where e.user_id=p.user_id and e.order_code=p.order_code order by e.expires_at desc limit 1";
        return "select 'PERSONAL' kind,"+fields+",p.site_code,cast(null as varchar) org_name,("+entitlement.formatted("starts_at")+") ent_start,("+entitlement.formatted("expires_at")+") ent_end from b2c_payment_transactions p left join b2c_subscriptions s on s.id=p.subscription_id and s.user_id=p.user_id where p.user_id=:user union all select 'ORGANIZATION' kind,"+fields+",cast(null as varchar) site_code,p.org_name,cast(null as timestamp with time zone) ent_start,cast(null as timestamp with time zone) ent_end from org_payment_transactions p left join subscriptions s on s.id=p.subscription_id where p.requester_user_id=:user";
    }

    private PaymentHistoryItem map(ResultSet rs,int row)throws SQLException{
        String kind=rs.getString("kind"),plan=rs.getString("plan_type"),status=rs.getString("normalized_status");
        Long amount=number(rs,"amount_vnd"),received=number(rs,"received_amount");
        Long remaining=received==null||amount==null||Set.of("REFUNDED","VOIDED").contains(status)
                ?null:Math.max(0,amount-received);
        Instant start=instant(rs,"ent_start"),end=instant(rs,"ent_end");
        java.sql.Date from=rs.getDate("start_date"),to=rs.getDate("end_date");
        if(start==null&&from!=null)start=from.toLocalDate().atStartOfDay(VN).toInstant();
        if(end==null&&to!=null)end=to.toLocalDate().plusDays(1).atStartOfDay(VN).toInstant();
        String activation;
        if("REFUNDED".equals(status)||"VOIDED".equals(status))activation="REVOKED";
        else if(!"PAID".equals(status))activation="NOT_ACTIVATED";
        else if(start==null||end==null)activation="PENDING";
        else if(!end.isAfter(Instant.now()))activation="EXPIRED";
        else if(start.isAfter(Instant.now()))activation="PENDING";
        else if(rs.getObject("ent_start")==null&&!Boolean.TRUE.equals(rs.getObject("sub_active",Boolean.class)))activation="REVOKED";
        else activation="ACTIVE";
        String name="ORGANIZATION".equals(kind)?"Gói tổ chức "+plan:"JOURNEY_PASS".equals(plan)?"Journey Pass":"Premium cá nhân";
        String message="PAID".equals(status)&&"PENDING".equals(activation)?"Đã ghi nhận thanh toán; quyền sử dụng đang chờ kích hoạt. Không chuyển khoản thêm.":null;
        return new PaymentHistoryItem(rs.getObject("id",UUID.class),kind,rs.getString("order_code"),plan,name,rs.getString("site_code"),rs.getString("org_name"),amount,received,remaining,status,rs.getString("status"),activation,rs.getString("provider"),rs.getString("transfer_content"),rs.getString("provider_reference_code"),instant(rs,"created_at"),instant(rs,"paid_at"),instant(rs,"expires_at"),instant(rs,"updated_at"),start,end,message);
    }
    private static Long number(ResultSet rs,String field)throws SQLException{Object value=rs.getObject(field);return value==null?null:((Number)value).longValue();}
    private static Instant instant(ResultSet rs,String field)throws SQLException{Timestamp value=rs.getTimestamp(field);return value==null?null:value.toInstant();}
}
