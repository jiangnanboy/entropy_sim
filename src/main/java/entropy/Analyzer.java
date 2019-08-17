package entropy;

import com.huaban.analysis.jieba.JiebaSegmenter;

import java.util.List;

/**
 * Created by sy on 2019/8/17.
 */
public class Analyzer {
    static JiebaSegmenter jiebaSeg = null;
    static {
        jiebaSeg = new JiebaSegmenter();
    }
    public static List<String> jieBaSegment(String text) {
        return  jiebaSeg.sentenceProcess(text);
    }
}
