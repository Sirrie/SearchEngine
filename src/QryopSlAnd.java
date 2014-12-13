/**
 *  This class implements the AND operator for all retrieval models.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlAnd extends QryopSl {
	

  /**
   *  It is convenient for the constructor to accept a variable number
   *  of arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
   *  @param q A query argument (a query operator).
   */
  public QryopSlAnd(Qryop... q) {
    for (int i = 0; i < q.length; i++)
      this.args.add(q[i]);
  }

  /**
   *  Appends an argument to the list of query operator arguments.  This
   *  simplifies the design of some query parsing architectures.
   *  @param {q} q The query argument (query operator) to append.
   *  @return void
   *  @throws IOException
   */
  public void add (Qryop a) {
    this.args.add(a);
  }

  /**
   *  Evaluates the query operator, including any child operators and
   *  returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluate(RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean || r instanceof RetrievalModelRankedBoolean)
      return (evaluateBoolean (r));
    if (r instanceof RetrievalModelBM25)
    	return (evaluateBoolean(r));
    if (r instanceof RetrievalModelIndri)
    	return (evaluateIndri(r));
    return null;
  }

  public QryResult evaluateIndri(RetrievalModel r) throws IOException {
	  	//  Initialization
	    allocDaaTPtrs (r);
	    QryResult result = new QryResult ();

	    //  Sort the arguments so that the shortest lists are first.  This
	    //  improves the efficiency of exact-match AND without changing
	    //  the result.
	  	int minDocId = getSmallestCurrentDocid();
		while (minDocId != Integer.MAX_VALUE) {
			int currentDocId = minDocId;
			ArrayList<Double> scores = getScoresForEachArg(currentDocId, (RetrievalModelIndri) r);
			double docScore = 1.0;
			for(Double score : scores) {
				docScore *= Math.pow(score, 1.0/(double)this.args.size());
			}
			result.docScores.add(currentDocId, docScore);
			minDocId = getSmallestCurrentDocid();
		}
		return result;
	}
	
    // this method will return a list of scores of all args, if there is no match for the arg, set the socre to default score
	private ArrayList<Double> getScoresForEachArg(int docid, RetrievalModelIndri r) throws IOException {
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

/**
   *  Evaluates the query operator for boolean retrieval models,
   *  including any child operators and returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateBoolean (RetrievalModel r) throws IOException {

    //  Initialization

    allocDaaTPtrs (r);
    QryResult result = new QryResult ();

    //  Sort the arguments so that the shortest lists are first.  This
    //  improves the efficiency of exact-match AND without changing
    //  the result.

    for (int i=0; i<(this.daatPtrs.size()-1); i++) {
      for (int j=i+1; j<this.daatPtrs.size(); j++) {
	if (this.daatPtrs.get(i).scoreList.scores.size() >
	    this.daatPtrs.get(j).scoreList.scores.size()) {
	    ScoreList tmpScoreList = this.daatPtrs.get(i).scoreList;
	    this.daatPtrs.get(i).scoreList = this.daatPtrs.get(j).scoreList;
	    this.daatPtrs.get(j).scoreList = tmpScoreList;
	}
      }
    }

    //  Exact-match AND requires that ALL scoreLists contain a
    //  document id.  Use the first (shortest) list to control the
    //  search for matches.

    //  Named loops are a little ugly.  However, they make it easy
    //  to terminate an outer loop from within an inner loop.
    //  Otherwise it is necessary to use flags, which is also ugly.

    DaaTPtr ptr0 = this.daatPtrs.get(0);

    EVALUATEDOCUMENTS:
    for ( ; ptr0.nextDoc < ptr0.scoreList.scores.size(); ptr0.nextDoc ++) {

      int ptr0Docid = ptr0.scoreList.getDocid (ptr0.nextDoc);
      double docScore = 1.0;

      //  Do the other query arguments have the ptr0Docid?

      for (int j=1; j<this.daatPtrs.size(); j++) {

		DaaTPtr ptrj = this.daatPtrs.get(j);
	
		while (true) {
		  if (ptrj.nextDoc >= ptrj.scoreList.scores.size())
		    break EVALUATEDOCUMENTS;		// No more docs can match
		  else
		    if (ptrj.scoreList.getDocid (ptrj.nextDoc) > ptr0Docid)
		      continue EVALUATEDOCUMENTS;	// The ptr0docid can't match.
		  else
		    if (ptrj.scoreList.getDocid (ptrj.nextDoc) < ptr0Docid)
		      ptrj.nextDoc ++;			// Not yet at the right doc.
		  else
		      break;				// ptrj matches ptr0Docid
		}
      }
      // all terms  come to the same docId find the min score
      double minScore = Double.MAX_VALUE;
      for (int i = 0; i < this.daatPtrs.size(); i++ ) {
    	  DaaTPtr ptri = this.daatPtrs.get(i);
    	  if ( minScore > ptri.scoreList.getDocidScore(ptri.nextDoc)) {
    		  minScore = ptri.scoreList.getDocidScore(ptri.nextDoc);
    	  }
      }
      if (r instanceof RetrievalModelRankedBoolean) {
    	  docScore = minScore;
      }
      //  The ptr0Docid matched all query arguments, so save it.

      result.docScores.add (ptr0Docid, docScore);
    }

    freeDaaTPtrs ();

    return result;
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
		int qCount = args.size();
	
		// loop throgh all args 
		for (int i = 0; i < this.args.size(); i ++) {
			//double score =  r.lambda * (0 + r.mu * prob) /((double)doclength + r.mu) + (1 - r.lambda) * prob;
			double score = ((QryopSl)args.get(i)).getDefaultScore(r, docid);
			result *= Math.pow(score, 1.0/(double)qCount);
		}
		return result;
	}
  /*
   *  Return a string version of this query operator.  
   *  @return The string version of this query operator.
   */
  public String toString(){
    
    String result = new String ();

    for (int i=0; i<this.args.size(); i++)
      result += this.args.get(i).toString() + " ";

    return ("#AND( " + result + ")");
  }
}
