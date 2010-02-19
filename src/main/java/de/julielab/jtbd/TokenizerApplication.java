/** 
 * TokenizerApplication.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 2.0
 * Since version:   1.0
 *
 * Creation date: Aug 01, 2006 
 * 
 * The user interface (command line version) for the JULIE Token Boundary Detector. 
 * Includes training, prediction, file format check, and evaluation.
 * 
 * Some info on logging: to control mallet's logging, please use 
 * the logging.properties file via -Djava.util.logging.config.file
 *
 **/

package de.julielab.jtbd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import cc.mallet.fst.CRF;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;

public class TokenizerApplication {

	public static void main(String[] args) {

		if (args.length < 1) {
			System.err.println("usage: JTBD <mode> <mode-specific-parameters>");
			showModes();
			System.exit(-1);
		}

		String mode = args[0];

		if (mode.equals("c")) { // check mode
			startCheckMode(args);

		} else if (mode.equals("s")) { // 90-10 split evaluation
			start9010ValidationMode(args);

		} else if (mode.equals("x")) { // cross validaton mode
			startXValidationMode(args);

		} else if (mode.equals("t")) { // training mode
			startTrainingMode(args);

		} else if (mode.equals("p")) { // prediction mode
			startPredictionMode(args);

		} else if (mode.equals("e")) { // compare validation mode
			startCompareValidationMode(args);

		} else { // unknown mode
			System.err.println("unknown mode");
			showModes();
		}

	}

	/**
	 * Entry point for compare validation mode
	 * 
	 * @param args
	 *            the command line arguments
	 */
	private static void startCompareValidationMode(String[] args) {
		if (args.length != 6) {
			System.err
					.println("usage: JTBD e <modelFile> <sent-file> <tok-file> <predout-file> <errout-file>");
			System.exit(-1);
		}

		ObjectInputStream in;
		CRF crf = null;
		try {
			// load model
			in = new ObjectInputStream(new FileInputStream(args[1]));
			crf = (CRF) in.readObject();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		File orgSentencesFile = new File(args[2]);
		File tokSentencesFile = new File(args[3]);
		ArrayList<String> orgSentences = readFile(orgSentencesFile);
		ArrayList<String> tokSentences = readFile(tokSentencesFile);
		File predOutFile = new File(args[4]);
		File errOutFile = new File(args[5]);

		ArrayList<String> errors = new ArrayList<String>();
		ArrayList<String> predictions = new ArrayList<String>();
		doEvaluation(crf, orgSentences, tokSentences, predictions, errors);

		writeFile(predictions, predOutFile);
		writeFile(errors, errOutFile);
	}

	/**
	 * Entry point for prediction mode
	 * 
	 * @param args
	 *            the command line arguments
	 */
	private static void startPredictionMode(String[] args) {
		if (args.length != 4) {
			System.err.println("usage: JTBD p <inDir> <outDir> <model-file>");
			System.exit(-1);
		}

		File inDir = new File(args[1]);
		if (!inDir.isDirectory()) {
			System.err
					.println("Error: the specified input directory does not exist.");
			System.exit(-1);
		}

		File outDir = new File(args[2]);
		if (!outDir.isDirectory() || !outDir.canWrite()) {
			System.err
					.println("Error: the specified output directory does not exist or is not writable.");
			System.exit(-1);
		}

		String modelFilename = args[3];

		doPrediction(inDir, outDir, modelFilename);
	}

	/**
	 * Entry point for training mode
	 * 
	 * @param args
	 *            the command line arguments
	 */
	private static void startTrainingMode(String[] args) {
		if (args.length != 4) {
			System.err
					.println("usage: JTBD t <sent-file> <tok-file> <model-file>");
			System.exit(-1);
		}

		File orgSentencesFile = new File(args[1]);
		File tokSentencesFile = new File(args[2]);
		String modelFilename = args[3];

		doTraining(orgSentencesFile, tokSentencesFile, modelFilename);
	}

	/**
	 * Entry point for cross-validation mode
	 * 
	 * @param args
	 *            the command line arguments
	 */
	private static void startXValidationMode(String[] args) {
		if (args.length != 6) {
			System.err
					.println("usage: JTBD x <sent-file> <tok-file> <cross-val-rounds> <predout-file> <errout-file>");
			System.exit(-1);
		}
		File orgSentencesFile = new File(args[1]);
		File tokSentencesFile = new File(args[2]);
		int n = (new Integer(args[3])).intValue();
		File predOutFile = new File(args[4]);
		File errOutFile = new File(args[5]);

		ArrayList<String> errors = new ArrayList<String>();
		ArrayList<String> predictions = new ArrayList<String>();

		doCrossEvaluation(n, orgSentencesFile, tokSentencesFile, predictions,
				errors);

		writeFile(predictions, predOutFile);
		writeFile(errors, errOutFile);
	}

	/**
	 * Entry point for 90-10 split validation mode
	 * 
	 * @param args
	 *            the command line arguments
	 */
	private static void start9010ValidationMode(String[] args) {
		if (args.length != 5) {
			System.err
					.println("usage: JTBD s <sent-file> <tok-file> <predout-file> <errout-file>");
			System.exit(-1);
		}
		File orgSentencesFile = new File(args[1]);
		File tokSentencesFile = new File(args[2]);
		File predOutFile = new File(args[3]);
		File errOutFile = new File(args[4]);

		ArrayList<String> errors = new ArrayList<String>();
		ArrayList<String> predictions = new ArrayList<String>();
		do9010Evaluation(orgSentencesFile, tokSentencesFile, predictions,
				errors);

		writeFile(predictions, predOutFile);
		writeFile(errors, errOutFile);
	}

	/**
	 * Entry poing for file format check mode
	 * 
	 * @param args
	 *            the command line arguments
	 */
	private static void startCheckMode(String[] args) {
		if (args.length != 3) {
			System.err.println("usage: JTBD c <sent-file> <tok-file>");
			System.exit(-1);
		}
		File orgSentencesFile = new File(args[1]);
		File tokSentencesFile = new File(args[2]);
		doCheck(orgSentencesFile, tokSentencesFile);
	}

	/**
	 * shows available modes
	 */
	private static void showModes() {
		System.err.println("\nAvailable modes:");
		System.err.println("c: check data ");
		System.err.println("s: 90-10 split evaluation");
		System.err.println("x: cross validation ");
		System.err.println("t: train a tokenizer ");
		System.err.println("p: predict with tokenizer ");
		System.err.println("e: evaluation on previously trained model");
		System.exit(-1);
	}

	/**
	 * check the file format
	 * 
	 * @param orgSentencesFile
	 * @param tokSentencesFile
	 */
	private static void doCheck(File orgSentencesFile, File tokSentencesFile) {

		Tokenizer tokenizer = new Tokenizer();

		System.out.println("checking on files: \n * "
				+ orgSentencesFile.toString() + "\n * "
				+ tokSentencesFile.toString() + "\n");

		ArrayList<String> orgSentences = readFile(orgSentencesFile);
		ArrayList<String> tokSentences = readFile(tokSentencesFile);

		InstanceList trainData = tokenizer.makeTrainingData(orgSentences,
				tokSentences);
		Pipe myPipe = trainData.getPipe();
		// System.out.println("\n" + myPipe.getDataAlphabet().toString());
		System.out.println("\n\n\n# Features resulting from training data: "
				+ myPipe.getDataAlphabet().size());
		System.out
				.println("(critical sentences were omitted for feature generation)");

		System.out.println("Done.");

	}

	/**
	 * perform cross validation
	 * 
	 * @param n
	 *            number of splits
	 * @param orgSentencesFile
	 * @param tokSentencesFile
	 * @param errors
	 * @param predictions
	 * @return
	 */
	private static double doCrossEvaluation(int n, File orgSentencesFile,
			File tokSentencesFile, ArrayList<String> errors,
			ArrayList<String> predictions) {

		ArrayList<String> orgSentences = readFile(orgSentencesFile);
		ArrayList<String> tokSentences = readFile(tokSentencesFile);

		long seed = 1;
		Collections.shuffle(orgSentences, new Random(seed));
		Collections.shuffle(tokSentences, new Random(seed));

		int pos = 0;
		int sizeRound = orgSentences.size() / n;
		int sizeAll = orgSentences.size();
		int sizeLastRound = sizeRound + sizeAll % n;
		System.out.println("number of files in directory: " + sizeAll);
		System.out.println("size of each/last round: " + sizeRound + "/"
				+ sizeLastRound);
		System.out.println();

		double[] acc = new double[n]; // 
		double avgAcc = 0;

		for (int i = 0; i < n; i++) { // in each round

			ArrayList<String> predictOrgSentences = new ArrayList<String>();
			ArrayList<String> predictTokSentences = new ArrayList<String>();
			ArrayList<String> trainOrgSentences = new ArrayList<String>();
			ArrayList<String> trainTokSentences = new ArrayList<String>();

			int p = 0;
			int t = 0;

			if (i == n - 1) {
				// last round
				for (int j = 0; j < orgSentences.size(); j++) {
					if (j < pos) {
						trainOrgSentences.add(orgSentences.get(j));
						trainTokSentences.add(tokSentences.get(j));
						t++;
					} else {
						predictOrgSentences.add(orgSentences.get(j));
						predictTokSentences.add(tokSentences.get(j));
						p++;
					}
				}

			} else {
				// other rounds
				for (int j = 0; j < orgSentences.size(); j++) {
					if (j < pos || j >= (pos + sizeRound)) {
						// System.out.println(j + " - add to train");
						trainOrgSentences.add(orgSentences.get(j));
						trainTokSentences.add(tokSentences.get(j));
						t++;
					} else {
						predictOrgSentences.add(orgSentences.get(j));
						predictTokSentences.add(tokSentences.get(j));
						p++;
					}
				}
				pos += sizeRound;
			}

			// now evaluate for this round
			System.out.println("training size: " + trainOrgSentences.size());
			System.out
					.println("prediction size: " + predictOrgSentences.size());
			acc[i] = doEvaluation(trainOrgSentences, trainTokSentences,
					predictOrgSentences, predictTokSentences, predictions,
					errors);
		}

		DecimalFormat df = new DecimalFormat("0.000");
		for (int i = 0; i < acc.length; i++) {
			avgAcc += acc[i];
			System.out.println("ACC in round " + i + ": " + df.format(acc[i]));
		}
		avgAcc = avgAcc / (double) n;

		System.out.println("\n\n------------------------------------");
		System.out.println("avg accuracy: " + df.format(avgAcc));
		System.out.println("------------------------------------");
		return avgAcc;

	}

	/**
	 * 90-10 split evaluation
	 * 
	 * @param orgSentencesFile
	 * @param tokSentencesFile
	 * @param errors
	 * @param predictions
	 * @return
	 */
	private static double do9010Evaluation(File orgSentencesFile,
			File tokSentencesFile, ArrayList<String> errors,
			ArrayList<String> predictions) {

		ArrayList<String> orgSentences = readFile(orgSentencesFile);
		ArrayList<String> tokSentences = readFile(tokSentencesFile);


		long seed = 1;
		Collections.shuffle(orgSentences, new Random(seed));
		Collections.shuffle(tokSentences, new Random(seed));

		int sizeAll = orgSentences.size();
		int sizeTest = (int) (sizeAll * 0.1);
		int sizeTrain = sizeAll - sizeTest;

		if (sizeTest == 0) {
			System.err.println("Error: no test files for this split.");
			System.exit(-1);
		}
		System.out.println("all: " + sizeAll + "\ttrain: " + sizeTrain + "\t"
				+ "test: " + sizeTest);

		ArrayList<String> trainOrgSentences = new ArrayList<String>();
		ArrayList<String> trainTokSentences = new ArrayList<String>();
		ArrayList<String> predictOrgSentences = new ArrayList<String>();
		ArrayList<String> predictTokSentences = new ArrayList<String>();

		for (int i = 0; i < sizeTrain; i++) {

			trainOrgSentences.add(orgSentences.get(i));
			trainTokSentences.add(tokSentences.get(i));
		}

		for (int i = sizeTrain; i < sizeAll; i++) {
			predictOrgSentences.add(orgSentences.get(i));
			predictTokSentences.add(tokSentences.get(i));
		}

		// System.out.println(trainOrgSentences.toString());
		// System.out.println(trainTokSentences.toString());
		// System.out.println(predictOrgSentences.toString());
		// System.out.println(predictTokSentences.toString());
		return doEvaluation(trainOrgSentences, trainTokSentences,
				predictOrgSentences, predictTokSentences, predictions, errors);

	}

	/**
	 * general evaluation function, is called from doEvaluation
	 * 
	 * @param crf
	 *            the crf model
	 * @param predictOrgSentences
	 * @param predictTokSentences
	 * @param errors
	 * @param predictions
	 * @return
	 */
	private static double doEvaluation(CRF crf,
			ArrayList<String> predictOrgSentences,
			ArrayList<String> predictTokSentences, ArrayList<String> errors,
			ArrayList<String> predictions) {

		Tokenizer tokenizer = new Tokenizer();
		tokenizer.setModel(crf);

		// 2. prediction
		InstanceList predData = tokenizer.makePredictionData(
				predictOrgSentences, predictTokSentences);

		int nrDecisions = 0;
		int corrDecisions = 0;
		int fp = 0;
		int fn = 0;

		for (int i = 0; i < predData.size(); i++) {
			String orgSentence = predictOrgSentences.get(i);
			String tokSentence = predictTokSentences.get(i);
			String sentenceBoundary = orgSentence.substring(orgSentence
					.length() - 1, orgSentence.length());

			Instance inst = (Instance) predData.get(i);
			ArrayList<Unit> units = null;
			units = tokenizer.predict(inst);

			// 3. evaluation
			ArrayList<String> orgLabels = tokenizer
					.getLabelsFromLabelSequence((LabelSequence) inst
							.getTarget());
			ArrayList<String> wSpaces = (ArrayList) inst.getSource();

			String sentence = "";

			int localDec = 0;
			int localCorr = 0;
			boolean hasError = false;

			for (int j = 0; j < units.size(); j++) {
				String sp = (units.get(j).label.equals("P")) ? " " : "";
				sentence += units.get(j).rep + sp;

				if (!wSpaces.get(j).equals("WS") && j < units.size() - 1) {
					// this is a critical split decision... count!
					// do not count last label (i.e. sentence boundary is not
					// critical)
					localDec++;
					// compare labels here
					if (orgLabels.get(j).equals(units.get(j).label))
						localCorr++;
					else {
						hasError = true;
						if (orgLabels.get(j).equals("P")
								&& units.get(j).label.equals("N"))
							fn++;
						if (orgLabels.get(j).equals("N")
								&& units.get(j).label.equals("P"))
							fp++;

						errors.add("@" + orgLabels.get(j) + "->"
								+ units.get(j).label);
						errors.add(tokenizer.showErrorContext(j, units,
								orgLabels));

					}
				}

			}

			nrDecisions += localDec;
			corrDecisions += localCorr;

			// System.out.println("local critical: " + localDec);
			// System.out.println("local correct: " + localCorr);

			// System.out.println(" IN: " + orgSentence);
			// System.out.println("PRED: " + sentence + sentenceBoundary);
			// System.out.println("GOLD: " + tokSentence);

			// System.out.println();
			if (!sentence.substring(sentence.length() - 1, sentence.length())
					.equals(" "))
				sentenceBoundary = " " + sentenceBoundary;

			predictions.add(sentence + sentenceBoundary);
			if (hasError) {
				errors.add(sentence + sentenceBoundary);
				errors.add(tokSentence);
				errors.add("\n");
			}
		}

		double ACC = (corrDecisions / (double) nrDecisions);
		System.out.println("\n* ------------------------------------");
		System.out.println("* critical decisions: " + nrDecisions);
		System.out.println("* correct decisions: " + corrDecisions);
		System.out.println("* fp: " + fp);
		System.out.println("* fn: " + fn);
		System.out.println("* ACC = " + ACC);
		System.out.println("* ------------------------------------\n");

		return ACC;

	}

	/**
	 * general evaluation function, is called from doCrossEvaluation or
	 * do9010Evaluation.
	 * 
	 * @param crf
	 *            the crf model
	 * @param predictOrgSentences
	 * @param predictTokSentences
	 * @param errors
	 * @param predictions
	 * @return
	 */
	public static double doEvaluation(ArrayList<String> trainOrgSentences,
			ArrayList<String> trainTokSentences,
			ArrayList<String> predictOrgSentences,
			ArrayList<String> predictTokSentences, ArrayList<String> errors,
			ArrayList<String> predictions) {

		Tokenizer tokenizer = new Tokenizer();

		// 1. training
		InstanceList trainData = tokenizer.makeTrainingData(trainOrgSentences,
				trainTokSentences);
		Pipe myPipe = trainData.getPipe();

		System.out.println("training model...");
		tokenizer.train(trainData, myPipe);

		if (true)
			return doEvaluation(tokenizer.getModel(), predictOrgSentences,
					predictTokSentences, errors, predictions);

		// 2. prediction
		InstanceList predData = tokenizer.makePredictionData(
				predictOrgSentences, predictTokSentences);

		int nrDecisions = 0;
		int corrDecisions = 0;
		int fp = 0;
		int fn = 0;

		for (int i = 0; i < predData.size(); i++) {
			String orgSentence = predictOrgSentences.get(i);
			String tokSentence = predictTokSentences.get(i);
			String sentenceBoundary = orgSentence.substring(orgSentence
					.length() - 1, orgSentence.length());

			Instance inst = (Instance) predData.get(i);
			ArrayList<Unit> units = null;
			units = tokenizer.predict(inst);
			

			// 3. evaluation
			ArrayList<String> orgLabels = tokenizer
					.getLabelsFromLabelSequence((LabelSequence) inst
							.getTarget());
			ArrayList<String> wSpaces = (ArrayList) inst.getSource();

			String sentence = "";

			int localDec = 0;
			int localCorr = 0;
			boolean hasError = false;

			for (int j = 0; j < units.size(); j++) {
				String sp = (units.get(j).label.equals("P")) ? " " : "";
				sentence += units.get(j).rep + sp;

				if (!wSpaces.get(j).equals("WS") && j < units.size() - 1) {
					// this is a critical split decision... count!
					// do not count last label (i.e. sentence boundary is not
					// critical)
					localDec++;
					// System.out.println("U: " + units.get(j).rep);
					// compare labels here
					if (orgLabels.get(j).equals(units.get(j).label))
						localCorr++;
					else {
						hasError = true;
						if (orgLabels.get(j).equals("P")
								&& units.get(j).label.equals("N"))
							fn++;
						if (orgLabels.get(j).equals("N")
								&& units.get(j).label.equals("P"))
							fp++;

						errors.add("@" + orgLabels.get(j) + "->"
								+ units.get(j).label);
						errors.add(tokenizer.showErrorContext(j, units,
								orgLabels));

					}
				}

			}

			nrDecisions += localDec;
			corrDecisions += localCorr;

			// System.out.println("local critical: " + localDec);
			// System.out.println("local correct: " + localCorr);

			// System.out.println(" IN: " + orgSentence);
			// System.out.println("PRED: " + sentence + sentenceBoundary);
			// System.out.println("GOLD: " + tokSentence);

			// System.out.println();
			if (!sentence.substring(sentence.length() - 1, sentence.length())
					.equals(" "))
				sentenceBoundary = " " + sentenceBoundary;

			predictions.add(sentence + sentenceBoundary);
			if (hasError) {
				errors.add(sentence + sentenceBoundary);
				errors.add(tokSentence);
				errors.add("\n");
			}
		}

		double ACC = (corrDecisions / (double) nrDecisions);
		System.out.println("\n* ------------------------------------");
		System.out.println("* critical decisions: " + nrDecisions);
		System.out.println("* correct decisions: " + corrDecisions);
		System.out.println("* fp: " + fp);
		System.out.println("* fn: " + fn);
		System.out.println("* ACC = " + ACC);
		System.out.println("* ------------------------------------\n");

		return ACC;

	}

	/**
	 * train a model
	 * 
	 * @param orgSentencesFile
	 * @param tokSentencesFile
	 * @param modelFilename
	 */
	public static void doTraining(File orgSentencesFile, File tokSentencesFile,
			String modelFilename) {

		Tokenizer tokenizer = new Tokenizer();

		ArrayList<String> trainTokSentences = readFile(tokSentencesFile);
		ArrayList<String> trainOrgSentences = readFile(orgSentencesFile);

		// get training data
		InstanceList trainData = tokenizer.makeTrainingData(trainOrgSentences,
				trainTokSentences);
		Pipe myPipe = trainData.getPipe();

		// train a model
		System.out.println("training model...");
		tokenizer.train(trainData, myPipe);
		tokenizer.writeModel(modelFilename);

		System.out.println("\nmodel written to: " + modelFilename);
	}

	/**
	 * tokenize documents
	 * 
	 * @param inDir
	 *            the directory with the documents to be tokenized
	 * @param outDir
	 *            the directory where the tokenized documents should be written
	 *            to
	 * @param modelFile
	 *            the model to use for tokenization
	 */
	public static void doPrediction(File inDir, File outDir,
			String modelFilename) {

		Tokenizer tokenizer = new Tokenizer();
		try {
			tokenizer.readModel(modelFilename);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// get list of all files in directory
		File[] predictOrgFiles = inDir.listFiles();

		// loop over all files
		for (int f = 0; f < predictOrgFiles.length; f++) {
			long start = System.currentTimeMillis();

			ArrayList<String> orgSentences = readFile(predictOrgFiles[f]);
			ArrayList<String> tokSentences = new ArrayList<String>();

			ArrayList<String> predictions = new ArrayList<String>();

			// force empty labels
			for (int j = 0; j < orgSentences.size(); j++)
				tokSentences.add("");

			// make prediction data
			InstanceList predData = tokenizer.makePredictionData(orgSentences,
					tokSentences);

			// predict
			for (int i = 0; i < predData.size(); i++) {
				String orgSentence = orgSentences.get(i);
				String sentenceBoundary = orgSentence.substring(orgSentence
						.length() - 1, orgSentence.length());

				Instance inst = (Instance) predData.get(i);
				ArrayList<Unit> units = null;
				units = tokenizer.predict(inst);

				// ArrayList<Unit> units = (ArrayList) inst.getName();

				String sentence = "";

				for (int j = 0; j < units.size(); j++) {
					String sp = (units.get(j).label.equals("P")) ? " " : "";
					sentence += units.get(j).rep + sp;
				}

				if (!sentence.substring(sentence.length() - 1,
						sentence.length()).equals(" "))
					sentenceBoundary = " " + sentenceBoundary;

				sentence += sentenceBoundary;
				sentence = sentence.replaceAll("[ ]+", " ");

				predictions.add(sentence);

			}

			// write predictions into file
			String fName = predictOrgFiles[f].toString();
			String newfName = fName.substring(fName.lastIndexOf("/") + 1, fName
					.length());
			File fNew = new File(outDir.toString() + "/" + newfName);
			writeFile(predictions, fNew);
			// System.out.println("\ntokenized sentences written to: " +
			// fNew.toString());

			// set all arraylists to null so that GC can get them
			orgSentences = null;
			tokSentences = null;
			predictions = null;
			predData = null;
			System.gc();

			long stop = System.currentTimeMillis();
			System.out.println("took: " + (stop - start));
		} // out loop over files

		System.out.println("Tokenized texts written to: " + outDir.toString());

	}

	/**
	 * reads in all lines of a file and writes each line as a string into an
	 * arraylist the following lines are omitted: - empty lines - those
	 * consisting of spaces only - and lines with less than 2 characters
	 * 
	 * @param myFile
	 * @return
	 * @throws IOException
	 */
	static ArrayList<String> readFile(File myFile) {
		ArrayList<String> lines = new ArrayList<String>();
		try {
			BufferedReader b = new BufferedReader(new FileReader(myFile));
			String line = "";
			while ((line = b.readLine()) != null) {
				line = line.replaceAll("[ ]+", " ");
				line = line.trim();
				if (line.length() > 1 && !line.equals(" ")) // add only if line
															// is not empty or
															// does not only
															// consist of white
															// spaces or has at
															// least 2
															// characters
					lines.add(line);

			}
			b.close();
		} catch (Exception e) {
			System.err.println("ERR: error reading file: " + myFile.toString());
			e.printStackTrace();
			System.exit(-1);
		}

		return lines;
	}

	/**
	 * writes an ArrayList of Strings to a file
	 * 
	 * @param lines
	 *            the ArrayList
	 * @param outFile
	 */
	static void writeFile(ArrayList<String> lines, File outFile) {
		try {
			FileWriter fw = new FileWriter(outFile);

			for (int i = 0; i < lines.size(); i++)
				fw.write(lines.get(i) + "\n");
			fw.close();
		} catch (Exception e) {
			System.err
					.println("ERR: error writing file: " + outFile.toString());
			e.printStackTrace();
			System.exit(-1);
		}

	}

}
