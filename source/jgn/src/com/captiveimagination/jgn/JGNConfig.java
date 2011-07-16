/**
 * Copyright (c) 2005-2006 JavaGameNetworking
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'JavaGameNetworking' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Created: Jan 21, 2007
 */
package com.captiveimagination.jgn;

import java.io.*;
import java.net.URL;
import java.util.logging.LogManager;

/**
 * Sets up the logging system for JGN.
 * <p/>
 * Currently hardwired to use JDK14 logging. This was done in order to not make
 * JGN overly dependent on outside libraries...
 * <p/>
 * This class will later manage all configurable entities within JGN, too.
 *
 * @author Alfons Seul
 */
public class JGNConfig {
	private static boolean CONFIG_READY = false;
	private static boolean LOG_READY = false;

	/**
	 * this constructor can be used when configuring the global Logging system within
	 * Java 1.4+, by setting a system prop:
	 * -Djava.util.logging.config.class=com.captiveimagination.jgn.JGNConfig
	 *
	 * @see java.util.logging.LogManager
	 */
	public JGNConfig() {
		ensureJGNConfigured();
	}

	public static void ensureJGNConfigured() {
		ensureConfigReady();
		ensureLogReady();
	}

	private static void ensureConfigReady() {
		if (CONFIG_READY) return;
		// do initial configuration setup
		CONFIG_READY = true;
	}

	/**
	 * if not initialized, this will set up the Logging system of JGN.
	 * <p/>
	 * Strategy:
	 * 	check the existence of following files in order:
	 * 		1. com/captiveimagination/jgn/resources/test-log.properties
	 * 		2. com/captiveimagination/jgn/resources/log.properties
	 * 		3. <user.home>/jgn/log.properties
	 * 		4. <user.dir>/lib/jgnlog.properties
	 * 	Then, the LogManager's readConfiguration(inputStream) will be called to
	 * 		install the properties.
	 * <p/>
	 * 	if none of these files exist, the <jre>/lib/logging.properties will be used by default
	 * <p/>
	 * 	set LOG_READY as initialized.
	 * <p/>
	 * Note: searching first for test-log.properties makes it possible to have a special configuration
	 * for testing purposes that can coexist with normal, production, config. (log.properties).
	 * After testing is done, just delete (or rename) test-log.properties.
	 * <p/>
	 * If there are errors during initialize, an error report will be given to
	 * System.err. No exception should be thrown, that is NOT application specific...
	 * this.initialized will be set however, so that the error will at most happen once.
	 * <p/>
	 * This method should (and will) be called from all major entry points within JGN
	 */
	private static void ensureLogReady() {

		if (LOG_READY) return; // this was already done before, buddy

		InputStream in = null;
		String fname;
		final String resourceLocation = "./com/captiveimagination/jgn/resources/";
		LOG_READY = true; // do all errors at least (..and at most) once ... :-)

		try {
			// check for test-log.properties
			URL myUrl = JGNConfig.class.getClassLoader().getResource(resourceLocation + "test-log.properties");
			if (myUrl == null)
				// check for log.properties
				myUrl = JGNConfig.class.getClassLoader().getResource(resourceLocation + "log.properties");
			if (myUrl == null) {
				// check for <user.home>/jgn/log.properties
				fname = System.getProperty("user.home");
				if (fname != null) {
					File f = new File(fname, "jgn");
					f = new File(f, "log.properties");
					if (! f.exists()) fname = null;
					else fname = f.getCanonicalPath();
				}
				if (fname == null) {
					// check for <user.dir>/lib/jgnlog.properties
					fname = System.getProperty("user.dir");
					if (fname != null) {
						File f = new File(fname, "lib");
						f = new File(f, "jgnlog.properties");
						if (! f.exists()) fname = null;
						else fname = f.getCanonicalPath();
					}
				}
			} else {
				fname = new File(myUrl.toURI()).getAbsolutePath();
			}

			if (fname == null) {
				// no success at all, let LogManager use it's global config
				System.err.println("WARNING: can't read any configuration for JGN-logging");
				return; // note LOG_READY was set true, above
			}

			in = new FileInputStream(fname);
			BufferedInputStream bin = new BufferedInputStream(in);
			LogManager.getLogManager().readConfiguration(bin);

// for testing this method, ask for a property that doesn't exist in default config, but in our's
//      String xxx = LogManager.getLogManager().getProperty("com.captiveimagination.jgn.level");
//      System.out.println(xxx);

		} catch (Exception e) {
			// Don't stop a running system, when only the logging system doesn't work!
			System.err.println("WARNING: Error reading configuration for JGN logging.");
			System.err.println(e.getMessage());
		} finally {
			if (in != null) try {
				in.close();
			} catch (IOException e) {/**/}
		}
	}

	// testing only:
	// for each level SEVERE .. FINEST: test logging all levels available
//  public static void main(String[] args) {
//    ensureJGNConfigured();
//    java.util.logging.Logger LOG = java.util.logging.Logger.getLogger("com.captiveimagination.jgn.JGNConfig");
//    testOutput(LOG,java.util.logging.Level.SEVERE);
//    testOutput(LOG,java.util.logging.Level.WARNING);
//    testOutput(LOG,java.util.logging.Level.INFO);
//    testOutput(LOG,java.util.logging.Level.CONFIG);
//    testOutput(LOG,java.util.logging.Level.FINE);
//    testOutput(LOG,java.util.logging.Level.FINER);
//    testOutput(LOG,java.util.logging.Level.FINEST);
//    // Level is now finest
//    LOG.log(Level.FINE, "Array message: {0},{1},{2}", new Byte[]{5,6,7});
//    LOG.log(Level.WARNING, "A fake exception", new IOException("this is a fake 'n NO error ..."));
//    // let the logging system finish before abruptly shut down output
//    try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace();}
//  }
//
//  public static void testOutput(java.util.logging.Logger LOG, java.util.logging.Level lvl) {
//    LOG.setLevel(lvl);
//    LOG.severe("Filtering level = "+lvl);
//    LOG.severe("  on SEVERE level");
//    LOG.warning("  on WARNING level");
//    LOG.info("  on INFO level");
//    LOG.config("  on CONFIG level");
//    LOG.fine("  on FINE level");
//    LOG.finer("  on FINER level");
//    LOG.finest("  on FINEST level");
//  }
}
