
package arcane.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

/**
 * An InputStream wrapper than can update a UI element (usually a progress bar) as data is ready.
 */
abstract public class InputStreamMonitor extends FilterInputStream {
	private int bytesRead = 0;
	private int totalBytes = 0;

	public InputStreamMonitor (InputStream input) {
		super(input);
		if (input == null) throw new IllegalArgumentException("input cannot be null.");

		try {
			totalBytes = input.available();
		} catch (IOException ioe) {
			totalBytes = 0;
		}
	}

	/**
	 * Sets the progress bar's percentage complete based on the number of bytes read so far.
	 */
	abstract protected void updateProgress ();

	/**
	 * Returns true if the UI is still in a valid state for this InputStream to be processed. If false is returned an
	 * InterruptedIOException will be thrown if any of the read methods are called.
	 */
	protected boolean isValid () {
		return true;
	}

	/**
	 * Returns the total number of bytes to read, or 0 if it could not be determined.
	 */
	public int getTotalBytes () {
		return totalBytes;
	}

	/**
	 * Returns the bytes read so far.
	 */
	public int getBytesRead () {
		return bytesRead;
	}

	/**
	 * Returns the percentage complete, a number between 0 to 1 (inclusive).
	 */
	public float getPercentageComplete () {
		return (float)getBytesRead() / getTotalBytes();
	}

	/**
	 * Overrides <code>FilterInputStream.read</code> to update the progress bar after the read.
	 */
	public int read () throws IOException {
		int b = in.read();
		if (b >= 0) {
			++bytesRead;
			updateProgress();
		}
		if (!isValid()) {
			InterruptedIOException ex = new InterruptedIOException("progress");
			ex.bytesTransferred = bytesRead;
			throw ex;
		}
		return b;
	}

	/**
	 * Overrides <code>FilterInputStream.read</code> to update the progress bar after the read.
	 */
	public int read (byte b[]) throws IOException {
		int bytes = in.read(b);
		if (bytes > 0) {
			bytesRead += bytes;
			updateProgress();
		}
		if (!isValid()) {
			InterruptedIOException ex = new InterruptedIOException("progress");
			ex.bytesTransferred = bytesRead;
			throw ex;
		}
		return bytes;
	}

	/**
	 * Overrides <code>FilterInputStream.read</code> to update the progress bar after the read.
	 */
	public int read (byte b[], int off, int len) throws IOException {
		int bytes = in.read(b, off, len);
		if (bytes > 0) {
			bytesRead += bytes;
			updateProgress();
		}
		if (!isValid()) {
			InterruptedIOException ex = new InterruptedIOException("progress");
			ex.bytesTransferred = bytesRead;
			throw ex;
		}
		return bytes;
	}

	/**
	 * Overrides <code>FilterInputStream.skip</code> to update the progress bar after the skip.
	 */
	public long skip (long n) throws IOException {
		long bytes = in.skip(n);
		if (bytes > 0) {
			bytesRead += bytes;
			updateProgress();
		}
		return bytes;
	}

	/**
	 * Overrides <code>FilterInputStream.close</code> to close the progress bar as well as the stream.
	 */
	public void close () throws IOException {
		in.close();
	}

	/**
	 * Overrides <code>FilterInputStream.reset</code> to reset the progress bar as well as the stream.
	 */
	public synchronized void reset () throws IOException {
		in.reset();
		bytesRead = totalBytes - in.available();
		updateProgress();
	}
}
