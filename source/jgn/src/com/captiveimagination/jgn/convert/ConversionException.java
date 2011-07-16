
package com.captiveimagination.jgn.convert;

/**
 * @author Nathan Sweet <misc@n4te.com>
 */
public class ConversionException extends Exception {
	public ConversionException () {
		super();
	}

	public ConversionException (String message, Throwable cause) {
		super(message, cause);
	}

	public ConversionException (String message) {
		super(message);
	}

	public ConversionException (Throwable cause) {
		super(cause);
	}
}
