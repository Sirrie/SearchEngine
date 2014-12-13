
public class RetrievalModelIndri extends RetrievalModel{
	public double mu;
	public double lambda;
	public DocLengthStore dls;
	
	public RetrievalModelIndri(DocLengthStore dls) {
		this.dls = dls;
	}

	public RetrievalModelIndri() {
		this.dls = null;
	}

	@Override
	public boolean setParameter(String parameterName, double value) {
		if (parameterName.endsWith("mu")) {
			this.mu = value;
		} else if (parameterName.equals("lambda")) {
			this.lambda = value;
		} else {
			System.out.println("Error:  Unknown parameter name for retrieval model " +
					"Indri: " +
					parameterName);
			return false;
		}
		return true;
	}

	@Override
	public boolean setParameter(String parameterName, String value) {
		 System.err.println ("Error: Unknown parameter name for retrieval model " +
					"Indri: " +
					parameterName);
		    return false;
	}
	@Override 
	public String toString() {
		String result = "";
		result += "Indri: ";
		result += " mu "+this.mu;
		result += " lambda " + this.lambda;
		return result;
	}
}
