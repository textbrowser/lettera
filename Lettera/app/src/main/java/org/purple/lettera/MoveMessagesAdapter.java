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

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import java.util.HashSet;

public class MoveMessagesAdapter extends RecyclerView.Adapter
{
    private abstract class IconsEnumerator
    {
	public final static int DRAFTS = 0;
	public final static int IMPORTANT = 1;
	public final static int INBOX = 2;
	public final static int SENT = 3;
	public final static int SPAM = 4;
	public final static int STARRED = 5;
	public final static int TRASH = 6;
	public final static int XYZ = 7;
    }

    private abstract class ViewHolderTypeEnumerator
    {
	public final static int BUTTON1 = 0;
	public final static int BUTTON2 = 2;
	public final static int SEPARATOR = 1;
    }

    public class ViewHolderButton extends RecyclerView.ViewHolder
    {
	private RadioButton m_button = null;

	private void perform_click(String folder_name, View view, float density)
	{
	    if(view == null)
		return;

	    ((RadioButton) view).setCompoundDrawablesWithIntrinsicBounds
		(s_selected_icons[view.getId()], 0, 0, 0);
	    ((RadioButton) view).setTextColor(Color.parseColor("#5e35b1"));
	    view.setBackgroundResource(R.drawable.folder_selection);
	    view.setPaddingRelative
		((int) (10 * density), // Start
		 (int) (5 * density),  // Top
		 (int) (10 * density), // End
		 (int) (5 * density)); // Bottom
	}

	public RadioButton button()
	{
	    return m_button;
	}

	public ViewHolderButton(RadioButton button, View parent)
	{
	    super(button);
	    m_button = button;
	}

	public void set_data(FolderElement folder_element)
	{
	    if(folder_element == null || m_button == null)
		return;

	    String name = folder_element.m_name.toLowerCase();
	    StringBuilder string_builder = new StringBuilder();

	    if(name.contains("draft"))
		name = "Drafts";
	    else if(name.contains("important"))
		name = "Important";
	    else if(name.contains("inbox"))
		name = "Inbox";
	    else if(name.contains("sent"))
		name = "Sent";
	    else if(name.contains("spam"))
		name = "Spam";
	    else if(name.contains("star"))
		name = "Starred";
	    else if(name.contains("trash"))
		name = "Trash";
	    else
		name = folder_element.m_name;

	    string_builder.append(folder_element.m_name);
	    m_button.setAllCaps(false);
	    m_button.setBackgroundColor(Color.TRANSPARENT);
	    m_button.setButtonDrawable(null);
	    m_button.setCompoundDrawablePadding(15);

	    switch(name)
	    {
	    case "Drafts":
		m_button.setCompoundDrawablesWithIntrinsicBounds
		    (R.drawable.drafts_folder, 0, 0, 0);
		m_button.setId(IconsEnumerator.DRAFTS);
		break;
	    case "Important":
		m_button.setCompoundDrawablesWithIntrinsicBounds
		    (R.drawable.important_folder, 0, 0, 0);
		m_button.setId(IconsEnumerator.IMPORTANT);
		break;
	    case "Inbox":
		m_button.setCompoundDrawablesWithIntrinsicBounds
		    (R.drawable.inbox_folder, 0, 0, 0);
		m_button.setId(IconsEnumerator.INBOX);
		break;
	    case "Sent":
		m_button.setCompoundDrawablesWithIntrinsicBounds
		    (R.drawable.sent_folder, 0, 0, 0);
		m_button.setId(IconsEnumerator.SENT);
		break;
	    case "Spam":
		m_button.setCompoundDrawablesWithIntrinsicBounds
		    (R.drawable.spam_folder, 0, 0, 0);
		m_button.setId(IconsEnumerator.SPAM);
		break;
	    case "Starred":
		m_button.setCompoundDrawablesWithIntrinsicBounds
		    (R.drawable.starred_folder, 0, 0, 0);
		m_button.setId(IconsEnumerator.STARRED);
		break;
	    case "Trash":
		m_button.setCompoundDrawablesWithIntrinsicBounds
		    (R.drawable.trash_folder, 0, 0, 0);
		m_button.setId(IconsEnumerator.TRASH);
		break;
	    default:
		m_button.setCompoundDrawablesWithIntrinsicBounds
		    (R.drawable.folder_folder, 0, 0, 0);
		m_button.setId(IconsEnumerator.XYZ);
		break;
	    }

	    final String folder_name = folder_element.m_name;
	    final float density = m_button.getContext().getResources().
		getDisplayMetrics().density;

	    m_button.setEllipsize(TextUtils.TruncateAt.MIDDLE);
	    m_button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
	    m_button.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    clicked();
		    perform_click(folder_name, view, density);
		    dismiss();
		}
	    });
	    m_button.setSingleLine(true);

	    LinearLayout.LayoutParams layout_params =
		new LinearLayout.LayoutParams
		(LinearLayout.LayoutParams.MATCH_PARENT,
		 LinearLayout.LayoutParams.WRAP_CONTENT);

	    layout_params.setMargins
		((int) (5 * density),  // Left
                 (int) (5 * density),  // Top
                 (int) (5 * density),  // Right
                 (int) (5 * density)); // Bottom
	    m_button.setLayoutParams(layout_params);
	    m_button.setPaddingRelative
                ((int) (10 * density), // Start
                 (int) (5 * density),  // Top
                 (int) (10 * density), // End
                 (int) (5 * density)); // Bottom
	    m_button.setText(string_builder.toString());
	    m_button.setTextColor(Lettera.text_color());
	}
    }

    public class ViewHolderSeparator extends RecyclerView.ViewHolder
    {
	private View m_line = null;

	public ViewHolderSeparator(View line, View parent)
	{
	    super(line);
	    m_line = line;
	}
    }

    private Database m_database = null;
    private String m_email_address = "";
    private String m_folder_name = "";
    private final HashSet<RadioButton> m_visible_buttons = new HashSet<> ();
    private final static Database s_database = Database.instance();
    private final static int s_icons[] = new int[IconsEnumerator.XYZ + 1];
    private final static int s_selected_icons[] =
	new int[IconsEnumerator.XYZ + 1];

    private void clicked()
    {
	try
	{
	    for(RadioButton button : m_visible_buttons)
	    {
		if(button == null)
		    continue;

		button.setCompoundDrawablesWithIntrinsicBounds
		    (s_icons[button.getId()], 0, 0, 0);
		button.setBackgroundColor(Color.TRANSPARENT);
		button.setTextColor(Lettera.text_color());
	    }
	}
	catch(Exception exception)
	{
	}
    }

    private void dismiss()
    {
	/*
	** Display the selection and then close the dialog.
	*/

	new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
	{
	    @Override
	    public void run()
	    {
	    }
	}, 250);
    }

    public MoveMessagesAdapter(String email_address, String folder_name)
    {
	m_database = Database.instance();
	m_email_address = email_address;
	m_folder_name = folder_name;
	s_icons[IconsEnumerator.DRAFTS] = R.drawable.drafts_folder;
	s_icons[IconsEnumerator.IMPORTANT] = R.drawable.important_folder;
	s_icons[IconsEnumerator.INBOX] = R.drawable.inbox_folder;
	s_icons[IconsEnumerator.SENT] = R.drawable.sent_folder;
	s_icons[IconsEnumerator.SPAM] = R.drawable.spam_folder;
	s_icons[IconsEnumerator.STARRED] = R.drawable.starred_folder;
	s_icons[IconsEnumerator.TRASH] = R.drawable.trash_folder;
	s_icons[IconsEnumerator.XYZ] = R.drawable.folder_folder;
	s_selected_icons[IconsEnumerator.DRAFTS] = R.drawable.
	    drafts_folder_selected;
	s_selected_icons[IconsEnumerator.IMPORTANT] = R.drawable.
	    important_folder_selected;
	s_selected_icons[IconsEnumerator.INBOX] = R.drawable.
	    inbox_folder_selected;
	s_selected_icons[IconsEnumerator.SENT] = R.drawable.
	    sent_folder_selected;
	s_selected_icons[IconsEnumerator.SPAM] = R.drawable.
	    spam_folder_selected;
	s_selected_icons[IconsEnumerator.STARRED] = R.drawable.
	    starred_folder_selected;
	s_selected_icons[IconsEnumerator.TRASH] = R.drawable.
	    trash_folder_selected;
	s_selected_icons[IconsEnumerator.XYZ] = R.drawable.
	    folder_folder_selected;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder
	(ViewGroup parent, int view_type)
    {
	switch(view_type)
	{
	case ViewHolderTypeEnumerator.BUTTON1:
	case ViewHolderTypeEnumerator.BUTTON2:
	    RadioButton button = new RadioButton(parent.getContext());
	    RecyclerView.LayoutParams layout_params = new RecyclerView.
		LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
			     ViewGroup.LayoutParams.WRAP_CONTENT);

	    button.setLayoutParams(layout_params);
	    return new ViewHolderButton(button, parent);
	case ViewHolderTypeEnumerator.SEPARATOR:
	    View line = new View(parent.getContext());
	    float density = parent.getContext().getResources().
		getDisplayMetrics().density;

	    line.setLayoutParams
		(new LinearLayout.
		 LayoutParams(LinearLayout.
			      LayoutParams.MATCH_PARENT, (int) (1 * density)));
	    line.setBackgroundColor(Color.parseColor("#1f000000"));
	    return new ViewHolderSeparator(line, parent);
	default:
	    break;
	}

	return new ViewHolderButton(null, null);
    }

    @Override
    public int getItemCount()
    {
	return s_database.folder_count(m_email_address, m_folder_name);
    }

    @Override
    public int getItemViewType(int position)
    {
	FolderElement folder_element = s_database.folder
	    (m_email_address, m_folder_name, position);

	if(folder_element == null)
	    return ViewHolderTypeEnumerator.BUTTON1;
	else if(folder_element.m_is_regular_folder == 0)
	    return ViewHolderTypeEnumerator.BUTTON1;
	else if(folder_element.m_is_regular_folder == 2)
	    return ViewHolderTypeEnumerator.BUTTON2;
	else
	    return ViewHolderTypeEnumerator.SEPARATOR;
    }

    @Override
    public void onBindViewHolder(ViewHolder view_holder, int position)
    {
	if(view_holder == null)
	    return;

	switch(getItemViewType(position))
	{
	case ViewHolderTypeEnumerator.BUTTON1:
	case ViewHolderTypeEnumerator.BUTTON2:
	    ViewHolderButton view_holder_button = (ViewHolderButton)
		view_holder;

	    if(view_holder_button == null)
		return;

	    m_visible_buttons.add(view_holder_button.button());

	    FolderElement folder_element = s_database.folder
		(m_email_address, m_folder_name, position);

	    view_holder_button.set_data(folder_element);
	    break;
	default:
	    break;
	}
    }

    @Override
    public void onViewRecycled(ViewHolder view_holder)
    {
	if(view_holder instanceof ViewHolderButton)
	    m_visible_buttons.remove
		(((ViewHolderButton) view_holder).button());
    }
}
