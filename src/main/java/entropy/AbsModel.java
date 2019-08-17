package entropy;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by sy on 2019/8/17.
 */
abstract class AbsModel {
    /**
     * 排序,desc
     * @param scoreDocs
     * @return
     */
    public List<DocItem> rankByScore(Map<String, Double> scoreDocs) {
        Queue<DocItem> docItemQueue = new PriorityQueue<DocItem>();
        scoreDocs.forEach((id, score) -> {
            docItemQueue.add(new DocItem(id, score));
        });
        int size = docItemQueue.size();
        List<DocItem> result = new ArrayList<DocItem>(size);
        result.addAll(docItemQueue);
        Collections.sort(result);
        return result;
    }

    /**
     * 排序topN
     * @param scoreDocs
     * @param topN
     * @return
     */
    public List<DocItem> rankByScore(Map<String, Double> scoreDocs, int topN) {
        Queue<DocItem> topItems = new PriorityQueue<DocItem>(topN);
        boolean full = false;
        double lowestTopValue = Double.NEGATIVE_INFINITY;
        for(Map.Entry<String, Double> entry : scoreDocs.entrySet()) {
            String id = entry.getKey();
            double score = entry.getValue();
            if (!full || score > lowestTopValue) {
                topItems.add(new DocItem(id, score));
                if (full) {
                    topItems.poll();
                } else if (topItems.size() > topN) {
                    full = true;
                    topItems.poll();
                }
                lowestTopValue = topItems.peek().getScore();
            }
        }
        int size = topItems.size();
        List<DocItem> result = new ArrayList<DocItem>(size);
        result.addAll(topItems);
        Collections.sort(result);
        return result;
    }

    /**
     * 这里利用jieba进行分词
     * @param sent
     * @return
     */
    public List<String> segByWord(String sent) {
        List<String> words = Analyzer.jieBaSegment(sent);
        return words.stream().map(String::trim).filter(w -> !w.isEmpty()).collect(Collectors.toList());
    }

    /**
     * 按字分
     * @param sent
     * @return
     */
    public List<String> segByChar(String sent) {
        List<String> stringList = new ArrayList<>();
        for(char c : sent.toCharArray()) {
            String s = String.valueOf(c).trim();
            stringList.add(s);
        }
        return stringList;
    }
}