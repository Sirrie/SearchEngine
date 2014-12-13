/**
 *  QryEval illustrates the architecture for the portion of a search
 *  engine that evaluates queries.  It is a template for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.benchmark.byTask.tasks.SetPropTask;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

public class QryEval {
	final static int amountBound = 100;
	static String usage = "Usage:  java "
			+ System.getProperty("sun.java.command") + " paramFile\n\n";
	static int totalCount = 0;
	// The index file reader is accessible via a global variable. This
	// isn't great programming style, but the alternative is for every
	// query operator to store or pass this value, which creates its
	// own headaches.

	public static IndexReader READER;

	// Create and configure an English analyzer that will be used for
	// query parsing.

	public static EnglishAnalyzerConfigurable analyzer = new EnglishAnalyzerConfigurable(
			Version.LUCENE_43);
	static {
		analyzer.setLowercase(true);
		analyzer.setStopwordRemoval(true);
		analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
	}

	/**
	 * @param args
	 *            The only argument is the path to the parameter file.
	 * @throws Throwable
	 */
	public static void main(String[] args) throws Throwable {
		// start counting time
		long startTime = System.currentTimeMillis();
		// must supply parameter file
		if (args.length < 1) {
			System.err.println(usage);
			System.exit(1);
		}
		// read in the parameter file; one parameter per line in format of
		// key=value
		Map<String, String> params = new HashMap<String, String>();
		Scanner scan = new Scanner(new File(args[0]));
		String line = null;
		do {
			line = scan.nextLine();
			String[] pair = line.split("=");
			if (pair.length == 2) {
				params.put(pair[0].trim(), pair[1].trim());
			}
		} while (scan.hasNext());
		scan.close();

		// parameters required for each field
		if (!params.containsKey("indexPath")) {
			System.err.println("Error: Parameters were missing.");
			System.exit(1);
		}

		if (!params.containsKey("queryFilePath")) {
			System.err.println("Error: Parameters were missing.");
			System.exit(1);
		}

		if (!params.containsKey("retrievalAlgorithm")) {
			System.err.println("Error: Parameters were missing.");
			System.exit(1);
		}

		if (!params.containsKey("trecEvalOutputPath")) {
			System.err.println("Error: Parameters were missing.");
			System.exit(1);
		}

		// open the index
		READER = DirectoryReader.open(FSDirectory.open(new File(params
				.get("indexPath"))));

		if (READER == null) {
			System.err.println(usage);
			System.exit(1);
		}

		try {
			System.out.println("internal ID at the begining "
					+ getInternalDocid("clueweb09-en0000-01-21462"));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		DocLengthStore s = new DocLengthStore(READER);

		// read in all test queries and store them into a hashmap
		Map<String, String> queriesInFile = new LinkedHashMap<String, String>();
		BufferedReader br = new BufferedReader(new FileReader(
				params.get("queryFilePath")));
		String readFileLine = null;
		while ((readFileLine = br.readLine()) != null) {
			String[] segmentedLine = readFileLine.split(":");
			queriesInFile.put(segmentedLine[0], segmentedLine[1]);
		}
		BufferedWriter writer = null;
		BufferedWriter queryExWriter = null;
		Boolean ifLTR = false;
		try {
			writer = new BufferedWriter(new FileWriter(new File(
					params.get("trecEvalOutputPath"))));
			if (params.get("letor:trainingQueryFile") != null) {
				ifLTR = true;
			} else if (params.get("fbExpansionQueryFile") != null) {
				queryExWriter = new BufferedWriter(new FileWriter(new File(
						params.get("fbExpansionQueryFile"))));
			}

			List<FeatureVector> relJudList = null;
			List<String> disable = null;
			Map<String, Double> docPageRank = null;
			// use SVM to train a model from training query and doc scores
			if (ifLTR) {
				String disableStr = params.get("letor:featureDisable");
				disable = new ArrayList<String>();
				if (disableStr != null) {
					disable = Arrays.asList(disableStr.substring(1,
							disableStr.length() - 1).split(","));
				}
				// store page rank index
				docPageRank = new HashMap<String, Double>();
				BufferedReader brpgrank = new BufferedReader(new FileReader(
						params.get("letor:pageRankFile")));
				readFileLine = null;
				while ((readFileLine = brpgrank.readLine()) != null) {
					// System.out.println("we see the string" + readFileLine);
					String[] segmentedLine = readFileLine.split("\t");
					docPageRank.put(segmentedLine[0],
							Double.valueOf(segmentedLine[1]));
				}
				// relJudList = new ArrayList<FeatureVector>();
				processQuery2Feature(params.get("letor:trainingQueryFile"),
						params.get("letor:trainingQrelsFile"),
						params.get("letor:trainingFeatureVectorsFile"), params,
						relJudList, disable, docPageRank);

				System.out.println("Start trainning a model");
				trainModel(params);
			}

			// process queries
			for (Entry<String, String> queryEntry : queriesInFile.entrySet()) {

				String queryId = queryEntry.getKey();
				String query = queryEntry.getValue();
				if (ifLTR) {
					// initialize model
					RetrievalModelIndri indriModel = null;
					RetrievalModelBM25 BM25Model = null;
					double k1value = Double.valueOf(params.get("BM25:k_1"));
					double bvalue = Double.valueOf(params.get("BM25:b"));
					double k3value = Double.valueOf(params.get("BM25:k_3"));
					BM25Model = new RetrievalModelBM25(s);
					boolean configStatus = BM25Model.setParameter("k_1",
							k1value)
							&& BM25Model.setParameter("b", bvalue)
							&& BM25Model.setParameter("k_3", k3value);
					double mu = Double.valueOf(params.get("Indri:mu"));
					double lambda = Double.valueOf(params.get("Indri:lambda"));
					indriModel = new RetrievalModelIndri(s);
					boolean configStatusIndri = indriModel.setParameter("mu",
							mu) && indriModel.setParameter("lambda", lambda);
					if (!(configStatus && configStatusIndri)) {
						throw new Exception("Parameter setting failed");
					}
					String retrievalModelName = params
							.get("retrievalAlgorithm");
					RetrievalModel model = retrievalModelName.equals("BM25") ? BM25Model
							: indriModel;
					// create inital ranking

					List<ScoreEntry> docList = createInitialRanking(query,
							model);
					relJudList = new ArrayList<FeatureVector>();
					String[] terms = tokenizeQuery(query);
					for (ScoreEntry doc : docList) {
						FeatureVector tempFeature = new FeatureVector(queryId,
								query, doc.exdocid, docPageRank, 0);
						tempFeature.computeFeatures(terms, indriModel,
								BM25Model, disable);
						relJudList.add(tempFeature);
					}
					// normalize feature values for query
					normalizeFeaturesAndWrite(relJudList, disable,
							params.get("letor:testingFeatureVectorsFile"));

					// run SVM classfier to rerank test data
					classifyBySVM(docList, params);
					reRankDocAndWrit(queryId, params, docList);
					totalCount += docList.size();

				}
				// choose sources for the initial document ranking
				else if (!params.containsKey("fb")
						|| params.get("fb").equals("false")) {
					// set the retrieval Model
					String retrievalModelName = params
							.get("retrievalAlgorithm");
					RetrievalModel model = null;

					if (retrievalModelName.equals("UnrankedBoolean")) {
						model = new RetrievalModelUnrankedBoolean();
					} else if (retrievalModelName.equals("RankedBoolean")) {
						model = new RetrievalModelRankedBoolean();
					} else if (retrievalModelName.equals("BM25")) {
						double k1value = Double.valueOf(params.get("BM25:k_1"));
						double bvalue = Double.valueOf(params.get("BM25:b"));
						double k3value = Double.valueOf(params.get("BM25:k_3"));
						model = new RetrievalModelBM25(s);
						boolean configStatus = model.setParameter("k_1",
								k1value)
								&& model.setParameter("b", bvalue)
								&& model.setParameter("k_3", k3value);
					} else if (retrievalModelName.equals("Indri")) {
						double mu = Double.valueOf(params.get("Indri:mu"));
						double lambda = Double.valueOf(params
								.get("Indri:lambda"));
						model = new RetrievalModelIndri(s);
						boolean configStatus = model.setParameter("mu", mu)
								&& model.setParameter("lambda", lambda);

					}
					System.out.println("we set the model to "
							+ model.toString());
					Qryop qTree;

					qTree = parseQuery(query, model);
					System.out.println("we see the query after parsing "
							+ query);
					// printResults(query, qTree.evaluate(model));
					// write to the file in required format
					writeToFile(writer, queryId, qTree.evaluate(model),
							amountBound);
				} else {
					// set Parameter
					int fbDocNumber = Integer.valueOf(params.get("fbDocs"));
					int fbTermNumber = Integer.valueOf(params.get("fbTerms"));
					Double fbMu = Double.valueOf(params.get("fbMu"));
					Double fbOriginalWeight = Double.valueOf(params
							.get("fbOrigWeight"));
					String retrievalModelName = params
							.get("retrievalAlgorithm");
					RetrievalModel model = null;
					if (retrievalModelName.equals("Indri")) {
						double mu = Double.valueOf(params.get("Indri:mu"));
						double lambda = Double.valueOf(params
								.get("Indri:lambda"));
						model = new RetrievalModelIndri(s);
						boolean configStatus = model.setParameter("mu", mu)
								&& model.setParameter("lambda", lambda);

					}
					System.out.println("we set the model  " + model.toString());
					// query expansion
					List<DocInfo> retriveDocuments = new ArrayList<DocInfo>();
					if (params.containsKey("fbInitialRankingFile")) {
						String initialRankingFile = params
								.get("fbInitialRankingFile");
						// read in and give return all DocumentIds
						Scanner scanFile = new Scanner(new File(
								initialRankingFile));
						line = null;

						int counter = 1;
						do {

							line = scanFile.nextLine();
							String[] pairs = line.split(" ");
							// use queryId to match the target query
							if (pairs[0].equals(queryId)) {
								retriveDocuments.add(new DocInfo(Integer
										.valueOf(getInternalDocid(pairs[2])),
										Double.valueOf(pairs[4])));
								counter++;
							}
							if (counter > fbDocNumber) {
								break;
							}
						} while (scanFile.hasNext());
						scanFile.close();
					} else {

						Qryop qTree;
						qTree = parseQuery(query, model);
						System.out.println("we see the query after parsing "
								+ query);
						retriveDocuments = generateDocuments(
								qTree.evaluate(model), fbDocNumber);
					}
					// use the restriveDocuments to train expansion terms
					Map<String, Double> termMap = new HashMap<String, Double>();
					for (DocInfo d : retriveDocuments) {
						TermVector temp = new TermVector(d.docid, "body");
						for (String stem : temp.stems) {

							if (stem == null) {
								continue;
							}
							if (stem.contains(",") || stem.contains(".")) {
								continue;
							}
							if (!termMap.containsKey(stem)) {
								termMap.put(stem, 0.0);
							}
						}
					}
					Map<String, Double> termCTFmap = new HashMap<String, Double>();
					double lengthC = (double) QryEval.READER
							.getSumTotalTermFreq("body");
					// compute stem scores
					for (DocInfo d : retriveDocuments) {
						TermVector temp = new TermVector(d.docid, "body");
						double docScore = d.score;
						Map<String, Double> stemMap = new HashMap<String, Double>();
						for (int i = 1; i < temp.stemsLength(); i++) {
							String stem = temp.stemString(i);
							// double lengthC = (double)
							// QryEval.READER.getSumTotalTermFreq("body");
							double ctf = temp.totalStemFreq(i);
							double score = (temp.stemFreq(i) + fbMu * ctf
									/ lengthC)
									/ (s.getDocLength("body", d.docid) + fbMu)
									* Math.log((lengthC) / (ctf)) * docScore;
							stemMap.put(stem, score);
						}
						for (Entry<String, Double> entry : termMap.entrySet()) {
							if (stemMap.containsKey(entry.getKey())) {
								termMap.put(entry.getKey(), entry.getValue()
										+ stemMap.get(entry.getKey()));
							} else {
								// double lengthC = (double)
								// QryEval.READER.getSumTotalTermFreq("body");
								// double ctf = QryEval.READER.totalTermFreq(new
								// Term("body",new BytesRef(entry.getKey())));
								double ctf;
								if (!termCTFmap.containsKey(entry.getKey())) {
									ctf = QryEval.READER
											.totalTermFreq(new Term(
													"body",
													new BytesRef(entry.getKey())));
									termCTFmap.put(entry.getKey(), ctf);
								} else {
									ctf = termCTFmap.get(entry.getKey());
								}
								double defaultScore = fbMu
										* ctf
										/ lengthC
										/ (s.getDocLength("body", d.docid) + fbMu)
										* Math.log((lengthC) / (ctf))
										* docScore;
								termMap.put(entry.getKey(), entry.getValue()
										+ defaultScore);
							}
						}
					}
					// get top N stems as expansion stem
					PriorityQueue<StemInfo> pqs = new PriorityQueue<StemInfo>(
							fbTermNumber, new StemInfoComparator());
					for (Entry<String, Double> entry : termMap.entrySet()) {
						if (pqs.size() < fbTermNumber) {
							pqs.offer(new StemInfo(entry.getKey(), entry
									.getValue()));
						} else {
							if (entry.getValue() > pqs.peek().score) {
								pqs.poll();
								pqs.offer(new StemInfo(entry.getKey(), entry
										.getValue()));
							}
						}
					}
					System.out.println("we have the heap " + pqs.size());
					// generate expantion query
					// List<String> expanedQueries = getExpansionQuery(pqs);
					String expainquery = writeExpanedQuery(queryExWriter, pqs,
							queryId);

					System.out.println("we form the new query Expasion "
							+ expainquery);
					String newquery = "#WAND( " + fbOriginalWeight + " #AND("
							+ query + ")";
					newquery += " " + (1 - Double.valueOf(fbOriginalWeight))
							+ " " + expainquery.split(":")[1] + " )";
					Qryop qTreeNew;
					qTreeNew = parseQuery(newquery, model);
					System.out.println("we see the new query after parsing "
							+ qTreeNew);
					writeToFile(writer, queryId, qTreeNew.evaluate(model),
							amountBound);

				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
				queryExWriter.close();
			} catch (Exception e) {
			}
		}
		long endTime = System.currentTimeMillis();
		long processTime = endTime - startTime;
		printMemoryUsage(false);
		String output = String.format(
				"%d min, %d sec",
				TimeUnit.MILLISECONDS.toMinutes(processTime),
				TimeUnit.MILLISECONDS.toSeconds(processTime)
						- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS
								.toMinutes(processTime)));
		System.out.println("program running time: " + output);
	}

	private static void classifyBySVM(List<ScoreEntry> docList,
			Map<String, String> params) throws Exception {
		String execPath = params.get("letor:svmRankClassifyPath");
		String testFile = params.get("letor:testingFeatureVectorsFile");
		String predictFile = params.get("letor:testingDocumentScores");
		String modelPath = params.get("letor:svmRankModelFile");
		Double FEAT_GEN = Double.valueOf(params.get("letor:svmRankParamC"));
		// run svm to predict using a model
		Process cmdProc = Runtime.getRuntime().exec(
				new String[] { execPath, testFile, modelPath, predictFile });

		// The stdout/stderr consuming code MUST be included.
		// It prevents the OS from running out of output buffer space and
		// stalling.

		// consume stdout and print it out for debugging purposes
		BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(
				cmdProc.getInputStream()));
		String line;
		while ((line = stdoutReader.readLine()) != null) {
			System.out.println(line);
		}
		// consume stderr and print it for debugging purposes
		BufferedReader stderrReader = new BufferedReader(new InputStreamReader(
				cmdProc.getErrorStream()));
		while ((line = stderrReader.readLine()) != null) {
			System.out.println(line);
		}

		// get the return value from the executable. 0 means success, non-zero
		// indicates a problem
		int retValue = cmdProc.waitFor();
		if (retValue != 0) {
			throw new Exception("SVM Classfy crashed.");
		}
	}

	private static void reRankDocAndWrit(String queryId,
			Map<String, String> params, List<ScoreEntry> docList)
			throws IOException {
		try {
			BufferedReader br = new BufferedReader(new FileReader(
					params.get("letor:testingDocumentScores")));
			List<ScoreEntry> result = new ArrayList<ScoreEntry>();
			int DocNumber = 100;
			Comparator<ScoreEntry> cp = new ScoreDocidComparator();
			PriorityQueue<ScoreEntry> pq = new PriorityQueue<ScoreEntry>(
					DocNumber, cp);
			String line = null;
			int index = 0;
			if (docList.size() < 100) {
				System.out.println("hello smaller " + queryId + "   "
						+ docList.size());
			}
			int count = 0;
			while ((line = br.readLine()) != null && index < docList.size()) {
				if (count < totalCount) {
					count++;
				} else {
					String exid = docList.get(index).exdocid;
					index++;
					Double score = Double.valueOf(line.trim());
					ScoreEntry tempEntry = new ScoreEntry(exid, score);
					if (pq.size() < DocNumber) {
						pq.offer(tempEntry);
					} else {
						if (cp.compare(tempEntry, pq.peek()) > 0) {
							pq.poll();
							pq.offer(tempEntry);
						}
					}
				}
			}
			while (!pq.isEmpty()) {
				result.add(pq.poll());
			}
			Collections.reverse(result);
			BufferedWriter bw = new BufferedWriter(new FileWriter(
					params.get("trecEvalOutputPath"), true));
			DecimalFormat df = new DecimalFormat("0.000000000000");
			for (int i = 0; i < result.size(); i++) {
				bw.write(queryId + "\t" + "Q0\t" + result.get(i).exdocid + "\t"
						+ (i + 1) + "\t" + df.format(result.get(i).score)
						+ "\t" + "run-1" + "\n");
				/*
				 * System.out.println(queryId + "\t" + "Q0\t" +
				 * result.get(i).exdocid + "\t" + (i + 1) + "\t" +
				 * df.format(result.get(i).score) + "\t" + "run-1");
				 */
			}
			bw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static List<ScoreEntry> createInitialRanking(String query,
			RetrievalModel model) throws IOException {
		// return the top 100 docList
		Qryop qTree;
		qTree = parseQuery(query, model);
		System.out.println("we see the query after parsing " + query);
		// printResults(query, qTree.evaluate(model));
		// write to the file in required format
		// writeToFile(writer, queryId, qTree.evaluate(model), amountBound);
		QryResult qresult = qTree.evaluate(model);
		List<ScoreEntry> docList;
		int Bound = qresult.docScores.scores.size() < 100 ? qresult.docScores.scores
				.size() : 100;
		docList = generateTopKDocument(qresult.docScores, Bound);
		return docList;
	}

	private static void processQuery2Feature(String QueryFile,
			String RelevanceFile, String outputPath,
			Map<String, String> params, List<FeatureVector> relJudList,
			List<String> disable, Map<String, Double> docPageRank)
			throws IOException {
		Map<Integer, String> trainqs = new HashMap<Integer, String>();
		BufferedReader brtrain = new BufferedReader(new FileReader(QueryFile));
		String readFileLine = null;
		while ((readFileLine = brtrain.readLine()) != null) {
			String[] segmentedLine = readFileLine.split(":");
			trainqs.put(Integer.valueOf(segmentedLine[0]), segmentedLine[1]);
		}

		// store the training relevance doc info
		LinkedHashMap<String, LinkedHashMap<String, Integer>> qdocScore = new LinkedHashMap<String, LinkedHashMap<String, Integer>>();
		BufferedReader brrel = new BufferedReader(new FileReader(RelevanceFile));
		readFileLine = null;
		while ((readFileLine = brrel.readLine()) != null) {
			String[] segmentedLine = readFileLine.split(" ");
			String queryId = segmentedLine[0];
			String docName = segmentedLine[2];
			int relevance = Integer.parseInt(segmentedLine[3]);
			if (qdocScore.containsKey(queryId)) {
				qdocScore.get(queryId).put(docName, relevance);
			} else {
				LinkedHashMap<String, Integer> tempmap = new LinkedHashMap<String, Integer>();
				tempmap.put(docName, relevance);
				qdocScore.put(queryId, tempmap);
			}
		}

		RetrievalModelIndri indriModel = null;
		RetrievalModelBM25 BM25Model = null;
		double k1value = Double.valueOf(params.get("BM25:k_1"));
		double bvalue = Double.valueOf(params.get("BM25:b"));
		double k3value = Double.valueOf(params.get("BM25:k_3"));
		BM25Model = new RetrievalModelBM25(new DocLengthStore(READER));
		boolean configStatus = BM25Model.setParameter("k_1", k1value)
				&& BM25Model.setParameter("b", bvalue)
				&& BM25Model.setParameter("k_3", k3value);
		double mu = Double.valueOf(params.get("Indri:mu"));
		double lambda = Double.valueOf(params.get("Indri:lambda"));
		indriModel = new RetrievalModelIndri(new DocLengthStore(READER));
		boolean configStatusIndri = indriModel.setParameter("mu", mu)
				&& indriModel.setParameter("lambda", lambda);
		if (!(configStatus && configStatusIndri)) {
			try {
				throw new Exception("Parameter setting failed");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		List<Integer> keys = new ArrayList(trainqs.keySet());
		Collections.sort(keys);
		for (Integer key : keys) {
			relJudList = new ArrayList<FeatureVector>();
			String queryId = String.valueOf(key);
			String query = trainqs.get(key);
			System.out.println("Tringing process " + queryId + "  " + query);
			String[] terms = tokenizeQuery(query);
			for (Entry<String, Integer> docEntry : qdocScore.get(queryId)
					.entrySet()) {
				FeatureVector docFeatures = new FeatureVector(queryId, query,
						docEntry.getKey(), docPageRank, docEntry.getValue());
				docFeatures.computeFeatures(terms, indriModel, BM25Model,
						disable);
				relJudList.add(docFeatures);
			}
			// normalize feature values for query
			normalizeFeaturesAndWrite(relJudList, disable, outputPath);
		}
	}

	private static void trainModel(Map<String, String> params) throws Exception {
		String execPath = params.get("letor:svmRankLearnPath");
		String qrelsFeatureOutputFile = params
				.get("letor:trainingFeatureVectorsFile");
		Double FEAT_GEN = Double.valueOf(params.get("letor:svmRankParamC"));
		String modelOutputFile = params.get("letor:svmRankModelFile");
		// run svm to train a model
		Process cmdProc = Runtime.getRuntime().exec(
				new String[] { execPath, "-c", String.valueOf(FEAT_GEN),
						qrelsFeatureOutputFile, modelOutputFile });

		// The stdout/stderr consuming code MUST be included.
		// It prevents the OS from running out of output buffer space and
		// stalling.

		// consume stdout and print it out for debugging purposes
		BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(
				cmdProc.getInputStream()));
		String line;
		while ((line = stdoutReader.readLine()) != null) {
			System.out.println(line);
		}
		// consume stderr and print it for debugging purposes
		BufferedReader stderrReader = new BufferedReader(new InputStreamReader(
				cmdProc.getErrorStream()));
		while ((line = stderrReader.readLine()) != null) {
			System.out.println(line);
		}

		// get the return value from the executable. 0 means success, non-zero
		// indicates a problem
		int retValue = cmdProc.waitFor();
		if (retValue != 0) {
			throw new Exception("SVM Rank crashed.");
		}
	}

	private static void normalizeFeaturesAndWrite(
			List<FeatureVector> relJudList, List<String> disable,
			String outputPath) {
		int size = (disable == null) ? 0 : disable.size();
		int totalFeatures = 18;
		for (int i = 0; i < totalFeatures; i++) {
			Double minValue = Double.MAX_VALUE;
			Double maxValue = Double.MIN_VALUE;
			for (FeatureVector fv : relJudList) {
				double tempValue = fv.featureList.get(i);
				if (tempValue != -1.0 && tempValue != -2.0) {
					minValue = Math.min(minValue, tempValue);
					maxValue = Math.max(maxValue, tempValue);
				}
			}
			if (i == 6) {
				System.out.println("we get the max: " + maxValue + " and min: "
						+ minValue);
			}
			for (FeatureVector fv : relJudList) {
				/*
				 * if (fv.docName.equals("clueweb09-en0000-56-19359") && i == 6)
				 * { System.out.println("term over lap  for doc before  " +
				 * fv.featureList.get(6)); }
				 */
				fv.normalization(i, minValue, maxValue);
				/*
				 * if (fv.docName.equals("clueweb09-en0000-56-19359") && i == 6)
				 * { System.out.println("term over lap  for doc " +
				 * fv.featureList.get(6)); }
				 */
			}
		}

		BufferedWriter bw = null;

		try {
			bw = new BufferedWriter(new FileWriter(new File(outputPath), true));
			for (FeatureVector fv : relJudList) {
				fv.writeToFile(bw, disable);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out
					.println("Cannot open the output Path file " + outputPath);
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static String writeExpanedQuery(BufferedWriter queryExWriter,
			PriorityQueue<StemInfo> pqs, String queryId) {
		StringBuilder sb = new StringBuilder();
		sb.append(queryId);
		sb.append(": #WAND (");
		for (StemInfo temp : pqs) {

			// sb.append(String.format("%.4g", temp.score));
			sb.append(new java.text.DecimalFormat("0.0000").format(temp.score));
			sb.append(" ");
			sb.append(temp.stem);
			sb.append(" ");
		}
		sb.append(")\n");
		try {
			queryExWriter.write(sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	private static List<DocInfo> generateDocuments(QryResult evaluateResult,
			int fbDocNumber) throws Throwable {
		List<DocInfo> result = new ArrayList<DocInfo>();
		List<ScoreEntry> tempR;
		tempR = generateTopKDocument(evaluateResult.docScores, fbDocNumber);
		for (int i = 0; i < fbDocNumber; i++) {
			result.add(new DocInfo(getInternalDocid(tempR.get(i).exdocid),
					tempR.get(i).score));
		}
		return result;
	}

	private static List<ScoreEntry> generateTopKDocument(ScoreList docScores,
			int DocNumber) throws IOException {
		List<ScoreEntry> result = new ArrayList<ScoreEntry>();
		Comparator<ScoreEntry> cp = new ScoreDocidComparator();
		PriorityQueue<ScoreEntry> pq = new PriorityQueue<ScoreEntry>(DocNumber,
				cp);
		for (int i = 0; i < docScores.scores.size(); i++) {
			String externalId = READER.document(docScores.getDocid(i)).get(
					"externalId");
			double score = docScores.getDocidScore(i);
			ScoreEntry temp = new ScoreEntry(externalId, score);
			if (pq.size() < DocNumber) {
				pq.offer(temp);
			} else {
				if (cp.compare(temp, pq.peek()) > 0) {
					pq.poll();
					pq.offer(temp);
				}
			}
		}
		for (int i = 0; i < DocNumber && i < docScores.scores.size(); i++) {
			result.add(pq.poll());
		}
		Collections.reverse(result);
		return result;
	}

	private static void writeToFile(BufferedWriter writer, String queryId,
			QryResult result, int amountbound) throws Exception {

		System.out.println("we processed query  " + queryId);

		// if print scoreList, sort scoreList
		// if print invertedList, sort invertedList
		int resultLength = 0;

		// result.invertedList.sortInvertedList();
		resultLength = result.docScores.scores.size();
		// update the docScore in to a new class for quicker sorting
		List<ScoreEntry> outputResults = new ArrayList<ScoreEntry>();
		// sort the output list according to requirements
		// result.docScores.sortScoreList();
		if (resultLength < 1) {
			writer.write(queryId + "\t" + "Q0" + "\t" + "dummy" + "\t" + "1"
					+ "\t" + "0 " + "\t" + "run-1" + "\n");
		} else {
			outputResults = generateTopKDocument(result.docScores,
					Math.min(resultLength, amountbound));
			DecimalFormat df = new DecimalFormat("0.000000000000");
			if (resultLength > amountbound)
				resultLength = amountbound;
			for (int i = 0; i < resultLength; i++) {
				writer.write(queryId + "\t" + "Q0\t"
						+ outputResults.get(i).exdocid + "\t" + (i + 1) + "\t"
						+ df.format(outputResults.get(i).score) + "\t"
						+ "run-1" + "\n");
				System.out.println(queryId + "\t" + "Q0\t"
						+ outputResults.get(i).exdocid + "\t" + (i + 1) + "\t"
						+ df.format(outputResults.get(i).score) + "\t"
						+ "run-1");
			}
			System.out.println("write Finished !!!!!");
		}
	}

	/**
	 * Write an error message and exit. This can be done in other ways, but I
	 * wanted something that takes just one statement so that it is easy to
	 * insert checks without cluttering the code.
	 * 
	 * @param message
	 *            The error message to write before exiting.
	 * @return void
	 */
	static void fatalError(String message) {
		System.err.println(message);
		System.exit(1);
	}

	/**
	 * Get the external document id for a document specified by an internal
	 * document id. If the internal id doesn't exists, returns null.
	 * 
	 * @param iid
	 *            The internal document id of the document.
	 * @throws IOException
	 */
	static String getExternalDocid(int iid) throws IOException {
		Document d = QryEval.READER.document(iid);
		String eid = d.get("externalId");
		return eid;
	}

	/**
	 * Finds the internal document id for a document specified by its external
	 * id, e.g. clueweb09-enwp00-88-09710. If no such document exists, it throws
	 * an exception.
	 * 
	 * @param externalId
	 *            The external document id of a document.s
	 * @return An internal doc id suitable for finding document vectors etc.
	 * @throws Exception
	 */
	static int getInternalDocid(String externalId) throws Exception {
		Query q = new TermQuery(new Term("externalId", externalId));

		IndexSearcher searcher = new IndexSearcher(QryEval.READER);
		TopScoreDocCollector collector = TopScoreDocCollector.create(1, false);
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		if (hits.length < 1) {
			throw new Exception("External id not found.");
		} else {
			return hits[0].doc;
		}
	}

	/**
	 * parseQuery converts a query string into a query tree.
	 * 
	 * @param qString
	 *            A string containing a query.
	 * @param model
	 * @param qTree
	 *            A query tree
	 * @throws IOException
	 */
	static Qryop parseQuery(String qString, RetrievalModel model)
			throws IOException {

		Qryop currentOp = null;
		Stack<Qryop> stack = new Stack<Qryop>();

		// Add a default query operator to an unstructured query. This
		// is a tiny bit easier if unnecessary whitespace is removed.

		qString = qString.trim();
		if (model instanceof RetrievalModelUnrankedBoolean
				|| model instanceof RetrievalModelRankedBoolean) {
			qString = "#or(" + qString + ")";
		} else if (model instanceof RetrievalModelBM25) {
			qString = "#sum(" + qString + ")";

		} else if (model instanceof RetrievalModelIndri) {
			qString = "#and(" + qString + ")";
		}

		// Tokenize the query.

		StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()",
				true);
		String token = null;

		// Each pass of the loop processes one token. To improve
		// efficiency and clarity, the query operator on the top of the
		// stack is also stored in currentOp.

		while (tokens.hasMoreTokens()) {

			token = tokens.nextToken();

			if (token.matches("[ ,(\t\n\r]")) {
				// Ignore most delimiters.
			} else if (token.equalsIgnoreCase("#and")) {
				currentOp = new QryopSlAnd();
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#syn")) {
				currentOp = new QryopIlSyn();
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#or")) {
				currentOp = new QryopSLOr();
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#score")) {
				currentOp = new QryopSlScore();
				stack.push(currentOp);
			} else if (token.toLowerCase().startsWith("#near")) {
				int distance = Integer.valueOf(token.split("/")[1]);
				currentOp = new QryopIlNEARn(distance);
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#sum")) {
				currentOp = new QryopSlSum();
				stack.push(currentOp);
			} else if (token.toLowerCase().startsWith("#window")) {
				int width = Integer.valueOf(token.split("/")[1]);
				currentOp = new QryopIlWINDOW(width);
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#wsum")) {
				currentOp = new QryopSlWSum();
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#wand")) {
				currentOp = new QryopSlWAnd();
				stack.push(currentOp);
			} else if (token.startsWith(")")) { // Finish current query
												// operator.
				// If the current query operator is not an argument to
				// another query operator (i.e., the stack is empty when it
				// is removed), we're done (assuming correct syntax - see
				// below). Otherwise, add the current operator as an
				// argument to the higher-level operator, and shift
				// processing back to the higher-level operator.
				stack.pop();

				if (stack.empty())
					break;

				Qryop arg = currentOp;
				currentOp = stack.peek();
				if (arg.args.size() == 0) {
					continue;
				}
				currentOp.add(arg);
			} else {
				if (QryUtil.isNumeric(token) && (currentOp instanceof QryopSlW)
						&& QryUtil.needWeight(currentOp)) {
					((QryopSlW) currentOp).addWeight(Double.valueOf(token));
				} else {
					// NOTE: You should do lexical processing of the token
					// before
					// creating the query term, and you should check to see
					// whether
					// the token specifies a particular field (e.g.,
					// apple.title).
					// should check the body part and then do lexical processing
					String[] tokenWithField = token.split("\\.");
					if (QryUtil.isNumeric(token)) {
						currentOp.add(new QryopIlTerm(token));
					} else if (tokenizeQuery(tokenWithField[0]).length > 0) {
						String[] tokenTemp = tokenizeQuery(tokenWithField[0]);
						// don't stem the field
						// String[] fieldTemp =
						// tokenizeQuery(tokenWithField[1]);
						if (tokenWithField.length > 1) {
							currentOp.add(new QryopIlTerm(tokenTemp[0],
									tokenWithField[1]));
						} else {
							currentOp.add(new QryopIlTerm(tokenTemp[0]));
						}
					} else {
						// stopwords pop out the weight
						if (currentOp instanceof QryopSlW) {
							try {
								((QryopSlW) currentOp).deleteWeight();
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}

			}
		}

		// A broken structured query can leave unprocessed tokens on the
		// stack, so check for that.

		if (tokens.hasMoreTokens()) {
			System.err
					.println("Error:  Query syntax is incorrect.  " + qString);
			return null;
		}
		System.out.println("query after parsing from the operators"
				+ currentOp.toString());

		return currentOp;
	}

	/**
	 * Print a message indicating the amount of memory used. The caller can
	 * indicate whether garbage collection should be performed, which slows the
	 * program but reduces memory usage.
	 * 
	 * @param gc
	 *            If true, run the garbage collector before reporting.
	 * @return void
	 */
	public static void printMemoryUsage(boolean gc) {

		Runtime runtime = Runtime.getRuntime();

		if (gc) {
			runtime.gc();
		}

		System.out
				.println("Memory used:  "
						+ ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L))
						+ " MB");
	}

	/**
	 * Print the query results.
	 * 
	 * THIS IS NOT THE CORRECT OUTPUT FORMAT. YOU MUST CHANGE THIS METHOD SO
	 * THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK PAGE, WHICH IS:
	 * 
	 * QueryID Q0 DocID Rank Score RunID
	 * 
	 * @param queryName
	 *            Original query.
	 * @param result
	 *            Result object generated by {@link Qryop#evaluate()}.
	 * @throws IOException
	 */
	static void printResults(String queryName, QryResult result)
			throws IOException {

		System.out.println(queryName + ":  ");
		result.docScores.sortScoreList();
		if (result.docScores.scores.size() < 1) {
			System.out.println("\tNo results.");
		} else {
			int resultLength = result.docScores.scores.size();
			if (resultLength <= 10) {
				for (int i = 0; i < result.docScores.scores.size(); i++) {
					System.out.println("\t" + i + ":  "
							+ getExternalDocid(result.docScores.getDocid(i))
							+ ", " + result.docScores.getDocidScore(i));
				}
			} else {
				for (int i = 0; i < 10; i++) {
					System.out.println("\t" + i + ":  "
							+ getExternalDocid(result.docScores.getDocid(i))
							+ ", " + result.docScores.getDocidScore(i));
				}
			}

		}
	}

	/**
	 * Given a query string, returns the terms one at a time with stopwords
	 * removed and the terms stemmed using the Krovetz stemmer.
	 * 
	 * Use this method to process raw query terms.
	 * 
	 * @param query
	 *            String containing query
	 * @return Array of query tokens
	 * @throws IOException
	 */
	static String[] tokenizeQuery(String query) throws IOException {

		TokenStreamComponents comp = analyzer.createComponents("dummy",
				new StringReader(query));
		TokenStream tokenStream = comp.getTokenStream();

		CharTermAttribute charTermAttribute = tokenStream
				.addAttribute(CharTermAttribute.class);
		tokenStream.reset();

		List<String> tokens = new ArrayList<String>();
		while (tokenStream.incrementToken()) {
			String term = charTermAttribute.toString();
			tokens.add(term);
		}
		return tokens.toArray(new String[tokens.size()]);
	}

	public static class DocInfo {
		int docid;
		double score;

		public DocInfo(int id, double s) {
			this.docid = id;
			this.score = s;
		}
	}

	public static class StemInfo {
		String stem;
		double score;

		public StemInfo(String str, double s) {
			this.stem = str;
			this.score = s;
		}
	}

	public static class ScoreEntry implements Comparable<ScoreEntry> {
		String exdocid;
		double score;

		public ScoreEntry(String exdocid, double score) {
			this.exdocid = exdocid;
			this.score = score;
		}

		public int compareTo(ScoreEntry a) {
			if (this.score - a.score == 0) {
				return this.exdocid.compareTo(a.exdocid);
			} else {
				if (this.score > a.score) {
					return -1;
				}
				if (this.score < a.score)
					return 1;
			}
			return 0;
		}

		public String toString() {
			return "(" + this.exdocid + ", " + this.score + ")";
		}
	}

	public static class StemInfoComparator implements Comparator<StemInfo> {

		@Override
		public int compare(StemInfo a, StemInfo b) {
			if (a.score - b.score > 0) {
				return 1;
			}
			if (a.score - b.score < 0) {
				return -1;
			}
			return 0;
		}

	}

	public static class ScoreDocidComparator implements Comparator<ScoreEntry> {
		@Override
		public int compare(ScoreEntry a0, ScoreEntry a1) {
			if (a0.score - a1.score == 0) {
				return -a0.exdocid.compareTo(a1.exdocid);
			} else {
				if (a0.score > a1.score)
					return 1;
				if (a0.score < a1.score)
					return -1;
			}
			return 0;
		}
	}
}
