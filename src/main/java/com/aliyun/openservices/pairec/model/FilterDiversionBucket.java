package com.aliyun.openservices.pairec.model;

import org.mvel2.MVEL;

public class FilterDiversionBucket implements DiversionBucket {
    private String filter;

    public FilterDiversionBucket(String filter) {
        this.filter = filter;
    }

    @Override
    public boolean match(ExperimentContext context) {
        return MVEL.evalToBoolean(this.filter, context.getFilterParams());
    }
}
