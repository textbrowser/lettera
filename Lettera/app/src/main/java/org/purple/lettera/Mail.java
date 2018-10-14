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

public class Mail
{
    private Database m_database = Database.instance();
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
}
