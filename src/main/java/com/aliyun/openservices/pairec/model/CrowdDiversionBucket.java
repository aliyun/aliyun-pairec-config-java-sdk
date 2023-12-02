package com.aliyun.openservices.pairec.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrowdDiversionBucket implements DiversionBucket {
    private Map<String, Boolean> crowdUserMap = new HashMap<>();

    public CrowdDiversionBucket(List<String> crowdUers) {
        for (String uid: crowdUers) {
            this.crowdUserMap.put(uid, true);
        }

    }

    @Override
    public boolean match(ExperimentContext context) {
        return this.crowdUserMap.containsKey(context.getUid());
    }
}
