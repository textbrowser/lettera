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

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class LettersAdapter extends RecyclerView.Adapter
{
    private abstract class ViewHolderTypeEnumerator
    {
	public final static int MESSAGE = 0;
	public final static int SEPARATOR = 1;
    }

    public class ViewHolderMessage extends RecyclerView.ViewHolder
    {
	private MessageItem m_message_item = null;

	public ViewHolderMessage(MessageItem message_item)
	{
	    super(message_item.view());
	    m_message_item = message_item;
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

    private String m_email_address = "";
    private String m_folder_name = "";
    private final static Database s_database = Database.instance();

    public LettersAdapter()
    {
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder
	(ViewGroup parent, int view_type)
    {
	return new ViewHolderMessage
	    (new MessageItem(parent.getContext(), parent));
    }

    @Override
    public int getItemCount()
    {
	return s_database.message_count(m_email_address, m_folder_name);
    }

    @Override
    public void onBindViewHolder(ViewHolder view_holder, int position)
    {
	if(view_holder == null)
	    return;
    }

    public void set_email_address(String email_address)
    {
	m_email_address = email_address;
    }

    public void set_folder_name(String folder_name)
    {
	m_folder_name = folder_name;
    }
}
