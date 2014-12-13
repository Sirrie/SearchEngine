import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeatureVector {
	public String docName;
	public String queryId;
	public String query;
	private double pageRank;
	private int pairScore;
	public List<Double> featureList;
	private double minValue;
	private double maxValue;

	public FeatureVector(String qid, String query, String docName,
			Map<String, Double> pgmap, Integer pairScore) {
		this.queryId = qid;
		this.docName = docName;
		this.query = query;
		this.pairScore = pairScore;
		this.pageRank = pgmap.get(docName) == null ? -1 : pgmap.get(docName);
		this.featureList = new ArrayList<Double>();
	}

	// -1 for missing value, not to add feature score in the disabled feature

	public void computeFeatures(String[] terms, RetrievalModelIndri indriModel,
			RetrievalModelBM25 BM25model, List<String> disable) {
		int index = 1;
		final Object o = new FeatureCompute();
		for (Method method : FeatureCompute.class.getMethods()) {
			if (method.getName().endsWith("_" + String.valueOf(index))) {
				// System.out.println("we compute feature " + index);
				if (disable != null && !disable.contains(String.valueOf(index))) {
					if (index == 4) {
						this.featureList.add(this.pageRank);
						index++;
						continue;
					}
					/*
					 * if (index == 18 || index == 19) {
					 * System.out.println("hello" + index); }
					 */
					try {
						// System.out.println("evoke");
						method.invoke(o, this.featureList, indriModel,
								BM25model, docName, query);
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if (disable.contains(String.valueOf(index))) {
					this.featureList.add(0.0);
				}
				index++;
			}
		}
		// System.out.println("we see the featureList length " +
		// featureList.size());
	}

	public void normalization(int index, double min, double max) {
		if (min == max) {
			featureList.set(index, 0.0);
		} else {
			double s = featureList.get(index);
			if (s == -1.0 || s == -2.0) {
				featureList.set(index, 0.0);
			} else {
				/*
				 * if (this.docName.equals("clueweb09-en0000-01-21462")) {
				 * System.out.println("score "+ index + " , " + (s - min) / (max
				 * - min)); }
				 */

				featureList.set(index, (s - min) / (max - min));
			}
		}
	}

	public void writeToFile(BufferedWriter bw, List<String> disable)
			throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append(pairScore);
		sb.append(" qid:" + queryId + " ");
		for (int i = 0; i < this.featureList.size(); i++) {
			if (disable.contains(String.valueOf(i + 1))
					|| featureList.get(i) == -2) {
				continue;
			}
			sb.append((i + 1) + ":" + featureList.get(i) + " ");
		}
		sb.append("# " + docName);
		if (queryId.equals("10")) {
			// System.out.println("the feature: " +sb.toString());
		}
		bw.write(sb.toString() + "\n");
	}
}
