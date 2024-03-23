package com.aliyun.openservices.pairec.common;

public class Constants {
    public static final String CODE_OK = "OK";
    public static final String Environment_Daily_Desc = "daily";
    public static final String Environment_Prepub_Desc = "prepub";

    public static final String Environment_Product_Desc = "product";

    public static int ExpRoom_Status_Offline = 1;
    public static int ExpRoom_Status_Online = 2;

    public static int ExpGroup_Status_Offline = 1;
    public static int ExpGroup_Status_Online = 2;

    public static final int ExpGroup_Distribution_Type_User = 1;

    public static final int ExpGroup_Distribution_Type_TimeDuration = 2;

    public static int Experiment_Status_Offline = 1;
    public static int Experiment_Status_Online = 2;

    public static int Bucket_Type_UID = 1;
    public static int Bucket_Type_UID_HASH = 2;
    public static int Bucket_Type_Custom = 3;
    public static int Bucket_Type_Filter = 4;

    public static int ExpRoom_Type_Base = 1;
    public static int ExpRoom_Type_Normal = 2;

    public static final int Experiment_Type_Base = 1;
    public static final int Experiment_Type_Test = 2;
    public static final int Experiment_Type_Default = 3;

    public static final String CrowdTargetType_ALL = "All";

    public static final String CrowdTargetType_Filter = "Filter";

    public static final String CrowdTargetType_CrowdId = "CrowdId";

    public static final String CrowdTargetType_Random = "Random";
    public static String environmentDesc2OpenApiString(String environment) {
        if (environment.equals(Environment_Daily_Desc)) {
            return "Daily";
        } else if (environment.equals(Environment_Prepub_Desc)) {
            return "Pre";
        } else if (environment.equals(Environment_Product_Desc)) {
            return "Prod";
        }

        return "";
    }

}

