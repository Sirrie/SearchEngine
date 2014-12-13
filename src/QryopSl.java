/**
 *  All query operators that return score lists are subclasses of the
 *  QryopSl class.  This class has two main purposes.  First, it
 *  allows query operators to easily recognize any nested query
 *  operator that returns a score list (e.g., #AND (a #OR (b c)).
 *  Second, it is a place to store data structures and methods that are
 *  common to all query operators that return score lists.
 *  
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public abstract class QryopSl extends Qryop {

	/**
	 * Use the specified retrieval model to evaluate the query arguments. Define
	 * and return DaaT pointers that the query operator can use.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return void
	 * @throws IOException
	 */
	public void allocDaaTPtrs(RetrievalModel r) throws IOException {
		for (int i = 0; i < this.args.size(); i++) {

			// If this argument doesn't return ScoreLists, wrap it
			// in a #SCORE operator.

			if (!QryopSl.class.isInstance(this.args.get(i)))
				this.args.set(i, new QryopSlScore(this.args.get(i)));

			DaaTPtr ptri = new DaaTPtr();
			ptri.invList = null;
			// System.out.println("we see the args" +this.args.get(i));
			// System.out.println("we see the reture result"
			// +this.args.get(i).evaluate(r).docScores.scores.size());
			ptri.scoreList = this.args.get(i).evaluate(r).docScores;
			ptri.nextDoc = 0;

			this.daatPtrs.add(ptri);
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
	public abstract double getDefaultScore(RetrievalModel r, long docid)
			throws IOException;

	public int getSmallestCurrentDocid() {
		int nextDocid = Integer.MAX_VALUE;
		for (int i = 0; i < this.daatPtrs.size(); i++) {
			DaaTPtr ptri = this.daatPtrs.get(i);
			if (ptri.scoreList.scores.size() == 0) {
				System.out.println("we have empty scorelist " + i);
			}
			if (ptri.nextDoc < ptri.scoreList.scores.size()) {
				if (nextDocid > ptri.scoreList.getDocid(ptri.nextDoc)) {
					nextDocid = ptri.scoreList.getDocid(ptri.nextDoc);
				}
			}
		}
		return nextDocid;
	}

	/**
	 * Return the smallest unexamined docid from the DaaTPtrs.
	 * 
	 * @return The smallest internal document id.
	 */
	public DocScore getSmallestCurrentDocidScore() {

		int nextDocid = Integer.MAX_VALUE;
		int termIndex = -1;
		double score = 0.0;
		DocScore result = null;
		for (int i = 0; i < this.daatPtrs.size(); i++) {
			DaaTPtr ptri = this.daatPtrs.get(i);
			if (ptri.scoreList.scores.size() == 0) {
				System.out.println("we have empty scorelist " + i);
			}
			if (ptri.nextDoc < ptri.scoreList.scores.size()) {
				if (nextDocid > ptri.scoreList.getDocid(ptri.nextDoc)) {
					nextDocid = ptri.scoreList.getDocid(ptri.nextDoc);
					termIndex = i;
					score = ptri.scoreList.getDocidScore(ptri.nextDoc);
				}
			}
		}
		if (termIndex != -1) {
			this.daatPtrs.get(termIndex).nextDoc++;
			result = new DocScore(nextDocid, score);
		}

		return result;
	}
	
	public DocScoreTerm getSmallestCurrentDocidScoreTerm() {

		int nextDocid = Integer.MAX_VALUE;
		int termIndex = -1;
		double score = 0.0;
		DocScoreTerm result = null;
		for (int i = 0; i < this.daatPtrs.size(); i++) {
			DaaTPtr ptri = this.daatPtrs.get(i);
			if (ptri.scoreList.scores.size() == 0) {
				System.out.println("we have empty scorelist " + i);
			}
			if (ptri.nextDoc < ptri.scoreList.scores.size()) {
				if (nextDocid > ptri.scoreList.getDocid(ptri.nextDoc)) {
					nextDocid = ptri.scoreList.getDocid(ptri.nextDoc);
					termIndex = i;
					score = ptri.scoreList.getDocidScore(ptri.nextDoc);
				}
			}
		}
		if (termIndex != -1) {
			this.daatPtrs.get(termIndex).nextDoc++;
			result = new DocScoreTerm(nextDocid, score, termIndex);
		}

		return result;
	}


}

/**
 * a class which takes in docId and Score
 * 
 * @author sirrie
 * 
 */
class DocScore {
	public int docId;
	public double score;

	public DocScore(int id, double s) {
		this.docId = id;
		this.score = s;
	}
}

class DocScoreTerm {
	public int docId;
	public double score;
	public int termIndex;
	
	public DocScoreTerm(int id, double s, int index) {
		this.docId = id;
		this.score = s;
		this.termIndex = index;
	}
}
