package com.histar.be.billing.policy;
import com.histar.be.organization.entity.OrgSubscription;
import com.histar.be.common.exception.BusinessRuleException;
public final class OrgSeatCapacityPolicy {
 private OrgSeatCapacityPolicy() {}
 public static class CapacityExceeded extends BusinessRuleException {
  public CapacityExceeded(long current,int limit){super("Tổ chức đang có "+current+" thành viên. Gói được chọn tối đa "+limit+" tài khoản (bao gồm chủ tổ chức, giáo viên và học sinh). Vui lòng giảm thành viên trước khi hạ gói; không cần thanh toán lại nếu tiền đã được ghi nhận.");}
 }
 public static void requireFits(long current,OrgSubscription plan){
  int limit=OrgPlanLimits.forPlan(plan).maxVerifiedAccounts();
  if(current>limit)throw new CapacityExceeded(current,limit);
 }
 public static int requireSingleLicense(Integer count){
  int value=count==null?1:count;
  if(value!=1)throw new BusinessRuleException("Hiện mỗi đơn hàng chỉ hỗ trợ 1 giấy phép cho 1 tổ chức trong 1 năm.");
  return value;
 }
}
