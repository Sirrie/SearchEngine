import java.io.IOException;
import java.util.ArrayList;

public class QryopSlWAnd extends QryopSlW {
	public Double totalWeight = Double.MIN_VALUE;
	@Override
	public QryResult evaluate(RetrievalModel r) throws IOException {
		// Initialization
		allocDaaTPtrs(r);
		QryResult result = new QryResult();
		if (this.totalWeight == Double.MIN_VALUE) {
			for (Double temp : this.weights) {
				this.totalWeight += temp;
			}
		}
		// similar to Indri WAnd
		int minDocId = getSmallestCurrentDocid();
		while (minDocId != Integer.MAX_VALUE) {
			int currentDocId = minDocId;
			ArrayList<Double> scores = getScoresForEachArg(currentDocId, (RetrievalModelIndri) r);
			double docScore = 1.0;
			for (int i = 0; i < scores.size(); i++) {
				docScore *= Math.pow(scores.get(i), this.weights.get(i)/this.totalWeight);
			}
			result.docScores.add(currentDocId, docScore);
			minDocId = getSmallestCurrentDocid();
		}
		return result;
		
	}
	
	private ArrayList<Double> getScoresForEachArg(int docid,
			RetrievalModelIndri r) throws IOException {
		ArrayList<Double> scores = new ArrayList<Double>();
		// loop every args to find all scores 
		for(int i = 0; i < this.args.size(); i ++) {
			DaaTPtr ptri = this.daatPtrs.get(i);
			if(ptri.nextDoc < ptri.scoreList.scores.size() && ptri.scoreList.getDocid(ptri.nextDoc) == docid) {
				scores.add(ptri.scoreList.getDocidScore(ptri.nextDoc));
				ptri.nextDoc++;
			} else {
				//scores.add(getDefaultScore(r, docid));
				// call the getdefaultScore for a certain arg 
				scores.add(((QryopSl)this.args.get(i)).getDefaultScore(r, docid));
			}
		}
		if (scores.size() != this.args.size()) {
			throw new RuntimeException(" can't form the wanted socres");
		}
		return scores;
	}
	/*
	   *  Calculate the default score for the specified document if it
	   *  does not match the query operator.  This score is 0 for many
	   *  retrieval models, but not all retrieval models.
	   *  @param r A retrieval model that controls how the operator behaves.
	   *  @param docid The internal id of the document that needs a default score.
	   *  @return The default score.
	   */
	  public double getDefaultScore (RetrievalModel r, long docid) throws IOException {

	    if (r instanceof RetrievalModelUnrankedBoolean)
	      return (0.0);
	    if (r instanceof RetrievalModelIndri)
			return computeDefaultScore((RetrievalModelIndri) r, docid);

	    return 0.0;
	  }
	  
	  private double computeDefaultScore(RetrievalModelIndri r, long docid) throws IOException {
			//#AND operator done on the default score of each argument
			double result = 1.0;
			if (this.totalWeight == Double.MIN_VALUE) {
				for (Double temp : this.weights) {
					this.totalWeight += temp;
				}
			}
		
			// loop throgh all args 
			for (int i = 0; i < this.args.size(); i ++) {
				//double score =  r.lambda * (0 + r.mu * prob) /((double)doclength + r.mu) + (1 - r.lambda) * prob;
				double score = ((QryopSl)args.get(i)).getDefaultScore(r, docid);
				result *= Math.pow(score, this.weights.get(i)/this.totalWeight);
			}
			return result;
		}

	@Override
	public String toString() {
		String result = new String();

		for (int i = 0; i < this.args.size(); i++)
			result += this.weights.get(i) + " " +this.args.get(i).toString() + " ";

		return ("#WAND( " + result + ")");
	}
}
