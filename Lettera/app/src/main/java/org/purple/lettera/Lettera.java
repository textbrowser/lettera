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
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import java.security.KeyPair;

public class Lettera extends AppCompatActivity
{
    private class PopulatePGP implements Runnable
    {
	private Dialog m_dialog = null;

	private PopulatePGP(Dialog dialog)
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

	    try
	    {
		Lettera.this.runOnUiThread(new Runnable()
		{
		    @Override
		    public void run()
		    {
			if(m_dialog != null)
			    m_dialog.dismiss();
		    }
		});
	    }
	    catch(Exception exception)
	    {
	    }
	}
    }

    private Button m_compose_button = null;
    private Button m_contacts_button = null;
    private Button m_download_button = null;
    private Button m_messaging_button = null;
    private Button m_settings_button = null;
    private Database m_database = null;
    private Settings m_settings = null;
    private Spinner m_folders_spinner = null;
    private final PGP m_pgp = PGP.instance();

    private void download()
    {
	Dialog dialog = null;

	try
	{
	    dialog = new Dialog(Lettera.this);
	    Windows.show_progress_dialog
		(Lettera.this,
		 dialog,
		 "Download e-mail folders. Please be patient.");
	}
	catch(Exception exception)
	{
	    if(dialog != null)
		dialog.dismiss();
	}
    }

    private void initialize_widget_members()
    {
	m_compose_button = (Button) findViewById(R.id.compose_button);
	m_contacts_button = (Button) findViewById(R.id.contacts_button);
	m_download_button = (Button) findViewById(R.id.download_button);
	m_folders_spinner = (Spinner) findViewById(R.id.folders);
	m_messaging_button = (Button) findViewById(R.id.messaging_button);
	m_settings_button = (Button) findViewById(R.id.settings_button);
    }

    private void prepare_button_listeners()
    {
	if(m_download_button != null && !m_download_button.
	                                 hasOnClickListeners())
	    m_download_button.setOnClickListener(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(Lettera.this.isFinishing() || m_settings == null)
			return;

		    download();
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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

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
	m_database = Database.instance(getApplicationContext());
	m_folders_spinner.setVisibility(View.GONE);
	m_settings = new Settings(Lettera.this, findViewById(R.id.main_layout));
	new Handler().postDelayed(new Runnable()
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
			 "Populating PGP container. Please be patient.");

		    Thread thread = new Thread(new PopulatePGP(dialog));

		    thread.start();
		}
		catch(Exception exception)
		{
		    if(dialog != null)
			dialog.dismiss();
		}
	    }
	}, 500);
	prepare_button_listeners();
	prepare_icons();
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
}
