/**
 *  This class implements the document score list data structure
 *  and provides methods for accessing and manipulating them.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;
import java.util.*;

import org.apache.lucene.document.Document;

public class ScoreList {
	
  //  A little utilty class to create a <docid, score> object.

  protected class ScoreListEntry {
    private int docid;
    private double score;

    private ScoreListEntry(int docid, double score) {
      this.docid = docid;
      this.score = score;
    }
    @Override
	public String toString() {
    	return String.valueOf(docid) + ","+ String.valueOf(score);
    }
  }

  List<ScoreListEntry> scores = new ArrayList<ScoreListEntry>();

  /**
   *  Append a document score to a score list.
   *  @param docid An internal document id.
   *  @param score The document's score.
   *  @return void
   */
  public void add(int docid, double score) {
    scores.add(new ScoreListEntry(docid, score));
  }

  /**
   *  Get the n'th document id.
   *  @param n The index of the requested document.
   *  @return The internal document id.
   */
  public int getDocid(int n) {
    return this.scores.get(n).docid;
  }

  /**
   *  Get the score of the n'th document.
   *  @param n The index of the requested document score.
   *  @return The document's score.
   */
  public double getDocidScore(int n) {
    return this.scores.get(n).score;
  }
  
  public void sortScoreList() {
	  Collections.sort(this.scores, new scoreDocidComparator());

  }

	public class scoreDocidComparator implements Comparator<ScoreListEntry> {
		// implement the sorting requirement
		// Sort the matching documents by their scores, in descending order.
		// The external document id should be a secondary sort key (i.e., for
		// breaking ties). Smaller ids should be ranked higher (i.e. ascending
		// order).
		@Override
		public int compare(ScoreListEntry a0, ScoreListEntry a1) {
			if (a0.score - a1.score != 0) {
				if ( a0.score > a1.score) {
					return -1;
				} else {
					return 1;
				}
			} else {
				try {
					//System.out.println("wee see the doc"+QryEval.READER.document(a0.docid).get("externalId"));
					/*int result = Integer.valueOf(QryEval.READER.document(a0.docid).get("externalId"))
							- Integer.valueOf(QryEval.READER.document(a1.docid).get("externalId"));*/
					/*String[] temps = QryEval.getExternalDocid(a0.docid).split("-");
					String temp = temps[temps.length-1];
					int result1 = Integer.valueOf(temp);
					temps = QryEval.getExternalDocid(a1.docid).split("-");
					temp = temps[temps.length-1];
					int result2 = Integer.valueOf(temp);
					int result = result1 - result2;
					if (result > 0) {
						return 1;
					} else if (result < 0) {
						return -1;
					} else {
						return 0;
					}*/
					int result1 = changeToInt(QryEval.READER.document(a0.docid).get("externalId"));
					int result2 = changeToInt(QryEval.READER.document(a1.docid).get("externalId"));
					//return result1 - result2;
					// directly string comparison is too slow
					return QryEval.READER.document(a0.docid).get("externalId").compareTo(QryEval.READER.document(a1.docid).get("externalId"));
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			return 0;
		}

		private int changeToInt(String str) {
			String first = str.split("-")[1];
			String second = str.split("-")[2];
			String third = str.split("-")[3];
			int firstTemp = 0;
			if (first.charAt(2)!='0') {
				firstTemp = 1;
			}
			return 0;
		}
  }
}

