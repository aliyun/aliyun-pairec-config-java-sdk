package com.aliyun.openservices.pairec.model;

public class CustomDiversionBucket implements DiversionBucket{
    @Override
    public boolean match(ExperimentContext context) {
        return false;
    }
}
