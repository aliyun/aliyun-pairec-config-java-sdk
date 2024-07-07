package com.aliyun.openservices.pairec.model;

import org.mvel2.MVEL;

public class FilterDiversionBucket implements DiversionBucket {
    private String filter;

    public FilterDiversionBucket(String filter) {
        this.filter = filter;
    }

    @Override
    public boolean match(ExperimentContext context) {
        try {
            return MVEL.evalToBoolean(this.filter, context.getFilterParams());
        } catch (Exception e) {
            return false;
        }
    }
}
