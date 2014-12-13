/**
 * This class implements the NEAR/n operator for all retrieval models
 */
import java.io.*;
import java.util.*;

public class QryopIlNEARn extends QryopIl {
	public int distance;

	/**
	 * It is convenient for the constructor to accept a variable number of
	 * arguments. Thus new QryopIlSyn (arg1, arg2, arg3, ...).
	 */
	public QryopIlNEARn(Qryop... q) {
		this.distance = 1;
		for (int i = 0; i < q.length; i++)
			this.args.add(q[i]);
	}

	public QryopIlNEARn(int distance, Qryop... q) {
		this.distance = distance;
		for (int i = 0; i < q.length; i++) {
			this.args.add(q[i]);
		}
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

		// Initialization
		// this will check whether it is an inverted List, if is change to score List 
		allocDaaTPtrs(r);

		syntaxCheckArgResults(this.daatPtrs);

		QryResult result = new QryResult();
		result.invertedList.field = new String (this.daatPtrs.get(0).invList.field);
		// Each pass of the loop search 1 document to the ptri, and

		DaaTPtr ptr0 = this.daatPtrs.get(0);

		EVALUATEDOCUMENTS: for (; ptr0.nextDoc < ptr0.invList.postings.size(); ptr0.nextDoc++) {

			int ptr0Docid = ptr0.invList.getDocid(ptr0.nextDoc);

			// Do the other query arguments have the ptr0Docid?
			// process the document with the same Docid if match, add to the
			// positions
			List<Integer> positions = new ArrayList<Integer>();
			for (int j = 1; j < this.daatPtrs.size(); j++) {
				DaaTPtr ptrj = this.daatPtrs.get(j);
				
				
				while (true) {
					if (ptrj.nextDoc >= ptrj.invList.postings.size())
						break EVALUATEDOCUMENTS; // No more docs can match
					else if (ptrj.invList.getDocid(ptrj.nextDoc) > ptr0Docid)
						continue EVALUATEDOCUMENTS; // The ptr0docid can't
													// match.
					else if (ptrj.invList.getDocid(ptrj.nextDoc) < ptr0Docid)
						ptrj.nextDoc++; // Not yet at the right doc.
					else {
						// we got all terms at the same docID
						// loop every term to genreate the right positions 
						break; // ptrj come to the same ptr0Docid
					}
				}
			}
			
			for (int startLocIndex = 0; startLocIndex < ptr0.invList.postings
					.get(ptr0.nextDoc).positions.size(); startLocIndex++) {
				// loop through all other terms to search for match
				
				int tempLocation;
				if (this.daatPtrs.size() == 1) {
					tempLocation = ptr0.invList.postings.get(ptr0.nextDoc).positions.get(startLocIndex);
				} else {
				tempLocation = checkMatch(1,
						ptr0.invList.postings.get(ptr0.nextDoc).positions
								.get(startLocIndex));
				}
				if (tempLocation > -1) {
					if (positions.size()>0 && positions.get(positions.size()-1) == tempLocation) continue;
					positions.add(tempLocation);
				}
			}
			
			Collections.sort(positions);
			if (positions.size() > 0) {
				// add position list to the positions
				result.invertedList.appendPosting(ptr0Docid, positions);
			}
		}
		
		freeDaaTPtrs();
		return result;
	}
	// use recursive method to check  whether there is a math in this term compared to the given location 
	private int checkMatch(int termIndex, int baseLocation) {
		DaaTPtr ptr = this.daatPtrs.get(termIndex);
		int currentLocIndex = ptr.invList.postings.get(ptr.nextDoc).currrentIndex;
		int currentLocation = 0;
		termIndex ++;
		while(true) {
			// when go to the end of this term, so there is no match for it
			if (ptr.invList.postings.get(ptr.nextDoc).currrentIndex >= ptr.invList.postings.get(ptr.nextDoc).positions.size()) {
				return -1;
			}
			currentLocIndex = ptr.invList.postings.get(ptr.nextDoc).currrentIndex;
			currentLocation = ptr.invList.postings.get(ptr.nextDoc).positions.get(currentLocIndex);
			// currentLocation is larger than base, the base term should move to next
			if (currentLocation - baseLocation > this.distance) {
				return -1;
			}
			// we find the possible match
			if (currentLocation - baseLocation > 0 && currentLocation - baseLocation <= this.distance) {
				// should return and move to the next
				ptr.invList.postings.get(ptr.nextDoc).currrentIndex++;
				if (termIndex == this.daatPtrs.size()) {
					return currentLocation;
				} else {
					int temp = checkMatch(termIndex, currentLocation);
					if (temp != -1){
						return temp;
					} 
				}
			} else {
				ptr.invList.postings.get(ptr.nextDoc).currrentIndex++;
			}
			
		}
	}

	/**
	 * Return the smallest unexamined docid from the DaaTPtrs.
	 * 
	 * @return The smallest internal document id.
	 */
	/*public int getSmallestCurrentDocid() {

		int nextDocid = Integer.MAX_VALUE;

		for (int i = 0; i < this.daatPtrs.size(); i++) {
			DaaTPtr ptri = this.daatPtrs.get(i);
			if (nextDocid > ptri.invList.getDocid(ptri.nextDoc))
				nextDocid = ptri.invList.getDocid(ptri.nextDoc);
		}

		return (nextDocid);
	}*/

	/**
	 * syntaxCheckArgResults does syntax checking that can only be done after
	 * query arguments are evaluated.
	 * 
	 * @param ptrs
	 *            A list of DaaTPtrs for this query operator.
	 * @return True if the syntax is valid, false otherwise.
	 */
	public Boolean syntaxCheckArgResults(List<DaaTPtr> ptrs) {

		for (int i = 0; i < this.args.size(); i++) {
			if (!(this.args.get(i) instanceof QryopIl))
				QryEval.fatalError("Error:  Invalid argument in "
						+ this.toString());
			else if ((i > 0)
					&& (!ptrs.get(i).invList.field
							.equals(ptrs.get(0).invList.field)))
				QryEval.fatalError("Error:  Arguments must be in the same field:  "
						+ this.toString());
		}

		return true;
	}

	/*
	 * Return a string version of this query operator.
	 * 
	 * @return The string version of this query operator.
	 */
	public String toString() {

		String result = new String();

		for (Iterator<Qryop> i = this.args.iterator(); i.hasNext();)
			result += (i.next().toString() + " ");

		return ("#NEAR( " + result + ")");
	}
}
