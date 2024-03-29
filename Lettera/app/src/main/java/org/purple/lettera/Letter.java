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
**    derived from Lettera without specific prior written permission.
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
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class Letter
{
    private class PopulateContainers implements Runnable
    {
	private Dialog m_dialog = null;
	private String m_email_account = "";
	private String m_folder_name = "";
	private long m_oid = -1L;

	private PopulateContainers(Dialog dialog,
				   String email_account,
				   String folder_name,
				   long oid)
	{
	    m_dialog = dialog;
	    m_email_account = email_account;
	    m_folder_name = folder_name;
	    m_oid = oid;
	}

	@Override
	public void run()
	{
	    final MessageElement message_element = s_database.message(m_oid);

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
			 email_element.m_proxy_user,
			 email_element.m_inbound_oauth,
			 email_element.m_outbound_oauth);
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

	    if(m_lettera.get() == null)
		return;

	    m_lettera.get().runOnUiThread(new Runnable()
	    {
		@Override
		public void run()
		{
		    if(message_element.m_from_email_account.
		       equals(message_element.m_from_name))
			m_from.setText(message_element.m_from_email_account);
		    else
			m_from.setText(message_element.m_from_name +
				       " (" +
				       message_element.m_from_email_account +
				       ")");

		    String string = Utilities.formatted_email_date_for_message
			(new Date(message_element.m_received_date_unix_epoch));

		    if(string.isEmpty())
			m_received_date.setText
			    (message_element.m_received_date);
		    else
			m_received_date.setText(string);

		    m_subject.setText(message_element.m_subject);
		    m_to_email_account.setText(message_element.m_email_account);

		    if(message_element.m_content_type.equals("text/html"))
		    {
			String content = message_element.m_message_html;

			content = content.replaceAll
			    ("<img ",
			     "<img onerror=\"this.style.display='none';\" ").
			    trim();

			int index = content.toLowerCase().indexOf("<html");

			if(index >= 0)
			    content = content.substring
				(index, content.length());

			m_web_view.loadData(content, "text/html", "UTF-8");
		    }
		    else
		    {
			String content = message_element.m_message_plain;

			m_web_view.loadData(content, "text/plain", "UTF-8");
		    }

		    s_database.set_message_read
			(m_email_account,
			 m_folder_name,
			 true,
			 m_oid);

		    if(m_lettera.get() != null)
			m_lettera.get().message_read();

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

    private Button m_delete_button = null;
    private Button m_menu_button = null;
    private Button m_move_to_folder_button = null;
    private Button m_return_button = null;
    private Dialog m_dialog = null;
    private String m_email_account = "";
    private String m_folder_name = "";
    private TextView m_from = null;
    private TextView m_received_date = null;
    private TextView m_subject = null;
    private TextView m_to_email_account = null;
    private View m_view = null;
    private WeakReference<Lettera> m_lettera = null;
    private WeakReference<MessagesAdapter> m_messages_adapter = null;
    private WeakReference<View> m_parent = null;
    private WebView m_web_view = null;
    private WindowManager.LayoutParams m_layout_params = null;
    private final static Database s_database = Database.instance();
    private long m_oid = -1L;

    public Letter(Lettera lettera,
		  MessagesAdapter messages_adapter,
		  View parent)
    {
	m_lettera = new WeakReference<> (lettera);
	m_messages_adapter = new WeakReference<> (messages_adapter);
	m_parent = new WeakReference<> (parent);

	LayoutInflater inflater = (LayoutInflater) m_lettera.get().
	    getSystemService(Context.LAYOUT_INFLATER_SERVICE);

	m_view = inflater.inflate(R.layout.letter, null);

	/*
	** The cute popup.
	*/

	m_dialog = new Dialog
	    (m_lettera.get(),
	     android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen);
	m_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
	m_dialog.setCancelable(true);
	m_dialog.setContentView(m_view);
	m_dialog.setTitle("Letter");

	if(m_dialog.getWindow() != null)
	{
	    m_layout_params = new WindowManager.LayoutParams();
	    m_layout_params.dimAmount = 0.0f;
	    m_layout_params.height = WindowManager.LayoutParams.MATCH_PARENT;
	    m_layout_params.width = WindowManager.LayoutParams.MATCH_PARENT;
	    m_dialog.getWindow().setAttributes(m_layout_params);
	}

	/*
	** Initialize other widgets.
	*/

	initialize_widget_members();
	prepare_listeners();
    }

    private void initialize_widget_members()
    {
	m_delete_button = (Button) m_view.findViewById(R.id.delete_button);
	m_from = (TextView) m_view.findViewById(R.id.from);
	m_menu_button = (Button) m_view.findViewById(R.id.menu);
	m_move_to_folder_button = (Button) m_view.findViewById
	    (R.id.move_to_folder);
	m_received_date = (TextView) m_view.findViewById(R.id.received_date);
	m_return_button = (Button) m_view.findViewById(R.id.return_button);
	m_subject = (TextView) m_view.findViewById(R.id.subject);
	m_to_email_account = (TextView) m_view.findViewById
	    (R.id.to_email_account);
	m_web_view = (WebView) m_view.findViewById(R.id.content);
	m_web_view.getSettings().setAppCacheEnabled(false);
	m_web_view.getSettings().setBlockNetworkLoads(false);
	m_web_view.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
	m_web_view.getSettings().setJavaScriptEnabled(false);
	m_web_view.setWebViewClient(new WebViewClient()
	{
	    @Override
	    public boolean shouldOverrideUrlLoading
		(WebView web_view, WebResourceRequest web_resource_request)
	    {
		return true;
	    }

	    public void onPageStarted
		(WebView web_view, String url, Bitmap bitmap)
	    {
		web_view.scrollTo(0, 0);
	    }

	    public void onPageFinished(WebView web_view, String url)
	    {
		web_view.scrollTo(0, 0);
	    }
	});
    }

    private void prepare_listeners()
    {
	if(!m_delete_button.hasOnClickListeners())
	    m_delete_button.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(m_lettera.get() != null &&
		       m_lettera.get().isFinishing())
			return;

		    final AtomicBoolean confirmed = new AtomicBoolean(false);

		    DialogInterface.OnCancelListener listener =
			new DialogInterface.OnCancelListener()
		    {
			public void onCancel(DialogInterface dialog)
			{
			    if(confirmed.get())
			    {
				s_database.delete_message
				    (m_lettera.get(),
				     m_messages_adapter.get(),
				     m_email_account,
				     m_folder_name,
				     m_oid);
				dismiss();
			    }
			}
		    };

		    Windows.show_prompt_dialog
			(m_lettera.get(),
			 listener,
			 "Delete the message?",
			 confirmed);
		}
	    });

	if(!m_menu_button.hasOnClickListeners())
	    m_menu_button.setOnClickListener
		(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(m_lettera.get() != null &&
		       m_lettera.get().isFinishing())
			return;

		    Menu menu = null;
		    MenuItem menu_item = null;
		    PopupMenu popup_menu  = new PopupMenu
			(m_lettera.get(), m_menu_button);

		    menu = popup_menu.getMenu();
		    menu_item = menu.add(0, 1, Menu.NONE, "Forward");
		    menu_item = menu.add(0, 2, Menu.NONE, "Mark As New");

		    menu_item.setOnMenuItemClickListener
			(new OnMenuItemClickListener()
			{
			    @Override
			    public boolean onMenuItemClick(MenuItem item)
			    {
				s_database.set_message_read
				    (m_email_account,
				     m_folder_name,
				     false,
				     m_oid);

				if(m_lettera.get() != null)
				    m_lettera.get().message_read();

				return true;
			    }
			});

		    menu_item = menu.add(0, 3, Menu.NONE, "Mark As Read");

		    menu_item.setOnMenuItemClickListener
			(new OnMenuItemClickListener()
			{
			    @Override
			    public boolean onMenuItemClick(MenuItem item)
			    {
				s_database.set_message_read
				    (m_email_account,
				     m_folder_name,
				     true,
				     m_oid);

				if(m_lettera.get() != null)
				    m_lettera.get().message_read();

				return true;
			    }
			});

		    menu_item = menu.add(0, 4, Menu.NONE, "Reply");
		    popup_menu.show();
		}
	    });

	if(!m_move_to_folder_button.hasOnClickListeners())
	    m_move_to_folder_button.setOnClickListener
		(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(m_lettera.get() != null &&
		       m_lettera.get().isFinishing())
			return;

		    MoveMessages move_messages = new MoveMessages
			(m_lettera.get(),
			 m_email_account,
			 m_folder_name,
			 m_view,
			 m_oid);

		    move_messages.show(m_move_to_folder_button);
		}
	    });

	if(!m_return_button.hasOnClickListeners())
	    m_return_button.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(m_lettera.get() != null &&
		       m_lettera.get().isFinishing())
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

    public void show(String email_account, String folder_name, long oid)
    {
	m_dialog.show();

	if(folder_name.toLowerCase().contains("trash"))
	    m_delete_button.setVisibility(View.GONE);
	else
	    m_delete_button.setVisibility(View.VISIBLE);

	m_email_account = email_account;
	m_folder_name = folder_name;
	m_from.setText("e-mail@e-mail.org");
	m_from.setTextColor(Lettera.text_color());
	m_oid = oid;
	m_received_date.setTextColor(Lettera.text_color());
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
	    dialog = new Dialog
		(m_lettera.get(),
		 android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
	    Windows.show_progress_dialog
		(m_lettera.get(),
		 dialog,
		 "Downloading content. Please be patient.",
		 null);

	    Thread thread = new Thread
		(new PopulateContainers(dialog,
					email_account,
					folder_name,
					oid));

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
