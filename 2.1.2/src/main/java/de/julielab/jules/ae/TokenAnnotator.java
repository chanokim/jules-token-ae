/** 
 * TokenAnnotatorTest.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 2.0
 * Since version:   1.0
 *
 * Creation date: Nov 29, 2006 
 * 
 * This is an UIMA wrapper for the JULIE Token Boundary Detector (JTBD). It produces token annotations, 
 * given sentence annotations. Each sentence is seperately split into its single tokens.
 * 
 * 
 * TODO: double-check whether last symbol is always correctly tokenized!
 **/

package de.julielab.jules.ae;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.julielab.jtbd.EOSSymbols;
import de.julielab.jtbd.JTBDException;
import de.julielab.jtbd.Tokenizer;
import de.julielab.jtbd.Unit;
import de.julielab.jules.types.Sentence;
import de.julielab.jules.types.Token;

public class TokenAnnotator extends JCasAnnotator_ImplBase {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(TokenAnnotator.class);

	private static final String COMPONENT_ID = "JULIE Token Boundary Detector";

	private Tokenizer tokenizer;

	/**
	 * Initialisiation of JTBD: load the model
	 * 
	 * @parm aContext the parameters in the descriptor
	 */
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {

		LOGGER.info("initialize() - JTBD initializing...");

		// invoke default initialization
		super.initialize(aContext);

		String modelFilename = "";

		// get modelfilename from parameters
		modelFilename = (String) aContext
				.getConfigParameterValue("ModelFilename");

		// load model
		tokenizer = new Tokenizer();
		try {
			tokenizer.readModel(modelFilename);
		} catch (Exception e) {
			LOGGER.error("initialize() - Could not load tokenizer model: "
					+ e.getMessage());
			throw new ResourceInitializationException();
		}
	}

	/**
	 * the process method is in charge of doing the tokenization
	 */
	public void process(JCas aJCas) {

		// get all sentences
		JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
		Iterator sentenceIter = indexes.getAnnotationIndex(Sentence.type)
				.iterator();

		int sentOffset = 0;

		while (sentenceIter.hasNext()) {
			Sentence sentence = (Sentence) sentenceIter.next();

			int len = sentence.getEnd() - sentence.getBegin();

			if (len <= 1 || sentence.getCoveredText().equals("")) {
				// skip empty sentence
				continue;
			}

			ArrayList<Unit> units;
			try {
				units = tokenizer.predict(sentence.getCoveredText());
				sentOffset = sentence.getBegin();
				
				if (units == null || units.size() == 0) {
					// ignore this sentence as it has no predictions!
					LOGGER.warn("writeToCAS() - current sentence was not handled by JTBD: " + sentence.getCoveredText());
				} else {
					writeToCAS(aJCas, units, sentOffset);
					handleLastCharacter(aJCas, sentence);
				}

			} catch (JTBDException e) {
				// if there is an exception during predicting the current
				// sentence
				// throw an error and omit this sentence!
				LOGGER
						.error("process() - Error while predicting with JTBD. Sentence omitted!\n"
								+ e.getMessage());
			}

		}
	}

	/**
	 * Writes tokens identified to CAS by interpreting the Unit objects. JTBD
	 * splits each sentence into several units (see Medinfo paper) and decides
	 * for each such unit whether it is at the end of a token or not (label "N"
	 * means: not at the end).
	 * 
	 * @param aJCas
	 *            The Cas that is filled.
	 * @param sentOffset
	 *            Begin offset of the current sentence.
	 * @param units
	 *            Unit objects within this sentence.
	 */

	private void writeToCAS(JCas aJCas, ArrayList<Unit> units, int sentOffset) {

		int begin = 0;

		for (int i = 0; i < units.size(); i++) {
			Unit unit = units.get(i);
			if (begin == 0) {
				begin = unit.begin + sentOffset;
			}
			if (!units.get(i).label.equals("N")) {
				// reached end of token
				int end = unit.end + sentOffset;
				Token annotation = new Token(aJCas);
				annotation.setBegin(begin);
				annotation.setEnd(end);
				annotation.setComponentId("JULIE Token Boundary Detector");
				annotation.addToIndexes();
				begin = 0;
			}
		}

	}

	/**
	 * Write last character of a sentence as separate token (if it is a known
	 * end-of-sentence symbol).
	 * 
	 * @param aJCas
	 *            The Cas that will contain the token.
	 * @param sentence
	 *            The current sentence.
	 */
	private void handleLastCharacter(JCas aJCas, Sentence sentence) {

		String sentText = sentence.getCoveredText();

		EOSSymbols eosSymbols = new EOSSymbols();

		if (sentText.length() > 1) {
			String lastChar = sentText.substring(sentText.length() - 1,
					sentText.length());
			if (eosSymbols.contains(lastChar)) {
				// annotate it as separate token
				Token annotation = new Token(aJCas);
				annotation.setBegin(sentence.getEnd() - 1);
				annotation.setEnd(sentence.getEnd());
				annotation.setComponentId(COMPONENT_ID);
				annotation.addToIndexes();
			}
		}
	}

}
