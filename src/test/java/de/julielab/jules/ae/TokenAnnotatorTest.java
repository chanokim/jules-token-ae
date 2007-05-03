/** 
 * TokenAnnotatorTest.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 1.2	
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

import com.ibm.uima.UIMAFramework;
import com.ibm.uima.analysis_engine.AnalysisEngine;
import com.ibm.uima.jcas.JFSIndexRepository;
import com.ibm.uima.jcas.impl.JCas;
import com.ibm.uima.resource.ResourceInitializationException;
import com.ibm.uima.resource.ResourceSpecifier;
import com.ibm.uima.util.XMLInputSource;

import de.julielab.jules.types.Sentence;
import de.julielab.jules.types.Token;

import junit.framework.TestCase;

public class TokenAnnotatorTest extends TestCase {

	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger
			.getLogger(TokenAnnotatorTest.class);

	
	final private String DESCRIPTOR = "src/test/resources/TokenAnnotatorTest.xml";

	String offsets = "0-4;4-5;6-8;9-12;13-18;18-19;20-22;23-24;25-28;28-29;30-34;34-35;35-42;43-48;49-50;50-53;53-54;54-55" ;

	protected void setUp() throws Exception {
		super.setUp();
		// set log4j properties file
		PropertyConfigurator.configure("src/test/java/log4j.properties");
	}
	
	public void initCas(JCas jcas) {
		jcas.reset();
		jcas.setDocumentText("CD44, at any stage, is a XYZ! CD44-related stuff (not).");
		Sentence s1 = new Sentence(jcas);
		s1.setBegin(0);
		s1.setEnd(29);
		s1.addToIndexes();
		
		Sentence s2 = new Sentence(jcas);
		s2.setBegin(30);
		s2.setEnd(55);
		s2.addToIndexes();
	}
	
	public void testProcess() {
	
		boolean annotationsOK = true;

		XMLInputSource tokenXML = null;
		ResourceSpecifier tokenSpec = null;
		AnalysisEngine tokenAnnotator = null;

		try {
			tokenXML = new XMLInputSource(DESCRIPTOR);
			tokenSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(
					tokenXML);
			tokenAnnotator = UIMAFramework
					.produceAnalysisEngine(tokenSpec);
		} catch (Exception e) {
			logger.error("testProcess()", e); //$NON-NLS-1$
		}


			JCas jcas = null;
			try {
				jcas = tokenAnnotator.newJCas();
			} catch (ResourceInitializationException e) {
				logger.error("testProcess()", e); //$NON-NLS-1$
			}

			// get test cas with sentence annotation
			initCas(jcas);
			
			try {
				tokenAnnotator.process(jcas, null);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// get the offsets of the sentences
			JFSIndexRepository indexes = jcas.getJFSIndexRepository();
			Iterator tokIter = indexes.getAnnotationIndex(Token.type)
					.iterator();

			String predictedOffsets = "";

			while (tokIter.hasNext()) {
				Token t = (Token) tokIter.next();
				if (logger.isDebugEnabled()) {
					logger.debug("OUT: " + t.getCoveredText() + ": " + t.getBegin()
						+ " - " + t.getEnd());
				}
				predictedOffsets += (predictedOffsets.length() > 0) ? ";" : "";
				predictedOffsets += t.getBegin() + "-" + t.getEnd();
			}

			if (logger.isDebugEnabled()) {
				logger.debug("testProcess() - predicted: " + predictedOffsets);
				logger.debug("testProcess() -    wanted: " + offsets);
			}


			// compare offsets
			if (!predictedOffsets.equals(offsets)) {
				annotationsOK = false;
			}


		assertTrue(annotationsOK);

	}

}
