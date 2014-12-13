/**
 *  This class implements the SCORE operator for all retrieval models.
 *  The single argument to a score operator is a query operator that
 *  produces an inverted list.  The SCORE operator uses this
 *  information to produce a score list that contains document ids and
 *  scores.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlScore extends QryopSl {

	/**
	 * Construct a new SCORE operator. The SCORE operator accepts just one
	 * argument.
	 * 
	 * @param q
	 *            The query operator argument.
	 * @return @link{QryopSlScore}
	 */
	public QryopSlScore(Qryop q) {
		this.args.add(q);
	}

	/**
	 * Construct a new SCORE operator. Allow a SCORE operator to be created with
	 * no arguments. This simplifies the design of some query parsing
	 * architectures.
	 * 
	 * @return @link{QryopSlScore}
	 */
	public QryopSlScore() {
		this.field = "body";
		this.ctf = 0;
	}

	/**
	 * Appends an argument to the list of query operator arguments. This
	 * simplifies the design of some query parsing architectures.
	 * 
	 * @param q
	 *            The query argument to append.
	 */
	public void add(Qryop a) {
		this.args.add(a);
	}

	/**
	 * Evaluate the query operator.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluate(RetrievalModel r) throws IOException {

		if (r instanceof RetrievalModelUnrankedBoolean
				|| r instanceof RetrievalModelRankedBoolean)
			return (evaluateBoolean(r));
		if (r instanceof RetrievalModelBM25 || r instanceof RetrievalModelIndri)
			return (evaluateBestMatch(r));

		return null;
	}

	private QryResult evaluateBestMatch(RetrievalModel r) throws IOException{
		// Evaluate the query argument.

		QryResult result = args.get(0).evaluate(r);
		this.ctf = result.invertedList.ctf;
		this.field = result.invertedList.field;
		// Each pass of the loop computes a score for one document. Note:
		// If the evaluate operation above returned a score list (which is
		// very possible), this loop gets skipped.

		for (int i = 0; i < result.invertedList.df; i++) {
			// DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
			// Unranked Boolean. All matching documents get a score of 1.0.
			if (r instanceof RetrievalModelBM25) {
				computeScore((RetrievalModelBM25) r, result, i);
				
			}
			if (r instanceof RetrievalModelIndri) {
				computeScore((RetrievalModelIndri) r, result, i);
			}
		}

		// The SCORE operator should not return a populated inverted list.
		// If there is one, replace it with an empty inverted list.

		if (result.invertedList.df > 0)
			result.invertedList = new InvList();

		return result;
	}

	/**
	 * Evaluate the query operator for boolean retrieval models.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateBoolean(RetrievalModel r) throws IOException {

		// Evaluate the query argument.

		QryResult result = args.get(0).evaluate(r);

		// Each pass of the loop computes a score for one document. Note:
		// If the evaluate operation above returned a score list (which is
		// very possible), this loop gets skipped.

		for (int i = 0; i < result.invertedList.df; i++) {

			// DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
			// Unranked Boolean. All matching documents get a score of 1.0.
			if (r instanceof RetrievalModelUnrankedBoolean) {
				result.docScores.add(result.invertedList.postings.get(i).docid,
						(float) 1.0);
			} else if (r instanceof RetrievalModelRankedBoolean) {
				result.docScores.add(result.invertedList.postings.get(i).docid,
						result.invertedList.postings.get(i).tf);
			} 
		}

		// The SCORE operator should not return a populated inverted list.
		// If there is one, replace it with an empty inverted list.

		if (result.invertedList.df > 0)
			result.invertedList = new InvList();

		return result;
	}
	
	//compute score when the model is Indri
	private void computeScore(RetrievalModelIndri r, QryResult result, int index) throws IOException {
		int tf = result.invertedList.postings.get(index).tf;
		int docId = result.invertedList.postings.get(index).docid;
		double prob = (double)this.ctf/(double)QryEval.READER.getSumTotalTermFreq(this.field);
		long doclen = r.dls.getDocLength(this.field, docId);
		double score = r.lambda * (tf + r.mu * prob) /((double)doclen + r.mu) + (1 - r.lambda) * prob;
		// add to the scorelist
		result.docScores.add(docId, score);
		
	}
	
	// compute score when the model is BM25
	private void computeScore(RetrievalModelBM25 r, QryResult result, int index) throws IOException {
		int docId = result.invertedList.postings.get(index).docid;
		int tf = result.invertedList.postings.get(index).tf;
		int N = QryEval.READER.numDocs();
		int df = result.invertedList.df;
		long doclen = r.dls.getDocLength(result.invertedList.field, docId);
		double avg_doclen = QryEval.READER.getSumTotalTermFreq(result.invertedList.field)
				/ (double) QryEval.READER.getDocCount(result.invertedList.field);
		double score = Math.log(((double) N - df + 0.5)/ ((double) df + 0.5))
				* tf/ (tf + r.k_1* (1 - r.b + r.b * (((double) doclen )/ avg_doclen)));
		result.docScores.add(docId, score);
	}

	/*
	 * Calculate the default score for a document that does not match the query
	 * argument. This score is 0 for many retrieval models, but not all
	 * retrieval models.
	 * 
	 * @param r A retrieval model that controls how the operator behaves.
	 * 
	 * @param docid The internal id of the document that needs a default score.
	 * 
	 * @return The default score.
	 */
	public double getDefaultScore(RetrievalModel r, long docid)
			throws IOException {

		if (r instanceof RetrievalModelUnrankedBoolean || r instanceof RetrievalModelRankedBoolean)
			return (0.0);
		if (r instanceof RetrievalModelIndri)
			return computeDefaultScore((RetrievalModelIndri) r, docid);
		return 0.0;
	}

	private double computeDefaultScore(RetrievalModelIndri r, long docid) throws IOException {
		double result = 1.0;
		long docId = docid;
		double prob = (double)this.ctf/(double)QryEval.READER.getSumTotalTermFreq(this.field);
		long doclen = r.dls.getDocLength(this.field, (int)docId);
		double score = r.lambda * (0 + r.mu * prob) /((double)doclen + r.mu) + (1 - r.lambda) * prob;
		return score;
	}

	/**
	 * Return a string version of this query operator.
	 * 
	 * @return The string version of this query operator.
	 */
	public String toString() {

		String result = new String();

		for (Iterator<Qryop> i = this.args.iterator(); i.hasNext();)
			result += (i.next().toString() + " ");

		return ("#SCORE( " + result + ")");
	}
}
