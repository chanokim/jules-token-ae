/** 
 * TokenBoundarySymbols.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 1.6	
 * Since version:   1.0
 *
 * Creation date: Aug 01, 2006 
 * 
 * This class holds a list of symbols where the tokenizer
 * checks for possible token boundaries. 
 * 
 **/

package de.julielab.jtbd;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;


class TokenBoundarySymbols {

	Set<String> tbSymbols;

	public TokenBoundarySymbols() {
		init();
	}

	private void init() {
		tbSymbols = new HashSet<String>();
		
		
		tbSymbols.add("-");
		tbSymbols.add("+");
//		tbSymbols.add("x");

		tbSymbols.add("?");
		tbSymbols.add("!");

		tbSymbols.add(">");
		tbSymbols.add("<");

		tbSymbols.add(".");
		tbSymbols.add(",");
		tbSymbols.add(";");
		tbSymbols.add(":");
		tbSymbols.add("=");

		tbSymbols.add("/");
		tbSymbols.add("\\");

		tbSymbols.add("\"");
		tbSymbols.add("'");
		tbSymbols.add("%");
		tbSymbols.add("&");

		tbSymbols.add("(");
		tbSymbols.add(")");
		tbSymbols.add("[");
		tbSymbols.add("]");
		tbSymbols.add("{");
		tbSymbols.add("}");
	}

	public Set<String> getSymbols() {
		return tbSymbols;
	}
}
