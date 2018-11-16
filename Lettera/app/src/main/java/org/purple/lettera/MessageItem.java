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
** SMOKE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
** IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
** OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
** IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
** INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
** NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
** DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
** THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
** (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
** SMOKE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.purple.lettera;

import android.content.Context;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.Date;

public class MessageItem extends View
{
    final private Database m_database = Database.instance();
    private CheckBox m_selected = null;
    private CompoundButton.OnCheckedChangeListener m_selected_listener = null;
    private Context m_context = null;
    private ImageView m_attachment = null;
    private LayoutInflater m_inflater = null;
    private String m_email_account = "";
    private String m_folder_name = "";
    private TextView m_date = null;
    private TextView m_from = null;
    private TextView m_subject = null;
    private View m_divider = null;
    private View m_view = null;
    private long m_uid = 0;

    private void initialize_widget_members()
    {
	m_attachment = (ImageView) m_view.findViewById(R.id.attachment);
	m_date = (TextView) m_view.findViewById(R.id.date);
	m_divider = m_view.findViewById(R.id.divider);
	m_from = (TextView) m_view.findViewById(R.id.from);
	m_selected = (CheckBox) m_view.findViewById(R.id.selected);
	m_subject = (TextView) m_view.findViewById(R.id.subject);
    }

    private void prepare_listeners()
    {
	if(m_selected_listener == null)
	    m_selected_listener = new CompoundButton.OnCheckedChangeListener()
	    {
		@Override
		public void onCheckedChanged
		    (CompoundButton button_view, boolean is_checked)
		{
		    m_database.set_message_selected
			(m_email_account,
			 m_folder_name,
			 is_checked,
			 m_uid);
		}
	    };
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
	super.onDraw(canvas);
    }

    public MessageItem(Context context, ViewGroup view_group)
    {
	super(context);
	m_context = context;
	m_inflater = (LayoutInflater) m_context.getSystemService
	    (Context.LAYOUT_INFLATER_SERVICE);
	m_view = m_inflater.inflate(R.layout.letter_line, view_group, false);
	initialize_widget_members();
	prepare_listeners();
    }

    public View view()
    {
	return m_view;
    }

    public void set_data(MessageElement message_element, boolean last_position)
    {
	if(message_element == null)
	{
	    m_view.setVisibility(View.GONE);
	    return;
	}

	m_attachment.setVisibility(View.GONE);

	try
	{
	    m_date.setText
		(Utilities.
		 formatted_email_date(new Date(message_element.
					       m_received_date_unix_epoch)));
	}
	catch(Exception exception)
	{
	    m_date.setText(message_element.m_received_date);
	}

	m_divider.setVisibility(last_position ? View.GONE : View.VISIBLE);
	m_email_account = message_element.m_email_account;
	m_folder_name = message_element.m_folder_name;
	m_from.setText(message_element.m_from_name);
	m_selected.setOnCheckedChangeListener(null);
	m_selected.setChecked(message_element.m_selected);
	m_selected.setOnCheckedChangeListener(m_selected_listener);
	m_subject.setText(message_element.m_subject);
	m_uid = message_element.m_uid;
	m_view.setVisibility(View.VISIBLE);
    }
}
