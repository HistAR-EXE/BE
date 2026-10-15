package com.histar.be.billing.dto;

public record PlanRevenuePoint(String plan, String description, long revenueVnd) {
    public PlanRevenuePoint(String plan, long revenueVnd) {
        this(plan, descriptionFor(plan), revenueVnd);
    }

    public static String descriptionFor(String plan) {
        if (plan == null) {
            return "Phân khúc hợp đồng";
        }
        String key = plan.toLowerCase();
        if (key.contains("cá nhân") || key.contains("b2c")) {
            return "Quest vé lẻ & Plus";
        }
        if (key.contains("trường") || key.contains("edu") || key.contains("k12")) {
            return "Bản quyền ngoại khóa K12";
        }
        if (key.contains("bảo tàng") || key.contains("tổ chức") || key.contains("museum")) {
            return "Hợp tác địa phương";
        }
        return "Phân khúc hợp đồng";
    }
}
