/** 
 * TokenAnnotatorTest.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 2.2.3
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
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
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
	
	private static final String USE_DOC_TEXT_PARAM = "UseDocText";

	private Tokenizer tokenizer;
	
	boolean useCompleteDocText = false;

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
		Object useDocTextParam = aContext.getConfigParameterValue(USE_DOC_TEXT_PARAM);		
		if (useDocTextParam != null ){
			useCompleteDocText = (Boolean) useDocTextParam;
		}		
		if (useCompleteDocText){ 
			LOGGER.info("initialize() - whole documentText is tokenized");
		}
		else LOGGER.info("initialize() - will tokenize only text covered by sentence annotations");

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
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		
		LOGGER.debug("process() - starting processing document" );
		
		//count tokens (token number will be used as token id)
		int tokenNumber = 1;

		// if useCompleteDocText is true, tokenize complete documentText
		if (useCompleteDocText){			
			LOGGER.debug("process() - tokenizing whole document text!");		
			String text = aJCas.getDocumentText();	
			String c = text.substring(text.length()-1);
			//prevent that ')' and ']' as last character are treated as end-of-sentence symbols
			if (c.equals(")") || c.equals("]"))
				text += " ";
			tokenNumber = writeTokensToCAS(text, 0, aJCas, tokenNumber);
		}	
		// if useCompleteDocText is false, tokenize sentence per sentence
		else {
			JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
			Iterator sentenceIter = indexes.getAnnotationIndex(Sentence.type).iterator();
			int sentOffset = 0;
			while (sentenceIter.hasNext()) {
				Sentence sentence = (Sentence) sentenceIter.next();					
				LOGGER.debug("process() - going to next sentence having length: " + (sentence.getEnd() - sentence.getBegin()));
				String text = sentence.getCoveredText();
				tokenNumber = writeTokensToCAS(text, sentence.getBegin(), aJCas, tokenNumber);
			}
		}		
	}
	
	/**
	 * Tokenize non empty input and write tokens to CAS by interpreting the Unit objects. JTBD splits each sentence
	 * into several units (see Tomanek et al. Medinfo 2007 paper) and decides for each such unit whether it is at the
	 * end of a token or not (label "N" means: not at the end, "P": at the end). Makes an exta token for terminal end
	 * of sentence symbols.
	 * @param text
	 * @param offset
	 * @param aJCas
	 * @param tokenNumber
	 * @return
	 * @throws AnalysisEngineProcessException
	 */
	private int writeTokensToCAS(String text, int offset, JCas aJCas, int tokenNumber) throws AnalysisEngineProcessException {	
		
		EOSSymbols eosSymbols = new EOSSymbols();
		
		//some debugging: skip empty input text
		if (text == null)
			LOGGER.debug("writeTokensToCAS() - input for JTBD tokenizer is null!"); 			
		else if (text.isEmpty() || eosSymbols.contains(text)) 
			LOGGER.debug("writeTokensToCAS() - input for JTBD is empty or is an end of sentence symbol");	
		else {
			LOGGER.debug("writeTokensToCAS() - tokenizing input: " + text);	
			//predict units
			ArrayList<Unit> units = tokenizer.predict(text);
			// ignore text that has no predictions!
			if (units == null || units.size() == 0)
				LOGGER.warn("writeTokensToCAS() - no units found by JTBD for: " + text);
			else {
				int begin = 0;		
				boolean startNewToken = true;		
				//iterate through units, write a token whenever a unit with label 'P' signals the end of a token
				for (int i = 0; i < units.size(); i++) {
					Unit unit = units.get(i);
					//get start of the new token
					if (startNewToken) { 
						begin = unit.begin + offset;
					}
					if (units.get(i).label.equals("N") ) {
						startNewToken = false;
						//last unit is expected to have label 'P' (otherwise this indicates an error in JTBD)
						if ((i+1) == units.size()){
							LOGGER.error("writeTokensToCAS() - last unit has label 'N', should have label 'P'");
							throw new AnalysisEngineProcessException();
						}
					}
					// write token when end of token (unit with label 'P') is reached
					else if (units.get(i).label.equals("P")) { 
						int end = unit.end + offset;
						Token annotation = new Token(aJCas);
						annotation.setBegin(begin);
						annotation.setEnd(end);
						annotation.setId("" + tokenNumber);
						annotation.setComponentId("JULIE Token Boundary Detector");
						annotation.addToIndexes();
						LOGGER.debug("writeTokensToCAS() - created token: " + aJCas.getDocumentText().
								substring(begin, end) + " " + begin + " - " + end);
						startNewToken = true;
						tokenNumber++;
					}
					else {
						LOGGER.error("writeTokensToCAS() - only 'N' and 'P' are allowed as unit labels, " +
								"but found '" + units.get(i).label);
						throw new AnalysisEngineProcessException();
					}
				}
				tokenNumber =  handleLastCharacter(aJCas, text, offset, tokenNumber);
			}
		}
		return tokenNumber;
	}

	/**
	 * Write last character of a sentence as separate token (if it is a known end-of-sentence
	 * symbol). 
	 * 
	 * @param aJCas
	 *            The CAS that will contain the token.
	 * @param text
	 *            The current sentence text.
	 */
	private int handleLastCharacter(JCas aJCas, String text, int offset, int tokenNumber) {

		EOSSymbols eosSymbols = new EOSSymbols();

		if (text.length() > 1) {
			String lastChar = text.substring(text.length() - 1, text.length());
			
			if (eosSymbols.contains(lastChar)) {
				// annotate it as separate token
				int start = offset + text.length() - 1;
				int end = offset + text.length();
				Token annotation = new Token(aJCas);
				annotation.setBegin(start);
				annotation.setEnd(end);
				annotation.setId("" + tokenNumber);
				annotation.setComponentId(COMPONENT_ID);
				annotation.addToIndexes();
				tokenNumber++;
				LOGGER.debug("handleLastCharacter() - created token: " + lastChar + 
						" " + start + " - " + end);
			}
		}
		return tokenNumber;
	}

}
