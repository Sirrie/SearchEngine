
public class RetrievalModelRankedBoolean extends RetrievalModel {

	@Override
	public boolean setParameter(String parameterName, double value) {
		 System.err.println ("Error: Unknown parameter name for retrieval model " +
					"RankedBoolean: " +
					parameterName);
		    return false;
	}

	@Override
	public boolean setParameter(String parameterName, String value) {
		 System.err.println ("Error: Unknown parameter name for retrieval model " +
					"RankedBoolean: " +
					parameterName);
		    return false;
	}

}
