package com.captiveimagination.jgn.translation.encryption.keybased;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;

/**
 * @author Matt Hicks
 * @author Riven (craterstudio.encryption)
 */
public class ByteArrayUtil {
	public static final BigInteger smallPrime = new BigInteger("17");
	private static final Random wheel = new PrettyRandom();

	public static boolean equals(byte[] a, byte[] b) {
		if (a == b)
			return true;
		if (a == null || b == null || a.length != b.length)
			return false;

		int length = a.length;

		for (int i = 0; i < length; i++)
			if (a[i] != b[i])
				return false;

		return true;
	}

	public static int getBlockLength(byte[] block, int offset) {
		int hi = block[offset] & 0xff;
		int lo = block[offset + 1] & 0xff;
		return (hi << 8) + lo;
	}

	public static void insert(byte[] target, int targetOffset, byte[] source,
			int sourceOffset, int length) {
		for (int i = 0; i < length; i++) {
			target[targetOffset + i] = source[sourceOffset + i];
		}
	}

	public static byte[] join(List<byte[]> blocks) {
		int size = blocks.size();
		if (size == 1)
			return blocks.get(0);

		int overallLength = 0;
		for (int i = 0; i < size; i++)
			overallLength += blocks.get(i).length;

		byte[] result = new byte[overallLength];
		int offset = 0;

		for (int i = 0; i < size; i++) {
			byte[] block = blocks.get(i);
			int length = block.length;
			insert(result, offset, block, 0, length);
			offset += length;
		}
		return result;
	}

	public static byte[] prependLength(byte[] block) {
		int length = block.length;
		byte[] result = new byte[length + 2];
		result[0] = (byte) (length >>> 8);
		result[1] = (byte) (length);
		insert(result, 2, block, 0, length);
		return result;
	}

	public static byte[] processWithPrivateKey(byte[] block, PrivateKey k) {
		return new BigInteger(block).modPow(k.d, k.n).toByteArray();
	}

	public static byte[] processWithPublicKey(byte[] block, PublicKey k) {
		return (new BigInteger(1, block).modPow(smallPrime, k.n)).toByteArray();
	}

	public static byte[] salt(byte[] block) {
		byte[] result = new byte[block.length + 4];

		result[0] = (byte) (wheel.nextInt(127) + 1);
		result[1] = (byte) (wheel.nextInt(256));
		result[2] = (byte) (wheel.nextInt(256));
		result[3] = (byte) (wheel.nextInt(256));

		ByteArrayUtil.insert(result, 4, block, 0, block.length);
		return result;
	}

	public static byte[] subarray(byte[] source, int offset, int length) {
		if (offset == 0 && source.length == length)
			return source;

		byte[] target = new byte[length];

		for (int i = 0; i < length; i++)
			target[i] = source[offset + i];

		return target;
	}

	public static byte[] unsalt(byte[] saltedBlock) {
		return ByteArrayUtil.subarray(saltedBlock, 4, saltedBlock.length - 4);
	}
}