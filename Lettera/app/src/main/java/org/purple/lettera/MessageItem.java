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
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.Date;

public class MessageItem extends View
{
    private Button m_open = null;
    private CompoundButton.OnCheckedChangeListener m_selected_listener = null;
    private ImageView m_attachment = null;
    private LayoutInflater m_inflater = null;
    private Lettera m_lettera = null;
    private String m_email_account = "";
    private String m_folder_name = "";
    private Switch m_selected = null;
    private TextView m_date = null;
    private TextView m_from = null;
    private TextView m_subject = null;
    private TextView m_summary = null;
    private View m_divider = null;
    private View m_view = null;
    private WeakReference<Context> m_context = null;
    private final static Database s_database = Database.instance();
    private int m_position = -1;
    private long m_oid = -1L;

    private void initialize_widget_members()
    {
	m_attachment = (ImageView) m_view.findViewById(R.id.attachment);
	m_date = (TextView) m_view.findViewById(R.id.date);
	m_divider = m_view.findViewById(R.id.divider);
	m_from = (TextView) m_view.findViewById(R.id.from);
	m_open = (Button) m_view.findViewById(R.id.open);
	m_selected = (Switch) m_view.findViewById(R.id.selected);
	m_subject = (TextView) m_view.findViewById(R.id.subject);
	m_summary = (TextView) m_view.findViewById(R.id.summary);
    }

    private void prepare_listeners()
    {
	if(!m_open.hasOnClickListeners())
	    m_open.setOnClickListener(new View.OnClickListener()
	    {
		@Override
		public void onClick(View view)
		{
		    if(m_lettera.isFinishing())
			return;

		    m_lettera.show_email_dialog(m_oid);
		}
	    });

	if(m_selected_listener == null)
	    m_selected_listener = new CompoundButton.OnCheckedChangeListener()
	    {
		@Override
		public void onCheckedChanged
		    (CompoundButton button_view, final boolean is_checked)
		{
		    s_database.set_message_selected(is_checked, m_oid);
		}
	    };
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
	super.onDraw(canvas);
    }

    public MessageItem(Context context, Lettera lettera, ViewGroup view_group)
    {
	super(context);
	m_context = new WeakReference<> (context);
	m_inflater = (LayoutInflater) m_context.get().getSystemService
	    (Context.LAYOUT_INFLATER_SERVICE);
	m_lettera = lettera;
	m_view = m_inflater.inflate(R.layout.letter_line, view_group, false);
	initialize_widget_members();
	prepare_listeners();
    }

    public View view()
    {
	return m_view;
    }

    public void set_data(MessageElement message_element,
			 String folder_name,
			 boolean last_position,
			 int position)
    {
	if(message_element == null)
	{
	    m_attachment.setVisibility(View.GONE);
	    m_date.setText("0000-00-00");
	    m_date.setTextColor(Lettera.text_color());
	    m_date.setTypeface(null, Typeface.BOLD);
	    m_from.setText("e-mail@e-mail.org");
	    m_from.setTextColor(Lettera.text_color());
	    m_from.setTypeface(null, Typeface.BOLD);
	    m_subject.setText("Invalid");
	    m_subject.setTextColor(Lettera.text_color());
	    m_subject.setTypeface(null, Typeface.BOLD);
	    m_summary.setText("Invalid");
	    m_summary.setTextColor(Lettera.text_color());
	    m_summary.setTypeface(null, Typeface.BOLD);
	    return;
	}

	Utilities.color_switch
	    (m_selected,
	     Lettera.background_color(),
	     Lettera.divider_color(),
	     Lettera.text_color());
	m_attachment.setVisibility(View.GONE);

	String string = Utilities.formatted_email_date_for_messages
	    (new Date(message_element.m_received_date_unix_epoch));

	if(string.isEmpty())
	    m_date.setText(message_element.m_received_date);
	else
	    m_date.setText(string);

	m_date.setTextColor(Lettera.text_color());
	m_date.setTypeface
	    (null,
	     message_element.m_has_been_read ? Typeface.NORMAL : Typeface.BOLD);
	m_divider.setBackgroundColor(Lettera.divider_color());
	m_divider.setVisibility(last_position ? View.GONE : View.VISIBLE);
	m_email_account = message_element.m_email_account;
	m_folder_name = folder_name;
	m_from.setText(message_element.m_from_name);
	m_from.setTextColor(Lettera.text_color());
	m_from.setTypeface
	    (null,
	     message_element.m_has_been_read ? Typeface.NORMAL : Typeface.BOLD);
	m_oid = message_element.m_oid;
	m_position = position;
	m_selected.setOnCheckedChangeListener(null);
	m_selected.setChecked
	    (s_database.message_selected(message_element.m_oid));
	m_selected.setOnCheckedChangeListener(m_selected_listener);
	m_subject.setText(message_element.m_subject.trim());
	m_subject.setTextColor(Lettera.text_color());
	m_subject.setTypeface
	    (null,
	     message_element.m_has_been_read ? Typeface.NORMAL : Typeface.BOLD);
	m_summary.setText
	    (message_element.m_content_downloaded ?
	     message_element.m_message_plain.trim() : "");
	m_summary.setTextColor(Lettera.text_color());
	m_summary.setVisibility
	    (m_summary.getText().length() == 0 ? View.GONE : View.VISIBLE);
	m_view.setBackgroundColor(Lettera.background_color());
    }
}
