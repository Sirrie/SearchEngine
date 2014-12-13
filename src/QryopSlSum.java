import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class QryopSlSum extends QryopSl {

	public QryopSlSum(Qryop... q) {
		for (int i = 0; i < q.length; i++) {
			this.args.add(q[i]);
		}
	}
	
	public void add(Qryop a) {
		this.args.add(a);
	}

	@Override
	public double getDefaultScore(RetrievalModel r, long docid)
			throws IOException {
		if (r instanceof RetrievalModelUnrankedBoolean || r instanceof RetrievalModelRankedBoolean) {
			return (0.0);
		}
		return 0.0;
	}

	@Override
	public QryResult evaluate(RetrievalModel r) throws IOException {
		// Initialization
		allocDaaTPtrs(r);
		QryResult result = new QryResult();
		// from the smallest docId add all the score together for the same docId
		DocScore minDoc = getSmallestCurrentDocidScore();
		while (minDoc != null) {
			int minDocId = minDoc.docId;
			double score = 0.0;
			if (r instanceof RetrievalModelBM25) {
			//	double parameter =(((RetrievalModelBM25) r).k_3 +1 )/(((RetrievalModelBM25) r).k_3+1);
				// parameter =1 so do nothing
				score = minDoc.score ;
			}
			// if this minDocId is the same as previous one and we have
			// we sum them up
			if (result.docScores.scores.size() > 0 && minDocId == result.docScores.getDocid(result.docScores.scores.size() - 1)) {
				double tempScore = result.docScores.getDocidScore(result.docScores.scores.size() - 1);
				score = score + tempScore;
				result.docScores.scores.remove(result.docScores.scores.size() - 1);
				result.docScores.add(minDocId, score);
			} else {
				result.docScores.add(minDocId, score);
			}
			minDoc = getSmallestCurrentDocidScore();
		}
		return result;
	}
	
	@Override
	public String toString() {
		String result = new String();

		for (int i = 0; i < this.args.size(); i++)
			result += this.args.get(i).toString() + " ";

		return ("#SUM( " + result + ")");
	}
}
