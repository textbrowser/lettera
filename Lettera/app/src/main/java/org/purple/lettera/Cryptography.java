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
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class Cryptography
{
    final static String s_empty_sha_1 =
	"0000000000000000000000000000000000000000";

    public static KeyPair key_pair_from_bytes(String algorithm,
					      byte private_bytes[],
					      byte public_bytes[])
    {
	try
	{
	    EncodedKeySpec encoded_key_spec_1 = new PKCS8EncodedKeySpec
		(private_bytes);
	    EncodedKeySpec encoded_key_spec_2 = new X509EncodedKeySpec
		(public_bytes);
	    KeyFactory key_factory = KeyFactory.getInstance(algorithm);
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

    public static byte[] sha_1(byte[] ... data)
    {
	try
	{
	    MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");

	    for(byte b[] : data)
		if(b != null)
		    messageDigest.update(b);

	    return messageDigest.digest();
	}
	catch(Exception exception)
	{
	}

	return null;
    }
}
