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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

public class Letter
{
    private Database m_database = null;
    private Dialog m_dialog = null;
    private ImageButton m_close_button = null;
    private Lettera m_lettera = null;
    private View m_parent = null;
    private View m_view = null;
    private WindowManager.LayoutParams m_layout_params = null;

    public Letter(Lettera lettera, View parent)
    {
	m_lettera = lettera;
	m_parent = parent;

	LayoutInflater inflater = (LayoutInflater) m_lettera.getSystemService
	    (Context.LAYOUT_INFLATER_SERVICE);

	m_layout_params = new WindowManager.LayoutParams();
	m_layout_params.gravity = Gravity.CENTER_VERTICAL | Gravity.TOP;
	m_layout_params.height = WindowManager.LayoutParams.MATCH_PARENT;
	m_layout_params.width = WindowManager.LayoutParams.MATCH_PARENT;
	m_view = inflater.inflate(R.layout.letter, null);

	/*
	** The cute popup.
	*/

	m_dialog = new Dialog(m_lettera);
	m_dialog.setCancelable(false);
	m_dialog.setContentView(m_view);
	m_dialog.setTitle("Letter");

	if(m_dialog.getWindow() != null)
	    m_dialog.getWindow().setAttributes(m_layout_params);

	/*
	** Initialize other widgets.
	*/

	initialize_widget_members();
	prepare_listeners();
    }

    private void initialize_widget_members()
    {
	m_close_button = m_view.findViewById(R.id.close_button);
    }

    private void prepare_listeners()
    {
	if(!m_close_button.hasOnClickListeners())
	    m_close_button.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(m_lettera.isFinishing())
			return;

		    dismiss();
		}
	    });
    }

    public void dismiss()
    {
	try
	{
	    m_dialog.dismiss();
	}
	catch(Exception exception)
	{
	}
    }

    public void show()
    {
	m_dialog.show();
	m_view.findViewById(R.id.top_divider).setBackgroundColor
	    (Lettera.divider_color());
	m_view.setBackgroundColor(Lettera.background_color());
    }
}
