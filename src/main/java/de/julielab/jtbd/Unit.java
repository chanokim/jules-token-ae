/** 
 * Unit.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 1.6.1
 * Since version:   1.0
 *
 * Creation date: Aug 01, 2006 
 * 
 * The Unit object. See MedInfo 2007 paper for 
 * a detailed description of what is a unit.
 * 
 **/

package de.julielab.jtbd;


public class Unit {

	private static final String DEFAULT_LABEL = "O";
	
	public int begin; // start offset
	public int end;  // end offset
	public String rep; // a string representation of this unit
	public String superUnitRep; // a string representation of the super-unit
	public String label; // a label for this unit
	
	public Unit(int begin, int end, String rep, String superUnitRep) {
		this.begin = begin;
		this.end = end;
		this.rep = rep;
		this.superUnitRep = superUnitRep;
		this.label = DEFAULT_LABEL;
	}

	public String toString() {
		return rep+": (" + label + ")" + begin+"-"+end;
	}
}
