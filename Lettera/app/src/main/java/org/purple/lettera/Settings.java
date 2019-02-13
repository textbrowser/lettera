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
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import com.sun.mail.smtp.SMTPTransport;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.mail.Session;
import javax.mail.Store;

public class Settings
{
    private abstract class PageEnumerator
    {
	private final static int DISPLAY_PAGE = 0;
	private final static int NETWORK_PAGE = 1;
	private final static int PRIVACY_PAGE = 2;
    }

    private class DeleteAccount implements Runnable
    {
	private Dialog m_dialog = null;
	private String m_email_account = "";

	private DeleteAccount(Dialog dialog, String email_account)
	{
	    m_dialog = dialog;
	    m_email_account = email_account;
	}

	@Override
	public void run()
	{
	    final AtomicBoolean ok = new AtomicBoolean(false);

	    try
	    {
		ok.set(s_database.delete_email_account(m_email_account));
	    }
	    catch(Exception exception)
	    {
	    }

	    m_lettera.runOnUiThread(new Runnable()
	    {
		@Override
		public void run()
		{
		    try
		    {
			if(m_dialog != null)
			    m_dialog.dismiss();
		    }
		    catch(Exception exception)
		    {
		    }

		    try
		    {
			if(ok.get())
			{
			    m_delete_account_verify_checkbox.setChecked(false);
			    populate_accounts_spinner();
			    populate_network();

			    /*
			    ** Order!
			    */

			    m_lettera.email_account_deleted();
			}
		    }
		    catch(Exception exception)
		    {
		    }
		}
	    });
	}
    }

    private class DeleteMessages implements Runnable
    {
	private Dialog m_dialog = null;
	private String m_email_account = "";

	private DeleteMessages(Dialog dialog, String email_account)
	{
	    m_dialog = dialog;
	    m_email_account = email_account;
	}

	@Override
	public void run()
	{
	    final AtomicBoolean ok = new AtomicBoolean(false);

	    try
	    {
		ok.set(s_database.delete_messages(m_email_account));
	    }
	    catch(Exception exception)
	    {
	    }

	    m_lettera.runOnUiThread(new Runnable()
	    {
		@Override
		public void run()
		{
		    try
		    {
			if(m_dialog != null)
			    m_dialog.dismiss();
		    }
		    catch(Exception exception)
		    {
		    }

		    try
		    {
			if(ok.get())
			{
			    m_lettera.messages_deleted();
			    m_remove_local_messages_verify_checkbox.
				setChecked(false);
			}
		    }
		    catch(Exception exception)
		    {
		    }
		}
	    });
	}
    }

    private class EmailTest implements Runnable
    {
	private Dialog m_dialog = null;
	private SMTPTransport m_smtp_transport = null;
	private Store m_store = null;
	private String m_email = "";
	private String m_host = "";
	private String m_password = "";
	private String m_port = "";
	private String m_protocol = "imaps";
	private String m_proxy_address = "";
	private String m_proxy_password = "";
	private String m_proxy_port = "";
	private String m_proxy_type = "";
	private String m_proxy_user = "";
	private boolean m_error = true;

	private EmailTest(Dialog dialog,
			  String email,
			  String host,
			  String password,
			  String port,
			  String protocol,
			  String proxy_address,
			  String proxy_password,
			  String proxy_port,
			  String proxy_type,
			  String proxy_user)
	{
	    m_dialog = dialog;
	    m_email = email.trim();
	    m_host = host.trim();
	    m_password = password;
	    m_port = port;
	    m_protocol = protocol;
	    m_proxy_address = proxy_address.trim();
	    m_proxy_password = proxy_password;
	    m_proxy_port = proxy_port;
	    m_proxy_type = proxy_type;
	    m_proxy_user = proxy_user;
	}

	@Override
	public void run()
	{
	    try
	    {
		Properties properties = Mail.properties
		    (m_email,
		     m_host,
		     m_password,
		     m_port,
		     m_protocol,
		     m_proxy_address,
		     m_proxy_password,
		     m_proxy_port,
		     m_proxy_type,
		     m_proxy_user,
		     "10000");
		Session session = Session.getInstance(properties);

		switch(m_protocol)
		{
		case "imaps":
		    m_store = session.getStore(m_protocol);
		    m_store.connect
			(m_host, Integer.valueOf(m_port), m_email, m_password);
		    m_error = false;
		    break;
		case "smtp":
		case "smtps":
		    m_smtp_transport = (SMTPTransport) session.getTransport
			("smtp"); // Not SMTPS!
		    m_smtp_transport.setRequireStartTLS(true);
		    m_smtp_transport.connect
			(m_host, Integer.valueOf(m_port), m_email, m_password);
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
		}
		catch(Exception exception)
		{
		}

		try
		{
		    if(m_store != null)
			m_store.close();
		}
		catch(Exception exception)
		{
		}
	    }

	    m_lettera.runOnUiThread(new Runnable()
	    {
		@Override
		public void run()
		{
		    try
		    {
			if(m_dialog != null)
			    m_dialog.dismiss();
		    }
		    catch(Exception exception)
		    {
		    }

		    try
		    {
			switch(m_protocol)
			{
			case "imaps":
			    break;
			case "smtp":
			case "smtps":
			    break;
			default:
			    break;
			}

			if(m_error)
			    Windows.show_dialog
				(m_lettera,
				 m_protocol.toUpperCase() + " test failed!",
				 "Error");
			else
			    Windows.show_dialog
				(m_lettera,
				 m_protocol.toUpperCase() +
				 " test succeeded!",
				 "Success");
		    }
		    catch(Exception exception)
		    {
		    }
		}
	    });
	}
    }

    private class GenerateKeyPairs implements Runnable
    {
	private Dialog m_dialog = null;
	private KeyPair m_encryption_key_pair = null;
	private KeyPair m_signature_key_pair = null;
	private String m_encryption_key_type = "";
	private String m_signature_key_type = "";
	private boolean m_error = false;

	private GenerateKeyPairs(Dialog dialog,
				 String encryption_key_type,
				 String signature_key_type)
	{
	    m_dialog = dialog;
	    m_encryption_key_type = encryption_key_type;
	    m_signature_key_type = signature_key_type;
	}

	@Override
	public void run()
	{
	    try
	    {
		m_encryption_key_pair = PGP.generate_key_pair
		    (m_encryption_key_type);
		m_signature_key_pair = PGP.generate_key_pair
		    (m_signature_key_type);

		if(m_encryption_key_pair == null ||
		   m_signature_key_pair == null)
		    m_error = true;
		else
		    s_database.delete("open_pgp");
	    }
	    catch(Exception exception)
	    {
		m_error = true;
	    }

	    m_lettera.runOnUiThread(new Runnable()
	    {
		@Override
		public void run()
		{
		    try
		    {
			if(m_dialog != null)
			    m_dialog.dismiss();
		    }
		    catch(Exception exception)
		    {
		    }

		    if(m_encryption_key_pair == null)
			m_encryption_key_data.setText
			    ("SHA-1: " +
			     Cryptography.sha_1_fingerprint(null));
		    else
		    {
			if(s_database.save_pgp_key_pair(m_encryption_key_pair,
							"encryption"))
			{
			    m_encryption_key_data.setText
				(Cryptography.
				 key_information(m_encryption_key_pair.
						 getPublic()));
			    s_pgp.set_encryption_key_pair
				(m_encryption_key_pair);
			}
			else
			{
			    m_encryption_key_data.setText
				("SHA-1: " +
				 Cryptography.sha_1_fingerprint(null));
			    m_error = true;
			}
		    }

		    if(m_signature_key_pair == null)
			m_signature_key_data.setText
			    ("SHA-1: " +
			     Cryptography.sha_1_fingerprint(null));
		    else
		    {
			if(s_database.save_pgp_key_pair(m_signature_key_pair,
							"signature"))
			{
			    s_pgp.set_signature_key_pair(m_signature_key_pair);
			    m_signature_key_data.setText
				(Cryptography.
				 key_information(m_signature_key_pair.
						 getPublic()));
			}
			else
			{
			    m_error = true;
			    m_signature_key_data.setText
				("SHA-1: " +
				 Cryptography.sha_1_fingerprint(null));
			}
		    }

		    if(!m_error)
			Windows.show_dialog
			    (m_lettera, "Key pairs generated!", "Success");
		    else
		    {
			s_database.delete("open_pgp");
			s_pgp.set_encryption_key_pair(null);
			s_pgp.set_signature_key_pair(null);
			Windows.show_dialog
			    (m_lettera, "Cannot generate key pairs!", "Error");
		    }
		}
	    });
	}
    }

    private Button m_apply_button = null;
    private Button m_close_button = null;
    private Button m_delete_account_button = null;
    private Button m_display_button = null;
    private Button m_generate_keys_button = null;
    private Button m_network_button = null;
    private Button m_privacy_button = null;
    private Button m_remove_local_messages_button = null;
    private Button m_test_inbound_button = null;
    private Button m_test_outbound_button = null;
    private CheckBox m_delete_on_server_checkbox = null;
    private CheckBox m_delete_account_verify_checkbox = null;
    private CheckBox m_generate_keys_checkbox = null;
    private CheckBox m_primary_account_checkbox = null;
    private CheckBox m_remove_local_messages_verify_checkbox = null;
    private CheckBox m_show_status_bar = null;
    private CheckBox m_show_vertical_separator_before_settings_checkbox = null;
    private Dialog m_dialog = null;
    private Lettera m_lettera = null;
    private Spinner m_accounts_spinner = null;
    private Spinner m_color_theme_spinner = null;
    private Spinner m_email_folders_spinner = null;
    private Spinner m_encryption_key_spinner = null;
    private Spinner m_icon_theme_spinner = null;
    private Spinner m_proxy_type_spinner = null;
    private Spinner m_signature_key_spinner = null;
    private Switch m_outbound_as_inbound = null;
    private TextView m_encryption_key_data = null;
    private TextView m_inbound_address = null;
    private TextView m_inbound_email = null;
    private TextView m_inbound_password = null;
    private TextView m_inbound_port = null;
    private TextView m_outbound_address = null;
    private TextView m_outbound_email = null;
    private TextView m_outbound_password = null;
    private TextView m_outbound_port = null;
    private TextView m_proxy_address = null;
    private TextView m_proxy_password = null;
    private TextView m_proxy_port = null;
    private TextView m_proxy_user = null;
    private TextView m_signature_key_data = null;
    private View m_display_layout = null;
    private View m_network_layout = null;
    private View m_parent = null;
    private View m_privacy_layout = null;
    private View m_view = null;
    private WindowManager.LayoutParams m_layout_params = null;
    private final static PGP s_pgp = PGP.instance();
    private final static Database s_database = Database.instance();
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
    private final static String s_color_themes[] = new String[]
	{"Black & Blue", "Black & Green", "Default"};
    private final static String s_email_folders[] = new String[] {"Drawer"};
    private final static String s_icon_themes[] = new String[]
	{"Default", "Funky", "Hand Drawn", "Material", "Nuvola", "SailFish"};
    private final static String s_proxy_types[] =
	new String[] {"HTTP", "SOCKS"};
    private int m_current_page = PageEnumerator.DISPLAY_PAGE;

    private int icon(String name)
    {
	if(m_icon_theme_spinner == null ||
	   m_icon_theme_spinner.getSelectedItem() == null ||
	   name == null)
	    return R.drawable.lettera;

	return icon_from_name
	    (m_icon_theme_spinner.getSelectedItem().toString().
	     toLowerCase() + "_" + name);
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

	    content_values.put("key", "color_theme");
	    content_values.put
		("value", m_color_theme_spinner.getSelectedItem().toString());
	    error = s_database.save_setting(content_values, false);

	    if(!error.isEmpty())
	    {
		show_display_page();
		Windows.show_dialog
		    (m_lettera, "Failure (" + error + ")!", "Error");
		return;
	    }

	    content_values.clear();
	    content_values.put("key", "email_folders");
	    content_values.put
		("value", m_email_folders_spinner.getSelectedItem().toString());
	    error = s_database.save_setting(content_values, false);

	    if(!error.isEmpty())
	    {
		show_display_page();
		Windows.show_dialog
		    (m_lettera, "Failure (" + error + ")!", "Error");
		return;
	    }

	    content_values.clear();
	    content_values.put("key", "show_status_bar");
	    content_values.put
		("value", m_show_status_bar.isChecked() ? "true" : "false");
	    error = s_database.save_setting(content_values, true);

	    if(!error.isEmpty())
	    {
		show_display_page();
		Windows.show_dialog
		    (m_lettera, "Failure (" + error + ")!", "Error");
		return;
	    }

	    content_values.clear();
	    content_values.put
		("key", "show_vertical_separator_before_settings");
	    content_values.put
		("value",
		 m_show_vertical_separator_before_settings_checkbox.
		 isChecked() ? "true" : "false");
	    error = s_database.save_setting(content_values, true);

	    if(!error.isEmpty())
	    {
		show_display_page();
		Windows.show_dialog
		    (m_lettera, "Failure (" + error + ")!", "Error");
		return;
	    }

	    content_values.clear();
	    content_values.put("key", "icon_theme");
	    content_values.put
		("value", m_icon_theme_spinner.getSelectedItem().toString());
	    error = s_database.save_setting(content_values, true);

	    if(!error.isEmpty())
	    {
		show_display_page();
		Windows.show_dialog
		    (m_lettera, "Failure (" + error + ")!", "Error");
		return;
	    }

	    m_lettera.prepare_generic_widgets();
	    m_lettera.prepare_icons(s_database.settings_element("icon_theme"));

	    /*
	    ** Network
	    */

	    content_values.clear();
	    content_values.put
		("delete_on_server",
		 String.
		 valueOf(m_delete_on_server_checkbox.isChecked() ? 1 : 0));
	    string = m_inbound_address.getText().toString().trim();
	    m_inbound_address.setText(string);

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
	    m_inbound_email.setText(string);

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
	    m_outbound_address.setText(string);

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
	    m_outbound_email.setText(string);

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
		show_network_page();
		return;
	    }
	    else
		content_values.put("out_port", string);

	    content_values.put
		("proxy_address", m_proxy_address.getText().toString().trim());
	    content_values.put
		("proxy_password",
		 m_proxy_password.getText().toString().trim());
	    content_values.put
		("proxy_port", m_proxy_port.getText().toString().trim());
	    content_values.put
		("proxy_type",
		 m_proxy_type_spinner.getSelectedItem().toString());
	    content_values.put
		("proxy_user", m_proxy_user.getText().toString().trim());
	    error = s_database.save_email(content_values).trim();

	    if(!error.isEmpty())
	    {
		show_network_page();
		Windows.show_dialog
		    (m_lettera, "Failure (" + error + ")!", "Error");
	    }
	    else
	    {
		if(m_primary_account_checkbox.isChecked())
		{
		    content_values.clear();
		    content_values.put("key", "primary_email_account");
		    content_values.put
			("value", m_inbound_email.getText().toString());
		    s_database.save_setting(content_values, true);
		    m_lettera.populate_folders_from_database();
		}

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

		m_lettera.prepare_colors(s_database.setting("color_theme"));
		m_lettera.prepare_folders_and_messages_widgets
		    (s_database.
		     setting("selected_folder_name_" +
			     s_database.setting("primary_email_account")));
		m_lettera.prepare_generic_widgets();
		m_lettera.prepare_icons
		    (s_database.settings_element("icon_theme"));
		prepare_colors
		    (m_color_theme_spinner.getSelectedItem().toString());
	    }
	}
	catch(Exception exception)
	{
	    show_network_page();
	    Windows.show_dialog
		(m_lettera,
		 "Failure (" + exception.getMessage() + ")!",
		 "Error");
	}
    }

    private void generate_key_pairs()
    {
	Dialog dialog = null;

	try
	{
	    dialog = new Dialog(m_lettera);
	    Windows.show_progress_dialog
		(m_lettera,
		 dialog,
		 "Generating key pairs.\nPlease be patient.",
		 null);

	    Thread thread = new Thread
		(new GenerateKeyPairs(dialog,
				      m_encryption_key_spinner.
				      getSelectedItem().toString(),
				      m_signature_key_spinner.
				      getSelectedItem().toString()));

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
	m_accounts_spinner = m_view.findViewById
	    (R.id.accounts_spinner);
	m_apply_button = m_view.findViewById(R.id.apply_button);
	m_close_button = m_view.findViewById(R.id.close_button);
	m_color_theme_spinner = m_view.findViewById
	    (R.id.color_theme_spinner);
	m_delete_account_button = m_view.findViewById
	    (R.id.delete_account_button);
	m_delete_account_verify_checkbox = m_view.findViewById
	    (R.id.delete_account_verify_checkbox);
	m_delete_on_server_checkbox = m_view.findViewById
	    (R.id.delete_on_server_checkbox);
	m_display_button = m_view.findViewById
	    (R.id.display_button);
	m_display_layout = m_view.findViewById(R.id.display_layout);
	m_email_folders_spinner = m_view.findViewById
	    (R.id.email_folders);
	m_encryption_key_data = m_view.findViewById
	    (R.id.encryption_key_data);
	m_encryption_key_spinner = m_view.findViewById
	    (R.id.encryption_key_spinner);
	m_generate_keys_button = m_view.findViewById(R.id.generate_keys_button);
	m_generate_keys_checkbox = m_view.findViewById
	    (R.id.generate_keys_checkbox);
	m_icon_theme_spinner = m_view.findViewById
	    (R.id.icon_theme_spinner);
	m_inbound_address = m_view.findViewById
	    (R.id.inbound_address);
	m_inbound_email = m_view.findViewById(R.id.inbound_email);
	m_inbound_password = m_view.findViewById
	    (R.id.inbound_password);
	m_inbound_port = m_view.findViewById(R.id.inbound_port);
	m_network_button = m_view.findViewById(R.id.network_button);
	m_outbound_address = m_view.findViewById
	    (R.id.outbound_address);
	m_outbound_as_inbound = m_view.findViewById
	    (R.id.outbound_as_inbound);
	m_outbound_email = m_view.findViewById(R.id.outbound_email);
	m_outbound_password = m_view.findViewById
	    (R.id.outbound_password);
	m_outbound_port = m_view.findViewById(R.id.outbound_port);
	m_network_layout = m_view.findViewById(R.id.network_layout);
	m_primary_account_checkbox = m_view.findViewById
	    (R.id.primary_account_checkbox);
	m_privacy_button = m_view.findViewById(R.id.privacy_button);
	m_privacy_layout = m_view.findViewById(R.id.privacy_layout);
	m_proxy_address = m_view.findViewById(R.id.proxy_address);
	m_proxy_password = m_view.findViewById(R.id.proxy_password);
	m_proxy_port = m_view.findViewById(R.id.proxy_port);
	m_proxy_type_spinner = m_view.findViewById
	    (R.id.proxy_type_spinner);
	m_proxy_user = m_view.findViewById(R.id.proxy_user);
	m_remove_local_messages_button = m_view.findViewById
	    (R.id.remove_local_messages);
	m_remove_local_messages_verify_checkbox = m_view.findViewById
	    (R.id.remove_local_messages_verify_checkbox);
	m_show_status_bar = m_view.findViewById
	    (R.id.show_status_bar);
	m_show_vertical_separator_before_settings_checkbox =
	    m_view.findViewById(R.id.show_vertical_separator_before_settings);
	m_signature_key_data = m_view.findViewById(R.id.signature_key_data);
	m_signature_key_spinner = m_view.findViewById
	    (R.id.signature_key_spinner);
	m_test_inbound_button = m_view.findViewById
	    (R.id.test_inbound_button);
	m_test_outbound_button = m_view.findViewById
	    (R.id.test_outbound_button);
    }

    private void populate()
    {
	populate_accounts_spinner();
	populate_display();
	populate_network();
	populate_privacy();
    }

    private void populate_accounts_spinner()
    {
	if(m_lettera == null)
	    return;

	if(m_lettera.isFinishing())
	    return;

	ArrayList<String> array_list = s_database.email_account_names();

	if(array_list == null || array_list.isEmpty())
	{
	    array_list = new ArrayList<> ();
	    array_list.add("(Empty)");
	    m_delete_account_button.setEnabled(false);
	    m_delete_account_verify_checkbox.setEnabled(false);
	    m_remove_local_messages_button.setEnabled(false);
	    m_remove_local_messages_verify_checkbox.setEnabled(false);
	}
	else
	{
	    m_delete_account_button.setEnabled(false);
	    m_delete_account_verify_checkbox.setEnabled(true);
	    m_remove_local_messages_button.setEnabled(false);
	    m_remove_local_messages_verify_checkbox.setEnabled(true);
	}

	ArrayAdapter<String> array_adapter = new ArrayAdapter<>
	    (m_lettera, android.R.layout.simple_spinner_item, array_list);

	m_accounts_spinner.setAdapter(array_adapter);
    }

    private void populate_display()
    {
	ArrayAdapter array_adapter = new ArrayAdapter<>
	    (m_lettera, android.R.layout.simple_spinner_item, s_color_themes);

	m_color_theme_spinner.setAdapter(array_adapter);

	switch(s_database.setting("color_theme").toLowerCase())
	{
	case "black & blue":
	    m_color_theme_spinner.setSelection(0);
	    break;
	case "black & green":
	    m_color_theme_spinner.setSelection(1);
	    break;
	default:
	    m_color_theme_spinner.setSelection(2);
	    break;
	}

	SettingsElement settings_element = s_database.settings_element
	    ("email_folders");

	if(settings_element == null)
	    m_email_folders_spinner.setSelection(0); // Drawer
	else
	    switch(settings_element.m_value.toLowerCase())
	    {
	    default:
		m_email_folders_spinner.setSelection(0);
		break;
	    }

	m_show_status_bar.setChecked
	    (s_database.setting("show_status_bar").equals("true"));
	m_show_vertical_separator_before_settings_checkbox.setChecked
	    (s_database.setting("show_vertical_separator_before_settings").
	     equals("true"));
	array_adapter = new ArrayAdapter<>
	    (m_lettera, android.R.layout.simple_spinner_item, s_icon_themes);
	m_icon_theme_spinner.setAdapter(array_adapter);
	settings_element = s_database.settings_element("icon_theme");

	if(settings_element == null)
	    m_icon_theme_spinner.setSelection(0);
	else
	    switch(settings_element.m_value.toLowerCase())
	    {
	    case "default":
		m_icon_theme_spinner.setSelection(0);
		break;
	    case "funky":
		m_icon_theme_spinner.setSelection(1);
		break;
	    case "hand drawn":
		m_icon_theme_spinner.setSelection(2);
		break;
	    case "material":
		m_icon_theme_spinner.setSelection(3);
		break;
	    case "nuvola":
		m_icon_theme_spinner.setSelection(4);
		break;
	    case "sailfish":
		m_icon_theme_spinner.setSelection(5);
		break;
	    default:
		m_icon_theme_spinner.setSelection(0);
		break;
	    }
    }

    private void populate_network()
    {
	ArrayAdapter<String> array_adapter = new ArrayAdapter<>
	    (m_lettera, android.R.layout.simple_spinner_item, s_proxy_types);
	EmailElement email_element =
	    m_accounts_spinner.getSelectedItem() == null ?
	    null : s_database.email_element(m_accounts_spinner.
					    getSelectedItem().toString());
	SettingsElement settings_element = s_database.settings_element
	    ("primary_email_account");

	m_delete_account_verify_checkbox.setChecked(false);
	m_outbound_as_inbound.setChecked(false);
	m_proxy_type_spinner.setAdapter(array_adapter);
	m_remove_local_messages_verify_checkbox.setChecked(false);

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
	    m_primary_account_checkbox.setChecked(false);
	    m_proxy_address.setText("");
	    m_proxy_password.setText("");
	    m_proxy_port.setText("");
	    m_proxy_type_spinner.setSelection(0);
	    m_proxy_user.setText("");
	}
	else
	{
	    if(m_accounts_spinner.getCount() == 1)
	    {
		ContentValues content_values = new ContentValues();

		content_values.put("key", "primary_email_account");
		content_values.put("value", email_element.m_inbound_email);
		s_database.save_setting(content_values, false);
	    }

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

	    if(settings_element != null)
		m_primary_account_checkbox.setChecked
		    (email_element.m_inbound_email.
		     equals(settings_element.m_value));
	    else
		m_primary_account_checkbox.setChecked(false);

	    m_proxy_address.setText(email_element.m_proxy_address);
	    m_proxy_password.setText(email_element.m_proxy_password);
	    m_proxy_port.setText(String.valueOf(email_element.m_proxy_port));

	    switch(email_element.m_proxy_type)
	    {
	    case "SOCKS":
		m_proxy_type_spinner.setSelection(1);
		break;
	    default:
		m_proxy_type_spinner.setSelection(0);
	    }

	    m_proxy_user.setText(email_element.m_proxy_user);
	}
    }

    private void populate_privacy()
    {
	try
	{
	    m_encryption_key_data.setText
		(Cryptography.key_information(s_pgp.encryption_key_pair().
					      getPublic()));
	}
	catch(Exception exception)
	{
	    m_encryption_key_data.setText
		("SHA-1: " + Cryptography.sha_1_fingerprint(null));
	}

	try
	{
	    m_signature_key_data.setText
		(Cryptography.key_information(s_pgp.signature_key_pair().
					      getPublic()));
	}
	catch(Exception exception)
	{
	    m_signature_key_data.setText
		("SHA-1: " + Cryptography.sha_1_fingerprint(null));
	}
    }

    private void prepare_colors(String color_theme)
    {
	int background_color = Lettera.default_background_color();
	int divider_color = Lettera.default_divider_color();
	int text_color = Lettera.default_text_color();

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

	Utilities.color_children
	    (m_view.findViewById(R.id.main_layout),
	     background_color,
	     divider_color,
	     text_color);
	m_accounts_spinner.getBackground().setColorFilter
	    (text_color, PorterDuff.Mode.SRC_ATOP);
	m_accounts_spinner.setSelection
	    (m_accounts_spinner.getSelectedItemPosition());
	m_color_theme_spinner.getBackground().setColorFilter
	    (text_color, PorterDuff.Mode.SRC_ATOP);
	m_color_theme_spinner.setSelection
	    (m_color_theme_spinner.getSelectedItemPosition());
	m_email_folders_spinner.getBackground().setColorFilter
	    (text_color, PorterDuff.Mode.SRC_ATOP);
	m_email_folders_spinner.setSelection
	    (m_email_folders_spinner.getSelectedItemPosition());
	m_encryption_key_spinner.getBackground().setColorFilter
	    (text_color, PorterDuff.Mode.SRC_ATOP);
	m_encryption_key_spinner.setSelection
	    (m_encryption_key_spinner.getSelectedItemPosition());
	m_icon_theme_spinner.getBackground().setColorFilter
	    (text_color, PorterDuff.Mode.SRC_ATOP);
	m_icon_theme_spinner.setSelection
	    (m_icon_theme_spinner.getSelectedItemPosition());
	m_proxy_type_spinner.getBackground().setColorFilter
	    (text_color, PorterDuff.Mode.SRC_ATOP);
	m_proxy_type_spinner.setSelection
	    (m_proxy_type_spinner.getSelectedItemPosition());
	m_signature_key_spinner.getBackground().setColorFilter
	    (text_color, PorterDuff.Mode.SRC_ATOP);
	m_signature_key_spinner.setSelection
	    (m_signature_key_spinner.getSelectedItemPosition());
	m_view.findViewById(R.id.bottom_divider).setBackgroundColor
	    (divider_color);
	m_view.findViewById(R.id.main_layout).setBackgroundColor
	    (background_color);
    }

    private void prepare_icons()
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
	default:
	    break;
	}
    }

    private void prepare_listeners()
    {
	if(m_lettera == null)
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
		    if(m_lettera.isFinishing())
			return;

		    try
		    {
			((TextView) parent.getChildAt(0)).
			    setTextColor(Lettera.text_color());
		    }
		    catch(Exception exception)
		    {
		    }

		    populate_network();
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent)
		{
		    try
		    {
			((TextView) parent.getChildAt(0)).
			    setTextColor(Lettera.text_color());
		    }
		    catch(Exception exception)
		    {
		    }
		}
	    });

	if(!m_apply_button.hasOnClickListeners())
	    m_apply_button.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(m_lettera.isFinishing())
			return;

		    apply_settings();
		}
	    });

	if(!m_close_button.hasOnClickListeners())
	    m_close_button.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(m_lettera.isFinishing())
			return;

		    m_lettera.prepare_colors(s_database.setting("color_theme"));
		    m_lettera.prepare_icons
			(s_database.settings_element("icon_theme"));

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

	m_color_theme_spinner.setOnItemSelectedListener
	    (new OnItemSelectedListener()
	    {
		@Override
		public void onItemSelected(AdapterView<?> parent,
					   View view,
					   int position,
					   long id)
		{
		    if(m_lettera.isFinishing())
			return;

		    try
		    {
			((TextView) parent.getChildAt(0)).
			    setTextColor(Lettera.text_color());
		    }
		    catch(Exception exception)
		    {
		    }

		    m_lettera.prepare_colors
			(m_color_theme_spinner.getSelectedItem().toString());
		    prepare_colors
			(m_color_theme_spinner.getSelectedItem().toString());
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent)
		{
		    try
		    {
			((TextView) parent.getChildAt(0)).
			    setTextColor(Lettera.text_color());
		    }
		    catch(Exception exception)
		    {
		    }
		}
	    });

	if(!m_delete_account_button.hasOnClickListeners())
	    m_delete_account_button.setOnClickListener
		(new View.OnClickListener()
		{
		    @Override
		    public void onClick(View view)
		    {
			if(m_lettera.isFinishing())
			    return;

			if(m_accounts_spinner.getSelectedItem() != null)
			{
			    Dialog dialog = null;

			    try
			    {
				dialog = new Dialog(m_lettera);
				Windows.show_progress_dialog
				    (m_lettera,
				     dialog,
				     "Deleting e-mail account. " +
				     "Please be patient.",
				     null);

				String email_account = m_accounts_spinner.
				    getSelectedItem().toString();
				Thread thread = new Thread
				    (new DeleteAccount(dialog, email_account));

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
		});

	m_delete_account_verify_checkbox.setOnCheckedChangeListener
	    (new CompoundButton.OnCheckedChangeListener()
	    {
		@Override
		public void onCheckedChanged
		    (CompoundButton button_view, boolean is_checked)
		{
		    if(m_accounts_spinner.getSelectedItem() != null)
			m_delete_account_button.setEnabled
			    (is_checked &&
			     !m_accounts_spinner.
			     getSelectedItem().equals("(Empty)"));
		}
	    });

	if(!m_display_button.hasOnClickListeners())
	    m_display_button.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(m_lettera.isFinishing())
			return;

		    show_display_page();
		}
	    });

	m_email_folders_spinner.setOnItemSelectedListener
	    (new OnItemSelectedListener()
	    {
		@Override
		public void onItemSelected(AdapterView<?> parent,
					   View view,
					   int position,
					   long id)
		{
		    if(m_lettera.isFinishing())
			return;

		    try
		    {
			((TextView) parent.getChildAt(0)).
			    setTextColor(Lettera.text_color());
		    }
		    catch(Exception exception)
		    {
		    }
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent)
		{
		    try
		    {
			((TextView) parent.getChildAt(0)).
			    setTextColor(Lettera.text_color());
		    }
		    catch(Exception exception)
		    {
		    }
		}
	    });

	m_encryption_key_spinner.setOnItemSelectedListener
	    (new OnItemSelectedListener()
	    {
		@Override
		public void onItemSelected(AdapterView<?> parent,
					   View view,
					   int position,
					   long id)
		{
		    if(m_lettera.isFinishing())
			return;

		    try
		    {
			((TextView) parent.getChildAt(0)).
			    setTextColor(Lettera.text_color());
		    }
		    catch(Exception exception)
		    {
		    }
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent)
		{
		    try
		    {
			((TextView) parent.getChildAt(0)).
			    setTextColor(Lettera.text_color());
		    }
		    catch(Exception exception)
		    {
		    }
		}
	    });

	if(!m_generate_keys_button.hasOnClickListeners())
	    m_generate_keys_button.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(m_lettera.isFinishing())
			return;

		    generate_key_pairs();
		}
	    });

	m_generate_keys_checkbox.setOnCheckedChangeListener
	    (new CompoundButton.OnCheckedChangeListener()
	    {
		@Override
		public void onCheckedChanged
		    (CompoundButton button_view, boolean is_checked)
		{
		    m_generate_keys_button.setEnabled(is_checked);
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
		    if(m_lettera.isFinishing())
			return;

		    try
		    {
			((TextView) parent.getChildAt(0)).
			    setTextColor(Lettera.text_color());
		    }
		    catch(Exception exception)
		    {
		    }

		    SettingsElement settings_element = new SettingsElement();

		    settings_element.m_value = m_icon_theme_spinner.
			getSelectedItem().toString();
		    m_lettera.prepare_icons(settings_element);
		    prepare_icons();
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent)
		{
		    try
		    {
			((TextView) parent.getChildAt(0)).
			    setTextColor(Lettera.text_color());
		    }
		    catch(Exception exception)
		    {
		    }
		}
	    });

	m_inbound_email.addTextChangedListener(new TextWatcher()
	{
	    @Override
	    public void afterTextChanged(Editable s)
	    {
	    }

	    @Override
	    public void beforeTextChanged(CharSequence s,
					  int start,
					  int count,
					  int after)
	    {
	    }

	    @Override
	    public void onTextChanged(CharSequence s,
				      int start,
				      int before,
				      int count)
	    {
		if(m_outbound_as_inbound.isChecked())
		    m_outbound_email.setText(s);
	    }
	});

	m_inbound_password.addTextChangedListener(new TextWatcher()
	{
	    @Override
	    public void afterTextChanged(Editable s)
	    {
	    }

	    @Override
	    public void beforeTextChanged(CharSequence s,
					  int start,
					  int count,
					  int after)
	    {
	    }

	    @Override
	    public void onTextChanged(CharSequence s,
				      int start,
				      int before,
				      int count)
	    {
		if(m_outbound_as_inbound.isChecked())
		    m_outbound_password.setText(s);
	    }
	});

	if(!m_network_button.hasOnClickListeners())
	    m_network_button.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(m_lettera.isFinishing())
			return;

		    show_network_page();
		}
	    });

	m_outbound_as_inbound.setOnCheckedChangeListener
	    (new CompoundButton.OnCheckedChangeListener()
	    {
		@Override
		public void onCheckedChanged
		    (CompoundButton button_view, boolean is_checked)
		{
		    m_outbound_email.setEnabled(!is_checked);
		    m_outbound_email.setText
			(m_inbound_email.getText().toString().trim());
		    m_outbound_password.setEnabled(!is_checked);
		    m_outbound_password.setText(m_inbound_password.getText());
		}
	    });

	if(!m_privacy_button.hasOnClickListeners())
	    m_privacy_button.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(m_lettera.isFinishing())
			return;

		    show_privacy_page();
		}
	     });

	m_proxy_type_spinner.setOnItemSelectedListener
	    (new OnItemSelectedListener()
	    {
		@Override
		public void onItemSelected(AdapterView<?> parent,
					   View view,
					   int position,
					   long id)
		{
		    if(m_lettera.isFinishing())
			return;

		    try
		    {
			((TextView) parent.getChildAt(0)).
			    setTextColor(Lettera.text_color());
		    }
		    catch(Exception exception)
		    {
		    }
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent)
		{
		    try
		    {
			((TextView) parent.getChildAt(0)).
			    setTextColor(Lettera.text_color());
		    }
		    catch(Exception exception)
		    {
		    }
		}
	    });

	if(!m_remove_local_messages_button.hasOnClickListeners())
	    m_remove_local_messages_button.setOnClickListener
		(new View.OnClickListener()
		{
		    @Override
		    public void onClick(View view)
		    {
			if(m_lettera.isFinishing())
			    return;

			if(m_accounts_spinner.getSelectedItem() != null)
			{
			    Dialog dialog = null;

			    try
			    {
				dialog = new Dialog(m_lettera);
				Windows.show_progress_dialog
				    (m_lettera,
				     dialog,
				     "Deleting messages. Please be patient.",
				     null);

				String email_account = m_accounts_spinner.
				    getSelectedItem().toString();
				Thread thread = new Thread
				    (new DeleteMessages(dialog, email_account));

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
		});

	m_remove_local_messages_verify_checkbox.setOnCheckedChangeListener
	    (new CompoundButton.OnCheckedChangeListener()
	    {
		@Override
		public void onCheckedChanged
		    (CompoundButton button_view, boolean is_checked)
		{
		    if(m_accounts_spinner.getSelectedItem() != null)
			m_remove_local_messages_button.setEnabled
			    (is_checked &&
			     !m_accounts_spinner.
			     getSelectedItem().equals("(Empty)"));
		}
	    });

	m_signature_key_spinner.setOnItemSelectedListener
	    (new OnItemSelectedListener()
	    {
		@Override
		public void onItemSelected(AdapterView<?> parent,
					   View view,
					   int position,
					   long id)
		{
		    if(m_lettera.isFinishing())
			return;

		    try
		    {
			((TextView) parent.getChildAt(0)).
			    setTextColor(Lettera.text_color());
		    }
		    catch(Exception exception)
		    {
		    }
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent)
		{
		    try
		    {
			((TextView) parent.getChildAt(0)).
			    setTextColor(Lettera.text_color());
		    }
		    catch(Exception exception)
		    {
		    }
		}
	    });

	if(!m_test_inbound_button.hasOnClickListeners())
	    m_test_inbound_button.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(m_lettera.isFinishing())
			return;

		    test_inbound_server();
		}
	    });

	if(!m_test_outbound_button.hasOnClickListeners())
	    m_test_outbound_button.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(m_lettera.isFinishing())
			return;

		    test_outbound_server();
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
	String array[] = null;

	/*
	** Display
	*/

	array_adapter = new ArrayAdapter<>
	    (m_lettera, android.R.layout.simple_spinner_item, s_color_themes);
	m_color_theme_spinner.setAdapter(array_adapter);
	array_adapter = new ArrayAdapter<>
	    (m_lettera,
	     android.R.layout.simple_spinner_item,
	     s_email_folders);
	m_email_folders_spinner.setAdapter(array_adapter);
	array_adapter = new ArrayAdapter<>
	    (m_lettera,
	     android.R.layout.simple_spinner_item,
	     s_icon_themes);
	m_icon_theme_spinner.setAdapter(array_adapter);

	/*
	** Network
	*/

	array = new String[] {"(Empty)"};
	array_adapter = new ArrayAdapter<>
	    (m_lettera, android.R.layout.simple_spinner_item, array);
	m_accounts_spinner.setAdapter(array_adapter);
	m_delete_account_button.setEnabled(false);
	m_inbound_port.setFilters(new InputFilter[] {s_port_filter});
	m_outbound_port.setFilters(new InputFilter[] {s_port_filter});
	m_proxy_port.setFilters(new InputFilter[] {s_port_filter});
	array_adapter = new ArrayAdapter<>
	    (m_lettera, android.R.layout.simple_spinner_item, s_proxy_types);
	m_proxy_type_spinner.setAdapter(array_adapter);
	m_remove_local_messages_button.setEnabled(false);

	/*
	** Privacy
	*/

	array = new String[] {"McEliece", "RSA"};
	array_adapter = new ArrayAdapter<>
	    (m_lettera, android.R.layout.simple_spinner_item, array);
	m_encryption_key_spinner.setAdapter(array_adapter);
	m_generate_keys_button.setEnabled(false);
	array = new String[] {"RSA"};
	array_adapter = new ArrayAdapter<>
	    (m_lettera, android.R.layout.simple_spinner_item, array);
	m_signature_key_spinner.setAdapter(array_adapter);
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

    private void show_privacy_page()
    {
	m_current_page = PageEnumerator.PRIVACY_PAGE;
	m_display_button.setBackgroundResource(icon("display"));
	m_display_layout.setVisibility(View.GONE);
	m_network_button.setBackgroundResource(icon("network"));
	m_network_layout.setVisibility(View.GONE);
	m_privacy_button.setBackgroundResource(icon("privacy_pressed"));
	m_privacy_layout.setVisibility(View.VISIBLE);
    }

    private void test_inbound_server()
    {
	Dialog dialog = null;

	try
	{
	    dialog = new Dialog(m_lettera);
	    Windows.show_progress_dialog
		(m_lettera, dialog, "Testing IMAPS.\nPlease be patient.", null);

	    Thread thread = new Thread
		(new EmailTest(dialog,
			       m_inbound_email.getText().toString(),
			       m_inbound_address.getText().toString(),
			       m_inbound_password.getText().toString(),
			       m_inbound_port.getText().toString(),
			       "imaps",
			       m_proxy_address.getText().toString(),
			       m_proxy_password.getText().toString(),
			       m_proxy_port.getText().toString(),
			       m_proxy_type_spinner.getSelectedItem().
			       toString(),
			       m_proxy_user.getText().toString()));

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

    private void test_outbound_server()
    {
	Dialog dialog = null;

	try
	{
	    dialog = new Dialog(m_lettera);
	    Windows.show_progress_dialog
		(m_lettera, dialog, "Testing SMTPS.\nPlease be patient.", null);

	    Thread thread = new Thread
		(new EmailTest(dialog,
			       m_outbound_email.getText().toString(),
			       m_outbound_address.getText().toString(),
			       m_outbound_password.getText().toString(),
			       m_outbound_port.getText().toString(),
			       "smtps",
			       m_proxy_address.getText().toString(),
			       m_proxy_password.getText().toString(),
			       m_proxy_port.getText().toString(),
			       m_proxy_type_spinner.getSelectedItem().
			       toString(),
			       m_proxy_user.getText().toString()));

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

    public Settings(Lettera lettera, View parent)
    {
	m_lettera = lettera;
	m_parent = parent;

	LayoutInflater inflater = (LayoutInflater) m_lettera.getSystemService
	    (Context.LAYOUT_INFLATER_SERVICE);

	m_layout_params = new WindowManager.LayoutParams();
	m_layout_params.height = WindowManager.LayoutParams.MATCH_PARENT;
	m_layout_params.width = WindowManager.LayoutParams.MATCH_PARENT;
	m_view = inflater.inflate(R.layout.settings, null);

	/*
	** Prepare things.
	*/

	initialize_widget_members();
	prepare_listeners();
	prepare_widgets();

	/*
	** The cute dialog.
	*/

	m_dialog = new Dialog
	    (m_lettera,
	     android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen);
	m_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
	m_dialog.setCancelable(false);
	m_dialog.setContentView(m_view);
	m_dialog.setTitle("Settings");

	if(m_dialog.getWindow() != null)
	    m_dialog.getWindow().setAttributes(m_layout_params);
    }

    public static int icon_from_name(String name)
    {
	if(name == null)
	    return R.drawable.lettera;

	switch(name.toLowerCase())
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
	case "funky_compose":
	    return R.drawable.funky_compose;
	case "funky_contacts":
	    return R.drawable.funky_contacts;
	case "funky_display":
	    return R.drawable.funky_display;
	case "funky_display_pressed":
	    return R.drawable.funky_display_pressed;
	case "funky_download":
	    return R.drawable.funky_download;
	case "funky_messaging":
	    return R.drawable.funky_messaging;
	case "funky_network":
	    return R.drawable.funky_network;
	case "funky_network_pressed":
	    return R.drawable.funky_network_pressed;
	case "funky_privacy":
	    return R.drawable.funky_privacy;
	case "funky_privacy_pressed":
	    return R.drawable.funky_privacy_pressed;
	case "funky_settings":
	    return R.drawable.funky_settings;
	case "hand drawn_compose":
	    return R.drawable.hand_drawn_compose;
	case "hand drawn_contacts":
	    return R.drawable.hand_drawn_contacts;
	case "hand drawn_display":
	    return R.drawable.hand_drawn_display;
	case "hand drawn_display_pressed":
	    return R.drawable.hand_drawn_display_pressed;
	case "hand drawn_download":
	    return R.drawable.hand_drawn_download;
	case "hand drawn_messaging":
	    return R.drawable.hand_drawn_messaging;
	case "hand drawn_network":
	    return R.drawable.hand_drawn_network;
	case "hand drawn_network_pressed":
	    return R.drawable.hand_drawn_network_pressed;
	case "hand drawn_privacy":
	    return R.drawable.hand_drawn_privacy;
	case "hand drawn_privacy_pressed":
	    return R.drawable.hand_drawn_privacy_pressed;
	case "hand drawn_settings":
	    return R.drawable.hand_drawn_settings;
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
	case "sailfish_compose":
	    return R.drawable.sailfish_compose;
	case "sailfish_contacts":
	    return R.drawable.sailfish_contacts;
	case "sailfish_display":
	    return R.drawable.sailfish_display;
	case "sailfish_display_pressed":
	    return R.drawable.sailfish_display_pressed;
	case "sailfish_download":
	    return R.drawable.sailfish_download;
	case "sailfish_messaging":
	    return R.drawable.sailfish_messaging;
	case "sailfish_network":
	    return R.drawable.sailfish_network;
	case "sailfish_network_pressed":
	    return R.drawable.sailfish_network_pressed;
	case "sailfish_privacy":
	    return R.drawable.sailfish_privacy;
	case "sailfish_privacy_pressed":
	    return R.drawable.sailfish_privacy_pressed;
	case "sailfish_settings":
	    return R.drawable.sailfish_settings;
	default:
	    return R.drawable.lettera;
	}
    }

    public void show()
    {
	m_delete_account_verify_checkbox.setChecked(false);
	m_dialog.show();
	m_encryption_key_spinner.setSelection(0);
	m_generate_keys_checkbox.setChecked(false);

	if(m_current_page == PageEnumerator.NETWORK_PAGE)
	    m_inbound_address.requestFocus();

	m_outbound_as_inbound.setChecked(false);
	populate();
	prepare_colors(s_database.setting("color_theme"));
	prepare_icons();
    }
}
