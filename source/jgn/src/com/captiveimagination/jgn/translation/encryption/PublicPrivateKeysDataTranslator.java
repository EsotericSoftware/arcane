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
 * Created: Oct 7, 2006
 */
package com.captiveimagination.jgn.translation.encryption;

import java.util.ArrayList;
import java.util.List;

import com.captiveimagination.jgn.translation.DataTranslator;
import com.captiveimagination.jgn.translation.encryption.keybased.ByteArrayUtil;
import com.captiveimagination.jgn.translation.encryption.keybased.PrivateKey;
import com.captiveimagination.jgn.translation.encryption.keybased.PublicKey;

/**
 * Provides public/private key encryption/decryption.
 * 
 * @author Matt Hicks
 * @author Riven (craterstudio.encryption)
 */
public class PublicPrivateKeysDataTranslator implements DataTranslator {
	private PublicKey publicKey;
	private PrivateKey privateKey;
	
	private List<byte[]> inboundStore;
	private List<byte[]> outboundStore;

	public PublicPrivateKeysDataTranslator(PublicKey publicKey, PrivateKey privateKey) {
		this.publicKey = publicKey;
		this.privateKey = privateKey;
		
		inboundStore = new ArrayList<byte[]>();
		outboundStore = new ArrayList<byte[]>();
	}

	public synchronized byte[] inbound(byte[] bytes) throws Exception {
		// Decrypt data
		inboundStore.clear();
		try {
			byte[] rawMessage = bytes;

			int blockLength = 0;

			for (int chunkStart = 0; chunkStart < rawMessage.length; chunkStart += blockLength + 2) {
				blockLength = ByteArrayUtil.getBlockLength(rawMessage, chunkStart);

				byte[] encrypted = ByteArrayUtil.subarray(rawMessage, chunkStart + 2, blockLength);
				byte[] decrypted = ByteArrayUtil.processWithPrivateKey(encrypted, privateKey);
				byte[] unsalted = ByteArrayUtil.unsalt(decrypted);

				inboundStore.add(unsalted);
			}

			return ByteArrayUtil.join(inboundStore);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new RuntimeException("garbled message");
		}
	}

	public synchronized byte[] outbound(byte[] bytes) throws Exception {
		// Encrypt data
		outboundStore.clear();
		
		int blockSize = publicKey.bits / 8 - 4;

		for (int chunkStart = 0; chunkStart < bytes.length; chunkStart += blockSize) {
			int blockLength = Math.min(blockSize, bytes.length - chunkStart);

			byte[] block = ByteArrayUtil.subarray(bytes, chunkStart, blockLength);
			byte[] salted = ByteArrayUtil.salt(block);
			byte[] encrypted = ByteArrayUtil.processWithPublicKey(salted, publicKey);
			byte[] counted = ByteArrayUtil.prependLength(encrypted);

			outboundStore.add(counted);
		}

		return ByteArrayUtil.join(outboundStore);
	}
}