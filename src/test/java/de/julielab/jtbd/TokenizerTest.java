/** 
 * Tokenizer.java
 * 
 * Copyright (c) 2008, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: muehlhausen
 * 
 * Current version: 2.0
 * Since version:   1.6
 *
 * Creation date: 14.10.2008 
 **/

package de.julielab.jtbd;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.junit.Test;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

/**
 * Test for the class {@link Tokenizer}
 * 
 * @author tomanek
 */
public class TokenizerTest {

	private static final Logger LOGGER = Logger.getLogger(TokenizerTest.class);

	private static final String FILENAME_MODEL = "src/test/resources/JTBD-2.0-biomed.mod.gz";
	private static final String FILENAME_TRAIN_DATA_ORG = "src/test/resources/testdata/train/train.sent";
	private static final String FILENAME_TRAIN_DATA_TOK = "src/test/resources/testdata/train/train.tok";
	private static final String FILENAME_TRAIN_MODEL_OUTPUT = "/tmp/TestModelOuput.mod";
	private static final String FILENAME_ABSTRACT = "src/test/resources/test/abstract.txt";

	/**
	 * @throws Test
	 *             reading a serialized model object
	 */
	@Test
	public void testReadModel() throws Exception {

		Tokenizer tokenizer = new Tokenizer();
		tokenizer.readModel(FILENAME_MODEL);
		assertNotNull(tokenizer.model);
	}

	/**
	 * Test training and outputting a model object using training data in a file
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTrain() throws Exception {

		Tokenizer tokenizer = new Tokenizer();
		ArrayList<String> trainDataORG = readLinesFromFile(FILENAME_TRAIN_DATA_ORG);
		ArrayList<String> trainDataTOK = readLinesFromFile(FILENAME_TRAIN_DATA_TOK);
		InstanceList trainData = tokenizer.makeTrainingData(trainDataORG,trainDataTOK);
		Pipe trainPipe = trainData.getPipe();
		tokenizer.train(trainData, trainPipe);
		tokenizer.writeModel(FILENAME_TRAIN_MODEL_OUTPUT);
		
		assertTrue(new File(FILENAME_TRAIN_MODEL_OUTPUT + ".gz").isFile());
	}

	/**
	 * Test predict
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPredict() throws Exception {

		Tokenizer tokenizer = new Tokenizer();
		tokenizer.readModel(FILENAME_MODEL);

		ArrayList<String> orgSentences = readLinesFromFile(FILENAME_ABSTRACT);
		ArrayList<String> tokSentences = new ArrayList<String>();

		InstanceList iList = tokenizer.makePredictionData(orgSentences,tokSentences);
		for(Instance instance: iList) {
			ArrayList<Unit> unitList = tokenizer.predict(instance);
			assertNotNull(unitList);
			for (Unit unit : unitList) {
				LOGGER.trace("unit=" + unit);
			}
		}


	}

	private ArrayList<String> readLinesFromFile(String filename) {
		ArrayList<String> list = new ArrayList<String>();
		File file = new File(filename);
		if (file.isFile()) {
			try {
				FileReader reader = new FileReader(file);
				// reader.r
				BufferedReader br = new BufferedReader(reader);

				while (true) {
					String line = br.readLine();
					if (line == null) {
						break;
					}
					list.add(line);
				}
			} catch (FileNotFoundException e) {
				LOGGER.error(e);
			} catch (IOException e) {
				LOGGER.error(e);
			}

		}
		return list;
	}

}
