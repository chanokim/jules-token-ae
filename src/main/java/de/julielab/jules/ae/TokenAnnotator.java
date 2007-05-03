/** 
 * TokenAnnotatorTest.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 1.1 	
 * Since version:   1.0
 *
 * Creation date: Nov 29, 2006 
 * 
 * This is an UIMA wrapper for the JULIE Token Boundary Detector (JTBD). It produces token annotations, 
 * given sentence annotations. Each sentence is seperately split into its single tokens.
 * 
 * 
 * TODO: someday: check wether last symbol is always correctly tokenized!
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
	private static final Logger logger = Logger
			.getLogger(TokenAnnotator.class);
	
	private Tokenizer tokenizer;

	/**
	 * initialisiation of JSBD: load the model
	 * 
	 * @parm aContext the parameters in the descriptor
	 */
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {

		logger.info("[JTBD] initializing...");

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
			logger.error("[JSBD] Could not load tokenizer model: "
					+ e.getMessage());
			throw new ResourceInitializationException();
		}
	}

	
	public void process(JCas aJCas) {

		logger.info("[JTBD] processing document...");

		// get all sentences
		JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
		Iterator sentenceIter = indexes.getAnnotationIndex(Sentence.type)
				.iterator();

		int sentOffset = 0;

		while (sentenceIter.hasNext()) {
			Sentence sentence = (Sentence) sentenceIter.next();

			int len = sentence.getEnd() - sentence.getBegin();
			if (len <= 1 || sentence.getCoveredText().equals("")) { // skip
																	// empty
																	// sentences
				continue;
			}

			ArrayList<Unit> units;
			try {
				units = tokenizer.predict(sentence.getCoveredText());
			} catch (JTBDException e) {
				logger.error("[JTBD] Error while predicting with JTBD: "
						+ e.getMessage());
				throw new RuntimeException();
			}

			// write the tokens found to CAS
			sentOffset = sentence.getBegin();
			writeToCAS(aJCas, units, sentOffset);

			// handle last char of sentence:
			handleLastCharacter(aJCas, sentence);

		} 

	}


	/**
	 * Writes tokens identified to CAS by interpreting the Unit objects. JTBD splits 
	 * each sentence into several units (see Medinfo paper) and decides for each such
	 * unit whether it is at the end of a token or not (label "N" means: not at the end).
	 * 
	 * @param aJCas
	 * @param sentOffset
	 *            begin offset of the current sentence
	 * @param units
	 *            Unit objects within this sentence
	 * @param begin begin offset of the current unit (relative to the current sentence only)
	 * @return
	 */

	private void writeToCAS(JCas aJCas, ArrayList<Unit> units, int sentOffset) {

		int begin = 0;

		for (int i = 0; i < units.size(); i++) {
			Unit unit = units.get(i);
			if (begin == 0) {
				begin = unit.begin + sentOffset;
			}
			if (units.get(i).label.equals("N")) {
				// we are still inside the token (do nothing)
			} else {
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
	 * @param sentence the current sentence
	 */
	private void handleLastCharacter(JCas aJCas, Sentence sentence) {

		String sentText = sentence.getCoveredText();
		EOSSymbols E = new EOSSymbols();

		if (sentText.length() > 1) {
			String lastChar = sentText.substring(sentText.length() - 1,
					sentText.length());
			if (E.contains(lastChar)) {
				// annotate it as separate token
				Token annotation = new Token(aJCas);
				annotation.setBegin(sentence.getEnd() - 1);
				annotation.setEnd(sentence.getEnd());
				annotation.addToIndexes();
			}

		}
	}
	
	
}
