package com.aliyun.openservices.pairec;

import com.aliyun.openservices.pairec.api.Configuration;
import com.aliyun.openservices.pairec.common.Constants;
import com.aliyun.openservices.pairec.model.ExperimentContext;
import com.aliyun.openservices.pairec.model.ExperimentResult;
import com.aliyun.openservices.pairec.api.ApiClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

@Ignore
public class ExperimentClientTest {
    ExperimentClient experimentClient;

    @Before
    public void setUpExperimentClient() throws Exception {
        String regionId = "cn-hangzhou";
        String instanceId = System.getenv("INSTANCE_ID");
        String accessId = System.getenv("ACCESS_ID");
        String accessKey = System.getenv("ACCESS_KEY");
        Configuration configuration = new Configuration(regionId, accessId, accessKey, instanceId);
        configuration.setEnvironment(Constants.Environment_Product_Desc);
        ApiClient apiClient = new ApiClient(configuration);


        experimentClient = new ExperimentClient(apiClient);
    }

    @Test
    public void loadExperimentDataTest() throws Exception {
        experimentClient.loadExperimentData();
        System.out.printf(String.valueOf(experimentClient.getSceneMap().size()));
    }

    @Test
    public void initTest() throws Exception {
        experimentClient.init();
    }

    @Test
    public void matchTest() throws Exception {
        experimentClient.init();
        ExperimentContext experimentContext = new ExperimentContext();
        experimentContext.setUid("1034416392");
        //experimentContext.setUid("");
        experimentContext.setRequestId("pvid");
        Map<String, Object> filters = new HashMap<>();
        filters.put("country", "new12");
        experimentContext.setFilterParams(filters);

        ExperimentResult experimentResult = experimentClient.matchExperiment("home_feed", experimentContext);

        System.out.println(experimentResult.getExpId());
        System.out.println(experimentResult.info());
        System.out.println(experimentResult.getLayerParams("recall").getString("rank_version", "not exist"));
        System.out.println(experimentResult.getLayerParams("rank").getString("version", "not exist"));

        System.out.println(experimentResult.getExperimentParams().getString("rank_version", "not exist"));
        System.out.println(experimentResult.getExperimentParams().getString("version", "not exist"));
        System.out.println(experimentResult.getExperimentParams().getString("recall", "not exist"));
        System.out.println(experimentResult.getExperimentParams().getDouble("recall_d", 0.0));
        System.out.println(experimentResult.getExperimentParams().getString("crowd", "not exist"));

    }


    @Test
    public void sceneParamTest() throws Exception {
        experimentClient.init();

        String param = experimentClient.getSceneParams("home_feed").getString("foo", "not exist");

        double dparam = experimentClient.getSceneParams("homepage").getDouble("foo_d", 0);

        float fparam = experimentClient.getSceneParams("home_feed").getFloat("foo_d", 0);

        System.out.println(param + ":" + dparam + ":" + fparam);
    }

}
