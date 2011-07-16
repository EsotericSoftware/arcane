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
package com.captiveimagination.jgn.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * A simple Formatter to be used within JGN
 * formats to the simple outputformat:
 * Millis|LEVEL|THREADID|CLASSNAME{.Methodname(..)}#message
 * { stacktrace }
 * <p/>
 * using un/comments, the format can also be changed to:
 * HH:mm:ss.SSS|LEVEL|THREADID|CLASSNAME{.Methodname(..)}#message
 * { stacktrace }
 * <p/>
 * Note: the thread id as output, really is dreamed of by JDK14 logging.
 * This is NOT the Thread.id as of Jdk15!!
 *
 * @author Alfons Seul
 */
public class JgnFormatter extends Formatter {

// uncomment following (2 lines) if using time format instead of elapsed millis
//  private Date dat = new Date(); // re-use to minimized object creation uncomment
//  private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

	private String lineSeparator = System.getProperty("line.separator");
	private String fieldSeparator = "|";
	private String messSeparator = "#";
	private long millisOnStart = 0L;

	/**
	 * Format the given LogRecord.
	 *
	 * @param record the log record to be formatted.
	 * @return a formatted log record as String
	 */
	public synchronized String format(LogRecord record) {
		final int maxClassLength = 35;
		StringBuffer sb = new StringBuffer();

		// millis since start
		long millis = record.getMillis();
		if (millisOnStart == 0) { // on first call only
			millisOnStart = millis;

// comment following (1 line) if using time format instead of elapsed millis
			Date dat = new Date();
			sb.append("JGN logging system started at: ").append(dat).append(lineSeparator);
		}
// comment following (1 line) if using time format instead of elapsed millis
		sb.append(formatMillis(millis - millisOnStart)).append(fieldSeparator);

		// Time: HH:MM:SS:SSS
// uncomment following (2 lines) if using time format instead of elapsed millis
//    dat.setTime(record.getMillis());
//    sb.append(sdf.format(dat)).append(fieldSeparator);

		// Level; note this is JDK14 specific and ugly
		int lv = record.getLevel().intValue();
		String levNam = "?????";
		switch (lv) {
			case 1000 :
				levNam = "ERROR";
				break;
			case 900 :
				levNam = "WARN ";
				break;
			case 800 :
				levNam = "Info ";
				break;
			case 700 :
				levNam = "Confg";
				break;
			case 500 :
				levNam = "fine ";
				break;
			case 400 :
				levNam = "finer";
				break;
			case 300 :
				levNam = "finst";
				break;
			default	 :
				break;
		}
		sb.append(levNam).append(fieldSeparator);

		// Threadid; note, this is unfortunately NOT the threadId as found in Jdk1.5 !!
		sb.append(record.getThreadID()).append(fieldSeparator);

		// classname or [loggername]
//		String cn = record.getSourceClassName();
//		if (cn == null) cn = "[" + cutStartOfString(record.getLoggerName(), maxClassLength - 2) + "]";
//		else cn = cutStartOfString(record.getLoggerName(), maxClassLength);
//		sb.append(cn);

		// .methodname()
//		if (record.getSourceMethodName() != null) {
//			sb.append(".").append(record.getSourceMethodName()).append("()");
//		}

		String cn = record.getSourceClassName();
		if (cn == null) cn = "[" + record.getLoggerName() + "]";
		if (record.getSourceMethodName() != null)
			cn = cn + "." + record.getSourceMethodName() + "()";
		else
		  cn = cn + ".[unknown]()";
		sb.append(cutStartOfString(cn, maxClassLength));


		// message
		String message = formatMessage(record);
		sb.append(messSeparator).append(message).append(lineSeparator);

		// Throwable on next line
		if (record.getThrown() != null) {
			try {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				pw.print("-->  ");
				record.getThrown().printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
			} catch (Exception ex) { /**/
			}
		}
		return sb.toString();
	}

	/**
	 * if a String is longer than maxLength, truncate it from the end
	 * and start it with ellipsis  ..
	 * eg when maxlength is 6, "abcdefghijk" will return " ..ijk"
	 *
	 * @param s				 String to work on
	 * @param maxLength of the result, will be silently floored to 4
	 * @return String, right truncated if needed
	 */
	public static String cutStartOfString(String s, int maxLength) {
		if (maxLength < 4) maxLength = 4;
		if (s == null || s.length() <= maxLength) return s;
		return " .." + s.substring(s.length() - (maxLength - 3));
	}

	/**
	 * fast rightalign of a long, retains only low 5 digits
	 *
	 * @param val a long to be formatted
	 * @return String lowest 5 digits, right aligned
	 */
	public static String formatMillis(long val) {
		if (val < 0) val = -val;
		if (val > 99999) val = val % 100000;
		if (val < 10) return "    " + val;
		if (val < 100) return "   " + val;
		if (val < 1000) return "  " + val;
		if (val < 10000) return " " + val;
		return "" + val;
	}

// test only
//  public static void main(String[] args) {
//    System.out.println("abcdefg,8 -> "+ cutStartOfString("abcdefg",8));  // abcdefg
//    System.out.println("abcdefg,7 -> "+ cutStartOfString("abcdefg",7));  // abcdefg
//    System.out.println("abcdefg,5 -> "+ cutStartOfString("abcdefg",5));  // _..fg    // _=space
//    System.out.println("abcdefg,1 -> "+ cutStartOfString("abcdefg",1));  // _..g     // _=space
//    System.out.println("null,10 -> "+ cutStartOfString(null,10));        // null
//    System.out.println("3      -->"+formatMillis(3L));                   //####3    // #=space
//    System.out.println("30     -->"+formatMillis(30L));                  //###30
//    System.out.println("300    -->"+formatMillis(300L));                 //##300
//    System.out.println("3000   -->"+formatMillis(3000L));                //#3000
//    System.out.println("30000  -->"+formatMillis(30000L));               //30000
//    System.out.println("300000 -->"+formatMillis(300000L));              //####0
//  }
}