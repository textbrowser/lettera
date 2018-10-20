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

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.asn1.PQCObjectIdentifiers;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.spec.McElieceCCA2KeyGenParameterSpec;

public class PGP
{
    static
    {
	Security.addProvider(new BouncyCastlePQCProvider());
	Security.addProvider(new BouncyCastleProvider());
    }

    private KeyPair m_encryption_key_pair = null;
    private KeyPair m_signature_key_pair = null;
    private final ReentrantReadWriteLock m_encryption_key_pair_lock =
	new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock m_signature_key_pair_lock =
	new ReentrantReadWriteLock();
    private final static int MCELIECE_PARAMETERS[] = new int[] {12, 68};
    private final static int RSA_KEY_SIZE = 4096;
    private static PGP s_instance = null;

    private PGP()
    {
    }

    public KeyPair encryption_key_pair()
    {
	m_encryption_key_pair_lock.readLock().lock();

	try
	{
	    return m_encryption_key_pair;
	}
	finally
	{
	    m_encryption_key_pair_lock.readLock().unlock();
	}
    }

    public KeyPair signature_key_pair()
    {
	m_signature_key_pair_lock.readLock().lock();

	try
	{
	    return m_signature_key_pair;
	}
	finally
	{
	    m_signature_key_pair_lock.readLock().unlock();
	}
    }

    public static KeyPair generate_key_pair(String type)
    {
	if(type == null)
	    return null;
	else
	    switch(type)
	    {
	    case "McEliece":
		try
		{
		    KeyPairGenerator key_pair_generator =
			KeyPairGenerator.getInstance("McElieceFujisaki");
		    McElieceCCA2KeyGenParameterSpec parameters =
			new McElieceCCA2KeyGenParameterSpec
			(MCELIECE_PARAMETERS[0],
			 MCELIECE_PARAMETERS[1],
			 McElieceCCA2KeyGenParameterSpec.SHA256);

		    key_pair_generator.initialize(parameters);
		    return key_pair_generator.generateKeyPair();
		}
		catch(Exception exception)
		{
		}

		break;
	    case "RSA":
		try
		{
		    KeyPairGenerator key_pair_generator =
			KeyPairGenerator.getInstance("RSA", "BC");

		    key_pair_generator.initialize(RSA_KEY_SIZE);
		    return key_pair_generator.generateKeyPair();
		}
		catch(Exception exception)
		{
		}

		break;
	    default:
		return null;
	    }

	return null;
    }

    public static KeyPair key_pair_from_bytes(byte private_bytes[],
					      byte public_bytes[])
    {
	for(int i = 0; i < 2; i++)
	    switch(i)
	    {
	    case 0:
		try
		{
		    EncodedKeySpec encoded_key_spec_1 = new PKCS8EncodedKeySpec
			(private_bytes);
		    EncodedKeySpec encoded_key_spec_2 = new X509EncodedKeySpec
			(public_bytes);
		    KeyFactory key_factory = KeyFactory.getInstance
			(PQCObjectIdentifiers.mcElieceCca2.getId());
		    PrivateKey private_key = key_factory.generatePrivate
			(encoded_key_spec_1);
		    PublicKey public_key = key_factory.generatePublic
			(encoded_key_spec_2);

		    return new KeyPair(public_key, private_key);
		}
		catch(Exception exception)
		{
		}

		break;
	    default:
		try
		{
		    EncodedKeySpec encoded_key_spec_1 = new PKCS8EncodedKeySpec
			(private_bytes);
		    EncodedKeySpec encoded_key_spec_2 = new X509EncodedKeySpec
			(public_bytes);
		    KeyFactory key_factory = KeyFactory.getInstance
			("RSA", "BC");
		    PrivateKey private_key = key_factory.generatePrivate
			(encoded_key_spec_1);
		    PublicKey public_key = key_factory.generatePublic
			(encoded_key_spec_2);

		    return new KeyPair(public_key, private_key);
		}
		catch(Exception exception)
		{
		}

		break;
	    }

	return null;
    }

    public static synchronized PGP instance()
    {
	if(s_instance == null)
	    s_instance = new PGP();

	return s_instance;
    }

    public void set_encryption_key_pair(KeyPair key_pair)
    {
	m_encryption_key_pair_lock.writeLock().lock();

	try
	{
	    m_encryption_key_pair = key_pair;
	}
	finally
	{
	    m_encryption_key_pair_lock.writeLock().unlock();
	}
    }

    public void set_signature_key_pair(KeyPair key_pair)
    {
	m_signature_key_pair_lock.writeLock().lock();

	try
	{
	    m_signature_key_pair = key_pair;
	}
	finally
	{
	    m_signature_key_pair_lock.writeLock().unlock();
	}
    }
}
