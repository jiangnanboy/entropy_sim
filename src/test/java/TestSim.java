import java.util.HashMap;
import java.util.Map;

import entropy.SimEntropy;
import junit.runner.BaseTestRunner;
import org.junit.Test;
import org.junit.Before;

/**
 * Created by sy on 2019/8/17.
 */
public class TestSim{
    @Before
    public void testBefore() {
        System.out.println("before!");
    }

    @Test
    public void test() {
        String query = "苹果";
        Map<String, String> docs = new HashMap<>();
        docs.put("doc1", "Apple iPhone XR 128GB 黑色 移动联通电信4G全网通手机 双卡双待");
        docs.put("doc2", "Apple iPhone 8 64GB 深空灰 移动联通电信4G全网通手机");
        docs.put("doc3", "苹果(Apple) iPhone 6s Plus 128GB 玫瑰金 A1699移动4G联通4G电信4G全网通");
        docs.put("doc4", "全新激活无锁Apple/苹果 iPhone XR 64GB 黑色 美版全新激活 移动联通4G");
        docs.put("doc5", "全新正品Apple/苹果 iPhone XS Max 64GB 白色 美版【有锁】 移动联通4G");
        docs.put("doc6", "全新正品Apple/苹果 iPhone 6S 全新港版未激活【裸机】 4.7寸移动联通4G");
        SimEntropy simEntropy = new SimEntropy.Builder().lambda(150).build();
        Map<String, Double> docSim = simEntropy.ratingDocs(query, docs);
        docSim.entrySet().stream().forEach(entry -> {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        });
    }
}
