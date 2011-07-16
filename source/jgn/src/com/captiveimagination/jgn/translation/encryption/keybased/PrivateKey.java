package com.captiveimagination.jgn.translation.encryption.keybased;

import java.math.BigInteger;

/**
 * @author Matt Hicks
 * @author Riven (craterstudio.encryption)
 */
public class PrivateKey {
	final BigInteger d, n;

	PrivateKey(BigInteger d, BigInteger n) {
		this.d = d;
		this.n = n;
	}

	public PrivateKey(String s) {
		String[] pair = s.split("_");

		n = new BigInteger(pair[0], 16);
		d = new BigInteger(pair[1], 16);
	}

	public final String toString() {
		return n.toString(16).toUpperCase() + "_" + d.toString(16).toUpperCase();
	}
}