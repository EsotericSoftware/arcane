/**
 * Copyright (c) 2005-2007 JavaGameNetworking
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
 * Created: Feb 2, 2007
 */
package com.captiveimagination.jgn.test.messages;

import com.captiveimagination.jgn.message.Message;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * a Message that takes all fieldtypes for which a converter exists.
 * This is to test the conversion process.
 * 
 * @author Alfons Seul
 *
 */
public class AllFieldTypesMessage extends Message {
	enum myEnum {A, B, C}

	private boolean fBool;
	private boolean[] fBoolArr;
	private byte fByte;
	private byte[] fByteArr;
	private char fChar;
	private char[] fCharArr;
	private double fDoub;
	private double[] fDoubArr;
	private myEnum fEnum;
	private float fFloat;
	private float[] fFloatArr;
	private int fInt;
	private int[] fIntArr;
	private long fLong;
	private long[] fLongArr;
	private Serializable fSer;
	private short fShort;
	private short[] fShortArr;
	private String fString;
	private String[] fStringArr;

	public AllFieldTypesMessage() {
		fBool = true;
		fBoolArr = new boolean[]{true,false,true};
		fByte = 100;
		fByteArr = new byte[]{1,-2,3,-4,5};
		fChar = 'x';
		fCharArr = new char[]{'a','b','c'};
		fDoub = -0.12345678;
		fDoubArr = new double[]{1.0,2.0,3.0,4.0,5.0};
		fEnum = myEnum.C;
		fFloat = 9.8765F;
		fFloatArr = new float[]{5.0F, 5.1F, 5.2F};
		fInt = 4321;
		fIntArr = new int[]{33, 44,44, 55,55,55};
		fLong = 3145687927810L;
		fLongArr = new long[]{-10,-200,-3000,-40000,0};
		ArrayList<Double> ald = new ArrayList<Double>();
		for (double d : fDoubArr) ald.add(d);
		fSer = ald;
		fShort = 12345;
		fShortArr = new short[]{7,3,6,4,5};
		fString = "This message is for testing correctness of type conversion";
		fStringArr = new String[]{fString, "got it?", "if true, good", "   else bad "};
	}

	public boolean getFBool() {
		return fBool;
	}

	public void setFBool(boolean fBool) {
		this.fBool = fBool;
	}

	public boolean[] getFBoolArr() {
		return fBoolArr;
	}

	public void setFBoolArr(boolean[] fBoolArr) {
		this.fBoolArr = fBoolArr;
	}

	public byte getFByte() {
		return fByte;
	}

	public void setFByte(byte fByte) {
		this.fByte = fByte;
	}

	public byte[] getFByteArr() {
		return fByteArr;
	}

	public void setFByteArr(byte[] fByteArr) {
		this.fByteArr = fByteArr;
	}

	public char getFChar() {
		return fChar;
	}

	public void setFChar(char fChar) {
		this.fChar = fChar;
	}

	public char[] getFCharArr() {
		return fCharArr;
	}

	public void setFCharArr(char[] fCharArr) {
		this.fCharArr = fCharArr;
	}

	public double getFDoub() {
		return fDoub;
	}

	public void setFDoub(double fDoub) {
		this.fDoub = fDoub;
	}

	public double[] getFDoubArr() {
		return fDoubArr;
	}

	public void setFDoubArr(double[] fDoubArr) {
		this.fDoubArr = fDoubArr;
	}

	public myEnum getFEnum() {
		return fEnum;
	}

	public void setFEnum(myEnum fEnum) {
		this.fEnum = fEnum;
	}

	public float getFFloat() {
		return fFloat;
	}

	public void setFFloat(float fFloat) {
		this.fFloat = fFloat;
	}

	public float[] getFFloatArr() {
		return fFloatArr;
	}

	public void setFFloatArr(float[] fFloatArr) {
		this.fFloatArr = fFloatArr;
	}

	public int getFInt() {
		return fInt;
	}

	public void setFInt(int fInt) {
		this.fInt = fInt;
	}

	public int[] getFIntArr() {
		return fIntArr;
	}

	public int getFIntArr(int index) {
		return fIntArr[index];
	}

	public void setFIntArr(int[] fIntArr) {
		this.fIntArr = fIntArr;
	}

	public long getFLong() {
		return fLong;
	}

	public void setFLong(long fLong) {
		this.fLong = fLong;
	}

	public long[] getFLongArr() {
		return fLongArr;
	}

	public void setFLongArr(long[] fLongArr) {
		this.fLongArr = fLongArr;
	}

	public Serializable getFSer() {
		return fSer;
	}

	public void setFSer(Serializable fSer) {
		this.fSer = fSer;
	}

	public short getFShort() {
		return fShort;
	}

	public void setFShort(short fShort) {
		this.fShort = fShort;
	}

	public short[] getFShortArr() {
		return fShortArr;
	}

	public void setFShortArr(short[] fShortArr) {
		this.fShortArr = fShortArr;
	}

	public String getFString() {
		return fString;
	}

	public void setFString(String fString) {
		this.fString = fString;
	}

	public String[] getFStringArr() {
		return fStringArr;
	}

	public void setFStringArr(String[] fStringArr) {
		this.fStringArr = fStringArr;
	}
}
