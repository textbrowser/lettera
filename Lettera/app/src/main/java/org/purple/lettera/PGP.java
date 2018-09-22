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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class PGP
{
    static
    {
	Security.addProvider(new BouncyCastleProvider());
    }

    private KeyPair m_encryption_key_pair = null;
    private KeyPair m_signature_key_pair = null;
    private final ReentrantReadWriteLock m_encryption_key_pair_lock =
	new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock m_signature_key_pair_lock =
	new ReentrantReadWriteLock();
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
	    case "RSA":
		try
		{
		    KeyPairGenerator key_pair_generator =
			KeyPairGenerator.getInstance("RSA", "BC");

		    key_pair_generator.initialize(1024);
		    return key_pair_generator.generateKeyPair();
		}
		catch(Exception exception)
		{
		}

		return null;
	    default:
		return null;
	    }
    }

    public static synchronized PGP get_instance()
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
