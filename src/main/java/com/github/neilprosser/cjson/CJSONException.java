package com.github.neilprosser.cjson;

/**
 * Exception thrown by {@link CJSON}.
 */
public class CJSONException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Creates a new instance of the exception.
	 * 
	 * @param message A message describing the problem
	 * @param cause The cause of the exception
	 */
	public CJSONException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
