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
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Lettera extends AppCompatActivity
{
    private class LetteraLinearLayoutManager extends LinearLayoutManager
    {
	LetteraLinearLayoutManager(Context context)
	{
	    super(context);
	}

	@Override
	public void onLayoutChildren(RecyclerView.Recycler recycler,
				     RecyclerView.State state)
	{
	    try
	    {
		super.onLayoutChildren(recycler, state);
	    }
	    catch(Exception exception)
	    {
	    }
	}

	@Override
	public void onScrollStateChanged(int state)
	{
	    switch(state)
	    {
            case RecyclerView.SCROLL_STATE_DRAGGING:
	    case RecyclerView.SCROLL_STATE_IDLE:
	    case RecyclerView.SCROLL_STATE_SETTLING:
		show_scroll_bottom();
		show_scroll_top();
                break;
	    }
	}
    }

    private class PopulateContainers implements Runnable
    {
	private Dialog m_dialog = null;

	private PopulateContainers(Dialog dialog)
	{
	    m_dialog = dialog;
	}

	@Override
	public void run()
	{
	    try
	    {
		byte bytes[][] = m_database.read_pgp_pair("encryption");

		m_pgp.set_encryption_key_pair
		    (PGP.key_pair_from_bytes(bytes[0], bytes[1]));
	    }
	    catch(Exception exception)
	    {
		m_pgp.set_encryption_key_pair(null);
	    }

	    try
	    {
		byte bytes[][] = m_database.read_pgp_pair("signature");

		m_pgp.set_signature_key_pair
		    (PGP.key_pair_from_bytes(bytes[0], bytes[1]));
	    }
	    catch(Exception exception)
	    {
		m_pgp.set_signature_key_pair(null);
	    }

	    Lettera.this.runOnUiThread(new Runnable()
	    {
		@Override
		public void run()
		{
		    populate_folders_from_database();

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

    private class PopulateFolders implements Runnable
    {
	private Dialog m_dialog = null;
	private String m_folder_name = "";

	private PopulateFolders(Dialog dialog, String folder_name)
	{
	    m_dialog = dialog;
	    m_folder_name = folder_name;
	}

	@Override
	public void run()
	{
	    try
	    {
		EmailElement email_element = m_database.email_element
		    (m_database.setting("primary_email_account"));
		Mail mail = new Mail
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
		m_database.write_folders
		    (mail.folder_elements(), email_element.m_inbound_email);
		m_database.write_messages
		    (mail.folder(m_folder_name), email_element.m_inbound_email);
	    }
	    catch(Exception exception)
	    {
	    }

	    Lettera.this.runOnUiThread(new Runnable()
	    {
		@Override
		public void run()
		{
		    try
		    {
			m_adapter.notifyDataSetChanged();
			m_folders_drawer.set_email_address
			    (m_database.setting("primary_email_account"));
			m_folders_drawer.update();
			m_items_count.setText
			    ("Items: " + m_adapter.getItemCount());
			m_layout_manager.scrollToPosition
			    (m_adapter.getItemCount() - 1);
			show_scroll_bottom();
			show_scroll_top();
		    }
		    catch(Exception exception)
		    {
		    }

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

    private Button m_compose_button = null;
    private Button m_contacts_button = null;
    private Button m_download_button = null;
    private Button m_messaging_button = null;
    private Button m_settings_button = null;
    private Database m_database = null;
    private FoldersDrawer m_folders_drawer = null;
    private ImageButton m_folders_drawer_button = null;
    private ImageButton m_scroll_bottom = null;
    private ImageButton m_scroll_top = null;
    private LetteraLinearLayoutManager m_layout_manager = null;
    private MessagesAdapter m_adapter = null;
    private RecyclerView m_recycler = null;
    private ScheduledExecutorService m_folders_drawer_scheduler = null;
    private Settings m_settings = null;
    private String m_selected_folder_name = "";
    private TextView m_current_folder = null;
    private TextView m_items_count = null;
    private View m_vertical_separator = null;
    private final Object m_selected_folder_name_mutex = new Object();
    private final PGP m_pgp = PGP.instance();
    private final int FOLDERS_DRAWER_INTERVAL = 5;
    private final long HIDE_SCROLL_TO_BUTTON_DELAY = 3000;

    private String selected_folder_full_name()
    {
	synchronized(m_selected_folder_name_mutex)
	{
	    return m_database.folder_full_name
		(m_database.setting("primary_email_account"),
		 m_selected_folder_name);
	}
    }

    private String selected_folder_name()
    {
	synchronized(m_selected_folder_name_mutex)
	{
	    return m_selected_folder_name;
	}
    }

    private boolean can_scroll_bottom()
    {
	return m_layout_manager.findFirstCompletelyVisibleItemPosition() > 0;
    }

    private boolean can_scroll_top()
    {
	return m_layout_manager.findLastCompletelyVisibleItemPosition() <
	    m_adapter.getItemCount() - 1;
    }

    private void download()
    {
	Dialog dialog = null;

	try
	{
	    dialog = new Dialog(Lettera.this);
	    Windows.show_progress_dialog
		(Lettera.this,
		 dialog,
		 "Downloading e-mail folders and messages.\n" +
		 "Please be patient.");

	    Thread thread = new Thread
		(new PopulateFolders(dialog, selected_folder_full_name()));

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

    private void initialize_widget_members()
    {
	m_compose_button = (Button) findViewById(R.id.compose_button);
	m_contacts_button = (Button) findViewById(R.id.contacts_button);
	m_current_folder = (TextView) findViewById(R.id.current_folder);
	m_download_button = (Button) findViewById(R.id.download_button);
	m_folders_drawer_button = (ImageButton) findViewById
	    (R.id.folders_drawer_button);
	m_items_count = (TextView) findViewById(R.id.message_count);
	m_messaging_button = (Button) findViewById(R.id.messaging_button);
	m_recycler = (RecyclerView) findViewById(R.id.messages);
	m_scroll_bottom = (ImageButton) findViewById(R.id.scroll_bottom);
	m_scroll_top = (ImageButton) findViewById(R.id.scroll_top);
	m_settings_button = (Button) findViewById(R.id.settings_button);
	m_vertical_separator = findViewById(R.id.vertical_separator);
    }

    private void prepare_listeners()
    {
	if(m_download_button != null && !m_download_button.
	                                 hasOnClickListeners())
	    m_download_button.setOnClickListener(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(Lettera.this.isFinishing())
			return;

		    download();
		}
	    });

	if(m_folders_drawer_button != null && !m_folders_drawer_button.
	                                       hasOnClickListeners())
	    m_folders_drawer_button.setOnClickListener
		(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(Lettera.this.isFinishing() || m_folders_drawer == null)
			return;

		    m_folders_drawer.show();
		}
	    });

	if(m_scroll_bottom != null && !m_scroll_bottom.hasOnClickListeners())
	    m_scroll_bottom.setOnClickListener(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(Lettera.this.isFinishing())
			return;

		    m_layout_manager.scrollToPosition(0);
		    show_scroll_bottom();
		    show_scroll_top();
		}
	    });

	if(m_scroll_top != null && !m_scroll_top.hasOnClickListeners())
	    m_scroll_top.setOnClickListener(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(Lettera.this.isFinishing())
			return;

		    m_layout_manager.scrollToPosition
			(m_adapter.getItemCount() - 1);
		    show_scroll_bottom();
		    show_scroll_top();
		}
	    });

	if(m_settings_button != null && !m_settings_button.
	                                 hasOnClickListeners())
	    m_settings_button.setOnClickListener(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(Lettera.this.isFinishing() || m_settings == null)
			return;

		    m_settings.show();
		}
	    });
    }

    private void prepare_schedulers()
    {
	if(m_folders_drawer_scheduler == null)
	{
	    m_folders_drawer_scheduler = Executors.
		newSingleThreadScheduledExecutor();
	    m_folders_drawer_scheduler.scheduleAtFixedRate(new Runnable()
	    {
		private ArrayList<String> m_folder_names = null;
		private Mail m_mail = null;

		@Override
		public void run()
		{
		    try
		    {
			EmailElement email_element = m_database.email_element
			    (m_database.setting("primary_email_account"));

			if(email_element == null)
			    return;

			if(m_mail != null)
			{
			    if(!m_mail.email_address().equals(email_element.
							      m_inbound_email))
			    {
				m_mail.disconnect();
				m_mail = null;
			    }
			    else if(!m_mail.imap_connected())
				m_mail.disconnect();
			}

			if(m_mail == null)
			    m_mail = new Mail
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
			else
			    m_mail.connect_imap();

			if(m_mail != null && m_mail.imap_connected())
			{
			    m_database.write_folders
				(m_mail.folder_elements(),
				 m_mail.email_address());
			    m_database.write_messages
				(m_mail.folder(selected_folder_full_name()),
				 m_mail.email_address());

			    if(m_folder_names == null ||
			       m_folder_names.isEmpty())
				m_folder_names = m_mail.folder_full_names();

			    if(m_folder_names != null &&
			       m_folder_names.size() > 0)
			    {
				m_database.write_messages
				    (m_mail.folder(m_folder_names.get(0)),
				     m_mail.email_address());
				m_folder_names.remove(0);
			    }

			    Lettera.this.runOnUiThread(new Runnable()
			    {
				@Override
				public void run()
				{
				    m_adapter.notifyDataSetChanged();
				    m_folders_drawer.update();
				    m_items_count.setText
					("Items: " + m_adapter.getItemCount());
				}
			    });
			}
		    }
		    catch(Exception exception)
		    {
		    }
		}
	    }, 5, FOLDERS_DRAWER_INTERVAL, TimeUnit.SECONDS);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
	m_database = Database.instance(getApplicationContext());

	/*
	** JavaMail may open sockets on the main thread. StrictMode
	** is not available in older versions of Android.
	*/

	StrictMode.ThreadPolicy policy = new
	    StrictMode.ThreadPolicy.Builder().permitAll().build();

	StrictMode.setThreadPolicy(policy);
	setContentView(R.layout.activity_lettera);

	/*
	** Prepare the rest.
	*/

	initialize_widget_members();
	m_adapter = new MessagesAdapter(getApplicationContext());
	m_folders_drawer = new FoldersDrawer
	    (Lettera.this, findViewById(R.id.main_layout));
	m_layout_manager = new LetteraLinearLayoutManager
	    (getApplicationContext());
	m_layout_manager.setOrientation(LinearLayoutManager.VERTICAL);
	m_layout_manager.setReverseLayout(true);
	m_layout_manager.setSmoothScrollbarEnabled(true);
	m_layout_manager.setStackFromEnd(true);
	m_recycler.setAdapter(m_adapter);
	m_recycler.setLayoutManager(m_layout_manager);
	m_recycler.setHasFixedSize(true);
	m_scroll_bottom.setVisibility(View.GONE);
	m_scroll_top.setVisibility(View.GONE);
	m_selected_folder_name = m_database.setting
	    ("selected_folder_name_" +
	     m_database.setting("primary_email_account"));

	if(m_selected_folder_name.isEmpty())
	    m_selected_folder_name = "Inbox";

	m_settings = new Settings(Lettera.this, findViewById(R.id.main_layout));
	new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
	{
	    @Override
	    public void run()
	    {
		Dialog dialog = null;

		try
		{
		    dialog = new Dialog(Lettera.this);
		    Windows.show_progress_dialog
			(Lettera.this,
			 dialog,
			 "Initializing Lettera.\nPlease be patient.");

		    Thread thread = new Thread(new PopulateContainers(dialog));

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
	}, 500);
	prepare_folders_and_messages_widgets(m_selected_folder_name);
	prepare_generic_widgets();
	prepare_icons();
	prepare_listeners();
	prepare_schedulers();
    }

    @Override
    protected void onDestroy()
    {
	super.onDestroy();

	try
	{
	    m_folders_drawer.dismiss();
	}
	catch(Exception exception)
	{
	}
    }

    @Override
    protected void onPause()
    {
	super.onPause();

	try
	{
	    m_folders_drawer.dismiss();
	}
	catch(Exception exception)
	{
	}
    }

    @Override
    public void onConfigurationChanged(Configuration new_config)
    {
	super.onConfigurationChanged(new_config);

	try
	{
	    m_folders_drawer.dismiss();
	}
	catch(Exception exception)
	{
	}
    }

    public void populate_folders_from_database()
    {
	try
	{
	    m_folders_drawer.update();
	}
	catch(Exception exception)
	{
	}
    }

    public void prepare_folders_and_messages_widgets(String folder_name)
    {
	SettingsElement settings_element = m_database.settings_element
	    ("primary_email_account");

	if(settings_element != null)
	{
	    m_adapter.set_email_address(settings_element.m_value);
	    m_folders_drawer.set_email_address(settings_element.m_value);
	}

	if(!folder_name.isEmpty())
	{
	    m_adapter.set_folder_name(folder_name);
	    m_current_folder.setText(folder_name);
	    m_folders_drawer.set_selected_folder_name(folder_name);
	    m_items_count.setText("Items: " + m_adapter.getItemCount());
	}

	try
	{
	    m_adapter.notifyDataSetChanged();
	    m_layout_manager.scrollToPosition(m_adapter.getItemCount() - 1);
	}
	catch(Exception exception)
	{
	}

	if(!folder_name.isEmpty())
	    synchronized(m_selected_folder_name_mutex)
	    {
		m_selected_folder_name = folder_name;
	    }

	show_scroll_bottom();
	show_scroll_top();
    }

    public void prepare_generic_widgets()
    {
	SettingsElement settings_element = m_database.settings_element
	    ("show_vertical_separator_before_settings");

	if(settings_element == null || settings_element.m_value.equals("true"))
	    m_vertical_separator.setVisibility(View.VISIBLE);
	else
	    m_vertical_separator.setVisibility(View.GONE);
    }

    public void prepare_icons()
    {
	SettingsElement settings_element = m_database.settings_element
	    ("icon_theme");

	if(settings_element == null)
	{
	    m_compose_button.setBackgroundResource
		(Settings.icon_from_name("default_compose"));
	    m_contacts_button.setBackgroundResource
		(Settings.icon_from_name("default_contacts"));
	    m_download_button.setBackgroundResource
		(Settings.icon_from_name("default_download"));
	    m_messaging_button.setBackgroundResource
		(Settings.icon_from_name("default_messaging"));
	    m_settings_button.setBackgroundResource
		(Settings.icon_from_name("default_settings"));
	}
	else
	{
	    m_compose_button.setBackgroundResource
		(Settings.
		 icon_from_name(settings_element.m_value + "_compose"));
	    m_contacts_button.setBackgroundResource
		(Settings.
		 icon_from_name(settings_element.m_value + "_contacts"));
	    m_download_button.setBackgroundResource
		(Settings.
		 icon_from_name(settings_element.m_value + "_download"));
	    m_messaging_button.setBackgroundResource
		(Settings.
		 icon_from_name(settings_element.m_value + "_messaging"));
	    m_settings_button.setBackgroundResource
		(Settings.
		 icon_from_name(settings_element.m_value + "_settings"));
	}
    }

    private void show_scroll_bottom()
    {
	new Handler(Looper.getMainLooper()).post(new Runnable()
	{
	    @Override
	    public void run()
	    {
		m_scroll_bottom.setVisibility
		    (can_scroll_bottom() ? View.VISIBLE : View.GONE);
	    }
	});
    }

    private void show_scroll_top()
    {
	new Handler(Looper.getMainLooper()).post(new Runnable()
	{
	    @Override
	    public void run()
	    {
		m_scroll_top.setVisibility
		    (can_scroll_top() ? View.VISIBLE : View.GONE);
	    }
	});
    }
}
