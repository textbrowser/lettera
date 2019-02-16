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

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.TextView;
import java.util.Date;

public class Letter
{
    private class PopulateContainers implements Runnable
    {
	private Dialog m_dialog = null;
	private String m_email_account = "";
	private String m_folder_name = "";
	private int m_position = -1;

	private PopulateContainers(Dialog dialog,
				   String email_account,
				   String folder_name,
				   int position)
	{
	    m_dialog = dialog;
	    m_email_account = email_account;
	    m_folder_name = folder_name;
	    m_position = position;
	}

	@Override
	public void run()
	{
	    final MessageElement message_element = s_database.message
		(m_email_account, m_folder_name, m_position);

	    if(message_element == null)
	    {
		try
		{
		    if(m_dialog != null)
			m_dialog.dismiss();
		}
		catch(Exception exception)
		{
		}

		return;
	    }

	    if(!message_element.m_content_downloaded)
	    {
		Mail mail = null;

		try
		{
		    EmailElement email_element = s_database.email_element
			(m_email_account);

		    mail = new Mail
			(email_element.m_inbound_address,
			 email_element.m_inbound_email,
			 email_element.m_inbound_password,
			 String.valueOf(email_element.m_inbound_port),
			 email_element.m_outbound_address,
			 email_element.m_outbound_email,
			 email_element.m_outbound_password,
			 String.valueOf(email_element.m_outbound_port),
			 email_element.m_proxy_address,
			 email_element.m_proxy_password,
			 String.valueOf(email_element.m_proxy_port),
			 email_element.m_proxy_type,
			 email_element.m_proxy_user);
		    mail.connect_imap();
		}
		catch(Exception exception)
		{
		}
		finally
		{
		    if(mail != null)
			mail.disconnect();
		}
	    }

	    m_lettera.runOnUiThread(new Runnable()
	    {
		@Override
		public void run()
		{
		    m_from.setText(message_element.m_from_name);

		    String string = Utilities.formatted_email_date_for_message
			(new Date(message_element.m_received_date_unix_epoch));

		    if(string.isEmpty())
			m_received_date.setText
			    (message_element.m_received_date);
		    else
			m_received_date.setText(string);

		    m_subject.setText(message_element.m_subject);
		    m_to_email_account.setText(message_element.m_email_account);

		    String content = message_element.m_message;

		    content = content.replaceAll
			("<img ",
			 "<img onerror=\"this.style.display='none';\" ");
		    m_web_view.loadDataWithBaseURL
			(null, content, "text/html", "UTF-8", null);
		    s_database.set_message_read
			(m_email_account,
			 m_folder_name,
			 true,
			 message_element.m_uid);
		    m_lettera.message_read();

		    try
		    {
			if(m_dialog != null)
			    m_dialog.dismiss();
		    }
		    catch(Exception exception)
		    {
		    }
		}
	    });
	}
    }

    private Dialog m_dialog = null;
    private ImageButton m_return_button = null;
    private Lettera m_lettera = null;
    private TextView m_from = null;
    private TextView m_received_date = null;
    private TextView m_subject = null;
    private TextView m_to_email_account = null;
    private View m_parent = null;
    private View m_view = null;
    private WebView m_web_view = null;
    private WindowManager.LayoutParams m_layout_params = null;
    private final static Database s_database = Database.instance();

    public Letter(Lettera lettera, View parent)
    {
	m_lettera = lettera;
	m_parent = parent;

	LayoutInflater inflater = (LayoutInflater) m_lettera.getSystemService
	    (Context.LAYOUT_INFLATER_SERVICE);

	m_layout_params = new WindowManager.LayoutParams();
	m_layout_params.dimAmount = 0.0f;
	m_layout_params.height = WindowManager.LayoutParams.MATCH_PARENT;
	m_layout_params.width = WindowManager.LayoutParams.MATCH_PARENT;
	m_view = inflater.inflate(R.layout.letter, null);

	/*
	** The cute popup.
	*/

	m_dialog = new Dialog
	    (m_lettera,
	     android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen);
	m_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
	m_dialog.setCancelable(true);
	m_dialog.setContentView(m_view);
	m_dialog.setTitle("Letter");

	if(m_dialog.getWindow() != null)
	    m_dialog.getWindow().setAttributes(m_layout_params);

	/*
	** Initialize other widgets.
	*/

	initialize_widget_members();
	prepare_listeners();
    }

    private void initialize_widget_members()
    {
	m_from = m_view.findViewById(R.id.from);
	m_received_date = m_view.findViewById(R.id.received_date);
	m_return_button = m_view.findViewById(R.id.return_button);
	m_subject = m_view.findViewById(R.id.subject);
	m_to_email_account = m_view.findViewById(R.id.to_email_account);
	m_web_view = m_view.findViewById(R.id.content);
	m_web_view.getSettings().setAppCacheEnabled(false);
	m_web_view.getSettings().setBlockNetworkLoads(true);
	m_web_view.getSettings().setJavaScriptEnabled(true);
        m_web_view.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
    }

    private void prepare_listeners()
    {
	if(!m_return_button.hasOnClickListeners())
	    m_return_button.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(m_lettera.isFinishing())
			return;

		    dismiss();
		}
	    });
    }

    public void dismiss()
    {
	m_web_view.loadDataWithBaseURL(null, "", "text/plain", "", null);
	m_web_view.scrollTo(0, 0);

	try
	{
	    m_dialog.dismiss();
	}
	catch(Exception exception)
	{
	}
    }

    public void show(String email_account, String folder_name, int position)
    {
	m_dialog.show();
	m_from.setText("e-mail@e-mail.org");
	m_from.setTextColor(Lettera.text_color());
	m_subject.setText("Subject");
	m_subject.setTextColor(Lettera.text_color());
	m_to_email_account.setText("e-mail@e-mail.org");
	m_to_email_account.setTextColor(Lettera.text_color());
	m_view.findViewById(R.id.top_divider).setBackgroundColor
	    (Lettera.divider_color());
	m_view.setBackgroundColor(Lettera.background_color());

	Dialog dialog = null;

	try
	{
	    dialog = new Dialog(m_lettera);
	    Windows.show_progress_dialog
		(m_lettera,
		 dialog,
		 "Downloading content. Please be patient.",
		 null);

	    Thread thread = new Thread
		(new PopulateContainers(dialog,
					email_account,
					folder_name,
					position));

	    thread.start();
	}
	catch(Exception exception_1)
	{
	    try
	    {
		if(dialog != null)
		    dialog.dismiss();
	    }
	    catch(Exception exception_2)
	    {
	    }
	}
    }
}
