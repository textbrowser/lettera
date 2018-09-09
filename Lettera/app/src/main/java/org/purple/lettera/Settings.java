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

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import com.sun.mail.smtp.SMTPTransport;
import java.util.ArrayList;
import java.util.Properties;
import javax.mail.Session;
import javax.mail.Store;

public class Settings
{
    private abstract class PageEnumerator
    {
	public final static int DISPLAY_PAGE = 0;
	public final static int NETWORK_PAGE = 1;
	public final static int PRIVACY_PAGE = 2;
    }

    private class EmailTest implements Runnable
    {
	private SMTPTransport m_smtp_transport = null;
	private Store m_store = null;
	private String m_email = "";
	private String m_host = "";
	private String m_password = "";
	private String m_protocol = "imaps";
	private boolean m_error = true;
	private int m_port = -1;

	public EmailTest(String email,
			 String host,
			 String password,
			 String port,
			 String protocol)
	{
	    m_email = email;
	    m_host = host;
	    m_password = password;
	    m_port = Integer.valueOf(port);
	    m_protocol = protocol;
	}

	@Override
	public void run()
	{
	    try
	    {
		/*
		** https://javaee.github.io/javamail/docs/api/com/sun/mail/imap/package-summary.html
		*/

		Properties properties = new Properties();

		properties.setProperty
		    ("mail." + m_protocol + ".connectiontimeout", "10000");

		if(m_protocol.equals("smtps"))
		{
		    properties.setProperty("mail.smtps.localhost", "localhost");
		    properties.setProperty
			("mail.smtp.starttls.enable", "true");
		}

		properties.setProperty
		    ("mail." + m_protocol + ".ssl.enable", "true");
		properties.setProperty
		    ("mail." + m_protocol + ".timeout", "10000");

		Session session = Session.getInstance(properties);

		switch(m_protocol)
		{
		case "imaps":
		    m_store = session.getStore(m_protocol);
		    m_store.connect(m_host, m_port, m_email, m_password);
		    m_error = false;
		    break;
		case "smtps":
		    m_smtp_transport = (SMTPTransport) session.getTransport
			("smtp"); // Not SMTPS!
		    m_smtp_transport.setRequireStartTLS(true);
		    m_smtp_transport.connect
			(m_host, m_port, m_email, m_password);
		    m_error = false;
		    break;
		default:
		    m_error = true;
		    break;
		}
	    }
	    catch(Exception exception)
	    {
		m_error = true;
	    }
	    finally
	    {
		try
		{
		    if(m_smtp_transport != null)
			m_smtp_transport.close();

		    if(m_store != null)
			m_store.close();
		}
		catch(Exception exception)
		{
		}
	    }

	    try
	    {
		((Activity) m_context).runOnUiThread(new Runnable()
		{
		    @Override
		    public void run()
		    {
			switch(m_protocol)
			{
			case "imaps":
			    if(m_test_inbound_network_progress_bar != null)
				m_test_inbound_network_progress_bar.
				    setVisibility(View.GONE);

			    break;
			case "smtps":
			    if(m_test_outbound_network_progress_bar != null)
				m_test_outbound_network_progress_bar.
				    setVisibility(View.GONE);

			    break;
			default:
			    break;
			}

			if(m_error)
			    Windows.show_dialog
				(m_context,
				 m_protocol.toUpperCase() + " test failed!",
				 "Error");
			else
			    Windows.show_dialog
				(m_context,
				 m_protocol.toUpperCase() + " test succeeded!",
				 "Success");
		    }
		});
	    }
	    catch(Exception exception)
	    {
	    }
	}
    }

    private Button m_apply_button = null;
    private Button m_close_button = null;
    private Button m_delete_account_button = null;
    private Button m_display_button = null;
    private Button m_network_button = null;
    private Button m_privacy_button = null;
    private Button m_test_inbound_button = null;
    private Button m_test_outbound_button = null;
    private Button m_x_button = null;
    private CheckBox m_delete_on_server_checkbox = null;
    private CheckBox m_delete_account_verify_check_box = null;
    private Context m_context = null;
    private Dialog m_dialog = null;
    private Spinner m_accounts_spinner = null;
    private Spinner m_icon_theme_spinner = null;
    private TextView m_inbound_address = null;
    private TextView m_inbound_email = null;
    private TextView m_inbound_password = null;
    private TextView m_inbound_port = null;
    private TextView m_outbound_address = null;
    private TextView m_outbound_email = null;
    private TextView m_outbound_password = null;
    private TextView m_outbound_port = null;
    private View m_display_layout = null;
    private View m_network_layout = null;
    private View m_parent = null;
    private View m_privacy_layout = null;
    private View m_generate_keys_progress_bar = null;
    private View m_test_inbound_network_progress_bar = null;
    private View m_test_outbound_network_progress_bar = null;
    private View m_view = null;
    private final Database m_database = Database.getInstance();
    private final static InputFilter s_port_filter = new InputFilter()
    {
	public CharSequence filter(CharSequence source,
				   int start,
				   int end,
				   Spanned dest,
				   int dstart,
				   int dend)
	{
	    try
	    {
		int port = Integer.parseInt
		    (dest.toString() + source.toString());

		if(port >= 1 && port <= 65535)
		    return null;
	    }
	    catch(Exception exception)
	    {
	    }

	    return "";
	}
    };
    private int m_current_page = PageEnumerator.DISPLAY_PAGE;

    private int icon(String name)
    {
	if(m_icon_theme_spinner == null || name == null)
	    return R.drawable.lettera;

	name = m_icon_theme_spinner.
	    getSelectedItem().toString().toLowerCase() + "_" + name;

	switch(name)
	{
	case "default_compose":
	    return R.drawable.default_compose;
	case "default_contacts":
	    return R.drawable.default_contacts;
	case "default_display":
	    return R.drawable.default_display;
	case "default_display_pressed":
	    return R.drawable.default_display_pressed;
	case "default_download":
	    return R.drawable.default_download;
	case "default_messaging":
	    return R.drawable.default_messaging;
	case "default_network":
	    return R.drawable.default_network;
	case "default_network_pressed":
	    return R.drawable.default_network_pressed;
	case "default_privacy":
	    return R.drawable.default_privacy;
	case "default_privacy_pressed":
	    return R.drawable.default_privacy_pressed;
	case "default_settings":
	    return R.drawable.default_settings;
	case "default_window_close":
	    return R.drawable.default_window_close;
	case "material_compose":
	    return R.drawable.material_compose;
	case "material_contacts":
	    return R.drawable.material_contacts;
	case "material_display":
	    return R.drawable.material_display;
	case "material_display_pressed":
	    return R.drawable.material_display_pressed;
	case "material_download":
	    return R.drawable.material_download;
	case "material_messaging":
	    return R.drawable.material_messaging;
	case "material_network":
	    return R.drawable.material_network;
	case "material_network_pressed":
	    return R.drawable.material_network_pressed;
	case "material_privacy":
	    return R.drawable.material_privacy;
	case "material_privacy_pressed":
	    return R.drawable.material_privacy_pressed;
	case "material_settings":
	    return R.drawable.material_settings;
	case "material_window_close":
	    return R.drawable.material_window_close;
	case "nuvola_compose":
	    return R.drawable.nuvola_compose;
	case "nuvola_contacts":
	    return R.drawable.nuvola_contacts;
	case "nuvola_display":
	    return R.drawable.nuvola_display;
	case "nuvola_display_pressed":
	    return R.drawable.nuvola_display_pressed;
	case "nuvola_download":
	    return R.drawable.nuvola_download;
	case "nuvola_messaging":
	    return R.drawable.nuvola_messaging;
	case "nuvola_network":
	    return R.drawable.nuvola_network;
	case "nuvola_network_pressed":
	    return R.drawable.nuvola_network_pressed;
	case "nuvola_privacy":
	    return R.drawable.nuvola_privacy;
	case "nuvola_privacy_pressed":
	    return R.drawable.nuvola_privacy_pressed;
	case "nuvola_settings":
	    return R.drawable.nuvola_settings;
	case "nuvola_window_close":
	    return R.drawable.nuvola_window_close;
	default:
	    return R.drawable.lettera;
	}
    }

    private void apply_settings()
    {
	try
	{
	    ContentValues content_values = new ContentValues();
	    String error = "";
	    String string = "";

	    /*
	    ** Display
	    */

	    content_values.put("key", "icon_theme");
	    content_values.put
		("value", m_icon_theme_spinner.getSelectedItem().toString());
	    error = m_database.save_setting(content_values);

	    if(!error.isEmpty())
	    {
		show_display_page();
		Windows.show_dialog
		    (m_context, "Failure (" + error + ")!", "Error");
		return;
	    }

	    /*
	    ** Network
	    */

	    content_values.clear();
	    content_values.put
		("delete_on_server",
		 String.
		 valueOf(m_delete_on_server_checkbox.isChecked() ? 1 : 0));
	    string = m_inbound_address.getText().toString().trim();

	    if(string.isEmpty())
	    {
		m_inbound_address.requestFocus();
		m_inbound_address.setText("");
		show_network_page();
		return;
	    }
	    else
		content_values.put("in_address", string);

	    string = m_inbound_email.getText().toString().trim();

	    if(string.isEmpty())
	    {
		m_inbound_email.requestFocus();
		m_inbound_email.setText("");
		show_network_page();
		return;
	    }
	    else
		content_values.put("email_account", string);

	    string = m_inbound_password.getText().toString();

	    if(string.isEmpty())
	    {
		m_inbound_password.requestFocus();
		m_inbound_password.setText("");
		show_network_page();
		return;
	    }
	    else
		content_values.put("in_password", string);

	    string = m_inbound_port.getText().toString().trim();

	    if(string.isEmpty())
	    {
		m_inbound_port.requestFocus();
		show_network_page();
		return;
	    }
	    else
		content_values.put("in_port", string);

	    string = m_outbound_address.getText().toString().trim();

	    if(string.isEmpty())
	    {
		m_outbound_address.requestFocus();
		m_outbound_address.setText("");
		show_network_page();
		return;
	    }
	    else
		content_values.put("out_address", string);

	    string = m_outbound_email.getText().toString().trim();

	    if(string.isEmpty())
	    {
		m_outbound_email.requestFocus();
		m_outbound_email.setText("");
		show_network_page();
		return;
	    }
	    else
		content_values.put("out_email", string);

	    string = m_outbound_password.getText().toString();

	    if(string.isEmpty())
	    {
		m_outbound_password.requestFocus();
		m_outbound_password.setText("");
		show_network_page();
		return;
	    }
	    else
		content_values.put("out_password", string);

	    string = m_outbound_port.getText().toString().trim();

	    if(string.isEmpty())
	    {
		m_outbound_port.requestFocus();
		m_outbound_port.setText("");
		show_network_page();
		return;
	    }
	    else
		content_values.put("out_port", string);

	    error = m_database.save_email(content_values).trim();

	    if(!error.isEmpty())
	    {
		show_network_page();
		Windows.show_dialog
		    (m_context, "Failure (" + error + ")!", "Error");
	    }
	    else
	    {
		String selected_item = m_accounts_spinner.
		    getSelectedItem().toString();
		int selected_position = m_accounts_spinner.
		    getSelectedItemPosition();

		populate_accounts_spinner();

		if(m_accounts_spinner.getItemAtPosition(selected_position).
		   toString().equals(selected_item))
		    m_accounts_spinner.setSelection(selected_position);
		else
		    populate_network();
	    }
	}
	catch(Exception exception)
	{
	    show_network_page();
	    Windows.show_dialog
		(m_context,
		 "Failure (" + exception.getMessage() + ")!",
		 "Error");
	}
    }

    private void initialize_widget_members()
    {
	m_accounts_spinner = (Spinner) m_view.findViewById
	    (R.id.accounts_spinner);
	m_apply_button = (Button) m_view.findViewById(R.id.apply_button);
	m_close_button = (Button) m_view.findViewById(R.id.close_button);
	m_delete_account_button = (Button) m_view.findViewById
	    (R.id.delete_account_button);
	m_delete_account_verify_check_box = (CheckBox)
	    m_view.findViewById(R.id.delete_account_verify_check_box);
	m_delete_on_server_checkbox = (CheckBox)
	    m_view.findViewById(R.id.delete_on_server_checkbox);
	m_display_button = (Button) m_view.findViewById
	    (R.id.display_button);
	m_display_layout = m_view.findViewById(R.id.display_layout);
	m_generate_keys_progress_bar = m_view.findViewById
	    (R.id.generate_keys_progress_bar);
	m_icon_theme_spinner = (Spinner) m_view.findViewById
	    (R.id.icon_theme_spinner);
	m_inbound_address = (TextView) m_view.findViewById
	    (R.id.inbound_address);
	m_inbound_email = (TextView) m_view.findViewById(R.id.inbound_email);
	m_inbound_password = (TextView) m_view.findViewById
	    (R.id.inbound_password);
	m_inbound_port = (TextView) m_view.findViewById(R.id.inbound_port);
	m_network_button = (Button) m_view.findViewById(R.id.network_button);
	m_outbound_address = (TextView) m_view.findViewById
	    (R.id.outbound_address);
	m_outbound_email = (TextView) m_view.findViewById(R.id.outbound_email);
	m_outbound_password = (TextView) m_view.findViewById
	    (R.id.outbound_password);
	m_outbound_port = (TextView) m_view.findViewById(R.id.outbound_port);
	m_network_layout = m_view.findViewById(R.id.network_layout);
	m_privacy_button = (Button) m_view.findViewById(R.id.privacy_button);
	m_privacy_layout = m_view.findViewById(R.id.privacy_layout);
	m_test_inbound_button = (Button) m_view.findViewById
	    (R.id.test_inbound_button);
	m_test_outbound_button = (Button) m_view.findViewById
	    (R.id.test_outbound_button);
	m_test_inbound_network_progress_bar = m_view.findViewById
	    (R.id.test_inbound_network_progress_bar);
	m_test_outbound_network_progress_bar = m_view.findViewById
	    (R.id.test_outbound_network_progress_bar);
	m_x_button = (Button) m_view.findViewById(R.id.x_button);
    }

    private void populate()
    {
	populate_accounts_spinner();
	populate_display();
	populate_network();
    }

    private void populate_accounts_spinner()
    {
	if(m_context == null)
	    return;

	try
	{
	    if(((Activity) m_context).isFinishing())
		return;

	    ArrayList<String> array_list = m_database.email_account_names();

	    if(array_list == null || array_list.isEmpty())
	    {
		array_list = new ArrayList<> ();
		array_list.add("(Empty)");
		m_delete_account_button.setEnabled(false);
	    }
	    else
		m_delete_account_button.setEnabled
		    (m_delete_account_verify_check_box.isChecked());

	    ArrayAdapter<String> array_adapter = new ArrayAdapter<>
		(m_context, android.R.layout.simple_spinner_item, array_list);

	    m_accounts_spinner.setAdapter(array_adapter);
	}
	catch(Exception exception)
	{
	}
    }

    private void populate_display()
    {
	try
	{
	    SettingsElement settings_element = m_database.settings_element
		("icon_theme");

	    switch(settings_element.m_value)
	    {
	    case "Default":
		m_icon_theme_spinner.setSelection(0);
		break;
	    case "Material":
		m_icon_theme_spinner.setSelection(1);
		break;
	    case "Nuvola":
		m_icon_theme_spinner.setSelection(2);
		break;
	    default:
		m_icon_theme_spinner.setSelection(0);
		break;
	    }
	}
	catch(Exception exception)
	{
	}
    }

    private void populate_network()
    {
	try
	{
	    EmailElement email_element = m_database.email_element
		(m_accounts_spinner.getSelectedItem().toString());

	    if(email_element == null)
	    {
		m_delete_on_server_checkbox.setChecked(false);
		m_inbound_address.setText("");
		m_inbound_email.setText("");
		m_inbound_password.setText("");
		m_inbound_port.setText("993");
		m_outbound_address.setText("");
		m_outbound_email.setText("");
		m_outbound_password.setText("");
		m_outbound_port.setText("587");
	    }
	    else
	    {
		m_delete_on_server_checkbox.setChecked
		    (email_element.m_delete_on_server);
		m_inbound_address.setText(email_element.m_inbound_address);
		m_inbound_email.setText(email_element.m_inbound_email);
		m_inbound_password.setText(email_element.m_inbound_password);
		m_inbound_port.setText
		    (String.valueOf(email_element.m_inbound_port));
		m_outbound_address.setText(email_element.m_outbound_address);
		m_outbound_email.setText(email_element.m_outbound_email);
		m_outbound_password.setText(email_element.m_outbound_password);
		m_outbound_port.setText
		    (String.valueOf(email_element.m_outbound_port));
	    }
	}
	catch(Exception exception)
	{
	}
    }

    private void prepare_icons()
    {
	try
	{
	    switch(m_current_page)
	    {
	    case PageEnumerator.DISPLAY_PAGE:
		m_display_button.setBackgroundResource(icon("display_pressed"));
		m_network_button.setBackgroundResource(icon("network"));
		m_privacy_button.setBackgroundResource(icon("privacy"));
		break;
	    case PageEnumerator.NETWORK_PAGE:
		m_display_button.setBackgroundResource(icon("display"));
		m_network_button.setBackgroundResource(icon("network_pressed"));
		m_privacy_button.setBackgroundResource(icon("privacy"));
		break;
	    case PageEnumerator.PRIVACY_PAGE:
		m_display_button.setBackgroundResource(icon("display"));
		m_network_button.setBackgroundResource(icon("network"));
		m_privacy_button.setBackgroundResource(icon("privacy_pressed"));
		break;
	    }
	}
	catch(Exception exception)
	{
	}
    }

    private void prepare_listeners()
    {
	if(m_context == null)
	    return;

	m_accounts_spinner.setOnItemSelectedListener
	    (new OnItemSelectedListener()
	    {
		@Override
		public void onItemSelected(AdapterView<?> parent,
					   View view,
					   int position,
					   long id)
		{
		    if(((Activity) m_context).isFinishing())
			return;

		    populate_network();
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent)
		{
		}
	    });

	if(!m_apply_button.hasOnClickListeners())
	    m_apply_button.setOnClickListener(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(((Activity) m_context).isFinishing())
			return;

		    apply_settings();
		}
	    });

	if(!m_close_button.hasOnClickListeners())
	    m_close_button.setOnClickListener(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(((Activity) m_context).isFinishing())
			return;

		    if(m_dialog != null)
			m_dialog.dismiss();
		}
	    });

	if(!m_delete_account_button.hasOnClickListeners())
	    m_delete_account_button.setOnClickListener
		(new View.OnClickListener()
		{
		    public void onClick(View view)
		    {
			if(((Activity) m_context).isFinishing())
			    return;

			if(m_database.
			   delete_email_account(m_accounts_spinner.
						getSelectedItem().
						toString()))
			{
			    m_delete_account_verify_check_box.setChecked(false);
			    populate_accounts_spinner();
			    populate_network();
			}
		    }
		});

	m_delete_account_verify_check_box.setOnCheckedChangeListener
	    (new CompoundButton.OnCheckedChangeListener()
	    {
		@Override
		public void onCheckedChanged
		    (CompoundButton buttonView, boolean isChecked)
		{
		    m_delete_account_button.setEnabled
			(isChecked &&
			 !m_accounts_spinner.
			 getSelectedItem().equals("(Empty)"));
		}
	    });

	if(!m_display_button.hasOnClickListeners())
	    m_display_button.setOnClickListener(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(((Activity) m_context).isFinishing())
			return;

		    show_display_page();
		    prepare_icons();
		}
	    });

	m_icon_theme_spinner.setOnItemSelectedListener
	    (new OnItemSelectedListener()
	    {
		@Override
		public void onItemSelected(AdapterView<?> parent,
					   View view,
					   int position,
					   long id)
		{
		    if(((Activity) m_context).isFinishing())
			return;

		    prepare_icons();
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent)
		{
		}
	    });

	if(!m_network_button.hasOnClickListeners())
	    m_network_button.setOnClickListener(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(((Activity) m_context).isFinishing())
			return;

		    m_current_page = PageEnumerator.NETWORK_PAGE;
		    show_network_page();
		}
	    });

	if(!m_privacy_button.hasOnClickListeners())
	    m_privacy_button.setOnClickListener(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(((Activity) m_context).isFinishing())
			return;

		    m_current_page = PageEnumerator.PRIVACY_PAGE;
		    m_display_layout.setVisibility(View.GONE);
		    m_network_layout.setVisibility(View.GONE);
		    m_privacy_layout.setVisibility(View.VISIBLE);
		    prepare_icons();
		}
	     });

	if(!m_test_inbound_button.hasOnClickListeners())
	    m_test_inbound_button.setOnClickListener(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(((Activity) m_context).isFinishing())
			return;

		    test_inbound_server();
		}
	    });

	if(!m_test_outbound_button.hasOnClickListeners())
	    m_test_outbound_button.setOnClickListener(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(((Activity) m_context).isFinishing())
			return;

		    test_outbound_server();
		}
	    });

	if(!m_x_button.hasOnClickListeners())
	    m_x_button.setOnClickListener(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(((Activity) m_context).isFinishing())
			return;

		    if(m_dialog != null)
			m_dialog.dismiss();
		}
	    });
    }

    private void prepare_widgets()
    {
	/*
	** Set Display as the primary section.
	*/

	m_display_button.setBackgroundResource
	    (R.drawable.default_display_pressed);
	m_network_layout.setVisibility(View.GONE);
	m_privacy_layout.setVisibility(View.GONE);

	ArrayAdapter<String> array_adapter;
	Spinner spinner = null;
	String array[] = null;

	/*
	** Display
	*/

	array = new String[] {"Default"};
	array_adapter = new ArrayAdapter<>
	    (m_context, android.R.layout.simple_spinner_item, array);
	spinner = (Spinner) m_view.findViewById(R.id.color_theme_spinner);
	spinner.setAdapter(array_adapter);
	array = new String[] {"Default", "Material", "Nuvola"};
	array_adapter = new ArrayAdapter<>
	    (m_context, android.R.layout.simple_spinner_item, array);
	m_icon_theme_spinner.setAdapter(array_adapter);

	/*
	** Network
	*/

	m_inbound_port.setFilters(new InputFilter[] {s_port_filter});
	m_outbound_port.setFilters(new InputFilter[] {s_port_filter});
	array = new String[] {"(Empty)"};
	array_adapter = new ArrayAdapter<>
	    (m_context, android.R.layout.simple_spinner_item, array);
	m_accounts_spinner.setAdapter(array_adapter);
	m_delete_account_button.setEnabled(false);

	/*
	** Privacy
	*/

	array = new String[] {"McEliece", "RSA"};
	array_adapter = new ArrayAdapter<>
	    (m_context, android.R.layout.simple_spinner_item, array);
	spinner = (Spinner) m_view.findViewById
	    (R.id.encryption_key_spinner);
	spinner.setAdapter(array_adapter);
	array = new String[] {"RSA"};
	array_adapter = new ArrayAdapter<>
	    (m_context, android.R.layout.simple_spinner_item, array);
	spinner = (Spinner) m_view.findViewById(R.id.signature_key_spinner);
	spinner.setAdapter(array_adapter);
    }

    private void show_display_page()
    {
	m_current_page = PageEnumerator.DISPLAY_PAGE;
	m_display_button.setBackgroundResource(icon("display_pressed"));
	m_display_layout.setVisibility(View.VISIBLE);
	m_network_button.setBackgroundResource(icon("network"));
	m_network_layout.setVisibility(View.GONE);
	m_privacy_button.setBackgroundResource(icon("privacy"));
	m_privacy_layout.setVisibility(View.GONE);
    }

    private void show_network_page()
    {
	m_current_page = PageEnumerator.NETWORK_PAGE;
	m_display_button.setBackgroundResource(icon("display"));
	m_display_layout.setVisibility(View.GONE);
	m_network_button.setBackgroundResource(icon("network_pressed"));
	m_network_layout.setVisibility(View.VISIBLE);
	m_privacy_button.setBackgroundResource(icon("privacy"));
	m_privacy_layout.setVisibility(View.GONE);
    }

    private void test_inbound_server()
    {
	if(m_test_inbound_network_progress_bar != null)
	    m_test_inbound_network_progress_bar.setVisibility(View.VISIBLE);

	try
	{
	    Thread thread = new Thread
		(new EmailTest(m_inbound_email.getText().toString(),
			       m_inbound_address.getText().toString(),
			       m_inbound_password.getText().toString(),
			       m_inbound_port.getText().toString(),
			       "imaps"));

	    thread.start();
	}
	catch(Exception exception)
	{
	}
    }

    private void test_outbound_server()
    {
	if(m_test_outbound_network_progress_bar != null)
	    m_test_outbound_network_progress_bar.setVisibility(View.VISIBLE);

	try
	{
	    Thread thread = new Thread
		(new EmailTest(m_outbound_email.getText().toString(),
			       m_outbound_address.getText().toString(),
			       m_outbound_password.getText().toString(),
			       m_outbound_port.getText().toString(),
			       "smtps"));

	    thread.start();
	}
	catch(Exception exception)
	{
	}
    }

    public Settings(Context context, View parent)
    {
	m_context = context;
	m_parent = parent;
    }

    public void show()
    {
	if(m_context == null || m_parent == null)
	    return;
	else if(m_dialog != null)
	{
	    m_dialog.show();

	    if(m_inbound_address != null)
		m_inbound_address.requestFocus();

	    populate();
	    return;
	}

	LayoutInflater inflater = (LayoutInflater) m_context.getSystemService
	    (Context.LAYOUT_INFLATER_SERVICE);
	WindowManager.LayoutParams layout_params = new
	    WindowManager.LayoutParams();

	layout_params.gravity = Gravity.CENTER_VERTICAL | Gravity.TOP;
	layout_params.height = WindowManager.LayoutParams.WRAP_CONTENT;
	layout_params.width = WindowManager.LayoutParams.MATCH_PARENT;
	m_view = inflater.inflate(R.layout.settings, null);

	/*
	** Prepare things.
	*/

	initialize_widget_members();
	prepare_icons();
	prepare_listeners();
	prepare_widgets();

	/*
	** The cute dialog.
	*/

	m_dialog = new Dialog(m_context);
	m_dialog.setCancelable(false);
	m_dialog.setContentView(m_view);
	m_dialog.setTitle("Settings");
	m_dialog.show();
	m_dialog.getWindow().setAttributes(layout_params); // After show().
	m_inbound_address.requestFocus();
	populate();
    }
}
