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

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;

public class Letter
{
    private Context m_context = null;
    private Database m_database = null;
    private PopupWindow m_popup_window = null;
    private View m_parent = null;
    private View m_view = null;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    public Letter(Context context, View parent)
    {
	m_context = context;
	m_parent = parent;

	LayoutInflater inflater = (LayoutInflater) m_context.getSystemService
	    (Context.LAYOUT_INFLATER_SERVICE);

	m_view = inflater.inflate(R.layout.folders_drawer_move, null);

	/*
	** The cute popup.
	*/

	m_popup_window = new PopupWindow(m_context);
	m_popup_window.setAttachedInDecor(true);
	m_popup_window.setContentView(m_view);
	m_popup_window.setFocusable(true);
	m_popup_window.setOutsideTouchable(true);
	m_popup_window.setWidth((int) (0.40 * m_parent.getWidth()));

	/*
	** Initialize other widgets.
	*/

	initialize_widget_members();
    }

    private void initialize_widget_members()
    {
    }

    public void dismiss()
    {
	try
	{
	    m_popup_window.dismiss();
	}
	catch(Exception exception)
	{
	}
    }

    public void show(View view)
    {
	m_popup_window.showAtLocation(m_parent, Gravity.CENTER, 0, 0);
	m_view.findViewById(R.id.divider).setBackgroundColor
	    (Lettera.divider_color());
	m_view.setBackgroundColor(Lettera.background_color());
    }
}
