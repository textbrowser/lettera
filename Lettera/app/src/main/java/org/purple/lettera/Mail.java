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

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.smtp.SMTPTransport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;

public class Mail
{
    private IMAPStore m_imap = null;
    private SMTPTransport m_smtp = null;
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

    private static void multipart_recursive
	(Part part,
	 String mime_type,
	 StringBuffer string_buffer) throws Exception
    {
	if(part == null || string_buffer == null)
	    return;

	if(part.isMimeType("message/rfc822"))
	    multipart_recursive
		((Part) part.getContent(), mime_type, string_buffer);
	else if(part.isMimeType("multipart/*"))
	{
	    Multipart multipart = (Multipart) part.getContent();

	    if(multipart == null)
		return;

	    int count = multipart.getCount();

	    for(int i = 0; i < count; i++)
		multipart_recursive
		    (multipart.getBodyPart(i), mime_type, string_buffer);
	}
	else if(part.isMimeType(mime_type))
	    string_buffer.append((String) part.getContent());
	else
	{
	    if(mime_type.isEmpty())
	    {
		Object object = part.getContent();

		if(object instanceof String)
		    string_buffer.append((String) object);
	    }
	}
    }

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
	m_inbound_address = inbound_address.trim();
	m_inbound_email = inbound_email.trim();
	m_inbound_password = inbound_password;
	m_inbound_port = inbound_port;
	m_outbound_address = outbound_address.trim();
	m_outbound_email = outbound_email.trim();
	m_outbound_password = outbound_password;
	m_outbound_port = outbound_port;
	m_proxy_address = proxy_address.trim();
	m_proxy_password = proxy_password;
	m_proxy_port = proxy_port;
	m_proxy_type = proxy_type;
	m_proxy_user = proxy_user.trim();
	imap();
	smtp();
    }

    public ArrayList<FolderElement> folder_elements(AtomicBoolean interrupt)
    {
	try
	{
	    ArrayList<FolderElement> array_list = new ArrayList<> ();
	    Folder folders[] = m_imap.getDefaultFolder().list("*");

	    for(Folder folder : folders)
	    {
		if(interrupt.get())
		    break;

		if((folder.getType() & Folder.HOLDS_MESSAGES) == 0)
		    continue;

		FolderElement folder_element = new FolderElement();

		/*
		** Ignore message counts.
		*/

		folder_element.m_email_account = m_inbound_email;
		folder_element.m_full_name = folder.getFullName();
		folder_element.m_name = folder.getName();
		array_list.add(folder_element);
	    }

	    return array_list;
	}
	catch(Exception exception)
	{
	    return null;
	}
    }

    public ArrayList<String> folder_full_names()
    {
	try
	{
	    ArrayList<String> array_list = new ArrayList<> ();
	    Folder folders[] = m_imap.getDefaultFolder().list("*");

	    for(Folder folder : folders)
		if((folder.getType() & Folder.HOLDS_MESSAGES) != 0)
		    array_list.add(folder.getFullName());

	    Collections.sort(array_list);
	    return array_list;
	}
	catch(Exception exception)
	{
	    return null;
	}
    }

    public IMAPFolder folder(String folder_name)
    {
	try
	{
	    return (IMAPFolder) m_imap.getFolder(folder_name);
	}
	catch(Exception exception)
	{
	    return null;
	}
    }

    public IMAPStore imap()
    {
	if(m_imap != null)
	    return m_imap;

	try
	{
	    m_imap = (IMAPStore) Session.getInstance
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
			    "10000")).getStore("imaps");
	}
	catch(Exception exception)
	{
	    m_imap = null;
	}

	return m_imap;
    }

    public Message[] messages(String folder_name)
    {
	IMAPFolder folder = null;
	Message messages[] = null;

	try
	{
	    if((folder = (IMAPFolder) m_imap.getFolder(folder_name)) == null)
		return null;
	    else
		folder.open(Folder.READ_ONLY);
	}
	catch(Exception exception)
	{
	    return null;
	}

	try
	{
	    return folder.getMessages();
	}
	catch(Exception exception)
	{
	}
	finally
	{
	    try
	    {
		folder.close();
	    }
	    catch(Exception exception)
	    {
	    }
	}

	return null;
    }

    public SMTPTransport smtp()
    {
	if(m_smtp != null)
	    return m_smtp;

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
						"10000"))).getTransport("smtp");
	}
	catch(Exception exception)
	{
	    m_smtp = null;
	}

	return m_smtp;
    }

    public String email_account()
    {
	return m_inbound_email;
    }

    public boolean connected()
    {
	if(m_imap != null && m_smtp != null)
	    return m_imap.isConnected() && m_smtp.isConnected();
	else
	    return false;
    }

    public boolean imap_connected()
    {
	return m_imap != null && m_imap.isConnected();
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

    public static String multipart(Message message, String mime_type)
    {
	if(message == null)
	    return "";

	try
	{
	    StringBuffer string_buffer = new StringBuffer();

	    multipart_recursive(message, mime_type, string_buffer);
	    return string_buffer.toString();
	}
	catch(Exception exception)
	{
	}

	return "";
    }

    public void connect()
    {
	connect_imap();
	connect_smtp();
    }

    public void connect_imap()
    {
	if(m_imap == null || m_imap.isConnected())
	    return;

	try
	{
	    m_imap.connect
		(m_inbound_address,
		 Integer.valueOf(m_inbound_port),
		 m_inbound_email,
		 m_inbound_password);
	}
	catch(Exception exception)
	{
	}
    }

    public void connect_smtp()
    {
	if(m_smtp == null || m_smtp.isConnected())
	    return;

	try
	{
	    m_smtp.setRequireStartTLS(true);
	    m_smtp.connect
		(m_outbound_address,
		 Integer.valueOf(m_outbound_port),
		 m_outbound_email,
		 m_outbound_password);
	}
	catch(Exception exception)
	{
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
    }
}
