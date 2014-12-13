
public class RetrievalModelBM25 extends RetrievalModel {
	public double k_1;
	public double b;
	public double k_3;
	public DocLengthStore dls;
	
	public RetrievalModelBM25 (DocLengthStore dls) {
		this.dls = dls;
	}
	
	public RetrievalModelBM25() {
		this.dls = null;
	}
	
	@Override
	public boolean setParameter(String parameterName, double value) {
		if (parameterName.equals("k_1")) {
			k_1 = value;
		} else if (parameterName.equals("b")) {
			b = value;
		} else if (parameterName.equals("k_3")) {
			k_3 = value;
		} else {
			System.out.println("Error:Error: Unknown parameter name for retrieval model " +
					"BM25: " +
					parameterName);
		    return false;
		}
		return true;
	}

	@Override
	public boolean setParameter(String parameterName, String value) {
		 System.err.println ("Error: Unknown parameter name for retrieval model " +
					"BM25: " +
					parameterName);
		    return false;
	}
	// print the element value of the model
	@Override
	public String toString () {
		String result = "";
		result += "BM25 : ";
		result += " k_1 : " +String.valueOf(k_1);
		result += "b : " + String.valueOf(b);
		result += "k_3 : " +String.valueOf(k_3);
		return result;
		
	}

}
