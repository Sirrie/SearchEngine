import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.util.BytesRef;

public class FeatureCompute {
	private Map<String, Integer> docIdMap;

	public FeatureCompute() {
		docIdMap = new HashMap<String, Integer>();
	}

	// Spam score for d (read from index).
	public void Feature_1(List<Double> featureList,
			RetrievalModelIndri indriModel, RetrievalModelBM25 BM25model,
			String docName, String query) throws IOException, Exception {
		Document d;
		int docid = fastGetInternalDocid(docName);
		d = QryEval.READER.document(docid);
		int spamScore = Integer.parseInt(d.get("score"));
		featureList.add((double) spamScore);
		// System.out.println("process feature 1");
	}

	// Url depth for d(number of '/' in the rawUrl field).
	public void Feature_2(List<Double> featureList,
			RetrievalModelIndri indriModel, RetrievalModelBM25 BM25model,
			String docName, String query) throws IOException, Exception {
		Document d;
		int docid = fastGetInternalDocid(docName);
		d = QryEval.READER.document(docid);
		String rawURL = d.get("rawUrl");
		int count = 0;
		for (char c : rawURL.toCharArray()) {
			if (c == '/') {
				count++;
			}
		}
		featureList.add((double) count);

	}

	// FromWikipedia score for d (1 if the rawUrl contains "wikipedia.org",
	// otherwise 0).
	public void Feature_3(List<Double> featureList,
			RetrievalModelIndri indriModel, RetrievalModelBM25 BM25model,
			String docName, String query) throws IOException, Exception {
		Document d;
		int docid = fastGetInternalDocid(docName);
		d = QryEval.READER.document(docid);
		String rawURL = d.get("rawUrl");
		int count = (rawURL.contains("wikipedia.org")) ? 1 : 0;
		featureList.add((double) count);

	}

	// PageRank score for d (read from file).
	public void Feature_4(List<Double> featureList,
			RetrievalModelIndri indriModel, RetrievalModelBM25 BM25model,
			String docName, String query) {

		// System.out.println("proecess feature 4");
	}

	// BM25 score for <q, dbody>.
	public void Feature_5(List<Double> featureList,
			RetrievalModelIndri indriModel, RetrievalModelBM25 BM25model,
			String docName, String query) throws IOException, Exception {
		int docid = fastGetInternalDocid(docName);
		String fieldName = "body";
		computeBM25ForField(featureList, docid, query, BM25model, fieldName);

	}

	// Indri score for <q, dbody>.
	public void Feature_6(List<Double> featureList,
			RetrievalModelIndri indriModel, RetrievalModelBM25 BM25model,
			String docName, String query) throws Exception {
		int docid = QryEval.getInternalDocid(docName);
		String fieldName = "body";
		computeIndriForField(featureList, docid, query, indriModel, fieldName);
	}

	// Term overlap score for <q, dbody>.
	public void Feature_7(List<Double> featureList,
			RetrievalModelIndri indriModel, RetrievalModelBM25 BM25model,
			String docName, String query) throws Exception {
		/*
		 * if (docName.equals("clueweb09-en0000-56-19359")) {
		 * System.out.println("lala"); }
		 */
		String[] stems = QryEval.tokenizeQuery(query);
		int qstemLength = stems.length;
		int inDocLength;
		int docid = fastGetInternalDocid(docName);
		String filedName = "body";
		inDocLength = computeOverlap(stems, filedName, docid);
		double score = (inDocLength >= 0) ? (double) inDocLength
				/ (double) qstemLength : -1;
		featureList.add(score);
	}

	// BM25 score for <q, dtitle>.
	public void Feature_8(List<Double> featureList,
			RetrievalModelIndri indriModel, RetrievalModelBM25 BM25model,
			String docName, String query) throws IOException, Exception {
		int docid = fastGetInternalDocid(docName);
		String fieldName = "title";
		computeBM25ForField(featureList, docid, query, BM25model, fieldName);

	}

	// Indri score for <q, dtitle>.
	public void Feature_9(List<Double> featureList,
			RetrievalModelIndri indriModel, RetrievalModelBM25 BM25model,
			String docName, String query) throws Exception {
		int docid = QryEval.getInternalDocid(docName);
		String fieldName = "title";
		computeIndriForField(featureList, docid, query, indriModel, fieldName);
	}

	// Term overlap score for <q, dtitle>.
	public void Feature_10(List<Double> featureList,
			RetrievalModelIndri indriModel, RetrievalModelBM25 BM25model,
			String docName, String query) throws Exception {
		String[] stems = QryEval.tokenizeQuery(query);
		int qstemLength = stems.length;
		int inDocLength;
		int docid = fastGetInternalDocid(docName);
		String filedName = "title";
		inDocLength = computeOverlap(stems, filedName, docid);
		double score = (inDocLength >= 0) ? (double) inDocLength
				/ (double) qstemLength : -1;
		featureList.add(score);
	}

	// BM25 score for <q, durl>.
	public void Feature_11(List<Double> featureList,
			RetrievalModelIndri indriModel, RetrievalModelBM25 BM25model,
			String docName, String query) throws IOException, Exception {
		int docid = fastGetInternalDocid(docName);
		String fieldName = "url";
		computeBM25ForField(featureList, docid, query, BM25model, fieldName);

	}

	// Indri score for <q, durl>.
	public void Feature_12(List<Double> featureList,
			RetrievalModelIndri indriModel, RetrievalModelBM25 BM25model,
			String docName, String query) throws Exception {
		int docid = QryEval.getInternalDocid(docName);
		String fieldName = "url";
		computeIndriForField(featureList, docid, query, indriModel, fieldName);
	}

	// Term overlap score for <q, durl>.
	public void Feature_13(List<Double> featureList,
			RetrievalModelIndri indriModel, RetrievalModelBM25 BM25model,
			String docName, String query) throws Exception {
		String[] stems = QryEval.tokenizeQuery(query);
		int qstemLength = stems.length;
		int inDocLength;
		int docid = fastGetInternalDocid(docName);
		String filedName = "url";
		inDocLength = computeOverlap(stems, filedName, docid);
		double score = (inDocLength >= 0) ? (double) inDocLength
				/ (double) qstemLength : -1;
		featureList.add(score);
	}

	// BM25 score for <q, dinlink>.
	public void Feature_14(List<Double> featureList,
			RetrievalModelIndri indriModel, RetrievalModelBM25 BM25model,
			String docName, String query) throws IOException, Exception {
		int docid = fastGetInternalDocid(docName);
		String fieldName = "inlink";
		computeBM25ForField(featureList, docid, query, BM25model, fieldName);

	}

	// Indri score for <q, dinlink>.
	public void Feature_15(List<Double> featureList,
			RetrievalModelIndri indriModel, RetrievalModelBM25 BM25model,
			String docName, String query) throws Exception {
		int docid = QryEval.getInternalDocid(docName);
		String fieldName = "inlink";
		computeIndriForField(featureList, docid, query, indriModel, fieldName);
	}

	// Term overlap score for <q, dinlink>.
	public void Feature_16(List<Double> featureList,
			RetrievalModelIndri indriModel, RetrievalModelBM25 BM25model,
			String docName, String query) throws Exception {
		String[] stems = QryEval.tokenizeQuery(query);
		int qstemLength = stems.length;
		int inDocLength;
		int docid = fastGetInternalDocid(docName);
		String filedName = "inlink";
		inDocLength = computeOverlap(stems, filedName, docid);
		double score = (inDocLength >= 0) ? (double) inDocLength
				/ (double) qstemLength : -1;
		featureList.add(score);
	}

	// tf-idf sum of the query
	public void Feature_17(List<Double> featureList,
			RetrievalModelIndri indriModel, RetrievalModelBM25 BM25model,
			String docName, String query) throws Exception {
		String[] stems = QryEval.tokenizeQuery(query);
		double sum = 0.0;
		for (String stem : stems) {
			// System.out.println("the original " +stem);
			double tf = computeTermFreq(stem, docName);
			double idf = computeIDF(stem, docName);
			sum += tf * idf;
		}
		featureList.add(sum);
	}

	// query - doc similarity score
	public void Feature_18(List<Double> featureList,
			RetrievalModelIndri indriModel, RetrievalModelBM25 BM25model,
			String docName, String query) throws IOException {
		double score = 0.0;
		String[] stems = QryEval.tokenizeQuery(query);
		int docid;
		try {
			docid = fastGetInternalDocid(docName);
			TermVector docTermVector = new TermVector(docid, "body");
			double denominator = 1.0;
			double numerator = 0.0;
			for (String stem : stems) {
				int index = docTermVector.indexOf(stem);
				int tf = (index == -1) ? 0 : docTermVector.stemDf(index);
				if (index > -1) {
					denominator += Math.pow(Math.log(tf) + 1, 2);
					numerator += Math.log(tf) + 1;
				}
			}
			denominator = Math.sqrt(denominator);
			score = numerator / denominator / Math.sqrt(1.0 / stems.length);
			featureList.add(score);
		} catch (Exception e) {
			featureList.add(-1.0);
		}
	}

	private double computeIDF(String stem, String docName) throws Exception {
		double df = 1.0;
		String[] fieldNames = new String[] { "body", "inlink", "url", "title" };
		int docid = fastGetInternalDocid(docName);
		for (String field : fieldNames) {
			try {
				TermVector docTermVector = new TermVector(docid, field);
				if (docTermVector != null) {
					int index = docTermVector.indexOf(stem);
					double fielddf = QryEval.READER.docFreq(new Term(field,
							stem));
					df += fielddf;
				}
			} catch (Exception e) {
				df += 0;
			}
		}

		double idf = Math.log(QryEval.READER.numDocs() / df);
		return idf;
	}

	private double computeTermFreq(String stem, String docName)
			throws Exception {
		double tf = 0.0;
		String[] fieldNames = new String[] { "body", "inlink", "url", "title",
				"keywords" };
		int docid = fastGetInternalDocid(docName);
		for (String field : fieldNames) {
			try {
				TermVector docTermVector = new TermVector(docid, field);
				if (docTermVector != null) {
					int index = docTermVector.indexOf(stem);
					double fieldtf = (index == -1) ? 0.0 : docTermVector
							.stemFreq(index);
					tf += fieldtf;
				}
			} catch (Exception e) {
			}
		}
		return tf;
	}

	private int computeOverlap(String[] stems, String fieldName, int docid) {
		int result = 0;

		try {
			TermVector docTermVector = new TermVector(docid, fieldName);
			Terms terms = QryEval.READER.getTermVector(docid, fieldName);
			if (terms == null) {
				return -1;
			}
			if (docTermVector == null) {
				return -1;
			}

			List<String> stemTempList = new ArrayList<String>(
					Arrays.asList(stems));

			for (int index = 0; index < docTermVector.stemsLength()
					&& stemTempList.size() > 0; index++) {
				String stem = docTermVector.stemString(index);
				if (stemTempList.contains(stem)
						&& docTermVector.stemFreq(index) > 0) {
					stemTempList.remove(stem);
					result++;
				}
			}

		} catch (Exception e) {
			// System.out.println("can't find the TermVector");
			// e.printStackTrace();
			return -1;
		}

		return result;
	}

	private int fastGetInternalDocid(String docName) throws Exception {
		int id;
		if (docIdMap.containsKey(docName)) {
			id = docIdMap.get(docName);
		} else {
			id = QryEval.getInternalDocid(docName);
			docIdMap.put(docName, id);
		}
		return id;
	}

	private void computeIndriForField(List<Double> featureList, int docid,
			String query, RetrievalModelIndri indriModel, String fieldName) {
		TermVector docTermVector;

		double score = 1.0;
		try {
			double lengthC = (double) QryEval.READER
					.getSumTotalTermFreq(fieldName);
			docTermVector = new TermVector(docid, fieldName);
			// for particular filed nothing missed
			if (docTermVector != null) {
				String[] stems = QryEval.tokenizeQuery(query);

				List<String> stemTempList = new ArrayList<String>(
						Arrays.asList(stems));
				int tf, df;
				long doclen = indriModel.dls.getDocLength(fieldName, docid);
				int qtermLength = stems.length;
				double ctf, prob;
				for (int index = 0; index < docTermVector.stemsLength()
						&& stemTempList.size() > 0; index++) {
					String stem = docTermVector.stemString(index);
					if (stemTempList.contains(stem)) {
						stemTempList.remove(stem);
						df = docTermVector.stemDf(index);
						tf = docTermVector.stemFreq(index);
						ctf = docTermVector.totalStemFreq(index);
						prob = ctf / lengthC;
						score *= Math.pow(indriModel.lambda
								* (tf + indriModel.mu * prob)
								/ ((double) doclen + indriModel.mu)
								+ (1 - indriModel.lambda) * prob,
								1 / (double) qtermLength);
					}
				}

				double defaultScore = 1.0;
				if (stemTempList.size() == stems.length) {
					score = 0;

				} else {
					while (stemTempList.size() > 0) {
						String stem = stemTempList.get(0);
						stemTempList.remove(0);
						ctf = QryEval.READER.totalTermFreq(new Term(fieldName,
								new BytesRef(stem)));
						prob = ctf / lengthC;
						defaultScore *= Math.pow(indriModel.lambda
								* (0 + indriModel.mu * prob)
								/ ((double) doclen + indriModel.mu)
								+ (1 - indriModel.lambda) * prob,
								1 / (double) qtermLength);

					}
					score = score * defaultScore;
					// score = Math.pow(score, 1 / (double) qtermLength);
				}
				featureList.add(score);
			} else {
				// QryEval.debugOut("Doc missing field: " + docid + " " +
				// fieldName);
				System.out.println("Doc Missing field" + docid + " "
						+ fieldName);
				featureList.add(-1.0);
			}
		} catch (Exception e) {
			// System.out.println("can't create the termVector for " + query +
			// " " + fieldName);
			featureList.add(-1.0);
		}

	}

	private void computeBM25ForField(List<Double> featureList, int docid,
			String query, RetrievalModelBM25 BM25model, String fieldName) {
		TermVector docTermVector;
		double score = 0.0;
		try {
			docTermVector = new TermVector(docid, fieldName);
			// for particular filed nothing missed
			if (docTermVector != null) {
				String[] stems = QryEval.tokenizeQuery(query);
				int N = QryEval.READER.numDocs();
				int df, tf;
				long doclen = BM25model.dls.getDocLength(fieldName, docid);
				double avg_doclen = QryEval.READER
						.getSumTotalTermFreq(fieldName)
						/ (double) QryEval.READER.getDocCount(fieldName);

				List<String> stemTempList = new ArrayList<String>(
						Arrays.asList(stems));

				for (int index = 0; index < docTermVector.stemsLength()
						&& stemTempList.size() > 0; index++) {
					String stem = docTermVector.stemString(index);
					if (stemTempList.contains(stem)) {
						stemTempList.remove(stem);
						df = docTermVector.stemDf(index);
						tf = docTermVector.stemFreq(index);
						score += Math.log(((double) N - df + 0.5)
								/ ((double) df + 0.5))
								* tf
								/ (tf + BM25model.k_1
										* (1 - BM25model.b + BM25model.b
												* (((double) doclen) / avg_doclen)));
					}
				}
			}
			featureList.add(score);
		} catch (Exception e) {
			// System.out.println("can't create the termVector for " + query +
			// " " + fieldName);
			featureList.add(-1.0);
		}
	}
}
