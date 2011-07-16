package com.captiveimagination.jgn.translation.encryption.keybased;

import java.math.BigInteger;

/**
 * @author Matt Hicks
 * @author Riven (craterstudio.encryption)
 */
public class PublicKey {
	public final int bits;
	public final BigInteger n;

	PublicKey(int bits, BigInteger n) {
		this.bits = bits;
		this.n = n;
	}

	public PublicKey(int bits, String s) {
		this.bits = bits;
		n = new BigInteger(s, 16);
	}

	public final String toString() {
		return n.toString(16).toUpperCase();
	}
}