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
 * given sentence annotations. Each sentence is separately split into its single tokens. It 
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
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceInitializationException;

import de.julielab.jtbd.EOSSymbols;
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
	
	private static final String USE_DOC_TEXT_PARAM = "UseDocTextIfNoSentenceIsFound";
	
	private boolean useCompleteDocText = false;

	private Tokenizer tokenizer;

	/**
	 * Initialisiation of JTBD: load the model
	 * 
	 * @parm aContext the parameters in the descriptor
	 */
	public void initialize(UimaContext aContext) throws ResourceInitializationException {

		LOGGER.info("initialize() - JTBD initializing...");

		// invoke default initialization
		super.initialize(aContext);

		String modelFilename = "";

		// get modelfilename from parameters
		modelFilename = (String) aContext.getConfigParameterValue("ModelFilename");
		
		// define if sentence annotations should be taken into account
		
		useCompleteDocText = (Boolean) aContext.getConfigParameterValue(USE_DOC_TEXT_PARAM);
		if (useCompleteDocText){
			LOGGER.info("initialize() - whole documentText is used, if no sentence annotations are found.");
			LOGGER.info(" ... terminal 'end-of-sentence' characters as specified in jtbd.EOSSymbols are " +
					"not considered during tokenization." );
		}

		// load model
		tokenizer = new Tokenizer();
		try {
			tokenizer.readModel(modelFilename);
		} catch (Exception e) {
			LOGGER.error("initialize() - Could not load tokenizer model: " + e.getMessage());
			throw new ResourceInitializationException();
		}
	}

	/**
	 * the process method is in charge of doing the tokenization
	 */
	public void process(JCas aJCas) {

		// get all sentences
		JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
		Iterator sentenceIter = indexes.getAnnotationIndex(Sentence.type).iterator();

		
		// if no sentence annotation is found and useCompleteDocText is true, tokenize complete documentText
		if (!sentenceIter.hasNext() && useCompleteDocText){			
			LOGGER.debug("process() - no sentence annotations found, tokenizing whole document text!");		
			String docText = aJCas.getDocumentText();			
			if (docText != null && !docText.isEmpty()) {			
				int len = aJCas.getDocumentText().length(); //TODO test				
				ArrayList<Unit> units;
				units = tokenizer.predict(docText);
				if (units == null || units.size() == 0) {
					// ignore this documentText as it has no predictions!
					LOGGER.warn("writeToCAS() - documentText was not handled by JTBD: " + docText);
				} else {
					writeToCAS(aJCas, units, 0);
				}				
			} else {
				LOGGER.info("process() - could not tokenize, empty document text");
			}
		}
		else {
			int sentOffset = 0;
			while (sentenceIter.hasNext()) {

				Sentence sentence = (Sentence) sentenceIter.next();
	
				int len = sentence.getEnd() - sentence.getBegin();
				
				/*
				 * some debugging...
				 */
				LOGGER.debug("process() - going to next sentence having length: " + len);
				String text = sentence.getCoveredText();
				if (text==null) {
					LOGGER.debug("process() - current sentence with length " + len + " has NO COVERED TEXT!");
				} else {
					LOGGER.debug("process() - sentence text: : " + sentence.getCoveredText());
				}
				
				// we wanna skip empty sentence
				if (len <= 1 || sentence.getCoveredText().equals("")) {
	
					continue;
				}
	
				ArrayList<Unit> units;
	
				units = tokenizer.predict(sentence.getCoveredText());
				sentOffset = sentence.getBegin();
	
				if (units == null || units.size() == 0) {
					// ignore this sentence as it has no predictions!
					LOGGER.warn("writeToCAS() - current sentence was not handled by JTBD: " + sentence.getCoveredText());
				} else {
					writeToCAS(aJCas, units, sentOffset);
					handleLastCharacter(aJCas, sentence);
				}
			}
		}		
	}

	/**
	 * Writes tokens identified to CAS by interpreting the Unit objects. JTBD splits each sentence
	 * into several units (see Medinfo paper) and decides for each such unit whether it is at the
	 * end of a token or not (label "N" means: not at the end).
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
			System.out.println(unit.begin + "-" + unit.end);
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
				System.out.println("setting " + begin + "-" + end);//TODO remove
				begin = 0;
			}
		}

	}

	/**
	 * Write last character of a sentence as separate token (if it is a known end-of-sentence
	 * symbol).
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
			String lastChar = sentText.substring(sentText.length() - 1, sentText.length());
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
