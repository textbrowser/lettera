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

import com.sun.mail.smtp.SMTPTransport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;

public class Mail
{
    private Database m_database = Database.instance();
    private SMTPTransport m_smtp = null;
    private Store m_imap = null;
    private String m_inbound_address = "";
    private String m_inbound_email = "";
    private String m_inbound_password = "";
    private String m_inbound_port = "";
    private String m_outbound_address = "";
    private String m_outbound_email = "";
    private String m_outbound_password = "";
    private String m_outbound_port = "";
    private String m_proxy_address = "";
    private String m_proxy_password = "";
    private String m_proxy_port = "";
    private String m_proxy_type = "";
    private String m_proxy_user = "";

    public Mail(String inbound_address,
		String inbound_email,
		String inbound_password,
		String inbound_port,
		String outbound_address,
		String outbound_email,
		String outbound_password,
		String outbound_port,
		String proxy_address,
		String proxy_password,
		String proxy_port,
		String proxy_type,
		String proxy_user)
    {
	m_inbound_address = inbound_address;
	m_inbound_email = inbound_email;
	m_inbound_password = inbound_password;
	m_inbound_port = inbound_port;
	m_outbound_address = outbound_address;
	m_outbound_email = outbound_email;
	m_outbound_password = outbound_password;
	m_outbound_port = outbound_port;
	m_proxy_address = proxy_address;
	m_proxy_password = proxy_password;
	m_proxy_port = proxy_port;
	m_proxy_type = proxy_type;
	m_proxy_user = proxy_user;
    }

    public ArrayList<String> folder_names()
    {
	try
	{
	    ArrayList<String> array_list = new ArrayList<> ();
	    Folder folders[] = m_imap.getDefaultFolder().list("*");

	    for(Folder folder : folders)
	    {
		int message_count = 0;
		int new_message_count = 0;

		try
		{
		    message_count = folder.getMessageCount();
		}
		catch(Exception exception)
		{
		    message_count = 0;
		}

		try
		{
		    new_message_count = folder.getNewMessageCount();
		}
		catch(Exception exception)
		{
		    new_message_count = 0;
		}

		array_list.add(folder.getName() + " (" + message_count + ")");
		m_database.write_folder(folder.getName(),
					m_inbound_email,
					message_count,
					new_message_count);
	    }

	    Collections.sort(array_list);
	    return array_list;
	}
	catch(Exception exception)
	{
	    return null;
	}
    }

    public static Properties properties(String email,
					String host,
					String password,
					String port,
					String protocol,
					String proxy_address,
					String proxy_password,
					String proxy_port,
					String proxy_type,
					String proxy_user,
					String read_timeout)
    {
	/*
	** https://javaee.github.io/javamail/docs/api/com/sun/mail/imap/package-summary.html
	** https://javaee.github.io/javamail/docs/api/com/sun/mail/smtp/SMTPTransport.html
	*/

	Properties properties = new Properties();

	properties.setProperty("mail." + protocol + ".ssl.enable", "true");
	properties.setProperty
	    ("mail." + protocol + ".starttls.enable", "true");
	properties.setProperty
	    ("mail." + protocol + ".starttls.required", "true");

	switch(protocol)
	{
	case "imaps":
	    properties.setProperty
		("mail." + protocol + ".connectiontimeout", "10000");
	    properties.setProperty
		("mail." + protocol + ".timeout", read_timeout);
	    break;
	case "smtp":
	case "smtps":
	    properties.setProperty("mail.smtp.connectiontimeout", "10000");
	    properties.setProperty("mail.smtp.localhost", "localhost");
	    properties.setProperty("mail.smtp.timeout", read_timeout);
	    properties.setProperty("mail.smtps.localhost", "localhost");
	    break;
	default:
	    break;
	}

	if(!proxy_address.isEmpty())
	    switch(proxy_type)
	    {
	    case "HTTP":
		switch(protocol)
		{
		case "imaps":
		    properties.setProperty
			("mail." + protocol + ".proxy.host", proxy_address);
		    properties.setProperty
			("mail." + protocol + ".proxy.password",
			 proxy_password);
		    properties.setProperty
			("mail." + protocol + ".proxy.port", proxy_port);
		    properties.setProperty
			("mail." + protocol + ".proxy.user", proxy_user);
		    break;
		case "smtp":
		case "smtps":
		    properties.setProperty("mail.smtp.proxy.host",
					   proxy_address);
		    properties.setProperty
			("mail.smtp.proxy.password", proxy_password);
		    properties.setProperty("mail.smtp.proxy.port", proxy_port);
		    properties.setProperty("mail.smtp.proxy.user", proxy_user);
		    break;
		default:
		    break;
		}

		break;
	    case "SOCKS":
		switch(protocol)
		{
		case "imaps":
		    properties.setProperty
			("mail.imaps.socks.host", proxy_address);
		    properties.setProperty("mail.imaps.socks.port", proxy_port);
		    break;
		case "smtp":
		case "smtps":
		    properties.setProperty("mail.smtp.socks.host",
					   proxy_address);
		    properties.setProperty("mail.smtp.socks.port", proxy_port);
		    break;
		default:
		    break;
		}

		break;
	    default:
		break;
	    }

	return properties;
    }

    public void connect()
    {
	connect_imap();
	connect_smtp();
    }

    public void connect_imap()
    {
	disconnect_imap();

	try
	{
	    m_imap = Session.getInstance
		(properties(m_inbound_email,
			    m_inbound_address,
			    m_inbound_password,
			    m_inbound_port,
			    "imaps",
			    m_proxy_address,
			    m_proxy_password,
			    m_proxy_port,
			    m_proxy_type,
			    m_proxy_user,
			    "60000")).getStore("imaps");
	    m_imap.connect
		(m_inbound_address,
		 Integer.valueOf(m_inbound_port),
		 m_inbound_email,
		 m_inbound_password);
	}
	catch(Exception exception)
	{
	    m_imap = null;
	}
    }

    public void connect_smtp()
    {
	disconnect_smtp();

	try
	{
	    m_smtp = (SMTPTransport)
		(Session.getInstance(properties(m_outbound_email,
						m_outbound_address,
						m_outbound_password,
						m_outbound_port,
						"smtps",
						m_proxy_address,
						m_proxy_password,
						m_proxy_port,
						m_proxy_type,
						m_proxy_user,
						"60000"))).
		getTransport("smtp");
	    m_smtp.setRequireStartTLS(true);
	    m_smtp.connect
		(m_outbound_address,
		 Integer.valueOf(m_outbound_port),
		 m_outbound_email,
		 m_outbound_password);
	}
	catch(Exception exception)
	{
	    m_smtp = null;
	}
    }

    public void disconnect()
    {
	disconnect_imap();
	disconnect_smtp();
    }

    public void disconnect_imap()
    {
	try
	{
	    if(m_imap != null)
		m_imap.close();
	}
	catch(Exception exception)
	{
	}

	m_imap = null;
    }

    public void disconnect_smtp()
    {
	try
	{
	    if(m_smtp != null)
		m_smtp.close();
	}
	catch(Exception exception)
	{
	}

	m_smtp = null;
    }
}
