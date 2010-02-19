/** 
 * EOSSymbols.java
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
 * A list of end-of-sentence symbols.
 **/

package de.julielab.jtbd;

import java.util.TreeSet;

public class EOSSymbols {

	TreeSet<String> symbols;

	public EOSSymbols() {
		init();
	}

	private void init() {
		symbols = new TreeSet<String>();
		symbols.add(".");
		symbols.add(":");
		symbols.add("!");
		symbols.add("?");
		symbols.add("]");
		symbols.add(")");
		symbols.add("\"");
	}
	
	public boolean contains(String c) {
		return symbols.contains(c);
	}
	
	public boolean tokenEndsWithEOSSymbol(String token) {
		if (token.length()>0) {
			String lastChar = token.substring(token.length() - 1, token.length());
			if (symbols.contains(lastChar))
				return true;
		} 
		return false;
	}

}
