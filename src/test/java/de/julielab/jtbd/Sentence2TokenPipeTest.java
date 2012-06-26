/** 
 * Sentence2TokenPipeTest.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: //TODO insert current version number 	
 * Since version:   //TODO insert version number of first appearance of this class
 *
 * Creation date: Sep 6, 2007 
 * 
 * //TODO insert short description
 **/

package de.julielab.jtbd;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sentence2TokenPipeTest extends TestCase{

	private static final Logger LOGGER = LoggerFactory.getLogger(Sentence2TokenPipeTest.class);
	
	private static final String TEST_SENTENCE = "this is   a \t junit -test";
	
	public void testMakeLabel() {
		ArrayList<String> expectedLabels = new ArrayList<String>();
		expectedLabels.add("P");
		expectedLabels.add("P");
		expectedLabels.add("P");
		expectedLabels.add("P");
		expectedLabels.add("N");
		expectedLabels.add("N");
		
		Sentence2TokenPipe p = new Sentence2TokenPipe();
		ArrayList<String> testLabels = p.makeLabels(TEST_SENTENCE);

		LOGGER.debug("testMakeLabel() - expected labels: " + expectedLabels);
		LOGGER.debug("testMakeLabel() - created labels: " + testLabels);
		
		// check labels
		boolean allOK = true;
		for (int i = 0; i < testLabels.size(); i++) {
			if (!testLabels.get(i).equals(expectedLabels.get(i))) {
				allOK = false;
				break;
			}
		}
		assertTrue(allOK);
	}
	
	
	
	public void testMakeUnits() {
		ArrayList<String> expectedUnits = new ArrayList<String>();
		expectedUnits.add("this");
		expectedUnits.add("is");
		expectedUnits.add("a");
		expectedUnits.add("junit");
		expectedUnits.add("-");
		expectedUnits.add("test");
		
		
		Sentence2TokenPipe p = new Sentence2TokenPipe();
 		
		ArrayList<Unit> testUnits = new ArrayList<Unit>();
		ArrayList<String> wSpaces = new ArrayList<String>();
		p.makeUnits(TEST_SENTENCE, testUnits, wSpaces);
		
		// now check whether expected units are created
		LOGGER.debug("testMakeUnits() - excepted units: " + expectedUnits.toString());
		LOGGER.debug("testMakeUnits() - created units: " + testUnits.toString());
		boolean allOK = true;
		for (int i = 0; i < testUnits.size(); i++) {
			if (!testUnits.get(i).rep.equals(expectedUnits.get(i))) {
				allOK = false;
				break;
			}
		}
		
		assertTrue(allOK);

	}
}
