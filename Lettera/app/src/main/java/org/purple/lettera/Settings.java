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
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Properties;
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
	    m_email = email;
	    m_host = host;
	    m_password = password;
	    m_port = port;
	    m_protocol = protocol;
	    m_proxy_address = proxy_address;
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
		/*
		** https://javaee.github.io/javamail/docs/api/com/sun/mail/imap/package-summary.html
		** https://javaee.github.io/javamail/docs/api/com/sun/mail/smtp/SMTPTransport.html
		*/

		Properties properties = new Properties();

		properties.setProperty
		    ("mail." + m_protocol + ".ssl.enable", "true");
		properties.setProperty
		    ("mail." + m_protocol + ".starttls.enable", "true");
		properties.setProperty
		    ("mail." + m_protocol + ".starttls.required", "true");

		switch(m_protocol)
		{
		case "imaps":
		    properties.setProperty
			("mail." + m_protocol + ".connectiontimeout", "10000");
		    properties.setProperty
			("mail." + m_protocol + ".timeout", "10000");
		    break;
		case "smtp":
		case "smtps":
		    properties.setProperty
			("mail.smtp.connectiontimeout", "10000");
		    properties.setProperty("mail.smtp.localhost", "localhost");
		    properties.setProperty("mail.smtp.timeout", "10000");
		    properties.setProperty
			("mail.smtps.localhost", "localhost");
		    break;
		default:
		    break;
		}

		if(!m_proxy_address.isEmpty())
		    switch(m_proxy_type)
		    {
		    case "HTTP":
			switch(m_protocol)
			{
			case "imaps":
			    properties.setProperty
				("mail." + m_protocol + ".proxy.host",
				 m_proxy_address);
			    properties.setProperty
				("mail." + m_protocol + ".proxy.password",
				 m_proxy_password);
			    properties.setProperty
				("mail." + m_protocol + ".proxy.port",
				 m_proxy_port);
			    properties.setProperty
				("mail." + m_protocol + ".proxy.user",
				 m_proxy_user);
			    break;
			case "smtp":
			case "smtps":
			    properties.setProperty
				("mail.smtp.proxy.host", m_proxy_address);
			    properties.setProperty
				("mail.smtp.proxy.password", m_proxy_password);
			    properties.setProperty
				("mail.smtp.proxy.port", m_proxy_port);
			    properties.setProperty
				("mail.smtp.proxy.user", m_proxy_user);
			    break;
			default:
			    break;
			}

			break;
		    case "SOCKS":
			switch(m_protocol)
			{
			case "imaps":
			    properties.setProperty
				("mail.imaps.socks.host", m_proxy_address);
			    properties.setProperty
				("mail.imaps.socks.port", m_proxy_port);
			    break;
			case "smtp":
			case "smtps":
			    properties.setProperty
				("mail.smtp.socks.host", m_proxy_address);
			    properties.setProperty
				("mail.smtp.socks.port", m_proxy_port);
			    break;
			default:
			    break;
			}

			break;
		    default:
			break;
		    }

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
			    break;
			case "smtp":
			case "smtps":
			    break;
			default:
			    break;
			}

			if(m_dialog != null)
			    m_dialog.dismiss();

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
	    finally
	    {
		if(m_dialog != null)
		    m_dialog.dismiss();
	    }
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
		    m_database.delete("open_pgp");
	    }
	    catch(Exception exception)
	    {
		m_error = true;
	    }

	    try
	    {
		((Activity) m_context).runOnUiThread(new Runnable()
		{
		    @Override
		    public void run()
		    {
			if(m_dialog != null)
			    m_dialog.dismiss();

			if(m_encryption_key_pair == null)
			    m_encryption_key_data.setText
				("SHA-1: " +
				 Cryptography.sha_1_fingerprint(null));
			else
			{
			    if(m_database.
			       save_pgp_key_pair(m_encryption_key_pair,
						 "encryption"))
			    {
				m_encryption_key_data.setText
				    (Cryptography.
				     key_information(m_encryption_key_pair.
						     getPublic()));
				m_pgp.set_encryption_key_pair
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
			    if(m_database.
			       save_pgp_key_pair(m_signature_key_pair,
						 "signature"))
			    {
				m_pgp.set_signature_key_pair
				    (m_signature_key_pair);
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
				(m_context, "Key pairs generated!", "Success");
			else
			{
			    m_database.delete("open_pgp");
			    m_pgp.set_encryption_key_pair(null);
			    m_pgp.set_signature_key_pair(null);
			    Windows.show_dialog
				(m_context,
				 "Cannot generate key pairs!",
				 "Error");
			}
		    }
		});
	    }
	    catch(Exception exception)
	    {
	    }
	    finally
	    {
		if(m_dialog != null)
		    m_dialog.dismiss();
	    }
	}
    }

    private Button m_apply_button = null;
    private Button m_close_button = null;
    private Button m_delete_account_button = null;
    private Button m_display_button = null;
    private Button m_generate_keys_button = null;
    private Button m_network_button = null;
    private Button m_privacy_button = null;
    private Button m_test_inbound_button = null;
    private Button m_test_outbound_button = null;
    private CheckBox m_delete_on_server_checkbox = null;
    private CheckBox m_delete_account_verify_checkbox = null;
    private CheckBox m_generate_keys_checkbox = null;
    private Context m_context = null;
    private Dialog m_dialog = null;
    private Spinner m_accounts_spinner = null;
    private Spinner m_encryption_key_spinner = null;
    private Spinner m_icon_theme_spinner = null;
    private Spinner m_proxy_type_spinner = null;
    private Spinner m_signature_key_spinner = null;
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
    private final Database m_database = Database.instance();
    private final PGP m_pgp = PGP.instance();
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
    private final static String s_icon_themes_array[] =	new String[]
	{"Default", "Hand Drawn", "Material", "Nuvola", "SAILFISH"};
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

	    if(m_context instanceof Lettera)
		((Lettera) m_context).prepare_icons();

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

    private void generate_key_pairs()
    {
	Dialog dialog = null;

	try
	{
	    dialog = new Dialog(m_context);
	    Windows.show_progress_dialog
		(m_context, dialog, "Generating key pairs. Please be patient.");

	    Thread thread = new Thread
		(new GenerateKeyPairs(dialog,
				      m_encryption_key_spinner.
				      getSelectedItem().toString(),
				      m_signature_key_spinner.
				      getSelectedItem().toString()));

	    thread.start();
	}
	catch(Exception exception)
	{
	    if(dialog != null)
		dialog.dismiss();
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
	m_delete_account_verify_checkbox = (CheckBox)
	    m_view.findViewById(R.id.delete_account_verify_checkbox);
	m_delete_on_server_checkbox = (CheckBox)
	    m_view.findViewById(R.id.delete_on_server_checkbox);
	m_display_button = (Button) m_view.findViewById
	    (R.id.display_button);
	m_display_layout = m_view.findViewById(R.id.display_layout);
	m_encryption_key_data = (TextView) m_view.findViewById
	    (R.id.encryption_key_data);
	m_encryption_key_spinner = (Spinner) m_view.findViewById
	    (R.id.encryption_key_spinner);
	m_generate_keys_button = m_view.findViewById(R.id.generate_keys_button);
	m_generate_keys_checkbox = (CheckBox) m_view.findViewById
	    (R.id.generate_keys_checkbox);
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
	m_proxy_address = (TextView) m_view.findViewById(R.id.proxy_address);
	m_proxy_password = (TextView) m_view.findViewById(R.id.proxy_password);
	m_proxy_port = (TextView) m_view.findViewById(R.id.proxy_port);
	m_proxy_type_spinner = (Spinner) m_view.findViewById
	    (R.id.proxy_type_spinner);
	m_proxy_user = (TextView) m_view.findViewById(R.id.proxy_user);
	m_signature_key_data = m_view.findViewById(R.id.signature_key_data);
	m_signature_key_spinner = (Spinner) m_view.findViewById
	    (R.id.signature_key_spinner);
	m_test_inbound_button = (Button) m_view.findViewById
	    (R.id.test_inbound_button);
	m_test_outbound_button = (Button) m_view.findViewById
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
	if(m_context == null)
	    return;

	if(((Activity) m_context).isFinishing())
	    return;

	ArrayList<String> array_list = m_database.email_account_names();

	if(array_list == null || array_list.isEmpty())
	{
	    array_list = new ArrayList<> ();
	    array_list.add("(Empty)");
	    m_delete_account_button.setEnabled(false);
	    m_delete_account_verify_checkbox.setEnabled(false);
	}
	else
	{
	    m_delete_account_button.setEnabled
		(m_delete_account_verify_checkbox.isChecked());
	    m_delete_account_verify_checkbox.setEnabled(true);
	}

	ArrayAdapter<String> array_adapter = new ArrayAdapter<>
	    (m_context, android.R.layout.simple_spinner_item, array_list);

	m_accounts_spinner.setAdapter(array_adapter);
    }

    private void populate_display()
    {
	ArrayAdapter array_adapter = new ArrayAdapter<>
	    (m_context,
	     android.R.layout.simple_spinner_item,
	     s_icon_themes_array);

	m_icon_theme_spinner.setAdapter(array_adapter);

	SettingsElement settings_element = m_database.settings_element
	    ("icon_theme");

	if(settings_element == null)
	    m_icon_theme_spinner.setSelection(0);
	else
	    switch(settings_element.m_value.toLowerCase())
	    {
	    case "default":
		m_icon_theme_spinner.setSelection(0);
		break;
	    case "hand drawn":
		m_icon_theme_spinner.setSelection(1);
		break;
	    case "material":
		m_icon_theme_spinner.setSelection(2);
		break;
	    case "nuvola":
		m_icon_theme_spinner.setSelection(3);
		break;
	    case "sailfish":
		m_icon_theme_spinner.setSelection(4);
		break;
	    default:
		m_icon_theme_spinner.setSelection(0);
		break;
	    }
    }

    private void populate_network()
    {
	ArrayAdapter<String> array_adapter = new ArrayAdapter<>
	    (m_context, android.R.layout.simple_spinner_item, s_proxy_types);
	EmailElement email_element =
	    m_accounts_spinner.getSelectedItem() == null ?
	    null : m_database.email_element(m_accounts_spinner.
					    getSelectedItem().toString());

	m_proxy_type_spinner.setAdapter(array_adapter);

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
	    m_proxy_address.setText("");
	    m_proxy_password.setText("");
	    m_proxy_port.setText("");
	    m_proxy_type_spinner.setSelection(0);
	    m_proxy_user.setText("");
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
		(Cryptography.key_information(m_pgp.encryption_key_pair().
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
		(Cryptography.key_information(m_pgp.signature_key_pair().
					      getPublic()));
	}
	catch(Exception exception)
	{
	    m_signature_key_data.setText
		("SHA-1: " + Cryptography.sha_1_fingerprint(null));
	}
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

			if(m_accounts_spinner.getSelectedItem() != null &&
			   m_database.
			   delete_email_account(m_accounts_spinner.
						getSelectedItem().toString()))
			{
			    m_delete_account_verify_checkbox.setChecked(false);
			    populate_accounts_spinner();
			    populate_network();
			}
		    }
		});

	m_delete_account_verify_checkbox.setOnCheckedChangeListener
	    (new CompoundButton.OnCheckedChangeListener()
	    {
		@Override
		public void onCheckedChanged
		    (CompoundButton buttonView, boolean isChecked)
		{
		    if(m_accounts_spinner.getSelectedItem() != null)
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

		    m_current_page = PageEnumerator.DISPLAY_PAGE;
		    show_display_page();
		}
	    });

	if(!m_generate_keys_button.hasOnClickListeners())
	    m_generate_keys_button.setOnClickListener(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(((Activity) m_context).isFinishing())
			return;

		    generate_key_pairs();
		}
	    });

	m_generate_keys_checkbox.setOnCheckedChangeListener
	    (new CompoundButton.OnCheckedChangeListener()
	    {
		@Override
		public void onCheckedChanged
		    (CompoundButton buttonView, boolean isChecked)
		{
		    m_generate_keys_button.setEnabled(isChecked);
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
	array_adapter = new ArrayAdapter<>
	    (m_context,
	     android.R.layout.simple_spinner_item,
	     s_icon_themes_array);
	m_icon_theme_spinner.setAdapter(array_adapter);

	/*
	** Network
	*/

	array = new String[] {"(Empty)"};
	array_adapter = new ArrayAdapter<>
	    (m_context, android.R.layout.simple_spinner_item, array);
	m_accounts_spinner.setAdapter(array_adapter);
	m_delete_account_button.setEnabled(false);
	m_inbound_port.setFilters(new InputFilter[] {s_port_filter});
	m_outbound_port.setFilters(new InputFilter[] {s_port_filter});
	m_proxy_port.setFilters(new InputFilter[] {s_port_filter});
	array_adapter = new ArrayAdapter<>
	    (m_context, android.R.layout.simple_spinner_item, s_proxy_types);
	m_proxy_type_spinner.setAdapter(array_adapter);

	/*
	** Privacy
	*/

	array = new String[] {"McEliece", "RSA"};
	array_adapter = new ArrayAdapter<>
	    (m_context, android.R.layout.simple_spinner_item, array);
	m_encryption_key_spinner.setAdapter(array_adapter);
	m_generate_keys_button.setEnabled(false);
	array = new String[] {"RSA"};
	array_adapter = new ArrayAdapter<>
	    (m_context, android.R.layout.simple_spinner_item, array);
	m_signature_key_spinner.setAdapter(array_adapter);
    }

    private void show_display_page()
    {
	m_display_button.setBackgroundResource(icon("display_pressed"));
	m_display_layout.setVisibility(View.VISIBLE);
	m_network_button.setBackgroundResource(icon("network"));
	m_network_layout.setVisibility(View.GONE);
	m_privacy_button.setBackgroundResource(icon("privacy"));
	m_privacy_layout.setVisibility(View.GONE);
    }

    private void show_network_page()
    {
	m_display_button.setBackgroundResource(icon("display"));
	m_display_layout.setVisibility(View.GONE);
	m_network_button.setBackgroundResource(icon("network_pressed"));
	m_network_layout.setVisibility(View.VISIBLE);
	m_privacy_button.setBackgroundResource(icon("privacy"));
	m_privacy_layout.setVisibility(View.GONE);
    }

    private void test_inbound_server()
    {
	Dialog dialog = null;

	try
	{
	    dialog = new Dialog(m_context);
	    Windows.show_progress_dialog
		(m_context, dialog, "Testing IMAPS. Please be patient.");

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
	catch(Exception exception)
	{
	    if(dialog != null)
		dialog.dismiss();
	}
    }

    private void test_outbound_server()
    {
	Dialog dialog = null;

	try
	{
	    dialog = new Dialog(m_context);
	    Windows.show_progress_dialog
		(m_context, dialog, "Testing SMTPS. Please be patient.");

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
	catch(Exception exception)
	{
	    if(dialog != null)
		dialog.dismiss();
	}
    }

    public Settings(Context context, View parent)
    {
	m_context = context;
	m_parent = parent;

	LayoutInflater inflater = (LayoutInflater) m_context.getSystemService
	    (Context.LAYOUT_INFLATER_SERVICE);

	m_layout_params = new WindowManager.LayoutParams();
	m_layout_params.gravity = Gravity.CENTER_VERTICAL | Gravity.TOP;
	m_layout_params.height = WindowManager.LayoutParams.WRAP_CONTENT;
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

	m_dialog = new Dialog(m_context);
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
	m_dialog.show();
	populate();
	prepare_icons();
    }
}
