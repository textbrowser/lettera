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
import android.os.Build;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import java.lang.ref.WeakReference;

public class FoldersDrawer
{
    private class FoldersDrawerLinearLayoutManager extends LinearLayoutManager
    {
	FoldersDrawerLinearLayoutManager(Context context)
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

    private FoldersDrawerAdapter m_adapter = null;
    private FoldersDrawerLinearLayoutManager m_layout_manager = null;
    private ImageButton m_close_button = null;
    private PopupWindow m_popup_window = null;
    private RecyclerView m_recycler = null;
    private TextView m_email_account = null;
    private View m_view = null;
    private WeakReference<Context> m_context = null;
    private WeakReference<View> m_parent = null;
    private final static Database s_database = Database.instance();

    public FoldersDrawer(Context context, View parent)
    {
	m_context = new WeakReference<> (context);
	m_parent = new WeakReference<> (parent);

	LayoutInflater inflater = (LayoutInflater) m_context.get().
	    getSystemService(Context.LAYOUT_INFLATER_SERVICE);

	m_view = inflater.inflate(R.layout.folders_drawer, null);

	/*
	** The cute popup.
	*/

	m_popup_window = new PopupWindow(m_context.get());
	m_popup_window.setContentView(m_view);
	m_popup_window.setFocusable(true);
	m_popup_window.setHeight(WindowManager.LayoutParams.MATCH_PARENT);
	m_popup_window.setOutsideTouchable(true);
	m_popup_window.setWidth
	    ((int) (0.80 *
		    m_context.get().getResources().getDisplayMetrics().
		    widthPixels));

	/*
	** Initialize other widgets.
	*/

	initialize_widget_members();
	m_adapter = new FoldersDrawerAdapter(FoldersDrawer.this);
	m_layout_manager = new FoldersDrawerLinearLayoutManager
	    (m_context.get());
	m_layout_manager.setOrientation(LinearLayoutManager.VERTICAL);
	m_recycler.setAdapter(m_adapter);
	m_recycler.setLayoutManager(m_layout_manager);
	m_recycler.setHasFixedSize(true);
	prepare_listeners();
    }

    private void initialize_widget_members()
    {
	m_close_button = (ImageButton) m_view.findViewById(R.id.close_button);
	m_email_account = (TextView) m_view.findViewById(R.id.email_account);
	m_recycler = (RecyclerView) m_view.findViewById(R.id.recycler);
    }

    private void prepare_listeners()
    {
	if(!m_close_button.hasOnClickListeners())
	    m_close_button.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    dismiss();
		}
	    });
    }

    public String email_account()
    {
	return m_email_account.getText().toString();
    }

    public String selected_folder_name()
    {
	return m_adapter.selected_folder_name();
    }

    public void dismiss()
    {
	if(m_context.get() != null && m_context.get() instanceof Lettera)
	    ((Lettera) m_context.get()).prepare_folders_and_messages_widgets
		(selected_folder_name());

	s_database.save_setting
	    ("selected_folder_name_" + m_email_account.getText().toString(),
	     selected_folder_name(),
	     false);

	try
	{
	    m_popup_window.dismiss();
	}
	catch(Exception exception)
	{
	}
    }

    public void set_email_account(String email_account)
    {
	m_adapter.set_email_account(email_account);

	if(email_account.trim().isEmpty())
	    m_email_account.setText("e-mail@e-mail.org");
	else
	    m_email_account.setText(email_account);

	update();
    }

    public void set_selected_folder_name(String folder_name)
    {
	m_adapter.set_selected_folder_name(folder_name);
    }

    public void show()
    {
	((TextView) m_view.findViewById(R.id.folders_textview)).setTextColor
	    (Lettera.text_color());
	m_email_account.setTextColor(Lettera.text_color());

	if(m_context.get() != null)
	    m_popup_window.showAsDropDown
		(new View(m_context.get()), 0, 0, Gravity.START | Gravity.TOP);

	m_view.findViewById(R.id.top_divider).setBackgroundColor
	    (Lettera.divider_color());

	try
	{
	    View view = null;

	    if(m_popup_window.getBackground() == null)
		view = m_popup_window.getContentView();
	    else
		view = (View) m_popup_window.getContentView().getParent();

	    WindowManager window_manager = (WindowManager) m_context.get().
		getSystemService(Context.WINDOW_SERVICE);
	    WindowManager.LayoutParams layout_params =
		(WindowManager.LayoutParams) view.getLayoutParams();

	    layout_params.dimAmount = 0.5f;
	    layout_params.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
	    window_manager.updateViewLayout(view, layout_params);
	}
	catch(Exception exception)
	{
	}

	m_view.setBackgroundColor(Lettera.background_color());
    }

    public void update()
    {
	m_adapter.notifyDataSetChanged();
    }
}
