/*
** Copyright (c) Alexis Megas.
** All rights reserved.
**
** Redistribution and use in source and binary forms, with or without
** modification, are permitted provided that the following conditions
** are met:
** 1. Redistributions of source code must retain the above copyright
**    notice, this list of conditions and the following disclaimer.
** 2. Redistributions in binary form must reproduce the above copyright
**    notice, this list of conditions and the following disclaimer in the
**    documentation and/or other materials provided with the distribution.
** 3. The name of the author may not be used to endorse or promote products
**    derived from Smoke without specific prior written permission.
**
** LETTERA IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
** IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
** OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
** IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
** INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
** NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
** DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
** THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
** (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
** LETTERA, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.purple.lettera;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.EncodedKeySpec;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Cryptography
{
    private KeyPair m_encryption_key = null;
    private KeyPair m_mac_key = null;
    private final ReentrantReadWriteLock m_encryption_key_mutex = new
	ReentrantReadWriteLock();
    private final ReentrantReadWriteLock m_mac_key_mutex = new
	ReentrantReadWriteLock();
    private final static String HASH_ALGORITHM = "SHA-512";
    private final static String HMAC_ALGORITHM = "HmacSHA512";
    private final static String SYMMETRIC_ALGORITHM = "AES";
    private final static String SYMMETRIC_CIPHER_TRANSFORMATION =
	"AES/CTS/NoPadding";
    private final static String s_empty_sha_1 =
	"0000000000000000000000000000000000000000";
    private static SecureRandom s_secure_random = null;
    protected AtomicBoolean m_is_plaintext = new AtomicBoolean(true);

    public static KeyPair key_pair_from_bytes(byte private_bytes[],
					      byte public_bytes[])
    {
	try
	{
	    EncodedKeySpec encoded_key_spec_1 = new PKCS8EncodedKeySpec
		(private_bytes);
	    EncodedKeySpec encoded_key_spec_2 = new X509EncodedKeySpec
		(public_bytes);
	    KeyFactory key_factory = KeyFactory.getInstance("RSA");
	    PrivateKey private_key = key_factory.generatePrivate
		(encoded_key_spec_1);
	    PublicKey public_key = key_factory.generatePublic
		(encoded_key_spec_2);

	    return new KeyPair(public_key, private_key);
	}
	catch(Exception exception)
	{
	}

	return null;
    }

    public static String sha_1_fingerprint(Key key)
    {
	if(key == null)
	    return s_empty_sha_1;
	else
	{
	    byte bytes[] = sha_1(key.getEncoded());

	    if(bytes != null)
		return Utilities.bytes_to_hex(bytes);
	}

	return s_empty_sha_1;
    }

    public static byte[] pbkdf2(byte salt[],
				char password[],
				int iteration_count,
				int length)
    {
	try
	{
	    KeySpec key_spec = new PBEKeySpec
		(password, salt, iteration_count, length);
	    SecretKeyFactory secret_key_factory = SecretKeyFactory.getInstance
		("PBKDF2WithHmacSHA1");

	    return secret_key_factory.generateSecret(key_spec).getEncoded();
	}
	catch(Exception exception)
	{
	}

	return null;
    }

    public static byte[] sha_1(byte[] ... data)
    {
	if(data == null)
	    return null;

	try
	{
	    MessageDigest message_digest = MessageDigest.getInstance("SHA-1");

	    for(byte b[] : data)
		if(b != null)
		    message_digest.update(b);

	    return message_digest.digest();
	}
	catch(Exception exception)
	{
	}

	return null;
    }

    private static synchronized void prepare_secure_random()
    {
	if(s_secure_random != null)
	    return;

	try
	{
	    s_secure_random = SecureRandom.getInstance("SHA1PRNG");
	}
	catch(Exception exception)
	{
	    s_secure_random = new SecureRandom();
	}
    }
}
