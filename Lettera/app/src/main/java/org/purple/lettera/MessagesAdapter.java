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
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import java.lang.ref.WeakReference;

public class MessagesAdapter extends RecyclerView.Adapter
{
    private String m_email_account = "";
    private String m_folder_name = "";
    private WeakReference<Lettera> m_lettera = null;
    private final static Database s_database = Database.instance();

    public class ViewHolderMessage extends RecyclerView.ViewHolder
    {
	private MessageItem m_message_item = null;

	public ViewHolderMessage(MessageItem message_item)
	{
	    super(message_item.view());
	    m_message_item = message_item;
	}

	public void set_data(MessageElement message_element,
			     String folder_name,
			     boolean last_position,
			     int position)
	{
	    if(m_message_item != null)
		m_message_item.set_data
		    (message_element, folder_name, last_position, position);
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

    public MessagesAdapter(Context context, Lettera lettera)
    {
	m_lettera = new WeakReference<> (lettera);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder
	(ViewGroup parent, int view_type)
    {
	return new ViewHolderMessage
	    (new MessageItem(parent.getContext(), m_lettera.get(), parent));
    }

    public String folder_name()
    {
	return m_folder_name;
    }

    @Override
    public int getItemCount()
    {
	return s_database.message_count_cursor(m_email_account, m_folder_name);
    }

    @Override
    public void onBindViewHolder(ViewHolder view_holder, int position)
    {
	if(view_holder == null)
	    return;

	ViewHolderMessage view_holder_message = (ViewHolderMessage)
	    view_holder;

	if(view_holder_message == null)
	    return;

	MessageElement message_element = s_database.message
	    (m_email_account, m_folder_name, position);

	view_holder_message.set_data
	    (message_element, m_folder_name, position == 0, position);
    }

    public void set_email_account(String email_account)
    {
	m_email_account = email_account;

	if(m_email_account.isEmpty())
	    m_email_account = "e-mail@e-mail.org";
    }

    public void set_folder_name(String folder_name)
    {
	m_folder_name = folder_name;

	if(m_folder_name.isEmpty())
	    m_folder_name = s_database.setting
		("selected_folder_name_" + m_email_account);

	if(m_folder_name.isEmpty())
	    m_folder_name = Lettera.NONE_FOLDER;
    }
}
