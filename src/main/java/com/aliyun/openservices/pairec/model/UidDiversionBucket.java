package com.aliyun.openservices.pairec.model;

import com.aliyun.tea.utils.StringUtils;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class UidDiversionBucket implements DiversionBucket {
    protected Map<Integer, Boolean> buckets = new HashMap<>();
    protected int bucketCount;

    public UidDiversionBucket(int bucketCount, String bucketStr) {
        this.bucketCount = bucketCount;

        if (!StringUtils.isEmpty(bucketStr)) {
            String[] expBuckets = bucketStr.split(",");
            for (String bucket : expBuckets) {
                if (bucket.contains("-")) {
                    String[] bucketStrings = bucket.split("-");
                    if (bucketStrings.length == 2) {
                        try {
                            int start = Integer.valueOf(bucketStrings[0]);
                            int end = Integer.valueOf(bucketStrings[1]);
                            for (int i = start; i < end; i++) {
                                if (i >= this.bucketCount) {
                                    break;
                                }

                                buckets.put(i, true);
                            }
                        } catch (Exception e) {

                        }
                    }
                } else {
                    try {
                        Integer val = Integer.valueOf(bucket);
                        buckets.put(val, true);
                    } catch (Exception e) {

                    }
                }
            }

        }
    }

    @Override
    public boolean match(ExperimentContext context) {
        try {
            BigInteger uid = new BigInteger(context.getUid());
            BigInteger mod =  uid.mod(BigInteger.valueOf(this.bucketCount));
            if (this.buckets.containsKey(mod.intValue())) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

}
