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

import java.nio.ByteBuffer;
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
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;

public class Cryptography
{
    private SecretKey m_cipher_key = null;
    private SecretKey m_mac_key = null;
    private final ReentrantReadWriteLock m_cipher_key_mutex = new
	ReentrantReadWriteLock();
    private final ReentrantReadWriteLock m_mac_key_mutex = new
	ReentrantReadWriteLock();
    private final static String HMAC_ALGORITHM = "HmacSHA512";
    private final static String SYMMETRIC_ALGORITHM = "AES";
    private final static String SYMMETRIC_CIPHER_TRANSFORMATION =
	"AES/CTS/NoPadding";
    private final static String s_empty_sha_1 =
	"0000000000000000000000000000000000000000";
    private static Cryptography s_instance = null;
    private static SecureRandom s_secure_random = null;
    protected AtomicBoolean m_is_plaintext = new AtomicBoolean(true);

    private Cryptography()
    {
	prepare_secure_random();
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

    public byte[] etm(byte data[]) // Encrypt-Then-MAC
    {
	if(data == null || data.length == 0)
	    return null;

	try
	{
	    ByteBuffer byte_buffer = ByteBuffer.allocate
		(16 + 64 + data.length); // IV + MAC + Data

	    m_cipher_key_mutex.readLock().lock();

	    try
	    {
		if(m_cipher_key == null)
		    return null;

		Cipher cipher = Cipher.getInstance
		    (SYMMETRIC_CIPHER_TRANSFORMATION);
		byte iv[] = new byte[16];

		s_secure_random.nextBytes(iv);
		cipher.init(Cipher.ENCRYPT_MODE,
			    m_cipher_key,
			    new IvParameterSpec(iv));
		byte_buffer.put(iv);
		byte_buffer.put(cipher.doFinal(data));
	    }
	    finally
	    {
		m_cipher_key_mutex.readLock().unlock();
	    }

	    m_mac_key_mutex.readLock().lock();

	    try
	    {
		if(m_mac_key == null)
		    return null;

		Mac mac = Mac.getInstance(HMAC_ALGORITHM);

		mac.init(m_mac_key);
		byte_buffer.put(mac.doFinal(byte_buffer.array()));
	    }
	    finally
	    {
		m_mac_key_mutex.readLock().unlock();
	    }

	    return byte_buffer.array();
	}
	catch(Exception exception)
	{
	    return null;
	}
    }

    public byte[] mtd(byte data[]) // MAC-Then-Decrypt
    {
	if(data == null || data.length == 0)
	    return null;

	/*
	** Verify the computed digest with the provided digest.
	*/

	m_mac_key_mutex.readLock().lock();

	try
	{
	    if(m_mac_key == null)
		return null;

	    Mac mac = Mac.getInstance(HMAC_ALGORITHM);

	    mac.init(m_mac_key);

	    byte digest1[] = Arrays.copyOfRange // Provided Digest
		(data, data.length - 64, data.length);
	    byte digest2[] = mac.doFinal // Computed Digest
		(Arrays.copyOf(data, data.length - 64));

	    if(!memcmp(digest1, digest2))
		return null;
	}
	catch(Exception exception)
	{
	    return null;
	}
	finally
	{
	    m_mac_key_mutex.readLock().unlock();
	}

	m_cipher_key_mutex.readLock().lock();

	try
	{
	    if(m_cipher_key_mutex == null)
		return null;

	    Cipher cipher = Cipher.getInstance(SYMMETRIC_CIPHER_TRANSFORMATION);
	    byte iv[] = Arrays.copyOf(data, 16);

	    cipher.init(Cipher.DECRYPT_MODE,
			m_cipher_key,
			new IvParameterSpec(iv));
	    return cipher.doFinal
		(Arrays.copyOfRange(data, 16, data.length - 64));
	}
	catch(Exception exception)
	{
	    return null;
	}
	finally
	{
	    m_cipher_key_mutex.readLock().unlock();
	}
    }

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

    public static boolean memcmp(byte a[], byte b[])
    {
	if(a == null || b == null)
	    return false;

	int rc = 0;
	int size = java.lang.Math.max(a.length, b.length);

	for(int i = 0; i < size; i++)
	    rc |= (i < a.length ? a[i] : 0) ^ (i < b.length ? b[i] : 0);

	return rc == 0;
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

    public static synchronized Cryptography instance()
    {
	if(s_instance == null)
	    s_instance = new Cryptography();

	return s_instance;
    }
}
