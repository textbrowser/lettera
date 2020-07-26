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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Lettera extends AppCompatActivity
{
    private class LetteraBroadcastReceiver extends BroadcastReceiver
    {
	public LetteraBroadcastReceiver()
	{
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
	    if(intent == null || intent.getAction() == null)
		return;

	    switch(intent.getAction())
	    {
	    case "org.purple.lettera.set_message_selected":
		prepare_current_folder_widgets();
		break;
	    case "org.purple.lettera.set_messages_unread":
		m_messages_adapter.notifyDataSetChanged();
		break;
	    default:
		break;
	    }
	}
    }

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
	    m_database.purge_dangling();

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
	private String m_folder_full_name = "";
	private boolean m_connected = false;

	private PopulateFolders(Dialog dialog,
				String folder_full_name)
	{
	    m_dialog = dialog;
	    m_folder_full_name = folder_full_name;
	}

	@Override
	public void run()
	{
	    Mail mail = null;

	    try
	    {
		EmailElement email_element = m_database.email_element
		    (email_account());

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

		if((m_connected = mail.imap_connected()))
		{
		    m_database.write_folders
			(mail.folder_elements(m_download_interrupted),
			 email_element.m_inbound_email);
		    m_database.write_messages
			(m_download_interrupted,
			 mail.folder(m_folder_full_name),
			 email_element.m_inbound_email,
			 false,
			 true);
		}
	    }
	    catch(Exception exception)
	    {
	    }
	    finally
	    {
		if(mail != null)
		    mail.disconnect();
	    }

	    Lettera.this.runOnUiThread(new Runnable()
	    {
		@Override
		public void run()
		{
		    if(m_connected)
			try
			{
			    m_messages_adapter.notifyDataSetChanged();
			    m_folders_drawer.set_email_account(email_account());
			    m_folders_drawer.update();
			    m_layout_manager.scrollToPosition
				(m_messages_adapter.getItemCount() - 1);
			    m_scroll_bottom.setVisibility(View.GONE);
			    m_scroll_top.setVisibility(View.GONE);
			    prepare_current_folder_text(selected_folder_name());
			    prepare_current_folder_widgets();
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

    public interface MessageClickListener
    {
	void onClick(View view, int position);
	void onLongClick(View view, int position);
    }

    private Button m_artificial_button = null;
    private Button m_compose_button = null;
    private Button m_delete_button = null;
    private Button m_download_button = null;
    private Button m_exit_lettera = null;
    private Button m_mark_as_unread = null;
    private Button m_move_to_folder_button = null;
    private Button m_settings_button = null;
    private CheckBox m_select_all_checkbox = null;
    private CompoundButton.OnCheckedChangeListener
	m_select_all_checkbox_listener = null;
    private Database m_database = null;
    private FoldersDrawer m_folders_drawer = null;
    private Handler m_scroll_hander = null;
    private ImageButton m_folders_drawer_button = null;
    private ImageButton m_scroll_bottom = null;
    private ImageButton m_scroll_top = null;
    private Letter m_letter_dialog = null;
    private LetteraBroadcastReceiver m_receiver = null;
    private LetteraLinearLayoutManager m_layout_manager = null;
    private LinearLayout m_status_bar = null;
    private MessagesAdapter m_messages_adapter = null;
    private Object m_status_message_mutex = new Object();
    private RecyclerView m_recycler = null;
    private Runnable m_scroll_runnable = null;
    private ScheduledExecutorService m_folders_drawer_schedule = null;
    private ScheduledExecutorService m_status_message_schedule = null;
    private String m_selected_folder_name = "";
    private String m_status_message = "";
    private TextView m_current_folder = null;
    private TextView m_items_count = null;
    private boolean m_receiver_registered = false;
    private final AtomicBoolean m_download_interrupted =
	new AtomicBoolean(false);
    private final AtomicBoolean m_scrolling = new AtomicBoolean(false);
    private final AtomicInteger m_folders_drawer_interval =
	new AtomicInteger(7500);
    private final Object m_selected_folder_name_mutex = new Object();
    private final PGP m_pgp = PGP.instance();
    private final static AtomicInteger s_background_color = new AtomicInteger
	(Color.WHITE);
    private final static AtomicInteger s_divider_color = new AtomicInteger
	(Color.GRAY);
    private final static AtomicInteger s_text_color = new AtomicInteger
	(Color.BLACK);
    private final static int SELECTION_COLOR = Color.parseColor("#bbdefb");
    private final static long HIDE_SCROLL_TO_BUTTON_DELAY = 2500L;
    private final static long SCHEDULE_AWAIT_TERMINATION_TIMEOUT = 60L;
    private final static long STATUS_MESSAGE_INTERVAL = 2500L;
    private int m_selected_position = -1;
    private static Lettera s_instance = null;
    private static int s_default_background_color = 0;
    private static int s_default_divider_color = 0;
    private static int s_default_text_color = 0;
    public final static String NONE_FOLDER = "(Please Select)";

    private String email_account()
    {
	return m_database.primary_email_account();
    }

    private String selected_folder_full_name()
    {
	synchronized(m_selected_folder_name_mutex)
	{
	    return m_database.folder_full_name
		(email_account(), m_selected_folder_name);
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
	    m_messages_adapter.getItemCount() - 1;
    }

    private void download()
    {
	m_download_interrupted.set(false);

	Dialog dialog = null;

	try
	{
	    dialog = new Dialog
		(Lettera.this,
		 android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
	    Windows.show_progress_dialog
		(Lettera.this,
		 dialog,
		 "Downloading e-mail folders and messages.\n" +
		 "Please be patient.",
		 m_download_interrupted);

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
	m_artificial_button = (Button) findViewById(R.id.artificial_button);
	m_compose_button = (Button) findViewById(R.id.compose_button);
	m_current_folder = (TextView) findViewById(R.id.current_folder);
	m_delete_button = (Button) findViewById(R.id.delete_button);
	m_download_button = (Button) findViewById(R.id.download_button);
	m_exit_lettera = (Button) findViewById(R.id.exit_lettera);
	m_folders_drawer_button = (ImageButton) findViewById
	    (R.id.folders_drawer_button);
	m_items_count = (TextView) findViewById(R.id.message_count);
	m_mark_as_unread = (Button) findViewById(R.id.mark_as_unread);
	m_move_to_folder_button = (Button) findViewById(R.id.move_to_folder);
	m_recycler = (RecyclerView) findViewById(R.id.messages);
	m_scroll_bottom = (ImageButton) findViewById(R.id.scroll_bottom);
	m_scroll_top = (ImageButton) findViewById(R.id.scroll_top);
	m_select_all_checkbox = (CheckBox) findViewById
	    (R.id.select_all_checkbox);
	m_settings_button = (Button) findViewById(R.id.settings_button);
	m_status_bar = (LinearLayout) findViewById(R.id.status_bar);
    }

    private void prepare_listeners()
    {
	if(m_artificial_button != null && !m_artificial_button.
	                                   hasOnClickListeners())
	    m_artificial_button.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		}
	    });

	if(m_delete_button != null && !m_delete_button.hasOnClickListeners())
	    m_delete_button.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(Lettera.this.isFinishing())
			return;

		    final String email_account = email_account();
		    final String folder_name = selected_folder_name();

		    if(m_database.
		       messages_selected(email_account,
					 selected_folder_name()) == 0)
			return;

		    final AtomicBoolean confirmed = new AtomicBoolean(false);

		    DialogInterface.OnCancelListener listener =
			new DialogInterface.OnCancelListener()
		    {
			public void onCancel(DialogInterface dialog)
			{
			    if(confirmed.get())
				m_database.delete_selected_messages
				    (Lettera.this,
				     m_messages_adapter,
				     email_account,
				     folder_name);
			}
		    };

		    Windows.show_prompt_dialog
			(Lettera.this,
			 listener,
			 "Delete the selected message(s)?",
			 confirmed);
		}
	    });

	if(m_download_button != null && !m_download_button.
	                                 hasOnClickListeners())
	    m_download_button.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(Lettera.this.isFinishing())
			return;

		    download();
		}
	    });

	if(m_exit_lettera != null && !m_exit_lettera.hasOnClickListeners())
	    m_exit_lettera.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(Lettera.this.isFinishing())
			return;

		    final AtomicBoolean confirmed = new AtomicBoolean(false);

		    DialogInterface.OnCancelListener listener =
			new DialogInterface.OnCancelListener()
		    {
			public void onCancel(DialogInterface dialog)
			{
			    if(confirmed.get())
			    {
				LetteraService.stopForegroundTask
				    (getApplicationContext());
				finishAndRemoveTask();
				android.os.Process.killProcess
				    (android.os.Process.myPid());
			    }
			}
		    };

		    Windows.show_prompt_dialog
			(Lettera.this, listener, "Exit Lettera?", confirmed);
		}
	    });

	if(m_folders_drawer_button != null && !m_folders_drawer_button.
	                                       hasOnClickListeners())
	    m_folders_drawer_button.setOnClickListener
		(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(Lettera.this.isFinishing() || m_folders_drawer == null)
			return;

		    m_folders_drawer.show();
		}
	    });

	if(m_mark_as_unread != null && !m_mark_as_unread.hasOnClickListeners())
	    m_mark_as_unread.setOnClickListener
		(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(Lettera.this.isFinishing())
			return;

		    m_database.set_messages_unread
			(Lettera.this, email_account(), selected_folder_name());
		}
	    });

	if(m_move_to_folder_button != null && !m_move_to_folder_button.
	                                       hasOnClickListeners())
	    m_move_to_folder_button.setOnClickListener
		(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(Lettera.this.isFinishing() || m_folders_drawer == null)
			return;

		    String email_account = email_account();

		    if(m_database.
		       messages_selected
		       (email_account, selected_folder_name()) == 0)
			return;

		    MoveMessages move_messages = new MoveMessages
			(Lettera.this,
			 email_account,
			 selected_folder_name(),
			 findViewById(R.id.main_layout),
			 -1);

		    move_messages.show(m_move_to_folder_button);
		}
	    });

	if(m_scroll_bottom != null && !m_scroll_bottom.hasOnClickListeners())
	    m_scroll_bottom.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(Lettera.this.isFinishing())
			return;

		    m_layout_manager.scrollToPosition(0);
		    m_scroll_bottom.setVisibility(View.GONE);
		    m_scroll_hander.removeCallbacks(m_scroll_runnable);
		    m_scroll_hander.postDelayed
			(m_scroll_runnable, HIDE_SCROLL_TO_BUTTON_DELAY);
		    new Handler(Looper.getMainLooper()).post(new Runnable()
		    {
			@Override
			public void run()
			{
			    if(can_scroll_top())
				m_scroll_top.setVisibility(View.VISIBLE);
			}
		    });
		}
	    });

	if(m_scroll_top != null && !m_scroll_top.hasOnClickListeners())
	    m_scroll_top.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(Lettera.this.isFinishing())
			return;

		    m_layout_manager.scrollToPosition
			(m_messages_adapter.getItemCount() - 1);
		    m_scroll_hander.removeCallbacks(m_scroll_runnable);
		    m_scroll_hander.postDelayed
			(m_scroll_runnable, HIDE_SCROLL_TO_BUTTON_DELAY);
		    m_scroll_top.setVisibility(View.GONE);
		    new Handler(Looper.getMainLooper()).post(new Runnable()
		    {
			@Override
			public void run()
			{
			    if(can_scroll_bottom())
				m_scroll_bottom.setVisibility(View.VISIBLE);
			}
		    });
		}
	    });

	if(m_select_all_checkbox_listener == null)
	{
	    m_select_all_checkbox_listener =
		new CompoundButton.OnCheckedChangeListener()
	    {
		@Override
		public void onCheckedChanged
		    (CompoundButton button_view, final boolean is_checked)
		{
		    new Handler
			(Looper.getMainLooper()).post(new Runnable()
		    {
			@Override
			public void run()
			{
			    m_database.select_all_messages
				(Lettera.this,
				 m_messages_adapter,
				 email_account(),
				 selected_folder_name(),
				 is_checked);
			}
		    });
		}
	    };
	    m_select_all_checkbox.setOnCheckedChangeListener
		(m_select_all_checkbox_listener);
	}

	if(m_settings_button != null && !m_settings_button.
	                                 hasOnClickListeners())
	    m_settings_button.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(Lettera.this.isFinishing())
			return;

		    Settings settings = new Settings
			(Lettera.this, findViewById(R.id.main_layout));

		    settings.show();
		}
	    });
    }

    private void prepare_schedules()
    {
	if(m_folders_drawer_schedule == null)
	{
	    EmailElement email_element = m_database.email_element
		(email_account());

	    if(email_element != null)
		m_folders_drawer_interval.set
		    (Integer.valueOf(email_element.m_query_interval));
	    else
		m_folders_drawer_interval.set(7500);

	    m_folders_drawer_schedule = Executors.
		newSingleThreadScheduledExecutor();
	    m_folders_drawer_schedule.scheduleAtFixedRate(new Runnable()
	    {
		private ArrayList<String> m_folder_names = null;
		private Mail m_mail = null;
		private final AtomicBoolean m_interrupted =
		    new AtomicBoolean(false);
		private final AtomicLong m_last_tick = new AtomicLong
		    (System.currentTimeMillis());

		@Override
		public void run()
		{
		    try
		    {
			synchronized(m_status_message_mutex)
			{
			    m_status_message = "";
			}

			if(Math.abs(System.currentTimeMillis() -
				    m_last_tick.get()) <
			   (long) m_folders_drawer_interval.get())
			    return;

			EmailElement email_element = m_database.email_element
			    (email_account());

			if(email_element == null)
			{
			    if(m_mail != null)
			    {
				m_mail.disconnect();
				m_mail = null;
			    }

			    m_last_tick.set(System.currentTimeMillis());
			    return;
			}

			if(m_mail != null)
			{
			    if(!m_mail.email_account().equals(email_element.
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
			    Lettera.this.runOnUiThread(new Runnable()
			    {
				@Override
				public void run()
				{
				    synchronized(m_status_message_mutex)
				    {
					m_status_message = " (Downloading " +
					    selected_folder_name() +
					    ".)";
				    }
				}
			    });

			    m_database.write_folders
				(m_mail.folder_elements(m_interrupted),
				 m_mail.email_account());
			    m_database.write_messages
				(m_interrupted,
				 m_mail.folder(selected_folder_full_name()),
				 m_mail.email_account(),
				 true,
				 false);

			    if(m_folder_names == null ||
			       m_folder_names.isEmpty())
				m_folder_names = m_mail.folder_full_names();

			    if(m_folder_names != null &&
			       m_folder_names.size() > 0)
			    {
				final String folder_name =
				    m_mail.
				    folder(m_folder_names.get(0)) != null ?
				    m_mail.folder(m_folder_names.get(0)).
				    getName() : "";

				if(!folder_name.isEmpty())
				    Lettera.this.runOnUiThread(new Runnable()
				    {
					@Override
					public void run()
					{
					    synchronized(m_status_message_mutex)
					    {
						m_status_message =
						    " (Downloading " +
						    folder_name +
						    ".)";
					    }

					    m_messages_adapter.
						notifyDataSetChanged();
					    m_folders_drawer.update();
					    prepare_current_folder_widgets();
					}
				    });

				m_database.write_messages
				    (m_interrupted,
				     m_mail.folder(m_folder_names.get(0)),
				     m_mail.email_account(),
				     true,
				     false);
				m_folder_names.remove(0);
			    }

			    if(!m_scrolling.get())
				Lettera.this.runOnUiThread(new Runnable()
				{
				    @Override
				    public void run()
				    {
					m_messages_adapter.
					    notifyDataSetChanged();
					m_folders_drawer.update();
					prepare_current_folder_text
					    (selected_folder_name());
					prepare_current_folder_widgets();
				    }
				});
			}

			m_last_tick.set(System.currentTimeMillis());
		    }
		    catch(Exception exception)
		    {
		    }
		}
	    }, 5L, 250L, TimeUnit.MILLISECONDS);
        }

	if(m_status_message_schedule == null)
	{
	    m_status_message_schedule = Executors.
		newSingleThreadScheduledExecutor();
	    m_status_message_schedule.scheduleAtFixedRate(new Runnable()
	    {
		@Override
		public void run()
		{
		    try
		    {
			Lettera.this.runOnUiThread(new Runnable()
			{
			    @Override
			    public void run()
			    {
				synchronized(m_status_message_mutex)
				{
				    if(!m_status_message.isEmpty())
					m_items_count.setText
					    ("Items: " +
					     m_messages_adapter.getItemCount() +
					     m_status_message);
				}
			    }
			});
		    }
		    catch(Exception exception)
		    {
		    }
		}
	    }, 5L, STATUS_MESSAGE_INTERVAL, TimeUnit.MILLISECONDS);
	}
    }

    private void stop_schedules()
    {
	if(m_folders_drawer_schedule != null)
	{
	    try
	    {
		m_folders_drawer_schedule.shutdown();
	    }
	    catch(Exception exception)
	    {
	    }

	    try
	    {
		if(!m_folders_drawer_schedule.
		   awaitTermination(SCHEDULE_AWAIT_TERMINATION_TIMEOUT,
				    TimeUnit.SECONDS))
		    m_folders_drawer_schedule.shutdownNow();
	    }
	    catch(Exception exception)
	    {
	    }
	    finally
	    {
		m_folders_drawer_schedule = null;
	    }
	}

	if(m_status_message_schedule != null)
	{
	    try
	    {
		m_status_message_schedule.shutdown();
	    }
	    catch(Exception exception)
	    {
	    }

	    try
	    {
		if(!m_status_message_schedule.
		   awaitTermination(SCHEDULE_AWAIT_TERMINATION_TIMEOUT,
				    TimeUnit.SECONDS))
		    m_status_message_schedule.shutdownNow();
	    }
	    catch(Exception exception)
	    {
	    }
	    finally
	    {
		m_status_message_schedule = null;
	    }
	}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
	m_receiver = new LetteraBroadcastReceiver();
	s_instance = this;
	LetteraService.startForegroundTask(getApplicationContext());
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
	s_default_background_color = Color.WHITE;
	s_default_divider_color = Color.parseColor("#e0e0e0");
	s_default_text_color = m_current_folder.getCurrentTextColor();
	m_messages_adapter = new MessagesAdapter(getApplicationContext(), this);
	m_folders_drawer = new FoldersDrawer
	    (Lettera.this, findViewById(R.id.main_layout));
	m_layout_manager = new LetteraLinearLayoutManager
	    (getApplicationContext());
	m_layout_manager.setOrientation(LinearLayoutManager.VERTICAL);
	m_layout_manager.setReverseLayout(true);
	m_layout_manager.setSmoothScrollbarEnabled(true);
	m_layout_manager.setStackFromEnd(true);
	m_letter_dialog = new Letter
	    (Lettera.this, m_messages_adapter, findViewById(R.id.main_layout));
	m_recycler.addOnItemTouchListener
	    (new
	     MessagesRecyclerTouchListener(Lettera.this,
					   m_recycler,
					   new MessagesRecyclerTouchListener.
					   ClickListener()
	    {
		@Override
		public void onClick(View view, int position)
		{
		}

		@Override
		public void onLongClick(View view, int position)
		{
		    m_artificial_button.performClick();

		    if(m_layout_manager.
		       findViewByPosition(m_selected_position) != null)
			m_layout_manager.findViewByPosition
			    (m_selected_position).setBackgroundColor
			    (background_color());

		    m_letter_dialog.show
			(email_account(), selected_folder_name(), position);
		    m_selected_position = position;
		    view.setBackgroundColor(SELECTION_COLOR);
		}
	    }));
	m_recycler.addOnScrollListener
	    (new RecyclerView.OnScrollListener()
	    {
		public void onScrollStateChanged
		    (RecyclerView recycler_view, int new_state)
		{
		    if(new_state == RecyclerView.SCROLL_STATE_IDLE)
		    {
			m_scroll_hander.removeCallbacks(m_scroll_runnable);
			m_scroll_hander.postDelayed
			    (m_scroll_runnable, HIDE_SCROLL_TO_BUTTON_DELAY);
			m_scrolling.set(false);
		    }
		    else
			m_scrolling.set(true);
		}

		public void onScrolled
		    (RecyclerView recycler_view, int dx, int dy)
		{
		    if(can_scroll_bottom() && dy != 0)
			m_scroll_bottom.setVisibility(View.VISIBLE);

		    if(can_scroll_top() && dy != 0)
			m_scroll_top.setVisibility(View.VISIBLE);

		    if(dy != 0)
			m_scroll_hander.removeCallbacks(m_scroll_runnable);

		    if(m_layout_manager.
		       findViewByPosition(m_selected_position) != null)
			m_layout_manager.findViewByPosition
			    (m_selected_position).setBackgroundColor
			    (SELECTION_COLOR);
		}
	    });
	m_recycler.getViewTreeObserver().addOnGlobalLayoutListener
	    (new ViewTreeObserver.OnGlobalLayoutListener()
	    {
		@Override
		public void onGlobalLayout()
		{
		    if(m_layout_manager.
		       findViewByPosition(m_selected_position) != null)
			m_layout_manager.findViewByPosition
			    (m_selected_position).setBackgroundColor
			    (SELECTION_COLOR);
		}
	    });
	m_recycler.setAdapter(m_messages_adapter);
	m_recycler.setLayoutManager(m_layout_manager);
	m_recycler.setHasFixedSize(true);
	m_scroll_bottom.setVisibility(View.GONE);
	m_scroll_hander = new Handler(Looper.getMainLooper());
	m_scroll_runnable = new Runnable()
	{
	    @Override
	    public void run()
	    {
		m_scroll_bottom.setVisibility(View.GONE);
		m_scroll_top.setVisibility(View.GONE);
	    }
	};
	m_scroll_top.setVisibility(View.GONE);

	synchronized(m_selected_folder_name_mutex)
	{
	    m_selected_folder_name = m_database.setting
		("selected_folder_name_" + email_account());

	    if(m_selected_folder_name.isEmpty())
		m_selected_folder_name = NONE_FOLDER;
	}

	new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
	{
	    @Override
	    public void run()
	    {
		Dialog dialog = null;

		try
		{
		    dialog = new Dialog
			(Lettera.this,
			 android.R.style.
			 Theme_DeviceDefault_Dialog_NoActionBar);
		    Windows.show_progress_dialog
			(Lettera.this,
			 dialog,
			 "Initializing Lettera.\nPlease be patient.",
			 null);

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
	}, 750);
	prepare_colors(m_database.setting("color_theme"));
	prepare_folders_and_messages_widgets(selected_folder_name());
	prepare_generic_widgets();
	prepare_icons(m_database.settings_element("icon_theme"));
	prepare_listeners();
	prepare_schedules();
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

	if(m_receiver_registered)
	{
	    LocalBroadcastManager.getInstance(getApplicationContext()).
		unregisterReceiver(m_receiver);
	    m_receiver_registered = false;
	}

	try
	{
	    m_folders_drawer.dismiss();
	}
	catch(Exception exception)
	{
	}
    }

    @Override
    protected void onResume()
    {
	super.onResume();

	if(!m_receiver_registered)
	{
	    IntentFilter intent_filter = new IntentFilter();

	    intent_filter.addAction("org.purple.lettera.set_message_selected");
	    intent_filter.addAction("org.purple.lettera.set_messages_unread");
	    LocalBroadcastManager.getInstance(getApplicationContext()).
		registerReceiver(m_receiver, intent_filter);
	    m_receiver_registered = true;
	}
    }

    public static int background_color()
    {
	return s_background_color.get();
    }

    public static int default_background_color()
    {
	return s_default_background_color;
    }

    public static int default_divider_color()
    {
	return s_default_divider_color;
    }

    public static int default_text_color()
    {
	return s_default_text_color;
    }

    public static int divider_color()
    {
	return s_divider_color.get();
    }

    public static int text_color()
    {
	return s_text_color.get();
    }

    public static synchronized Lettera instance()
    {
	return s_instance;
    }

    public void email_account_deleted()
    {
	String folder_name = m_database.setting
	    ("selected_folder_name_" + email_account());

	if(folder_name.isEmpty())
	    folder_name = NONE_FOLDER;

	prepare_folders_and_messages_widgets(folder_name);
    }

    public void message_read()
    {
	m_messages_adapter.notifyDataSetChanged();
    }

    public void messages_deleted()
    {
	/*
	** All of the messages have been deleted.
	*/

	m_folders_drawer.update();
	m_messages_adapter.notifyDataSetChanged();
	m_selected_position = -1;
	prepare_current_folder_text(selected_folder_name());
	prepare_current_folder_widgets();
    }

    public void move_message(String to_folder_name, long message_oid)
    {
	m_database.move_message
	    (Lettera.this,
	     m_messages_adapter,
	     email_account(),
	     selected_folder_name(),
	     to_folder_name,
	     message_oid);

	try
	{
	    m_letter_dialog.dismiss();
	}
	catch(Exception exception)
	{
	}
    }

    public void move_selected_messages(String to_folder_name)
    {
	m_database.move_selected_messages
	    (Lettera.this,
	     m_messages_adapter,
	     email_account(),
	     selected_folder_name(),
	     to_folder_name);
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

    public void prepare_colors(String color_theme)
    {
	int background_color = s_default_background_color;
	int divider_color = s_default_divider_color;
	int text_color = s_default_text_color;

	if(color_theme != null)
	    switch(color_theme.toLowerCase())
	    {
	    case "black & blue":
		background_color = Color.BLACK;
		divider_color = Color.WHITE;
		text_color = Color.parseColor("#2196f3");
		break;
	    case "black & green":
		background_color = Color.BLACK;
		divider_color = Color.WHITE;
		text_color = Color.parseColor("#66bb6a");
		break;
	    case "grayish & blue":
		background_color = Color.parseColor("#bdbdbd");
		divider_color = Color.WHITE;
		text_color = Color.parseColor("#0d47a1");
		break;
	    case "night":
		background_color = Color.BLACK;
		divider_color = Color.parseColor("#1fffffff");
		text_color = Color.WHITE;
	    default:
		break;
	    }

	findViewById(R.id.bottom_divider).setBackgroundColor(divider_color);
	findViewById(R.id.main_layout).setBackgroundColor(background_color);
	findViewById(R.id.top_divider).setBackgroundColor(divider_color);
	m_current_folder.setTextColor(text_color);
	m_items_count.setTextColor(text_color);
	s_background_color.set(background_color);
	s_divider_color.set(divider_color);
	s_text_color.set(text_color);

	/*
	** Order!
	*/

	Utilities.color_checkbox
	    (m_select_all_checkbox,
	     background_color(),
	     divider_color(),
	     text_color());
	m_messages_adapter.notifyDataSetChanged();
    }

    public void prepare_current_folder_text(String folder_name)
    {
	m_delete_button.setVisibility
	    (folder_name.toLowerCase().equals("trash") ?
	     View.GONE : View.VISIBLE);

	if(m_database.setting("show_status_bar").equals("true"))
	    m_current_folder.setText(folder_name);
	else
	    m_current_folder.setText
		(folder_name + " (" + m_messages_adapter.getItemCount() + ")");

	m_items_count.setText("Items: " + m_messages_adapter.getItemCount());
    }

    public void prepare_current_folder_widgets()
    {
	int count = m_messages_adapter.getItemCount();

	if(count == 0)
	{
	    m_delete_button.setVisibility(View.GONE);
	    m_mark_as_unread.setVisibility(View.GONE);
	    m_move_to_folder_button.setVisibility(View.GONE);
	    m_select_all_checkbox.setChecked(false);
	    m_select_all_checkbox.setEnabled(false);
	}
	else
	{
	    count = m_database.messages_selected
		(email_account(), selected_folder_name());

	    if(count == 0)
		m_delete_button.setVisibility(View.GONE);
	    else
		m_delete_button.setVisibility
		    (m_current_folder.getText().toString().toLowerCase().
		     equals("trash") ? View.GONE : View.VISIBLE);

	    m_mark_as_unread.setVisibility
		(count == 0 ? View.GONE : View.VISIBLE);
	    m_move_to_folder_button.setVisibility
		(count == 0 ? View.GONE : View.VISIBLE);
	    m_select_all_checkbox.setEnabled(true);
	    m_select_all_checkbox.setOnCheckedChangeListener(null);
	    m_select_all_checkbox.setChecked
		(count == m_messages_adapter.getItemCount());
	    m_select_all_checkbox.setOnCheckedChangeListener
		(m_select_all_checkbox_listener);
	}
    }

    public void prepare_folders_and_messages_widgets(String folder_name)
    {
	if(!folder_name.equals(m_messages_adapter.folder_name()) &&
	   !folder_name.isEmpty())
	{
	    m_select_all_checkbox.setOnCheckedChangeListener(null);
	    m_select_all_checkbox.setChecked(false);
	    m_select_all_checkbox.setOnCheckedChangeListener
		(m_select_all_checkbox_listener);
	}

	String email_account = email_account();

	m_folders_drawer.set_email_account(email_account);
	m_messages_adapter.set_email_account(email_account);

	if(!folder_name.isEmpty())
	{
	    m_folders_drawer.set_selected_folder_name(folder_name);
	    m_messages_adapter.set_folder_name(folder_name);
	    m_items_count.setText
		("Items: " + m_messages_adapter.getItemCount());
	}
	else
	{
	    m_folders_drawer.set_selected_folder_name(NONE_FOLDER);
	    m_messages_adapter.set_folder_name(NONE_FOLDER);
	    m_items_count.setText("Items: 0");
	}

	try
	{
	    m_messages_adapter.notifyDataSetChanged();
	    m_layout_manager.scrollToPosition
		(m_messages_adapter.getItemCount() - 1);
	}
	catch(Exception exception)
	{
	}

	m_scroll_bottom.setVisibility(View.GONE);
	m_scroll_top.setVisibility(View.GONE);

	synchronized(m_selected_folder_name_mutex)
	{
	    if(folder_name.isEmpty())
	    {
		m_selected_folder_name = NONE_FOLDER;
		m_selected_position = -1;
	    }
	    else
	    {
		if(!folder_name.equals(m_selected_folder_name))
		    m_selected_position = -1;

		m_selected_folder_name = folder_name;
	    }
	}

	prepare_current_folder_text(selected_folder_name());
	prepare_current_folder_widgets();
    }

    public void prepare_generic_widgets()
    {
	findViewById(R.id.bottom_divider).setVisibility
	    (m_database.setting("show_status_bar").equals("true") ?
	     View.VISIBLE : View.GONE);
	m_status_bar.setVisibility
	    (m_database.setting("show_status_bar").equals("true") ?
	     View.VISIBLE : View.GONE);
    }

    public void prepare_icons(SettingsElement settings_element)
    {
	if(settings_element == null)
	{
	    m_compose_button.setBackgroundResource
		(Settings.icon_from_name("default_compose"));
	    m_download_button.setBackgroundResource
		(Settings.icon_from_name("default_download"));
	    m_settings_button.setBackgroundResource
		(Settings.icon_from_name("default_settings"));
	}
	else
	{
	    m_compose_button.setBackgroundResource
		(Settings.
		 icon_from_name(settings_element.m_value + "_compose"));
	    m_download_button.setBackgroundResource
		(Settings.
		 icon_from_name(settings_element.m_value + "_download"));
	    m_settings_button.setBackgroundResource
		(Settings.
		 icon_from_name(settings_element.m_value + "_settings"));
	}
    }

    public void primary_email_account_changed(String email_account)
    {
	/*
	** The primary e-mail account is now email_account.
	*/
    }

    public void show_email_dialog(int position)
    {
	m_letter_dialog.show(email_account(), selected_folder_name(), position);
    }

    public void reactivate_schedules(int folders_drawer_interval)
    {
	if(folders_drawer_interval != m_folders_drawer_interval.get())
	    m_folders_drawer_interval.set(folders_drawer_interval);
    }

    public void update_folders_drawer()
    {
	m_folders_drawer.update();
    }
}
