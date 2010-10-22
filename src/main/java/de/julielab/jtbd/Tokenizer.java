/** 
 * Tokenizer.java
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
 * The main class for the JULIE Token Boundary Detector. This class has all 
 * the function for training and prediction etc.
 * The following labels are used: 
 **/

package de.julielab.jtbd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFTrainerByLabelLikelihood;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.tsf.OffsetConjunctions;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.Sequence;

public class Tokenizer {

	private static final Logger LOGGER = Logger.getLogger(Tokenizer.class);

	CRF model = null;

	boolean trained = false;

	public Tokenizer() {
		LOGGER.debug("this is the JTBD constuctor");
		model = null;
		trained = false;
	}

	/**
	 * make material for training from given data
	 * 
	 * @param orgSentences
	 *            original sentence
	 * @param tokSentences
	 *            a tokenized sentence
	 * @return
	 */
	public InstanceList makeTrainingData(ArrayList<String> orgSentences, ArrayList<String> tokSentences) {

		LOGGER.debug("makeTrainingData() - making training data...");

		LabelAlphabet dict = new LabelAlphabet();
		dict.lookupLabel("P", true); // unit is a token boundary
		dict.lookupLabel("N", true); // unit is not a token boundary

		Pipe myPipe = new SerialPipes(new Pipe[] { new Sentence2TokenPipe(),
				new OffsetConjunctions(new int[][] { { -1 }, { 1 } }),
				// new PrintTokenSequenceFeatures(),
				new TokenSequence2FeatureVectorSequence(true, true) });
		InstanceList instList = new InstanceList(myPipe);

		System.out.print("preparing training data...");
		for (int i = 0; i < orgSentences.size(); i++) {

			// remove leading and trailing ws
			StringBuffer orgSentence = new StringBuffer(orgSentences.get(i).trim());
			StringBuffer tokSentence = new StringBuffer(tokSentences.get(i).trim());

			// remove last character of orgSentence if this is an EOS-symbol
			EOSSymbols E = new EOSSymbols();

			String lastChar = tokSentence.substring(tokSentence.length() - 1, tokSentence.length());
			if (E.contains(lastChar))
				tokSentence.deleteCharAt(tokSentence.length() - 1);

			lastChar = orgSentence.substring(orgSentence.length() - 1, orgSentence.length());
			if (E.contains(lastChar))
				orgSentence.deleteCharAt(orgSentence.length() - 1);

			// make instance
			instList.addThruPipe(new Instance(orgSentence.toString(), "", new Integer(i), tokSentence.toString()));
		}

		LOGGER.debug("makeTrainingData() -  number of features on training data: " + myPipe.getDataAlphabet().size());

		return instList;
	}

	/**
	 * make material for prediction
	 * 
	 * @param orgSentence
	 *            the original sentence
	 * @param tokSentence
	 *            empty string may be provided
	 * @param P
	 *            the pipe used for training
	 * @return
	 */
	public Instance makePredictionData(StringBuffer orgSentence, StringBuffer tokSentence) {
		// remove last character of orgSentence if this is an EOS-symbol
		EOSSymbols E = new EOSSymbols();

		String lastChar = "";
		if (tokSentence.length() > 0) {
			lastChar = tokSentence.substring(tokSentence.length() - 1, tokSentence.length());
			if (E.contains(lastChar))
				tokSentence.deleteCharAt(tokSentence.length() - 1);
		}

		if (orgSentence.length() > 0) {
			lastChar = orgSentence.substring(orgSentence.length() - 1, orgSentence.length());
			if (E.contains(lastChar))
				orgSentence.deleteCharAt(orgSentence.length() - 1);
		}
		Instance inst = null;
		//Logging level 'Trace' is used that is unknown to log4j versions older than 1.2.12.
		try {
			inst = model.getInputPipe().instanceFrom(
					new Instance(orgSentence.toString(), null, null, tokSentence.toString()));
		}
		catch (NoSuchMethodError e){
			e.printStackTrace();
			System.exit(0);
		}
		return inst;
	}

	/**
	 * make material for prediction from a collection of sentences
	 */
	public InstanceList makePredictionData(ArrayList<String> orgSentences, ArrayList<String> tokSentences) {

		LOGGER.debug("makePredictionData() - making prediction data");

		InstanceList predictData = new InstanceList(model.getInputPipe());
		for (int i = 0; i < orgSentences.size(); i++) {
			StringBuffer orgSentence = new StringBuffer(orgSentences.get(i));
			StringBuffer tokSentence = new StringBuffer(tokSentences.get(i));

			Instance inst = makePredictionData(orgSentence, tokSentence);
			predictData.add(inst);
		}
		return predictData;
	}

	/**
	 * do the training
	 * 
	 * @param instList
	 * @param myPipe
	 */
	public void train(InstanceList instList, Pipe myPipe) {
		long s1 = System.currentTimeMillis();

		// set up model
		model = new CRF(myPipe, null);
		model.addStatesForLabelsConnectedAsIn(instList);

		// get trainer
		CRFTrainerByLabelLikelihood crfTrainer = new CRFTrainerByLabelLikelihood(model);

		// do the training with unlimited amount of iterations
		boolean b = crfTrainer.trainOptimized(instList);
		LOGGER.info("Tokenizer training: model converged: " + b);

		long s2 = System.currentTimeMillis();

		// stop growth and set trained
		model.getInputPipe().getDataAlphabet().stopGrowth();
		trained = true;

		LOGGER.debug("train() - training time: " + (s2 - s1) / 1000 + " sec");
	}

	/**
	 * do the prediction
	 * 
	 * @param original
	 *            sentence
	 * @return an ArrayList of Unit objects containing the predicted label
	 */
	public ArrayList<Unit> predict(String sentence) {
		LOGGER.debug("predict() - before pedicting labelss ...");
		if (trained == false || model == null) {
			throw new IllegalStateException("No model available. Train or load trained model first.");
		}
		LOGGER.debug("predict() - now making pedictions ...");
		Instance inst = makePredictionData(new StringBuffer(sentence), new StringBuffer(""));
		LOGGER.debug("predict() - after pedicting labels ...");		
		return predict(inst);
	}

	/**
	 * do the prediction
	 * 
	 * @param an
	 *            instance for prediction
	 * @return an ArrayList of Unit objects containing the predicted label
	 */
	public ArrayList<Unit> predict(Instance inst) {

		if (trained == false || model == null) {
			throw new IllegalStateException("No model available. Train or load trained model first.");
		}

		ArrayList<Unit> units = (ArrayList<Unit>) inst.getName();
		if (units.size() > 0) {
			// get sequence
			Sequence input = (Sequence) inst.getData();

			// transduce and generate output
			Sequence crfOutput = model.transduce(input);
			for (int j = 0; j < crfOutput.size(); j++) {
				units.get(j).label = (String) crfOutput.get(j);
			}
		}
		return units;
	}

	/**
	 * retrieve the labels from a LabelSequence
	 * 
	 * @param ls
	 * @return
	 */
	public ArrayList<String> getLabelsFromLabelSequence(LabelSequence ls) {
		ArrayList<String> labels = new ArrayList<String>();
		for (int j = 0; j < ls.size(); j++)
			labels.add((String) ls.get(j));
		return labels;
	}

	/**
	 * show the context of c words around a error
	 * 
	 * @param i
	 * @param units
	 * @param orgLabels
	 * @return
	 */
	public String showErrorContext(int i, ArrayList<Unit> units, ArrayList<String> orgLabels) {

		final int c = 2;

		String orgContext = "";
		String newContext = "";

		for (int j = 0; j < units.size(); j++) {
			if (j >= i - c && j <= i + c) {
				String orgL = (orgLabels.get(j).equals("P")) ? " " : "";
				String newL = (units.get(j).label.equals("P")) ? " " : "";
				orgContext += units.get(j).rep + orgL;
				newContext += units.get(j).rep + newL;
			}
		}
		return newContext + "\n" + orgContext + "\n";
	}

	/**
	 * Save the model learned to disk. THis is done via Java's object serialization.
	 * 
	 * @param filename
	 *            where to write it (full path!)
	 */
	public void writeModel(String filename) {
		if (trained == false || model == null) {
			throw new IllegalStateException("train or load trained model first.");
		}
		try {
			FileOutputStream fos = new FileOutputStream(new File(filename + ".gz"));
			GZIPOutputStream gout = new GZIPOutputStream(fos);
			ObjectOutputStream oos = new ObjectOutputStream(gout);
			oos.writeObject(this.model);
			oos.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	/**
	 * load a previously trained FeatureSubsetModel (CRF4+Properties) which was stored as serialized
	 * object to disk.
	 * 
	 * @param filename
	 *            where to find the serialized featureSubsetModel (full path!)
	 */
	public void readModel(String filename) throws IOException, FileNotFoundException, ClassNotFoundException {
		FileInputStream fis = new FileInputStream(new File(filename));
		GZIPInputStream gin = new GZIPInputStream(fis);
		ObjectInputStream ois = new ObjectInputStream(gin);
		model = (CRF) ois.readObject();
		trained = true;
		model.getInputPipe().getDataAlphabet().stopGrowth();
	}

	public CRF getModel() {
		return model;
	}

	void setModel(CRF crf) {
		trained = true;
		this.model = crf;
	}

}
