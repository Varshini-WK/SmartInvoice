package com.smartinvoice.backend.tenant;

public class BusinessContext {

    private static final ThreadLocal<String> businessIdHolder = new ThreadLocal<>();

    public static void setBusinessId(String businessId) {
        businessIdHolder.set(businessId);
    }

    public static String getBusinessId() {
        return businessIdHolder.get();
    }

    public static void clear() {
        businessIdHolder.remove();
    }
}
