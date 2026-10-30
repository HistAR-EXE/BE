package com.histar.be.billing.service;

import com.histar.be.config.SepayProperties;
import com.histar.be.common.exception.*;
import java.time.*;
import java.util.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentHistoryServiceTest {
    JdbcTemplate db;
    PaymentHistoryService service;
    SepayB2cPaymentService personal;
    SepayOrgPaymentService organization;
    UUID owner=UUID.randomUUID(), stranger=UUID.randomUUID();
    @BeforeEach void setup(){
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        db=new JdbcTemplate(ds);
        String columns="id uuid primary key, provider varchar, order_code varchar, transfer_content varchar, amount_vnd bigint, status varchar, provider_payload varchar, provider_reference_code varchar, subscription_id uuid, plan_type varchar, expires_at timestamp with time zone, paid_at timestamp with time zone, created_at timestamp with time zone, updated_at timestamp with time zone";
        db.execute("create table b2c_payment_transactions ("+columns+", user_id uuid, site_code varchar)");
        db.execute("create table org_payment_transactions ("+columns+", requester_user_id uuid, org_name varchar)");
        db.execute("create table b2c_subscriptions(id uuid primary key,user_id uuid,start_date date,end_date date,is_active boolean)");
        db.execute("create table subscriptions(id uuid primary key,start_date date,end_date date,is_active boolean)");
        db.execute("create table b2c_visit_entitlements(id uuid,user_id uuid,order_code varchar,starts_at timestamp with time zone,expires_at timestamp with time zone)");
        personal=mock(SepayB2cPaymentService.class);organization=mock(SepayOrgPaymentService.class);
        service=new PaymentHistoryService(new NamedParameterJdbcTemplate(ds),new SepayProperties(),personal,organization);
    }
    UUID payment(boolean org,UUID user,String status,long amount,String payload,Instant created){
        UUID id=UUID.randomUUID();String table=org?"org_payment_transactions":"b2c_payment_transactions",userColumn=org?"requester_user_id":"user_id";
        db.update("insert into "+table+"(id,"+userColumn+",provider,order_code,transfer_content,amount_vnd,status,provider_payload,plan_type,created_at,expires_at) values (?,?,?,?,?,?,?,?,?,?,?)",id,user,"SEPAY","ORD"+id,"PAY "+id,amount,status,payload,org?"STANDARD":"PREMIUM",created,Instant.now().plusSeconds(300));return id;
    }
    @Test void unionPaginationAndSummaryAreScopedFilteredAndStable(){
        var now=Instant.now();payment(false,owner,"PAID",49000,"{\"receivedAmount\":50000}",now);
        payment(true,owner,"PAID",15000000,null,now.minusSeconds(10));payment(false,stranger,"PAID",999999,null,now);
        var page=service.list(owner,"ALL","PAID","",null,null,0,1);
        assertThat(page.totalItems()).isEqualTo(2);assertThat(page.totalPages()).isEqualTo(2);assertThat(page.items()).hasSize(1);
        assertThat(page.summary().totalReceivedVnd()).isEqualTo(15050000);
        assertThat(service.list(owner,"ORGANIZATION","ALL","",null,null,0,10).items()).hasSize(1);
        assertThat(service.list(owner,"ALL","ALL","%",null,null,0,10).items()).isEmpty();
        assertThat(service.list(owner,"ALL","ALL","",LocalDate.now().plusDays(2),null,0,10).items()).isEmpty();
    }
    @Test void detailAndRefreshRejectOtherUsersBeforeCallingPaymentServices(){
        UUID id=payment(true,stranger,"PAID",8000000,null,Instant.now());
        assertThatThrownBy(()->service.detail(owner,"ORGANIZATION",id)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(()->service.refresh(owner,"ORGANIZATION",id)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(personal,organization);
    }
    @Test void expiredUnderpaymentPreservesReceiptAndRefundDoesNotInventReceipt(){
        UUID id=payment(false,owner,"UNDERPAID",49000,"{\"receivedAmount\":10000}",Instant.now());
        db.update("update b2c_payment_transactions set expires_at=? where id=?",Instant.now().minusSeconds(7200),id);
        var item=service.detail(owner,"PERSONAL",id);
        assertThat(item.status()).isEqualTo("EXPIRED");assertThat(item.receivedAmountVnd()).isEqualTo(10000);assertThat(item.remainingAmountVnd()).isEqualTo(39000);
        UUID refund=payment(false,owner,"REFUNDED",49000,null,Instant.now());
        assertThat(service.detail(owner,"PERSONAL",refund).receivedAmountVnd()).isNull();
        assertThat(service.list(owner,"ALL","ALL","",null,null,0,10).summary().totalReceivedVnd()).isEqualTo(10000);
    }
    @Test void paidReceiptRemainsPaidWhenItsLinkedAccessExpiresAndReadsDoNotActivate(){
        UUID id=payment(false,owner,"PAID",49000,null,Instant.now());UUID sub=UUID.randomUUID();
        db.update("insert into b2c_subscriptions values(?,?,?,?,?)",sub,owner,LocalDate.now().minusMonths(2),LocalDate.now().minusDays(1),true);
        db.update("update b2c_payment_transactions set subscription_id=? where id=?",sub,id);
        var item=service.detail(owner,"PERSONAL",id);
        assertThat(item.status()).isEqualTo("PAID");assertThat(item.activationStatus()).isEqualTo("EXPIRED");
        assertThat(item.accessEndsAt()).isEqualTo(LocalDate.now().atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
        verifyNoInteractions(personal,organization);
    }
    @Test void invalidFiltersAndPageAreRejected(){
        assertThatThrownBy(()->service.list(owner,"OTHER","ALL","",null,null,0,10)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(()->service.list(owner,"ALL","ALL","",null,null,-1,10)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(()->service.detail(owner,"ALL",UUID.randomUUID())).isInstanceOf(BusinessRuleException.class);
    }
    @Test void activePackagesCountDistinctLinkedEntitlementsAndJourneyPass(){
        UUID first=payment(false,owner,"PAID",49000,null,Instant.now());
        UUID duplicate=payment(false,owner,"PAID",49000,null,Instant.now());
        UUID sub=UUID.randomUUID();
        db.update("insert into b2c_subscriptions values(?,?,?,?,?)",sub,owner,LocalDate.now().minusDays(1),LocalDate.now().plusDays(1),true);
        db.update("update b2c_payment_transactions set subscription_id=? where id in (?,?)",sub,first,duplicate);
        UUID journey=payment(false,owner,"PAID",29000,null,Instant.now());
        db.update("update b2c_payment_transactions set plan_type='JOURNEY_PASS',site_code='CU_CHI' where id=?",journey);
        db.update("insert into b2c_visit_entitlements values(?,?,?,?,?)",UUID.randomUUID(),owner,"ORD"+journey,Instant.now().minusSeconds(100),Instant.now().plusSeconds(1000));
        assertThat(service.detail(owner,"PERSONAL",journey).activationStatus()).isEqualTo("ACTIVE");
        assertThat(service.list(owner,"ALL","ALL","",null,null,0,10).summary().activePackages()).isEqualTo(2);
    }
    @Test void refreshCallsOnlyTheAuthorizedPaymentServiceAndTerminalStatusSurvivesExpiry(){
        UUID own=payment(false,owner,"PAID",49000,null,Instant.now());
        service.refresh(owner,"PERSONAL",own);
        verify(personal).getPaymentStatus(owner,"ORD"+own);verifyNoInteractions(organization);
        UUID other=payment(false,stranger,"PAID",49000,null,Instant.now());
        assertThatThrownBy(()->service.refresh(owner,"PERSONAL",other)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoMoreInteractions(personal);
        UUID failed=payment(true,owner,"FAILED",8000000,null,Instant.now());
        db.update("update org_payment_transactions set expires_at=? where id=?",Instant.now().minusSeconds(7200),failed);
        assertThat(service.detail(owner,"ORGANIZATION",failed).status()).isEqualTo("FAILED");
    }
    @Test void sameTimestampPagesDoNotRepeatAndSqlMetacharactersRemainLiteral(){
        Instant now=Instant.now();for(int i=0;i<3;i++)payment(false,owner,"PENDING",49000,null,now);
        var one=service.list(owner,"ALL","ALL","",null,null,0,2);
        var two=service.list(owner,"ALL","ALL","",null,null,1,2);
        assertThat(one.items()).hasSize(2);assertThat(two.items()).hasSize(1);
        assertThat(one.items().stream().map(item->item.id())).doesNotContain(two.items().get(0).id());
        assertThat(service.list(owner,"ALL","ALL","' OR 1=1 --",null,null,0,10).items()).isEmpty();
    }
}
