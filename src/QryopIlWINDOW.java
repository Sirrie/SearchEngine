/**
 * This class implements WINDOW operator
 * @author sirrie
 *
 */

import java.io.*;
import java.util.*;

public class QryopIlWINDOW extends QryopIl {
	public int width;

	public QryopIlWINDOW(Qryop... q) {
		this.width = 1;
		for (int i = 0; i < q.length; i++)
			this.args.add(q[i]);
	}
	
	public QryopIlWINDOW(int w, Qryop... q) {
		this.width = w;
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

	@Override
	public QryResult evaluate(RetrievalModel r) throws IOException {
		if (r instanceof RetrievalModelBM25 || r instanceof RetrievalModelIndri) 
			return (evaluateBestMatch(r));
		return null;
	}

	private QryResult evaluateBestMatch(RetrievalModel r) throws IOException {
		// Initialization
		allocDaaTPtrs(r);
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
			if (ptr0Docid == 241053) {
				System.out.println("find");
			}
			// when every term come to the same docId 
			boolean oneTermEnd = false;
			while (!oneTermEnd) {
				HashMap<Integer, Integer> locTerm = new HashMap<Integer,Integer>();
				ArrayList<Integer> locationList = new ArrayList<Integer>();
				for(int i = 0; i < this.daatPtrs.size(); i++) {
					DaaTPtr ptri = this.daatPtrs.get(i);
					// if any one of the term comes to the end of the position list break
					int currentLocationIndex = ptri.invList.postings.get(ptri.nextDoc).currrentIndex;
					if (currentLocationIndex >= ptri.invList.postings.get(ptri.nextDoc).positions.size()) {
						oneTermEnd = true;
						break;
					}
					int currentLocation = ptri.invList.postings.get(ptri.nextDoc).positions.get(currentLocationIndex);
					locationList.add(currentLocation);
					locTerm.put(currentLocation, i);
				}
				if (locationList.size() < this.daatPtrs.size()) {
					break;
				}
				Collections.sort(locationList);
				int tempDistance = 1 - locationList.get(0) + locationList.get(locationList.size() - 1);
				if (tempDistance  > this.width) {	
					//update the location pointer of the min location 
					int termIndex = locTerm.get(locationList.get(0));
					DaaTPtr ptrt = this.daatPtrs.get(termIndex);
					ptrt.invList.postings.get(ptrt.nextDoc).currrentIndex++;
				} else {
					// if a match is found, update all location pointers of all term
					positions.add(locationList.get(0));
					for (int k = 0; k < this.daatPtrs.size(); k ++ ) {
						DaaTPtr ptrk = this.daatPtrs.get(k); 
						ptrk.invList.postings.get(ptrk.nextDoc).currrentIndex++;
					}
				}
			}
			
			Collections.sort(positions);
			if (positions.size() > 0) {
				result.invertedList.appendPosting(ptr0Docid, positions);
			}
			
		}	
		
		freeDaaTPtrs();
				
		return result;
	}

	@Override
	public String toString() {
		String result = new String();
		for (Iterator<Qryop> i = this.args.iterator(); i.hasNext();) {
			result += (i.next().toString() + " ");
		}
		return ("#WINDOW(" + result + ")");
	}

}
