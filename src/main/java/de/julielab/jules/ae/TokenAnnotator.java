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
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uimafit.descriptor.ConfigurationParameter;

import de.julielab.jtbd.EOSSymbols;
import de.julielab.jtbd.Tokenizer;
import de.julielab.jtbd.Unit;
import de.julielab.jules.types.Sentence;
import de.julielab.jules.types.Token;

public class TokenAnnotator extends JCasAnnotator_ImplBase {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(TokenAnnotator.class);

	public static final String PARAM_MODEL = "ModelFilename";
	
	private static final String COMPONENT_ID = "JULIE Token Boundary Detector";
	
	private static final String USE_DOC_TEXT_PARAM = "UseDocText";

	private Tokenizer tokenizer;
		
	private static boolean useCompleteDocText = false;
	
	private static EOSSymbols eosSymbols = new EOSSymbols();

	private int tokenNumber; //used as token ID

	@ConfigurationParameter(name=PARAM_MODEL,mandatory=true,description="Path to the tokenizer model.")
	private String modelFilename;

	/**
	 * Initialisiation of JTBD: load the model
	 * 
	 * @parm aContext the parameters in the descriptor
	 */
	public void initialize(UimaContext aContext) throws ResourceInitializationException {

		LOGGER.info("initialize() - JTBD initializing...");

		// invoke default initialization
		super.initialize(aContext);

		// get model file name from parameters
		modelFilename = (String) aContext.getConfigParameterValue(PARAM_MODEL);
		
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
		
		tokenNumber = 1;

		// if useCompleteDocText is true, tokenize complete documentText
		if (useCompleteDocText){			
			LOGGER.debug("process() - tokenizing whole document text!");		
			String text = aJCas.getDocumentText();	
			writeTokensToCAS(text, 0, aJCas);
		}	
		// if useCompleteDocText is false, tokenize sentence per sentence
		else {
			JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
			Iterator sentenceIter = indexes.getAnnotationIndex(Sentence.type).iterator();
			while (sentenceIter.hasNext()) {
				Sentence sentence = (Sentence) sentenceIter.next();					
				LOGGER.debug("process() - going to next sentence having length: " + (sentence.getEnd() - sentence.getBegin()));
				String text = sentence.getCoveredText();
				writeTokensToCAS(text, sentence.getBegin(), aJCas);
			}
		}		
	}
	
	/**
	 * Tokenize non empty input and write tokens to CAS by interpreting the Unit objects. JTBD splits each sentence
	 * into several units (see Tomanek et al. Medinfo 2007 paper) and decides for each such unit whether it is at the
	 * end of a token or not (label "N" means: not at the end, "P": at the end). Makes an extra token for terminal end
	 * of sentence symbols.
	 * @param text
	 * @param offset
	 * @param aJCas
	 * @return
	 * @throws AnalysisEngineProcessException
	 */
	private void writeTokensToCAS(String text, int offset, JCas aJCas) throws AnalysisEngineProcessException {	
		
		//skip empty input text
		if (text == null || text.isEmpty()){
			LOGGER.debug("writeTokensToCAS() - input for JTBD tokenizer is null or empty!"); 		
		}
		else {
			//if input text is not a single EOS 
			if (text.length() > 1 || !eosSymbols.contains(text.charAt(text.length() - 1))) {	
				LOGGER.debug("writeTokensToCAS() - tokenizing input: " + text);
				
				//predict units
				List<Unit> units = tokenizer.predict(text);	
				
				LOGGER.debug("+++predition done!++++");
				
				// throw error if no units could be predicted
				if (units == null || units.size() == 0){
					LOGGER.error("writeTokensToCAS() - no units found by JTBD for: " + text);
					throw new AnalysisEngineProcessException();
				}
				
				int begin = 0;
				int end = 0;
				boolean startNewToken = true;					
				//iterate through units, write a token whenever a unit with label 'P' signals the end of a token
				//note that no unit exists for terminal EOS  in input text!
				for (Unit unit : units) {					
					if (startNewToken) { 
						begin = unit.begin + offset;
					}
					end = unit.end + offset;					
					if (unit.label.equals("N") ) {
						startNewToken = false;
					}
					// write token if 'end of token' (unit with label 'P') is reached
					else if (unit.label.equals("P")) { 
						createToken(aJCas, begin, end);
						startNewToken = true;
					}
					else {
						LOGGER.error("writeTokensToCAS() - found unit label '" + unit.label + "' (only 'N' and 'P' are allowed");
						throw new AnalysisEngineProcessException();
					}
				}
				//This case (last unit had label 'N') should not happen. Analysis of JTBD is pending. 
				if (!startNewToken){
					createToken(aJCas, begin, end);
					LOGGER.debug("writeTokensToCAS() - found terminal unit with label 'N' (expected 'P'). Check behaviour of JTBD! Token text: " +
					aJCas.getDocumentText().subSequence(begin, end));
					//throw new AnalysisEngineProcessException();	
				}
			}
			//if last character of a sentence is a EOS, make it a separate token 
			Character lastChar = text.charAt(text.length() - 1);	
			if (eosSymbols.contains(lastChar)) {
				int start = offset + text.length() - 1;
				int end = offset + text.length();
				createToken(aJCas, start, end);
			}
		}

	}
	
	private void createToken(JCas jcas, int begin, int end){
		Token annotation = new Token(jcas);
		annotation.setBegin(begin);
		annotation.setEnd(end);
		annotation.setId("" + tokenNumber);
		annotation.setComponentId(COMPONENT_ID);
		annotation.addToIndexes();
		LOGGER.debug("createToken() - created token: " + jcas.getDocumentText().
				substring(begin, end) + " " + begin + " - " + end);
		tokenNumber++;
	}


}
