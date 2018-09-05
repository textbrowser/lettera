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
import android.widget.Button;

public class Settings
{
    private Context m_context = null;
    private Dialog m_dialog = null;
    private View m_parent = null;
    private View m_view = null;

    public Settings(Context context, View parent)
    {
	m_context = context;
	m_parent = parent;
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
	    float density = m_context.getResources().getDisplayMetrics().
		density;

	    m_dialog = new Dialog(m_context);
	    m_view = inflater.inflate(R.layout.settings, null);
	    m_view.setPaddingRelative((int) (15 * density),
				      (int) (15 * density),
				      (int) (15 * density),
				      (int) (15 * density));

	    /*
	    ** Prepare listeners.
	    */

	    Button button = null;

	    button = (Button) m_view.findViewById(R.id.close_button);
	    button.setOnClickListener(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(((Activity) m_context).isFinishing())
			return;

		    m_dialog.dismiss();
		}
	    });
	    m_dialog.setCancelable(false);
	    m_dialog.setContentView(m_view);
	    m_dialog.setTitle("Settings");
	    m_dialog.show();
	    layout_params.height = WindowManager.LayoutParams.WRAP_CONTENT;
	    layout_params.width = WindowManager.LayoutParams.MATCH_PARENT;
	    m_dialog.getWindow().setAttributes(layout_params);
	}
	catch(Exception exception)
	{
	    m_dialog = null;
	    m_view = null;
	}
    }
}
