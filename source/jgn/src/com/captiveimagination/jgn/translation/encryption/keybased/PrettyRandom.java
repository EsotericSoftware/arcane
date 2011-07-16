package com.captiveimagination.jgn.translation.encryption.keybased;

import java.util.Random;

/**
 * @author Matt Hicks
 * @author Riven (craterstudio.encryption)
 */
public class PrettyRandom extends Random {
	private static final long serialVersionUID = -1L;

	private int reuseSeedCountdown;
	private final int reuseSeedCount;

	public PrettyRandom() {
		this(16);
	}

	public PrettyRandom(int reuseSeedCount) {
		this.reuseSeedCount = reuseSeedCount;
	}

	public int next(int bits) {
		if (reuseSeedCountdown-- > 0)
			return super.next(bits);

		long a = (System.nanoTime() & 0xFFFFL) << ((System.nanoTime() >> 1) & 31);
		long b = (System.nanoTime() & 0xFFFFL) << ((System.nanoTime() >> 2) & 31);
		long c = (System.nanoTime() & 0xFFFFL) << ((System.nanoTime() >> 3) & 31);
		long d = (System.nanoTime() & 0xFFFFL) << ((System.nanoTime() >> 4) & 31);

		this.setSeed(a | b | c | d);

		reuseSeedCountdown = reuseSeedCount;

		return super.next(bits);
	}
}