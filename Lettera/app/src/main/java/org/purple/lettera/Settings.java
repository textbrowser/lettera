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
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class Settings
{
    private Context m_context = null;
    private Dialog m_dialog = null;
    private View m_parent = null;
    private View m_view = null;

    private void prepare_listeners()
    {
	Button button = null;

	button = (Button) m_view.findViewById(R.id.close_button);

	if(!button.hasOnClickListeners())
	    button.setOnClickListener(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(((Activity) m_context).isFinishing())
			return;

		    m_dialog.dismiss();
		}
	    });

	button = (Button) m_view.findViewById(R.id.display_button);

	if(!button.hasOnClickListeners())
	    button.setOnClickListener(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(((Activity) m_context).isFinishing())
			return;

		    m_view.findViewById(R.id.display_layout).setVisibility
			(View.VISIBLE);
		    m_view.findViewById(R.id.network_layout).setVisibility
			(View.GONE);
		    m_view.findViewById(R.id.privacy_layout).setVisibility
			(View.GONE);
		}
	    });

	button = (Button) m_view.findViewById(R.id.network_button);

	if(!button.hasOnClickListeners())
	    button.setOnClickListener(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(((Activity) m_context).isFinishing())
			return;

		    m_view.findViewById(R.id.display_layout).setVisibility
			(View.GONE);
		    m_view.findViewById(R.id.network_layout).setVisibility
			(View.VISIBLE);
		    m_view.findViewById(R.id.privacy_layout).setVisibility
			(View.GONE);
		}
	    });

	button = (Button) m_view.findViewById(R.id.privacy_button);

	if(!button.hasOnClickListeners())
	    button.setOnClickListener(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(((Activity) m_context).isFinishing())
			return;

		    m_view.findViewById(R.id.display_layout).setVisibility
			(View.GONE);
		    m_view.findViewById(R.id.network_layout).setVisibility
			(View.GONE);
		    m_view.findViewById(R.id.privacy_layout).setVisibility
			(View.VISIBLE);
		}
	    });
    }

    private void prepare_widgets()
    {
	/*
	** Set Display as the primary section.
	*/

	m_view.findViewById(R.id.network_layout).setVisibility(View.GONE);
	m_view.findViewById(R.id.privacy_layout).setVisibility(View.GONE);

	ArrayAdapter<String> array_adapter;
	Spinner spinner = null;
	String array[] = null;

	/*
	** Network
	*/

	array = new String[] {"Default"};
	array_adapter = new ArrayAdapter<>
	    (m_context, android.R.layout.simple_spinner_item, array);
	spinner = (Spinner) m_view.findViewById(R.id.color_theme_spinner);
	spinner.setAdapter(array_adapter);
	array = new String[] {"(Empty)"};
	array_adapter = new ArrayAdapter<>
	    (m_context, android.R.layout.simple_spinner_item, array);
	spinner = (Spinner) m_view.findViewById(R.id.accounts_spinner);
	spinner.setAdapter(array_adapter);
	m_view.findViewById(R.id.delete_account_button).setEnabled(false);

	/*
	** Privacy
	*/

	array = new String[] {"McEliece", "RSA"};
	array_adapter = new ArrayAdapter<>
	    (m_context, android.R.layout.simple_spinner_item, array);
	spinner = (Spinner) m_view.findViewById(R.id.encryption_key_spinner);
	spinner.setAdapter(array_adapter);
	array = new String[] {"RSA"};
	array_adapter = new ArrayAdapter<>
	    (m_context, android.R.layout.simple_spinner_item, array);
	spinner = (Spinner) m_view.findViewById(R.id.signature_key_spinner);
	spinner.setAdapter(array_adapter);
    }

    public Settings(Context context, View parent)
    {
	m_context = context;
	m_parent = parent;
    }

    public void populate()
    {
    }

    public void show()
    {
	if(m_context == null || m_parent == null)
	    return;
	else if(m_dialog != null || m_view != null)
	{
	    try
	    {
		m_dialog.show();
	    }
	    catch(Exception exception)
	    {
	    }

	    return;
	}

	try
	{
	    LayoutInflater inflater = (LayoutInflater) m_context.
		getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    WindowManager.LayoutParams layout_params =
		new WindowManager.LayoutParams();

	    layout_params.height = WindowManager.LayoutParams.WRAP_CONTENT;
	    layout_params.width = WindowManager.LayoutParams.MATCH_PARENT;

	    float density = m_context.getResources().getDisplayMetrics().
		density;

	    m_dialog = new Dialog(m_context);
	    m_view = inflater.inflate(R.layout.settings, null);
	    m_view.setPaddingRelative((int) (15 * density),
				      (int) (15 * density),
				      (int) (15 * density),
				      (int) (15 * density));

	    /*
	    ** Prepare things.
	    */

	    prepare_listeners();
	    prepare_widgets();

	    /*
	    ** The cute dialog.
	    */

	    m_dialog.setCancelable(false);
	    m_dialog.setContentView(m_view);
	    m_dialog.setTitle("Settings");
	    m_dialog.show();
	    m_dialog.getWindow().setAttributes(layout_params); // After show().
	}
	catch(Exception exception)
	{
	    m_dialog = null;
	    m_view = null;
	}
    }
}
