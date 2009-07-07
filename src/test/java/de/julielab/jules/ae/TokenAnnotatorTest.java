/** 
 * TokenAnnotatorTest.java
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
 * Creation date: Nov 29, 2006 
 * 
 * This is a JUnit test for the TokenAnnotator.
 **/

package de.julielab.jules.ae;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;

import de.julielab.jules.types.Sentence;
import de.julielab.jules.types.Token;

import junit.framework.TestCase;

public class TokenAnnotatorTest extends TestCase {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger
			.getLogger(TokenAnnotatorTest.class);

	private static final String DESCRIPTOR = "src/test/resources/TokenAnnotatorTest.xml";

	private static final String TEST_TEXT = "CD44, at any stage, is a XYZ! CD44-related stuff \t(not).";

	private static final String TEST_TEXT_OFFSETS = "0-4;4-5;6-8;9-12;13-18;18-19;20-22;23-24;25-28;28-29;30-34;34-35;35-42;43-48;50-51;51-54;54-55";

	private static final String TEST_TEXT_TOKEN_NUMBERS = "1;2;3;4;5;6;7;8;9;10;11;12;13;14;15;16;17";
	
	protected void setUp() throws Exception {
		super.setUp();
		// set log4j properties file
		PropertyConfigurator.configure("src/test/resources/log4j.properties");
	}

	/**
	 * initialize a CAS which is then used for the test. 2 sentences are added
	 */
	public void initCas(JCas jcas) {
		jcas.reset();
		jcas.setDocumentText(TEST_TEXT);
		
		Sentence s1 = new Sentence(jcas);
		s1.setBegin(0);
		s1.setEnd(29);
		s1.addToIndexes();

		Sentence s2 = new Sentence(jcas);
		s2.setBegin(30);
		s2.setEnd(55);
		s2.addToIndexes();
	}
	
	/**
	 * initialize a CAS which is then used for the test, the CAS holds no token annotations
	 */
	public void initCasWithoutTokens(JCas jcas) {
		jcas.reset();
		jcas.setDocumentText(TEST_TEXT);
	}

	/**
	 * Test CAS with sentence annotations.
	 */
	public void testProcess() {

		XMLInputSource tokenXML = null;
		ResourceSpecifier tokenSpec = null;
		AnalysisEngine tokenAnnotator = null;

		try {
			tokenXML = new XMLInputSource(DESCRIPTOR);
			tokenSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(
					tokenXML);
			tokenAnnotator = UIMAFramework.produceAnalysisEngine(tokenSpec);
		} catch (Exception e) {
			LOGGER.error("testProcess()", e);
		}

		JCas jcas = null;
		try {
			jcas = tokenAnnotator.newJCas();
		} catch (ResourceInitializationException e) {
			LOGGER.error("testProcess()", e);
		}
		initCas(jcas);
		try {
			tokenAnnotator.process(jcas, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// get the offsets of the sentences
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator tokIter = indexes.getAnnotationIndex(Token.type).iterator();
		String predictedOffsets = getPredictedOffsets(tokIter);
		tokIter = indexes.getAnnotationIndex(Token.type).iterator();
		String tokenNumbers = getTokenNumbers(tokIter);
		
		// compare offsets
		assertEquals(TEST_TEXT_OFFSETS, predictedOffsets);
		
		// compare token numbers
		assertEquals(TEST_TEXT_TOKEN_NUMBERS, tokenNumbers);
	}
	
	/**
	 * Test CAS without sentence annotations.
	 */
	public void testProcessWithoutSentenceAnnotations() {

		XMLInputSource tokenXML = null;
		ResourceSpecifier tokenSpec = null;
		AnalysisEngine tokenAnnotator = null;

		try {
			tokenXML = new XMLInputSource(DESCRIPTOR);
			tokenSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(
					tokenXML);
			tokenAnnotator = UIMAFramework.produceAnalysisEngine(tokenSpec);
		} catch (Exception e) {
			LOGGER.error("testProcess()", e);
		}
		
		JCas jcas = null;
		try {
			jcas = tokenAnnotator.newJCas();
		} catch (ResourceInitializationException e) {
			LOGGER.error("testProcess()", e);
		}
		initCasWithoutTokens(jcas);
		try {
			tokenAnnotator.process(jcas, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// get the predicted token offsets 
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator tokIter = indexes.getAnnotationIndex(Token.type).iterator();
		String predictedOffsets = getPredictedOffsets(tokIter);
		tokIter = indexes.getAnnotationIndex(Token.type).iterator();
		String tokenNumbers = getTokenNumbers(tokIter);

		// compare offsets
		assertEquals(TEST_TEXT_OFFSETS, predictedOffsets);
		
		// compare token numbers
		assertEquals(TEST_TEXT_TOKEN_NUMBERS, tokenNumbers);
		
	}

	private String getPredictedOffsets(Iterator tokIter) {
		String predictedOffsets="";
		while (tokIter.hasNext()) {
			Token t = (Token) tokIter.next();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("OUT: " + t.getCoveredText() + ": " + t.getBegin()
						+ " - " + t.getEnd());
			}
			predictedOffsets += (predictedOffsets.length() > 0) ? ";" : "";
			predictedOffsets += t.getBegin() + "-" + t.getEnd();
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("testProcess() - predicted: " + predictedOffsets);
			LOGGER.debug("testProcess() -    wanted: " + TEST_TEXT_OFFSETS);
		}
		return predictedOffsets;
	}
	
	private String getTokenNumbers(Iterator tokIter) {
		String tokenNumbers="";
		while (tokIter.hasNext()) {
			Token t = (Token) tokIter.next();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("OUT: " + t.getCoveredText() + ": " + t.getId());
			}
			tokenNumbers += (tokenNumbers.length() > 0) ? ";" : "";
			tokenNumbers += t.getId();
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("testProcess() - predicted: " + tokenNumbers);
			LOGGER.debug("testProcess() -    wanted: " + TEST_TEXT_TOKEN_NUMBERS);
		}
		return tokenNumbers;
	}

}
