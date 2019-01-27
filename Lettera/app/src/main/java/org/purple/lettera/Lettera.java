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
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
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

	private PopulateFolders(Dialog dialog, String folder_full_name)
	{
	    m_dialog = dialog;
	    m_folder_full_name = folder_full_name;
	}

	@Override
	public void run()
	{
	    try
	    {
		EmailElement email_element = m_database.email_element
		    (email_account());
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
		    (mail.folder_elements(m_download_interrupted),
		     email_element.m_inbound_email);
		m_database.write_messages
		    (m_download_interrupted,
		     mail.folder(m_folder_full_name),
		     email_element.m_inbound_email,
		     true);
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
			m_messages_adapter.notifyDataSetChanged();
			m_folders_drawer.set_email_address(email_account());
			m_folders_drawer.update();
			m_layout_manager.scrollToPosition
			    (m_messages_adapter.getItemCount() - 1);
			m_scroll_bottom.setVisibility(View.GONE);
			m_scroll_top.setVisibility(View.GONE);
			prepare_current_folder_text(selected_folder_name());
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
    private Button m_contacts_button = null;
    private Button m_delete_button = null;
    private Button m_download_button = null;
    private Button m_messaging_button = null;
    private Button m_move_to_folder_button = null;
    private Button m_settings_button = null;
    private CheckBox m_all_checkbox = null;
    private CompoundButton.OnCheckedChangeListener
	m_all_checkbox_listener = null;
    private Database m_database = null;
    private FoldersDrawer m_folders_drawer = null;
    private Handler m_scroll_hander = null;
    private ImageButton m_folders_drawer_button = null;
    private ImageButton m_scroll_bottom = null;
    private ImageButton m_scroll_top = null;
    private LetteraLinearLayoutManager m_layout_manager = null;
    private LinearLayout m_status_bar = null;
    private MessagesAdapter m_messages_adapter = null;
    private RecyclerView m_recycler = null;
    private Runnable m_scroll_runnable = null;
    private ScheduledExecutorService m_folders_drawer_scheduler = null;
    private Settings m_settings = null;
    private String m_selected_folder_name = "";
    private TextView m_current_folder = null;
    private TextView m_items_count = null;
    private View m_vertical_separator = null;
    private final AtomicBoolean m_download_interrupted =
	new AtomicBoolean(false);
    private final Object m_selected_folder_name_mutex = new Object();
    private final PGP m_pgp = PGP.instance();
    private final static AtomicInteger s_background_color = new AtomicInteger
	(Color.WHITE);
    private final static AtomicInteger s_divider_color = new AtomicInteger
	(Color.GRAY);
    private final static AtomicInteger s_text_color = new AtomicInteger
	(Color.BLACK);
    private final static int FOLDERS_DRAWER_INTERVAL = 7500;
    private final static long HIDE_SCROLL_TO_BUTTON_DELAY = 2500;
    private static int s_default_background_color = 0;
    private static int s_default_divider_color = 0;
    private static int s_default_text_color = 0;
    public final static String NONE_FOLDER = "(None)";

    private String email_account()
    {
	return m_database.setting("primary_email_account");
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
	    dialog = new Dialog(Lettera.this);
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
	m_all_checkbox = findViewById(R.id.all_checkbox);
	m_artificial_button = findViewById(R.id.artificial_button);
	m_compose_button = findViewById(R.id.compose_button);
	m_contacts_button = findViewById(R.id.contacts_button);
	m_current_folder = findViewById(R.id.current_folder);
	m_delete_button = findViewById(R.id.delete_button);
	m_download_button = findViewById(R.id.download_button);
	m_folders_drawer_button = findViewById(R.id.folders_drawer_button);
	m_items_count = findViewById(R.id.message_count);
	m_messaging_button = findViewById(R.id.messaging_button);
	m_move_to_folder_button = findViewById(R.id.move_to_folder);
	m_recycler = findViewById(R.id.messages);
	m_scroll_bottom = findViewById(R.id.scroll_bottom);
	m_scroll_top = findViewById(R.id.scroll_top);
	m_settings_button = findViewById(R.id.settings_button);
	m_status_bar = findViewById(R.id.status_bar);
	m_vertical_separator = findViewById(R.id.vertical_separator);
    }

    private void prepare_listeners()
    {
	if(m_all_checkbox_listener == null)
	{
	    m_all_checkbox_listener =
		new CompoundButton.OnCheckedChangeListener()
	    {
		@Override
		public void onCheckedChanged
		    (CompoundButton button_view, final boolean is_checked)
		{
		    new Handler
			(Looper.getMainLooper()).postDelayed(new Runnable()
		    {
			@Override
			public void run()
			{
			    m_database.select_all
				(Lettera.this,
				 m_messages_adapter,
				 email_account(),
				 selected_folder_name(),
				 is_checked);
			}
		    }, 750);
		}
	    };
	    m_all_checkbox.setOnCheckedChangeListener(m_all_checkbox_listener);
	}

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
				m_database.delete_selected
				    (Lettera.this,
				     m_messages_adapter,
				     email_account,
				     folder_name);
			}
		    };

		    if(folder_name.toLowerCase().equals("trash"))
			Windows.show_prompt_dialog
			    (Lettera.this,
			     listener,
			     "Archive the selected message(s) in Lettera?",
			     confirmed);
		    else
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

	if(m_move_to_folder_button != null && !m_move_to_folder_button.
	                                       hasOnClickListeners())
	    m_move_to_folder_button.setOnClickListener
		(new View.OnClickListener()
	    {
		@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
		@Override
		public void onClick(View view)
		{
		    if(Lettera.this.isFinishing() || m_folders_drawer == null)
			return;

		    String email_account = email_account();

		    if(m_database.
		       messages_selected(email_account,
					 selected_folder_name()) == 0)
			return;

		    MoveMessages move_messages = new MoveMessages
			(Lettera.this,
			 email_account,
			 selected_folder_name(),
			 findViewById(R.id.main_layout));

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

	if(m_settings_button != null && !m_settings_button.
	                                 hasOnClickListeners())
	    m_settings_button.setOnClickListener(new View.OnClickListener()
	    {
		@Override
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
		private final AtomicBoolean m_interrupted =
		    new AtomicBoolean(false);

		@Override
		public void run()
		{
		    try
		    {
			EmailElement email_element = m_database.email_element
			    (email_account());

			if(email_element == null)
			{
			    if(m_mail != null)
			    {
				m_mail.disconnect();
				m_mail = null;
			    }

			    return;
			}

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
			    Lettera.this.runOnUiThread(new Runnable()
			    {
				@Override
				public void run()
				{
				    m_items_count.setText
					("Items: " +
					 m_messages_adapter.getItemCount() +
					 " (Downloading " +
					 selected_folder_name() +
					 ".)");
				}
			    });

			    m_database.write_folders
				(m_mail.folder_elements(m_interrupted),
				 m_mail.email_address());
			    m_database.write_messages
				(m_interrupted,
				 m_mail.folder(selected_folder_full_name()),
				 m_mail.email_address(),
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
					    m_items_count.setText
						("Items: " +
						 m_messages_adapter.
						 getItemCount() +
						 " (Downloading " +
						 folder_name +
						 ".)");
					}
				    });

				m_database.write_messages
				    (m_interrupted,
				     m_mail.folder(m_folder_names.get(0)),
				     m_mail.email_address(),
				     false);
				m_folder_names.remove(0);
			    }

			    Lettera.this.runOnUiThread(new Runnable()
			    {
				@Override
				public void run()
				{
				    m_messages_adapter.notifyDataSetChanged();
				    m_folders_drawer.update();
				    prepare_current_folder_text
					(selected_folder_name());
				}
			    });
			}
		    }
		    catch(Exception exception)
		    {
		    }
		}
	    }, 5, FOLDERS_DRAWER_INTERVAL, TimeUnit.MILLISECONDS);
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
	s_default_background_color = Color.WHITE;
	s_default_divider_color = Color.parseColor("#e0e0e0");
	s_default_text_color = m_current_folder.getCurrentTextColor();
	m_messages_adapter = new MessagesAdapter(getApplicationContext());
	m_folders_drawer = new FoldersDrawer
	    (Lettera.this, findViewById(R.id.main_layout));
	m_layout_manager = new LetteraLinearLayoutManager
	    (getApplicationContext());
	m_layout_manager.setOrientation(LinearLayoutManager.VERTICAL);
	m_layout_manager.setReverseLayout(true);
	m_layout_manager.setSmoothScrollbarEnabled(true);
	m_layout_manager.setStackFromEnd(true);
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
		public void onLongClick(final View view, int position)
		{
		    m_artificial_button.performClick();
		    view.setBackgroundColor(Color.parseColor("#90caf9"));
		    new Handler(Looper.getMainLooper()).
			postDelayed(new Runnable()
		    {
			@Override
			public void run()
			{
			    view.setBackgroundColor(background_color());
			}
		    }, 150);
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
		    }
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

    public void email_account_deleted()
    {
	String folder_name = m_database.setting
	    ("selected_folder_name_" + email_account());

	if(folder_name.isEmpty())
	    folder_name = NONE_FOLDER;

	prepare_folders_and_messages_widgets(folder_name);
    }

    public void messages_deleted()
    {
	/*
	** All of the messages have been deleted.
	*/

	m_folders_drawer.update();
	m_messages_adapter.notifyDataSetChanged();
	prepare_current_folder_text(selected_folder_name());
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
		divider_color = Color.parseColor("#2196f3");
		text_color = Color.parseColor("#2196f3");
		break;
	    case "black & green":
		background_color = Color.BLACK;
		divider_color = Color.parseColor("#66bb6a");
		text_color = Color.parseColor("#66bb6a");
		break;
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
	    (m_all_checkbox, background_color(), divider_color(), text_color());
	m_messages_adapter.notifyDataSetChanged();
    }

    public void prepare_current_folder_text(String folder_name)
    {
	if(m_database.setting("show_status_bar").equals("true"))
	    m_current_folder.setText(folder_name);
	else
	    m_current_folder.setText
		(folder_name + " (" + m_messages_adapter.getItemCount() + ")");

	m_items_count.setText("Items: " + m_messages_adapter.getItemCount());
    }

    public void prepare_folders_and_messages_widgets(String folder_name)
    {
	if(!folder_name.equals(m_messages_adapter.folder_name()) &&
	   !folder_name.isEmpty())
	{
	    m_all_checkbox.setOnCheckedChangeListener(null);
	    m_all_checkbox.setChecked(false);
	    m_all_checkbox.setOnCheckedChangeListener(m_all_checkbox_listener);
	}

	String email_account = email_account();

	m_folders_drawer.set_email_address(email_account);
	m_messages_adapter.set_email_address(email_account);

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

	synchronized(m_selected_folder_name_mutex)
	{
	    if(folder_name.isEmpty())
		m_selected_folder_name = NONE_FOLDER;
	    else
		m_selected_folder_name = folder_name;
	}

	m_scroll_bottom.setVisibility(View.GONE);
	m_scroll_top.setVisibility(View.GONE);
	prepare_current_folder_text(selected_folder_name());
    }

    public void prepare_generic_widgets()
    {
	findViewById(R.id.bottom_divider).setVisibility
	    (m_database.setting("show_status_bar").equals("true") ?
	     View.VISIBLE : View.GONE);
	m_status_bar.setVisibility
	    (m_database.setting("show_status_bar").equals("true") ?
	     View.VISIBLE : View.GONE);
	m_vertical_separator.setVisibility
	    (m_database.setting("show_vertical_separator_before_settings").
	     equals("true") ? View.VISIBLE : View.GONE);
    }

    public void prepare_icons(SettingsElement settings_element)
    {
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

    public void update_folders_drawer()
    {
	m_folders_drawer.update();
    }
}
