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

import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class Lettera extends AppCompatActivity
{
    private Button m_settings_button = null;
    private Database m_database = null;
    private Settings m_settings = null;

    private void initialize_widget_members()
    {
	m_settings_button = findViewById(R.id.settings_button);
    }

    private void prepare_button_listeners()
    {
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
	** JavaMail may open sockets on the main thread.
	*/

	StrictMode.ThreadPolicy policy = new
	    StrictMode.ThreadPolicy.Builder().permitAll().build();

	StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_lettera);

	/*
	** Prepare the rest.
	*/

	initialize_widget_members();
	m_database = Database.getInstance(getApplicationContext());
	m_settings = new Settings(Lettera.this, findViewById(R.id.main_layout));
	prepare_button_listeners();
    }
}
