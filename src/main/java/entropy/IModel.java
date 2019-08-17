package entropy;

import java.util.Map;

/**
 * Created by sy on 2019/8/17.
 */
interface IModel {
    Map<String, Double> ratingDocs(String query, Map<String, String> docs);
}
