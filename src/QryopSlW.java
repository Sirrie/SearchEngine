import java.io.IOException;
import java.util.ArrayList;


public abstract class QryopSlW extends QryopSl{
	public ArrayList<Double> weights;
	
	public QryopSlW(Qryop... q) {
		for (int i = 0; i < q.length ; i++) {
			this.args.add(q[i]);
		}
		this.weights = new ArrayList<Double>();
	}
	
	public void add(Qryop a) {
		this.args.add(a);
	}
	
	public void addWeight(Double w) {
		this.weights.add(w);
	}
	
	public void deleteWeight() throws Exception {
		try {
			this.weights.remove(weights.size() - 1);
		} catch (Exception e) {
			throw new Exception("no weights");
		}
	}
	
	public double getDefaultScore(RetrievalModel r, long docid)
			throws IOException {
		if (r instanceof RetrievalModelUnrankedBoolean || r instanceof RetrievalModelRankedBoolean) {
			return (0.0);
		}
		return 0.0;
	}

}
