import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class QryopSLOr extends QryopSl {

	/**
	 * It is convenient for the constructor to accept a variable number of
	 * arguments. Thus new qryopOr (arg1, arg2, arg3, ...).
	 * 
	 * @param q
	 *            A query argument (a query operator).
	 */
	public QryopSLOr(Qryop... q) {
		for (int i = 0; i < q.length; i++)
			this.args.add(q[i]);
	}

	/**
	 * Appends an argument to the list of query operator arguments. This
	 * simplifies the design of some query parsing architectures.
	 * 
	 * @param {q} q The query argument (query operator) to append.
	 * @return void
	 * @throws IOException
	 */
	public void add(Qryop a) {
		this.args.add(a);
	}

	/**
	 * Evaluates the query operator, including any child operators and returns
	 * the result.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluate(RetrievalModel r) throws IOException {
		if (r instanceof RetrievalModelUnrankedBoolean
				|| r instanceof RetrievalModelRankedBoolean || r instanceof RetrievalModelBM25)
			return (evaluateBoolean(r));
		/*if (r instanceof RetrievalModelBM25) 
			return (evaluateBestMatch(r));*/
		return null;
	}

	private QryResult evaluateBestMatch(RetrievalModel r) throws IOException {
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
			if (result.docScores.scores.size() > 0 && minDocId == result.docScores.getDocid(result.docScores.scores.size()-1)) {
				double tempScore = result.docScores.getDocidScore(result.docScores.scores.size()-1);
				// have to remove the last one otherwise there would be duplication
				result.docScores.scores.remove(result.docScores.scores.size()-1);
				score = score + tempScore;
				result.docScores.add(minDocId, score);
			} else {
				result.docScores.add(minDocId, score);
			}
			minDoc = getSmallestCurrentDocidScore();
		}
		return result;
	}

	/**
	 * Evaluates the query operator for boolean retrieval models, including any
	 * child operators and returns the result.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateBoolean(RetrievalModel r) throws IOException {

		// Initialization

		allocDaaTPtrs(r);
		QryResult result = new QryResult();
		DocScore minDoc = getSmallestCurrentDocidScore();
		while ( minDoc != null) {
			int minDocId = minDoc.docId;
			double score = 1.0;
			if (r instanceof RetrievalModelUnrankedBoolean) {
				score = 1.0;
			} else {
				score = minDoc.score;
			}
			// if this minDocId is the same as previous one and we have
			// larger score update the score
			if (result.docScores.scores.size() > 0 && minDocId == result.docScores.getDocid(result.docScores.scores.size()-1)  ){
				if (score > result.docScores.getDocidScore(result.docScores.scores.size()-1)) {
					result.docScores.scores.remove(result.docScores.scores.size()-1);
					result.docScores.add(minDocId, score);
				}
			} else {
				result.docScores.add(minDocId, score);
			}
			minDoc = getSmallestCurrentDocidScore();
		}
		return result;		
	}
	

	/*
	 * Add docId to the docIdList to build a combination of all docId list in
	 * ascending
	 */
	private void addToDocList(List<Integer> docIdList, int docid) {
		if (docIdList.size() == 0) {
			docIdList.add(docid);
		}
		int position = 0;
		while (position < docIdList.size()) {
			if (docid > docIdList.get(position)) {
				position++;
			} else if (docid == docIdList.get(position)) {
				break;
			} else {
				docIdList.add(position, docid);
			}
		}
	}

	/*
	 * Calculate the default score for the specified document if it does not
	 * match the query operator. This score is 0 for many retrieval models, but
	 * not all retrieval models.
	 * 
	 * @param r A retrieval model that controls how the operator behaves.
	 * 
	 * @param docid The internal id of the document that needs a default score.
	 * 
	 * @return The default score.
	 */
	public double getDefaultScore(RetrievalModel r, long docid)
			throws IOException {

		if (r instanceof RetrievalModelUnrankedBoolean)
			return (0.0);

		return 0.0;
	}

	/*
	 * Return a string version of this query operator.
	 * 
	 * @return The string version of this query operator.
	 */
	public String toString() {

		String result = new String();

		for (int i = 0; i < this.args.size(); i++)
			result += this.args.get(i).toString() + " ";

		return ("#OR( " + result + ")");
	}
	
}
