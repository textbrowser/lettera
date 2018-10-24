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
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.TextView;
import java.util.ArrayList;

public class FoldersDrawer
{
    private Context m_context = null;
    private LinearLayout m_main_folders_layout = null;
    private LinearLayout m_other_folders_layout = null;
    private PopupWindow m_popup_window = null;
    private TextView m_email_address = null;
    private View m_parent = null;
    private View m_separator = null;
    private View m_view = null;

    public FoldersDrawer(Context context, View parent)
    {
	m_context = context;
	m_parent = parent;

	LayoutInflater inflater = (LayoutInflater) m_context.getSystemService
	    (Context.LAYOUT_INFLATER_SERVICE);

	m_view = inflater.inflate(R.layout.folders_drawer, null);

	/*
	** The cute popup.
	*/

	m_popup_window = new PopupWindow(m_context);
	m_popup_window.setAttachedInDecor(true);
	m_popup_window.setBackgroundDrawable(null);
	m_popup_window.setContentView(m_view);
	m_popup_window.setFocusable(true);
	m_popup_window.setHeight(WindowManager.LayoutParams.MATCH_PARENT);
	m_popup_window.setOutsideTouchable(true);
	m_popup_window.setWidth
	    ((int) (0.80 *
		    m_context.getResources().getDisplayMetrics().widthPixels));

	/*
	** Initialize other widgets.
	*/

	initialize_widget_members();
	m_separator.setVisibility(View.GONE);
    }

    private void initialize_widget_members()
    {
	m_email_address = (TextView) m_view.findViewById(R.id.email_address);
	m_main_folders_layout = (LinearLayout) m_view.findViewById
	    (R.id.main_folders_layout);
	m_other_folders_layout = (LinearLayout) m_view.findViewById
	    (R.id.other_folders_layout);
	m_separator = m_view.findViewById(R.id.separator);
    }

    public void set_email_address(String email_address)
    {
	if(email_address.trim().isEmpty())
	    m_email_address.setText("e-mail@e-mail.org");
	else
	    m_email_address.setText(email_address);
    }

    public void set_folders(ArrayList<FolderElement> array_list)
    {
	m_main_folders_layout.removeAllViews();
	m_other_folders_layout.removeAllViews();
	m_separator.setVisibility(View.GONE);

	if(array_list == null || array_list.isEmpty())
	    return;

	for(FolderElement folder_element : array_list)
	{
	    if(folder_element == null)
		continue;

	    RadioButton button = null;
	    String name = folder_element.m_name.toLowerCase().trim();
	    StringBuffer string_buffer = new StringBuffer();
	    boolean is_main_folder = false;

	    if(name.contains("draft"))
		name = "Drafts";
	    else if(name.contains("inbox"))
		name = "Inbox";
	    else if(name.contains("sent"))
		name = "Sent";
	    else if(name.contains("spam"))
		name = "Spam";
	    else if(name.contains("star"))
		name = "Starred";
	    else
		name = folder_element.m_name;

	    string_buffer.append(name);
	    string_buffer.append(" (");
	    string_buffer.append(folder_element.m_message_count);
	    string_buffer.append(")");
	    button = new RadioButton(m_context);
	    button.setAllCaps(false);
	    button.setBackgroundColor(Color.TRANSPARENT);
	    button.setButtonDrawable(Color.TRANSPARENT);
	    button.setCompoundDrawablePadding(100);

	    switch(name)
	    {
	    case "Drafts":
		button.setCompoundDrawablesWithIntrinsicBounds
		    (R.drawable.drafts_folder, 0, 0, 0);
		is_main_folder = true;
		break;
	    case "Inbox":
		button.setBackgroundResource(R.drawable.folder_selection);
		button.setCompoundDrawablesWithIntrinsicBounds
		    (R.drawable.inbox_folder, 0, 0, 0);
		button.setId(0);
		button.setTextColor(Color.rgb(42, 11, 60));
		is_main_folder = true;
		break;
	    case "Sent":
		button.setCompoundDrawablesWithIntrinsicBounds
		    (R.drawable.sent_folder, 0, 0, 0);
		is_main_folder = true;
		break;
	    case "Spam":
		button.setCompoundDrawablesWithIntrinsicBounds
		    (R.drawable.spam_folder, 0, 0, 0);
		is_main_folder = true;
		break;
	    case "Starred":
		button.setCompoundDrawablesWithIntrinsicBounds
		    (R.drawable.starred_folder, 0, 0, 0);
		is_main_folder = true;
		break;
	    case "Trash":
		button.setCompoundDrawablesWithIntrinsicBounds
		    (R.drawable.trash_folder, 0, 0, 0);
		is_main_folder = true;
		break;
	    default:
		button.setCompoundDrawablesWithIntrinsicBounds
		    (R.drawable.folder_folder, 0, 0, 0);
		break;
	    }

	    button.setEllipsize(TextUtils.TruncateAt.END);
	    button.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
	    button.setMaxLines(1);
	    button.setOnClickListener(new View.OnClickListener()
	    {
		public void onClick(View view)
		{
		    if(view.getId() == 0)
			return;

		    ((RadioButton) view).setTextColor(Color.rgb(42, 11, 60));
		    view.setBackgroundResource(R.drawable.folder_selection);
		    view.setId(0);
		    view.setPadding(15, 15, 15, 15);

		    for(int i = 0;
			i < m_main_folders_layout.getChildCount();
			i++)
			if(m_main_folders_layout.getChildAt(i) != view)
			{
			    m_main_folders_layout.getChildAt(i).
				setBackgroundColor(Color.TRANSPARENT);
			    m_main_folders_layout.getChildAt(i).setId(-1);
			}

		    for(int i = 0;
			i < m_other_folders_layout.getChildCount();
			i++)
			if(m_other_folders_layout.getChildAt(i) != view)
			{
			    m_other_folders_layout.getChildAt(i).
				setBackgroundColor(Color.TRANSPARENT);
			    m_other_folders_layout.getChildAt(i).setId(-1);
			}
		}
	    });
	    button.setPadding(15, 15, 15, 15);
	    button.setText(string_buffer.toString());

	    if(is_main_folder)
		m_main_folders_layout.addView(button);
	    else
		m_other_folders_layout.addView(button);
	}

	if(m_main_folders_layout.getChildCount() > 0 &&
	   m_other_folders_layout.getChildCount() > 0)
	    m_separator.setVisibility(View.VISIBLE);
    }

    public void show()
    {
	m_popup_window.showAsDropDown
	    (m_parent, 0, 0, Gravity.LEFT | Gravity.TOP);

	try
	{
	    View view;

	    if(m_popup_window.getBackground() == null)
	    {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
		    view = (View) m_popup_window.getContentView().getParent();
		else
		    view = (View) m_popup_window.getContentView();
	    }
	    else
	    {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
		    view = (View) m_popup_window.getContentView().getParent().
			getParent();
		else
		    view = (View) m_popup_window.getContentView().getParent();
	    }

	    Context context = m_popup_window.getContentView().getContext();
	    WindowManager window_manager = (WindowManager) context.
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
    }
}
