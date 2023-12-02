package com.aliyun.openservices.pairec.model;

import com.aliyun.openservices.pairec.util.FNV;
import org.apache.commons.codec.digest.DigestUtils;

import java.math.BigInteger;

public class UidHashDiversionBucket extends  UidDiversionBucket{

    public UidHashDiversionBucket(int bucketCount, String bucketStr) {
        super(bucketCount, bucketStr);
    }

    @Override
    public boolean match(ExperimentContext context) {
        byte[] md5Hex = DigestUtils.md5(context.getUid());
        BigInteger mod =  ((new FNV()).fnv1_64(md5Hex).mod(BigInteger.valueOf(this.bucketCount)));

        if (this.buckets.containsKey(mod.intValue())) {
            return true;
        }

        return false;
    }
}
