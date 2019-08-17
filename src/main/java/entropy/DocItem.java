package entropy;

/**
 * Created by sy on 2019/8/17.
 */
public class DocItem implements Comparable<DocItem>{

    private String id;

    private double score;

    public DocItem(String id, double score) {
        this.id = id;
        this.score = score;
    }

    public String getID(){
        return id;
    }

    public double getScore(){
        return score;
    }

    @Override
    public int compareTo(DocItem o) {
        double diff = this.getScore() - o.getScore();
        if(diff > 0)
            return -1;
        else if (diff < 0)
            return 1;
        return 0;
    }
}
