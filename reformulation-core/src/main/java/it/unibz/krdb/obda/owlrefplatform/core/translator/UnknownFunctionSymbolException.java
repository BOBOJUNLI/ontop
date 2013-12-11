/*
 * Copyright (C) 2009-2013, Free University of Bozen Bolzano
 * This source code is available under the terms of the Affero General Public
 * License v3.
 * 
 * Please see LICENSE.txt for full license terms, including the availability of
 * proprietary exceptions.
 */
package it.unibz.krdb.obda.owlrefplatform.core.translator;

public abstract class UnknownFunctionSymbolException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private String unknownSymbol;

	public UnknownFunctionSymbolException(String unknownSymbol) {
		this.unknownSymbol = unknownSymbol;
	}

	@Override
	public String getMessage() {
		return "Found unknown function symbol: " + unknownSymbol;
	}
}