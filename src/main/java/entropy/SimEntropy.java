package entropy;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by sy on 2019/8/9.
 */

/**参考自: 1.《Content-based relevance estimation on the web using inter-document similarities》
 *        2.语言模型平滑：https://www.douban.com/note/337818244/
 *        3.https://blog.csdn.net/flying_all/article/details/77805285
 */
public class SimEntropy extends AbsModel implements IModel {

    private List<String> corpusTerms = null;
    private List<List<String>> documentList = null;
    private Map<List<String>, String> corpusHashMap = null;

    //狄里克雷平滑系数
    private double lambda = 0.0;

    public SimEntropy(){}
    public SimEntropy(SimEntropy SimEntropy){
        this.lambda = SimEntropy.lambda;
    }

    public static class Builder{
        private SimEntropy SimEntropy;
        public Builder() {
            this.SimEntropy = new SimEntropy();
        }
        public Builder lambda(double lambda) {
            if(lambda < 0)
                throw new IllegalArgumentException("lambda can't negative : " + lambda);
            SimEntropy.lambda = lambda;
            return this;
        }
        public SimEntropy build() {
            return new SimEntropy(SimEntropy);
        }
    }

    @Override
    public Map<String, Double> ratingDocs(String query, Map<String, String> docs) {
        List<String> quertTermList = segByWord(query);
        Map<String, Double> simQueryDocsMap = this.simQueryDocs(quertTermList, docs); //cross entropy.SimEntropy similarity
        Map<String, Double> SimEntropy = this.docsSimEntropy(docs); //doc entropy.SimEntropy
        Map<String, Double> simDocs = this.docSim(quertTermList); //docs similarity
        Map<String, Double> queryDocsSim = new HashMap<>();
        SimEntropy.forEach(
                (k, v) -> {
                    double value = simQueryDocsMap.get(k) * Math.exp(v) * simDocs.get(k);
                    queryDocsSim.put(k, value);
                }
        );
        this.clear();
        return queryDocsSim;
    }

    /**
     * 计算文档的熵
     * @param docs
     * @return
     */
    public Map<String, Double> docsSimEntropy(Map<String, String> docs) {
        Map<String, Double> SimEntropy = new HashMap<>();
        this.corpusHashMap.forEach(
                (k, v) -> {
                    //统计词数
                    Map<String, Long> wordCount = k
                            .stream()
                            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
                    //总词数
                    long totalWordsSize = wordCount
                            .values()
                            .stream()
                            .mapToLong(word -> word.longValue())
                            .sum();
                    //熵
                    double wordEntrySum = wordCount
                            .entrySet()
                            .stream()
                            .mapToDouble(word -> {
                                double pWord = (double) word.getValue() / totalWordsSize;
                                return -(pWord * Math.log(pWord));})
                            .reduce(0,Double::sum);
//                            .reduce(0, (wSimEntropy1, wSimEntropy2) -> wSimEntropy1 + wSimEntropy2);
//                            .reduce(Double::sum)
                    SimEntropy.put(v, wordEntrySum);
                }
        );

        /*docs.forEach(
                (k, v) -> {
                    List<String> segmentWords = segByWord(v);
                    //统计词数
                    Map<String, Long> wordCount = segmentWords
                            .stream()
                            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
                    //总词数
                    long totalWordsSize = wordCount
                            .values()
                            .stream()
                            .mapToLong(word -> word.longValue())
                            .sum();
                    //熵
                    double wordEntrySum = wordCount
                            .entrySet()
                            .stream()
                            .mapToDouble(word -> {
                                double pWord = (double) word.getValue() / totalWordsSize;
                                return -(pWord * Math.log(pWord));})
                            .reduce(0,Double::sum);
//                            .reduce(0, (wSimEntropy1, wSimEntropy2) -> wSimEntropy1 + wSimEntropy2);
//                            .reduce(Double::sum)
                    entropy.SimEntropy.put(k, wordEntrySum);
                }
        );*/

        return SimEntropy;
    }

    /**
     * 方法:
     *     1. dirichlet-smoothed : 平滑的语言模型表示文档
     *     2. cross-entropy.SimEntropy : 相关度
     * @param quertTermList
     * @param docs
     * @return
     */
    public Map<String, Double> simQueryDocs(List<String> quertTermList, Map<String, String> docs) {
        documentList = new ArrayList<>();
        corpusTerms = new ArrayList<>();
        corpusHashMap = new HashMap<>();

        docs.forEach((id, doc) -> {
            List<String> segs = segByWord(doc);
            documentList.add(segs);
            corpusTerms.addAll(segs);
            corpusHashMap.put(segs, id);
        });
        //获取查询与文档的相似度
        Map<String, Double> scoreDocs  = queryDocScore(quertTermList);
        return scoreDocs;
    }

    /**
     * 结合相对熵和狄里克雷平滑语言方法计算相关度
     * @param queryTerms
     * @return
     */
    private Map<String, Double> queryDocScore(List<String> queryTerms) {
        //统计查询中的词频
        Map<String, Long> queryTermsCount = queryTerms
                .stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        //查询中的总词频
        long queryTermsSize = queryTermsCount
                .values()
                .stream()
                .mapToLong(word -> word)
                .sum();

        //文档集中的词频
        Map<String, Long> collectionTermsCount = corpusTerms
                .stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        //文档集中的总词频
        long collectionTermsSize = collectionTermsCount
                .values()
                .stream()
                .mapToLong(word -> word)
                .sum();

        Map<String, Double> scoredDocument = new HashMap<>();
        documentList.forEach(docTerms -> {
            //文档中的词频
            Map<String, Long> docTermsCount = docTerms
                    .stream()
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            //文档中的总词频
            long docTermsSize = docTermsCount
                    .values()
                    .stream()
                    .mapToLong(word -> word)
                    .sum();

            //计算交叉熵(或者相对熵)
            OptionalDouble score = queryTerms
                    .stream()
                    .mapToDouble(queryTerm -> {
                        //queryTerm的似然
                        double queryCE = (double)queryTermsCount.get(queryTerm) / queryTermsSize;
                        //经过Dirichlet smooth的term weight
                        double docCE = (1.0 + docTermsCount.getOrDefault(queryTerm, 0L) +
                                this.lambda * (collectionTermsCount.getOrDefault(queryTerm, 0L) / collectionTermsSize)) /
                                (docTermsSize + this.lambda);
                        return queryCE * Math.log(1 / docCE);//交叉熵
                        //return queryCE * Math.log(queryCE / docCE);//相对熵
                    })
                    .reduce(Double::sum);
            String docID = corpusHashMap.get(docTerms);
            scoredDocument.put(docID, Math.exp(-score.getAsDouble()));
        });
        return scoredDocument;
    }

    /**
     *　计算docs间的relation(去除query中的词)
     * @param quertTermList
     * @return
     */
    public Map<String, Double> docSim(List<String> quertTermList) {

        Map<String, Double> docSimilarity = new HashMap<>(); //文档间的相关度

        //以下是过滤query中的所有词
        List<String> allTerms = corpusTerms
                .stream()
                .filter(term -> !quertTermList.contains(term))
                .collect(Collectors.toList());

        List<List<String>> docList = documentList
                .stream()
                .map(termList -> {
                    List<String> list = termList
                            .stream()
                            .filter(term -> !quertTermList.contains(term))
                            .collect(Collectors.toList());
                    return list;
                })
                .collect(Collectors.toList());

        Map<List<String>, String> docsHashMap = new HashMap<>();

        corpusHashMap.forEach(
                (k, v) ->{
                    List<String> list = k.stream().filter(term -> !quertTermList.contains(term))
                            .collect(Collectors.toList());
                    docsHashMap.put(list, v);
                }
        );

        //文档集中的词频
        Map<String, Long> collectionTermsCount = allTerms
                .stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        //文档集中的总词频
        long collectionTermsSize = collectionTermsCount
                .values()
                .stream()
                .mapToLong(word -> word)
                .sum();

        docList.forEach(docTerms -> {
            if (docTerms.size() == 0) {
                String docID = docsHashMap.get(docTerms);
                docSimilarity.put(docID, 1.0);
            } else {

                //文档中的词频
                Map<String, Long> docTermsCount = docTerms
                        .stream()
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
                //文档中的总词频
                long docTermsSize = docTermsCount
                        .values()
                        .stream()
                        .mapToLong(word -> word)
                        .sum();

                //计算文档的相关度
                OptionalDouble scoreSum = docsHashMap.entrySet().stream()
                        .filter(doc -> !doc.getKey().equals(docTerms))
                        .mapToDouble(map -> {
                            //统计查询中的词频
                            Map<String, Long> queryTermsCount = map.getKey()
                                    .stream()
                                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
                            //查询中的总词频
                            long queryTermsSize = queryTermsCount
                                    .values()
                                    .stream()
                                    .mapToLong(word -> word)
                                    .sum();
                            OptionalDouble score = docTerms.stream()
                                    .mapToDouble(docTerm -> {
                                        //queryTerm的似然
                                        double queryCE = (double) docTermsCount.get(docTerm) / docTermsSize;
                                        //经过Dirichlet smooth的term weight
                                        double docCE = (1.0 + queryTermsCount.getOrDefault(docTerm, 0L) +
                                                this.lambda * (collectionTermsCount.getOrDefault(docTerm, 0L) / collectionTermsSize)) /
                                                (queryTermsSize + this.lambda);
                                        return queryCE * Math.log(1 / docCE);//交叉熵
                                        //return queryCE * Math.log(queryCE / docCE);//相对熵
                                    }).reduce(Double::sum);
                            return Math.exp(-score.getAsDouble());

                        }).reduce(Double::sum);

                String docID = docsHashMap.get(docTerms);
                docSimilarity.put(docID, scoreSum.getAsDouble());
            }
        });

        return docSimilarity;
    }

    private void clear() {
        documentList.clear();
        corpusTerms.clear();
        corpusHashMap.clear();
    }

}