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
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import org.bouncycastle.pqc.jcajce.provider.mceliece.BCMcElieceCCA2PublicKey;

public class Cryptography
{
    private final static String s_empty_sha_1 =
	"0000000000000000000000000000000000000000";
    private static Cryptography s_instance = null;

    private Cryptography()
    {
    }

    private static byte[] hash(String algorithm, byte[] ... data)
    {
	if(data == null)
	    return null;

	try
	{
	    MessageDigest message_digest = MessageDigest.getInstance(algorithm);

	    for(byte b[] : data)
		if(b != null)
		    message_digest.update(b);

	    return message_digest.digest();
	}
	catch(Exception exception)
	{
	    return null;
	}
    }

    public static String key_information(PublicKey public_key)
    {
	if(public_key == null)
	    return "";

	try
	{
	    String algorithm = public_key.getAlgorithm();
	    StringBuilder string_builder = new StringBuilder();

	    string_builder.append("Algorithm: ");
	    string_builder.append(algorithm);
	    string_builder.append("\n");
	    string_builder.append("Disk Size: ");
	    string_builder.append(public_key.getEncoded().length);
	    string_builder.append(" Bytes\n");
	    string_builder.append("Format: ");
	    string_builder.append(public_key.getFormat());
	    string_builder.append("\n");
	    string_builder.append("SHA-1: ");
	    string_builder.append(sha_1_fingerprint(public_key));

	    if(algorithm.equals("McEliece-CCA2") ||
	       algorithm.equals("RSA"))
		try
		{
		    switch(algorithm)
		    {
		    case "McEliece-CCA2":
		    {
			BCMcElieceCCA2PublicKey pk =
			    (BCMcElieceCCA2PublicKey) public_key;

			if(pk != null)
			    string_builder.append("\n").append("m = ").
				append((int) (Math.log(pk.getN()) /
					      Math.log(2))).
				append(", t = ").
				append(pk.getT());

			break;
		    }
		    case "RSA":
		    {
			RSAPublicKey pk = (RSAPublicKey) public_key;

			if(pk != null)
			    string_builder.append("\n").append("Size: ").
				append(pk.getModulus().bitLength());

			break;
		    }
		    }
		}
		catch(Exception exception)
		{
		}

	    return string_builder.toString();
	}
	catch(Exception exception)
	{
	}

	return "";
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
	return hash("SHA-1", data);
    }

    public static byte[] sha_512(byte[] ... data)
    {
	return hash("SHA-512", data);
    }

    public static synchronized Cryptography instance()
    {
	if(s_instance == null)
	    s_instance = new Cryptography();

	return s_instance;
    }
}
