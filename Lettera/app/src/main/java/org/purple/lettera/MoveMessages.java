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
**    derived from Lettera without specific prior written permission.
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
import android.graphics.Rect;
import android.os.Build;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;
import java.lang.ref.WeakReference;

public class MoveMessages
{
    private class MoveMessagesLinearLayoutManager extends LinearLayoutManager
    {
	MoveMessagesLinearLayoutManager(Context context)
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

    private MoveMessagesAdapter m_adapter = null;
    private MoveMessagesLinearLayoutManager m_layout_manager = null;
    private PopupWindow m_popup_window = null;
    private RecyclerView m_recycler = null;
    private View m_view = null;
    private WeakReference<Lettera> m_lettera = null;
    private WeakReference<View> m_parent = null;
    private long m_message_oid = -1L;

    public MoveMessages(Lettera lettera,
			String email_account,
			String folder_name,
			View parent,
			long message_oid)
    {
	m_lettera = new WeakReference<> (lettera);
	m_message_oid = message_oid;
	m_parent = new WeakReference<> (parent);

	LayoutInflater inflater = (LayoutInflater) m_lettera.get().
	    getSystemService(Context.LAYOUT_INFLATER_SERVICE);

	m_view = inflater.inflate(R.layout.folders_drawer_move, null);

	/*
	** The cute popup.
	*/

	m_popup_window = new PopupWindow(m_lettera.get());
	m_popup_window.setHeight(WindowManager.LayoutParams.MATCH_PARENT);
	m_popup_window.setContentView(m_view);
	m_popup_window.setFocusable(true);
	m_popup_window.setOutsideTouchable(true);
	m_popup_window.setWidth((int) (0.50 * m_parent.get().getWidth()));

	/*
	** Initialize other widgets.
	*/

	initialize_widget_members();
	m_adapter = new MoveMessagesAdapter
	    (MoveMessages.this, email_account, folder_name);
	m_layout_manager = new MoveMessagesLinearLayoutManager(m_lettera.get());
	m_layout_manager.setOrientation(LinearLayoutManager.VERTICAL);
	m_recycler.setAdapter(m_adapter);
	m_recycler.setLayoutManager(m_layout_manager);
	m_recycler.setHasFixedSize(true);
    }

    private void initialize_widget_members()
    {
	m_recycler = (RecyclerView) m_view.findViewById(R.id.recycler);
    }

    public void dismiss()
    {
	if(m_lettera.get() != null)
	{
	    if(m_message_oid != -1)
		m_lettera.get().move_message
		    (m_adapter.selected_folder_name(), m_message_oid);
	    else
		m_lettera.get().move_selected_messages
		    (m_adapter.selected_folder_name());
	}

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
	if(view == null) // Should never, ever be true!
	{
	}
	else
	    m_popup_window.showAsDropDown
		(view, 0, 0, Gravity.START | Gravity.TOP);

	((TextView) m_view.findViewById(R.id.move_to_textview)).setTextColor
	    (Lettera.text_color());
	m_view.findViewById(R.id.divider).setBackgroundColor
	    (Lettera.divider_color());
	m_view.setBackgroundColor(Lettera.background_color());
    }
}
