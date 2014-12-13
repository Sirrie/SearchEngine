import java.util.Stack;

public class QryUtil {

	public static boolean isNumeric(String s) {

		return s.matches("-?\\d+(\\.\\d+)?");
	}

	public static boolean needWeight(Qryop currentOp) {
		if (currentOp instanceof QryopSlW) {
			if (((QryopSlW) currentOp).weights.size() == currentOp.args.size()) {
				return true;
			}
		}
		return false;
	}

}
