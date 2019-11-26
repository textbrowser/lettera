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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Base64;
import android.util.Log;
import com.sun.mail.imap.IMAPFolder;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.mail.Address;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.UIDFolder;
import javax.mail.internet.InternetAddress;

public class Database extends SQLiteOpenHelper
{
    private Cursor m_read_message_cursor = null;
    private SQLiteDatabase m_db = null;
    private String m_read_message_cursor_email_account = "";
    private String m_read_message_cursor_folder_name = "";
    private final Object m_read_message_cursor_mutex = new Object();
    private final static String DATABASE_NAME = "lettera.db";
    private final static String UNKNOWN_EMAIL = "unknown@unknown.org";
    private final static int DATABASE_VERSION = 1;
    private final static int MESSAGE_DOWNLOAD_TIME = 10000; // 10 Seconds
    private static Database s_instance = null;

    private Database(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

	try
	{
	    m_db = getWritableDatabase();
	}
	catch(Exception exception)
	{
	    m_db = null;
	}
    }

    private void write_recipient(InternetAddress internet_address,
				 String email_account,
				 String folder_name,
				 String recipient_type,
				 long message_uid)
    {
	if(internet_address == null || m_db == null)
	    return;

	try
	{
	    String recipient_email_account = UNKNOWN_EMAIL;
	    String recipient_name = "";

	    if(!(internet_address.getAddress() == null ||
		 internet_address.getAddress().isEmpty()))
		recipient_email_account = internet_address.getAddress();

	    if(!(internet_address.getPersonal() == null ||
		 internet_address.getPersonal().isEmpty()))
		recipient_name = internet_address.getPersonal();
	    else
		recipient_name = recipient_email_account;

	    m_db.execSQL
		("REPLACE INTO messages_recipients (" +
		 "email_account, " +
		 "folder_name, " +
		 "message_uid, " +
		 "recipient_email_account, " +
		 "recipient_name, " +
		 "recipient_type) VALUES " +
		 "(?, ?, ?, ?, ?, ?)",
		 new String[] {email_account,
			       folder_name,
			       String.valueOf(message_uid),
			       recipient_email_account,
			       recipient_name,
			       recipient_type});
	}
	catch(Exception exception)
	{
	}
    }

    public ArrayList<String> email_account_names()
    {
	if(m_db == null)
	    return null;

	Cursor cursor = null;

	try
	{
	    cursor = m_db.rawQuery
		("SELECT email_account FROM email_accounts ORDER BY 1", null);

	    if(cursor != null && cursor.moveToFirst())
	    {
		ArrayList<String> array_list = new ArrayList<> ();

		while(!cursor.isAfterLast())
		{
		    array_list.add(cursor.getString(0));
		    cursor.moveToNext();
		}

		return array_list;
	    }
	}
	catch(Exception exception)
	{
	}
	finally
	{
	    if(cursor != null)
		cursor.close();
	}

	return null;
    }

    public EmailElement email_element(String email_account)
    {
	if(m_db == null)
	    return null;

	Cursor cursor = null;

	try
	{
	    cursor = m_db.rawQuery
		("SELECT delete_on_server, " + // 0
		 "email_account, " +           // 1
		 "in_address, " +              // 2
		 "in_password, " +             // 3
		 "in_port, " +                 // 4
		 "out_address, " +             // 5
		 "out_email, " +               // 6
		 "out_password, " +            // 7
		 "out_port, " +                // 8
		 "proxy_address, " +           // 9
		 "proxy_password, " +          // 10
		 "proxy_port, " +              // 11
		 "proxy_type, " +              // 12
		 "proxy_user, " +              // 13
		 "OID " +                      // 14
		 "FROM email_accounts WHERE email_account = ?",
		 new String[] {email_account});

	    if(cursor != null && cursor.moveToFirst())
	    {
		EmailElement email_element = new EmailElement();
		int count = cursor.getColumnCount();

		for(int i = 0; i < count; i++)
		    switch(i)
		    {
		    case 0:
			email_element.m_delete_on_server =
			    cursor.getInt(i) != 0;
			break;
		    case 1:
			email_element.m_inbound_email = cursor.getString(i);
			break;
		    case 2:
			email_element.m_inbound_address = cursor.getString(i);
			break;
		    case 3:
			email_element.m_inbound_password = cursor.getString(i);
			break;
		    case 4:
			email_element.m_inbound_port = cursor.getInt(i);
			break;
		    case 5:
			email_element.m_outbound_address = cursor.getString(i);
			break;
		    case 6:
			email_element.m_outbound_email = cursor.getString(i);
			break;
		    case 7:
			email_element.m_outbound_password = cursor.getString(i);
			break;
		    case 8:
			email_element.m_outbound_port = cursor.getInt(i);
			break;
		    case 9:
			email_element.m_proxy_address = cursor.getString(i);
			break;
		    case 10:
			email_element.m_proxy_password = cursor.getString(i);
			break;
		    case 11:
			email_element.m_proxy_port = cursor.getInt(i);
			break;
		    case 12:
			email_element.m_proxy_type = cursor.getString(i);
			break;
		    case 13:
			email_element.m_proxy_user = cursor.getString(i);
			break;
		    case 14:
			email_element.m_oid = cursor.getLong(i);
			break;
		    default:
			break;
		    }

		return email_element;
	    }
	}
	catch(Exception exception)
	{
	}
	finally
	{
	    if(cursor != null)
		cursor.close();
	}

	return null;
    }

    public FolderElement folder(String email_account,
				String folder_name,
				int position)
    {
	if(m_db == null)
	    return null;

	Cursor cursor = null;

	try
	{
	    cursor = m_db.rawQuery
		("SELECT email_account, " +
		 "full_name, " +
		 "is_regular_folder, " +
		 "name, " +
		 "OID " +
		 "FROM folders WHERE email_account = ? AND " +
		 "name NOT IN (?) " +
		 "ORDER BY is_regular_folder, LOWER(name) " +
		 "LIMIT 1 OFFSET CAST(? AS INTEGER)",
		 new String[] {email_account,
			       folder_name,
			       String.valueOf(position)});

	    if(cursor != null && cursor.moveToFirst())
	    {
		FolderElement folder_element = new FolderElement();

		folder_element.m_email_account = cursor.getString(0);
		folder_element.m_full_name = cursor.getString(1);
		folder_element.m_is_regular_folder = cursor.getInt(2);
		folder_element.m_name = cursor.getString(3);
		folder_element.m_oid = cursor.getLong(4);
		return folder_element;
	    }
	}
	catch(Exception exception)
	{
	}
	finally
	{
	    if(cursor != null)
		cursor.close();
	}

	return null;
    }

    public MessageElement message(String email_account,
				  String folder_name,
				  int position)
    {
	if(m_db == null)
	    return null;

	synchronized(m_read_message_cursor_mutex)
	{
	    try
	    {
		if(m_read_message_cursor != null)
		    if(!email_account.
		       equals(m_read_message_cursor_email_account) ||
		       !folder_name.
		       equals(m_read_message_cursor_folder_name))
		    {
			m_read_message_cursor.close();
			m_read_message_cursor = null;
		    }

		if(m_read_message_cursor == null)
		{
		    String string = "SELECT " +
			"content_downloaded, " +
			"content_type, " +
			"email_account, " +
			"folder_name, " +
			"from_email_account, " +
			"from_name, " +
			"has_been_read, " +
			"message_html, " +
			"message_plain, " +
			"message_raw, " +
			"oid, " +
			"received_date, " +
			"received_date_unix_epoch, " +
			"sent_date, " +
			"subject, " +
			"to_folder_name, " +
			"uid " +
			"FROM messages " +
			"INDEXED BY messages_received_date_unix_epoch " +
			"WHERE email_account = ? AND " +
			"LOWER(to_folder_name) = LOWER(?) " +
			"ORDER BY received_date_unix_epoch";
		    m_read_message_cursor = m_db.rawQuery
			(string, new String[] {email_account, folder_name});
		    m_read_message_cursor_email_account = email_account;
		    m_read_message_cursor_folder_name = folder_name;
		}

		if(m_read_message_cursor != null &&
		   m_read_message_cursor.moveToPosition(position))
		{
		    MessageElement message_element = new MessageElement();
		    int i = 0;

		    message_element.m_content_downloaded =
			m_read_message_cursor.getInt(i++) == 1;
		    message_element.m_content_type =
			m_read_message_cursor.getString(i++);
		    message_element.m_email_account = m_read_message_cursor.
			getString(i++);
		    message_element.m_folder_name = m_read_message_cursor.
			getString(i++);
		    message_element.m_from_email_account =
			m_read_message_cursor.getString(i++);
		    message_element.m_from_name = m_read_message_cursor.
			getString(i++);
		    message_element.m_has_been_read = m_read_message_cursor.
			getLong(i++) == 1;
		    message_element.m_message_html = m_read_message_cursor.
			getString(i++).trim();
		    message_element.m_message_plain = m_read_message_cursor.
			getString(i++).trim();
		    message_element.m_message_raw = m_read_message_cursor.
			getString(i++).trim();
		    message_element.m_oid = m_read_message_cursor.
			getLong(i++);
		    message_element.m_received_date = m_read_message_cursor.
			getString(i++);
		    message_element.m_received_date_unix_epoch =
			m_read_message_cursor.getLong(i++);
		    message_element.m_sent_date = m_read_message_cursor.
			getString(i++);
		    message_element.m_subject = m_read_message_cursor.
			getString(i++).trim();
		    message_element.m_to_folder_name = m_read_message_cursor.
			getString(i++);
		    message_element.m_uid = m_read_message_cursor.getLong(i++);
		    return message_element;
		}
	    }
	    catch(Exception exception)
	    {
		if(m_read_message_cursor != null)
		    m_read_message_cursor.close();

		m_read_message_cursor = null;
	    }
	}

	return null;
    }

    public SettingsElement settings_element(String key)
    {
	if(m_db == null)
	    return null;

	Cursor cursor = null;

	try
	{
	    cursor = m_db.rawQuery
		("SELECT key_field, value FROM settings WHERE key_field = ?",
		 new String[] {key});

	    if(cursor != null && cursor.moveToFirst())
	    {
		SettingsElement settings_element = new SettingsElement();
		int count = cursor.getColumnCount();

		for(int i = 0; i < count; i++)
		    switch(i)
		    {
		    case 0:
			settings_element.m_key = key.trim();
			break;
		    case 1:
			settings_element.m_value = cursor.getString(i).trim();
			break;
		    default:
			break;
		    }

		return settings_element;
	    }
	}
	catch(Exception exception)
	{
	}
	finally
	{
	    if(cursor != null)
		cursor.close();
	}

	return null;
    }

    public String folder_full_name(String email_account, String folder_name)
    {
	if(m_db == null)
	    return "";

	Cursor cursor = null;

	try
	{
	    cursor = m_db.rawQuery
		("SELECT full_name FROM folders WHERE " +
		 "email_account = ? AND LOWER(name) = LOWER(?)",
		 new String[] {email_account, folder_name});

	    if(cursor != null && cursor.moveToFirst())
		return cursor.getString(0);
	}
	catch(Exception exception)
	{
	}
	finally
	{
	    if(cursor != null)
		cursor.close();
	}

	return "";
    }

    public String save_email(ContentValues content_values)
    {
	if(content_values == null || content_values.size() == 0)
	    return "empty container on save_email()";
	else if(m_db == null)
	    return "m_db is null on save_email()";

	m_db.beginTransactionNonExclusive();

	try
	{
	    m_db.insert("email_accounts", null, content_values);
	}
	catch(Exception exception)
	{
	}

	try
	{
	    m_db.update("email_accounts",
			content_values,
			"email_account = ?",
			new String[] {content_values.
				      getAsString("email_account")});
	    m_db.setTransactionSuccessful();
	}
	catch(Exception exception)
	{
	    return exception.getMessage();
	}
	finally
	{
	    m_db.endTransaction();
	}

	return "";
    }

    public String save_setting(ContentValues content_values, boolean join)
    {
	if(content_values == null || content_values.size() == 0)
	    return "empty container on save_setting()";
	else if(m_db == null)
	    return "m_db is null on save_setting()";

	Thread thread = null;
	final String key = content_values.getAsString("key");
	final String value = content_values.getAsString("value");

	thread = new Thread(new Runnable()
	{
	    @Override
	    public void run()
	    {
		m_db.beginTransactionNonExclusive();

		try
		{
		    SQLiteStatement sqlite_statement =
			m_db.compileStatement
			("REPLACE INTO settings (key_field, value) " +
			 "VALUES (?, ?)");

		    sqlite_statement.bindString(1, key);
		    sqlite_statement.bindString(2, value);
		    sqlite_statement.execute();
		    m_db.setTransactionSuccessful();
		}
		catch(Exception exception)
		{
		}
		finally
		{
		    m_db.endTransaction();
		}
	    }
	});

	thread.start();

	if(join)
	    try
	    {
		thread.join();
	    }
	    catch(Exception exception)
	    {
	    }

	return "";
    }

    public String save_setting(String key, String value, boolean join)
    {
	ContentValues content_values = new ContentValues();

	content_values.put("key", key);
	content_values.put("value", value);
	return save_setting(content_values, join);
    }

    public String setting(String key)
    {
	SettingsElement settings_element = settings_element(key);

	if(settings_element != null)
	    return settings_element.m_value;
	else
	    return "";
    }

    public boolean delete_email_account(String email_account)
    {
	if(m_db == null)
	    return false;

	boolean ok = false;

	m_db.beginTransactionNonExclusive();

	try
	{
	    ok = m_db.delete("email_accounts",
			     "email_account = ?",
			     new String[] {email_account}) > 0;

	    if(ok)
	    {
		m_db.delete
		    ("folders", "email_account = ?",
		     new String[] {email_account});
		m_db.delete
		    ("messages", "email_account = ?",
		     new String[] {email_account});
		m_db.delete
		    ("messages_attachments", "email_account = ?",
		     new String[] {email_account});
		m_db.delete
		    ("messages_recipients", "email_account = ?",
		     new String[] {email_account});
		m_db.delete
		    ("settings", "key_field = ?",
		     new String[] {"selected_folder_name_" + email_account});
		m_db.delete
		    ("settings",
		     "key_field = ? AND value = ?",
		     new String[] {"primary_email_account", email_account});
	    }

	    m_db.setTransactionSuccessful();
	}
	catch(Exception exception)
	{
	    ok = false;
	}
	finally
	{
	    m_db.endTransaction();
	}

	synchronized(m_read_message_cursor_mutex)
	{
	    if(m_read_message_cursor_email_account.equals(email_account))
		if(m_read_message_cursor != null)
		{
		    m_read_message_cursor.close();
		    m_read_message_cursor = null;
		}
	}

	return ok;
    }

    public boolean delete_folders(String email_account)
    {
	if(m_db == null)
	    return false;

	boolean ok = false;

	m_db.beginTransactionNonExclusive();

	try
	{
	    ok = m_db.delete
		("folders", "email_account = ?",
		 new String[] {email_account}) > 0;
	    m_db.setTransactionSuccessful();
	}
	catch(Exception exception)
	{
	    ok = false;
	}
	finally
	{
	    m_db.endTransaction();
	}

	return ok;
    }

    public boolean delete_messages(String email_account)
    {
	if(m_db == null)
	    return false;

	m_db.beginTransactionNonExclusive();

	try
	{
	    m_db.delete
		("messages", "email_account = ?",
		 new String[] {email_account});
	    m_db.delete
		("messages_attachments", "email_account = ?",
		 new String[] {email_account});
	    m_db.delete
		("messages_recipients", "email_account = ?",
		 new String[] {email_account});
	    m_db.setTransactionSuccessful();
	}
	catch(Exception exception)
	{
	    return false;
	}
	finally
	{
	    m_db.endTransaction();
	}

	synchronized(m_read_message_cursor_mutex)
	{
	    if(m_read_message_cursor_email_account.equals(email_account))
		if(m_read_message_cursor != null)
		{
		    m_read_message_cursor.close();
		    m_read_message_cursor = null;
		}
	}

	return true;
    }

    public boolean message_selected(String email_account,
				    String folder_name,
				    long uid)
    {
	if(m_db == null)
	    return false;

	Cursor cursor = null;

	try
	{
	    cursor = m_db.rawQuery
		("SELECT selected FROM messages " +
		 "WHERE email_account = ? AND " +
		 "LOWER(to_folder_name) = LOWER(?) AND " +
		 "uid = ?",
		 new String[] {email_account,
			       folder_name,
			       String.valueOf(uid)});

	    if(cursor != null && cursor.moveToFirst())
		return cursor.getInt(0) == 1;
	}
	catch(Exception exception)
	{
	}
	finally
	{
	    if(cursor != null)
		cursor.close();
	}

	return false;
    }

    public boolean save_pgp_key_pair(KeyPair key_pair,
				     String function)
    {
	if(key_pair == null || m_db == null)
	    return false;

	m_db.beginTransactionNonExclusive();

	try
	{
	    String strings[] = new String[4];

	    strings[0] = Base64.encodeToString
		(key_pair.getPrivate().getEncoded(), Base64.NO_WRAP);
	    strings[1] = Cryptography.sha_1_fingerprint(key_pair.getPrivate());
	    strings[2] = Base64.encodeToString
		(key_pair.getPublic().getEncoded(), Base64.NO_WRAP);
	    strings[3] = Cryptography.sha_1_fingerprint(key_pair.getPublic());
	    m_db.execSQL
		("REPLACE INTO open_pgp (" +
		 "function, " +
		 "private_key, " +
		 "private_key_digest, " +
		 "public_key, " +
		 "public_key_digest) VALUES " +
		 "(?, ?, ?, ?, ?)",
		 new String[] {function,
			       strings[0],
			       strings[1],
			       strings[2],
			       strings[3]});
	    m_db.setTransactionSuccessful();
	}
	catch(Exception exception)
	{
	    return false;
	}
	finally
	{
	    m_db.endTransaction();
	}

	return true;
    }

    public byte[][] read_pgp_pair(String function)
    {
	if(m_db == null)
	    return null;

	Cursor cursor = null;

	try
	{
	    cursor = m_db.rawQuery
		("SELECT private_key, public_key FROM open_pgp WHERE " +
		 "function = ?", new String[] {function});

	    if(cursor != null && cursor.moveToFirst())
	    {
		byte bytes[][] = new byte[2][];

		bytes[0] = Base64.decode(cursor.getString(0), Base64.NO_WRAP);
		bytes[1] = Base64.decode(cursor.getString(1), Base64.NO_WRAP);
		return bytes;
	    }
	}
	catch(Exception exception)
	{
	}
	finally
	{
	    if(cursor != null)
		cursor.close();
	}

	return null;
    }

    public int folder_count(String email_account, String folder_name)
    {
	if(m_db == null)
	    return 0;

	Cursor cursor = null;

	try
	{
	    cursor = m_db.rawQuery
		("SELECT COUNT(*) FROM folders WHERE email_account = ? AND " +
		 "name NOT IN (?)",
		 new String[] {email_account, folder_name});

	    if(cursor != null && cursor.moveToFirst())
		return cursor.getInt(0);
	}
	catch(Exception exception)
	{
	}
	finally
	{
	    if(cursor != null)
		cursor.close();
	}

	return 0;
    }

    public int message_count(String email_account, String folder_name)
    {
	if(m_db == null)
	    return 0;

	Cursor cursor = null;

	try
	{
	    cursor = m_db.rawQuery
		("SELECT COUNT(*) FROM messages WHERE " +
		 "email_account = ? AND " +
		 "LOWER(to_folder_name) = LOWER(?)",
		 new String[] {email_account, folder_name});

	    if(cursor != null && cursor.moveToFirst())
		return cursor.getInt(0);
	}
	catch(Exception exception)
	{
	}
	finally
	{
	    if(cursor != null)
		cursor.close();
	}

	return 0;
    }

    public int message_count_cursor(String email_account, String folder_name)
    {
	synchronized(m_read_message_cursor_mutex)
	{
	    if(m_read_message_cursor != null &&
	       m_read_message_cursor_email_account.equals(email_account) &&
	       m_read_message_cursor_folder_name.equals(folder_name))
		return m_read_message_cursor.getCount();
	    else
		return message_count(email_account, folder_name);
	}
    }

    public int messages_selected(String email_account, String folder_name)
    {
	if(m_db == null)
	    return 0;

	Cursor cursor = null;

	try
	{
	    cursor = m_db.rawQuery
		("SELECT COUNT(*) FROM messages WHERE " +
		 "email_account = ? AND " +
		 "LOWER(to_folder_name) = LOWER(?) AND " +
		 "selected = 1",
		 new String[] {email_account, folder_name});

	    if(cursor != null && cursor.moveToFirst())
		return cursor.getInt(0);
	}
	catch(Exception exception)
	{
	}
	finally
	{
	    if(cursor != null)
		cursor.close();
	}

	return 0;
    }

    public static synchronized Database instance()
    {
	return s_instance; // Should never be null.
    }

    public static synchronized Database instance(Context context)
    {
	if(s_instance == null)
	    s_instance = new Database(context.getApplicationContext());

	return s_instance;
    }

    public void clear()
    {
	if(m_db == null)
	    return;

	m_db.beginTransactionNonExclusive();

	try
	{
	    String tables[] = new String[]
		{"contacts",
		 "email_accounts",
		 "folders",
		 "messages",
		 "messages_attachments",
		 "messages_recipients",
		 "open_pgp",
		 "settings"};

	    for(String table : tables)
		try
		{
		    m_db.delete(table, null, null);
		}
		catch (Exception exception)
		{
		}

	    m_db.setTransactionSuccessful();
	}
	catch(Exception exception)
	{
	}
	finally
	{
	    m_db.endTransaction();
	}

	synchronized(m_read_message_cursor_mutex)
	{
	    if(m_read_message_cursor != null)
		m_read_message_cursor.close();

	    m_read_message_cursor = null;
	    m_read_message_cursor_email_account = "";
	    m_read_message_cursor_folder_name = "";
	}
    }

    public void delete(String table)
    {
	if(m_db == null)
	    return;

	m_db.beginTransactionNonExclusive();

	try
	{
	    m_db.delete(table, null, null);
	    m_db.setTransactionSuccessful();
	}
	catch(Exception exception)
	{
	}
	finally
	{
	    m_db.endTransaction();
	}

	if(table.equals("messages"))
	    synchronized(m_read_message_cursor_mutex)
	    {
		if(m_read_message_cursor != null)
		    m_read_message_cursor.close();

		m_read_message_cursor = null;
	    }
    }

    public void delete_message(final Lettera lettera,
			       final MessagesAdapter messages_adapter,
			       String email_account,
			       final String folder_name,
			       long oid)
    {
	if(m_db == null)
	    return;

	m_db.beginTransactionNonExclusive();

	try
	{
	    SQLiteStatement sqlite_statement = null;

	    sqlite_statement = m_db.compileStatement
		("UPDATE messages SET " +
		 "selected = 0, to_folder_name = 'Trash' " +
		 "WHERE OID = ? AND " +
		 "email_account = ? AND " +
		 "LOWER(to_folder_name) = LOWER(?)");
	    sqlite_statement.bindLong(1, oid);
	    sqlite_statement.bindString(2, email_account);
	    sqlite_statement.bindString(3, folder_name);
	    sqlite_statement.execute();
	    m_db.setTransactionSuccessful();
	}
	catch(Exception exception)
	{
	}
	finally
	{
	    m_db.endTransaction();
	}

	synchronized(m_read_message_cursor_mutex)
	{
	    if(m_read_message_cursor != null &&
	       m_read_message_cursor_email_account.equals(email_account) &&
	       m_read_message_cursor_folder_name.equals(folder_name))
	    {
		m_read_message_cursor.close();
		m_read_message_cursor = null;
	    }
	}

	if(messages_adapter != null)
	    lettera.runOnUiThread(new Runnable()
	    {
		@Override
		public void run()
		{
		    lettera.prepare_current_folder_text(folder_name);
		    lettera.update_folders_drawer();
		    messages_adapter.notifyDataSetChanged();
		}
	    });
    }

    public void delete_selected_messages(final Lettera lettera,
					 final MessagesAdapter messages_adapter,
					 final String email_account,
					 final String folder_name)
    {
	if(m_db == null)
	    return;

	Thread thread = new Thread(new Runnable()
	{
	    @Override
	    public void run()
	    {
		m_db.beginTransactionNonExclusive();

		try
		{
		    SQLiteStatement sqlite_statement = null;

		    sqlite_statement = m_db.compileStatement
			("UPDATE messages SET " +
			 "selected = 0, to_folder_name = 'Trash' " +
			 "WHERE email_account = ? AND " +
			 "LOWER(to_folder_name) = LOWER(?) AND " +
			 "selected = 1");
		    sqlite_statement.bindString(1, email_account);
		    sqlite_statement.bindString(2, folder_name);
		    sqlite_statement.execute();
		    m_db.setTransactionSuccessful();
		}
		catch(Exception exception)
		{
		}
		finally
		{
		    m_db.endTransaction();
		}

		synchronized(m_read_message_cursor_mutex)
		{
		    if(m_read_message_cursor != null &&
		       m_read_message_cursor_email_account.
		       equals(email_account) &&
		       m_read_message_cursor_folder_name.equals(folder_name))
		    {
			m_read_message_cursor.close();
			m_read_message_cursor = null;
		    }
		}

		if(messages_adapter != null)
		    lettera.runOnUiThread(new Runnable()
		    {
			@Override
			public void run()
			{
			    lettera.prepare_current_folder_text(folder_name);
			    lettera.update_folders_drawer();
			    messages_adapter.notifyDataSetChanged();
			}
		    });
	    }
	});

	thread.start();
    }

    public void move_selected_messages(final Lettera lettera,
				       final MessagesAdapter messages_adapter,
				       final String email_account,
				       final String from_folder_name,
				       final String to_folder_name)
    {
	if(m_db == null)
	    return;

	Thread thread = new Thread(new Runnable()
	{
	    @Override
	    public void run()
	    {
		m_db.beginTransactionNonExclusive();

		try
		{
		    SQLiteStatement sqlite_statement = null;

		    sqlite_statement = m_db.compileStatement
			("UPDATE messages SET " +
			 "selected = 0, to_folder_name = ? " +
			 "WHERE email_account = ? AND " +
			 "LOWER(to_folder_name) = LOWER(?) AND " +
			 "selected = 1");
		    sqlite_statement.bindString(1, to_folder_name);
		    sqlite_statement.bindString(2, email_account);
		    sqlite_statement.bindString(3, from_folder_name);
		    sqlite_statement.execute();
		    m_db.setTransactionSuccessful();
		}
		catch(Exception exception)
		{
		}
		finally
		{
		    m_db.endTransaction();
		}

		synchronized(m_read_message_cursor_mutex)
		{
		    if(m_read_message_cursor != null &&
		       m_read_message_cursor_email_account.
		       equals(email_account) &&
		       m_read_message_cursor_folder_name.
		       equals(from_folder_name))
		    {
			m_read_message_cursor.close();
			m_read_message_cursor = null;
		    }
		}

		if(messages_adapter != null)
		    lettera.runOnUiThread(new Runnable()
		    {
			@Override
			public void run()
			{
			    lettera.prepare_current_folder_text
				(from_folder_name);
			    lettera.update_folders_drawer();
			    messages_adapter.notifyDataSetChanged();
			}
		    });
	    }
	});

	thread.start();
    }

    @Override
    public void onConfigure(SQLiteDatabase db)
    {
	try
	{
	    db.enableWriteAheadLogging();
	}
	catch(Exception exception)
	{
	}

	try
	{
	    db.execSQL("PRAGMA secure_delete = True");
	}
	catch(Exception exception)
	{
	}

	try
	{
	    db.setForeignKeyConstraintsEnabled(true);
        }
	catch(Exception exception)
	{
	}
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
	String str = "";

	str = "CREATE TABLE IF NOT EXISTS contacts (" +
	    "email_account TEXT NOT NULL PRIMARY KEY, " +
	    "name TEXT NOT NULL, " +
	    "notes TEXT, " +
	    "telephone_number TEXT)";

	try
	{
	    db.execSQL(str);
	}
	catch(Exception exception)
	{
	}

	str = "CREATE TABLE IF NOT EXISTS email_accounts (" +
	    "delete_on_server INTEGER DEFAULT 0 NOT NULL, " +
	    "email_account TEXT NOT NULL PRIMARY KEY, " +
	    "in_address TEXT NOT NULL, " +
	    "in_password TEXT NOT NULL, " +
	    "in_port INTEGER NOT NULL, " +
	    "out_address TEXT NOT NULL, " +
	    "out_email TEXT NOT NULL, " +
	    "out_password TEXT NOT NULL, " +
	    "out_port INTEGER NOT NULL, " +
	    "proxy_address TEXT, " +
	    "proxy_password TEXT, " +
	    "proxy_port INTEGER, " +
	    "proxy_type TEXT, " +
	    "proxy_user TEXT)";

	try
	{
	    db.execSQL(str);
	}
	catch(Exception exception)
	{
	}

	str = "CREATE TABLE IF NOT EXISTS folders (" +
	    "current_folder INTEGER NOT NULL DEFAULT 1, " +
	    "email_account TEXT NOT NULL, " +
	    "full_name TEXT NOT NULL, " +
	    "is_regular_folder INTEGER NOT NULL DEFAULT 1, " +
	    "name TEXT NOT NULL, " +
	    "FOREIGN KEY (email_account) REFERENCES " +
	    "email_accounts (email_account) ON DELETE CASCADE, " +
	    "PRIMARY KEY (email_account, name))";

	try
	{
	    db.execSQL(str);
	}
	catch(Exception exception)
	{
	}

	str = "CREATE TABLE IF NOT EXISTS messages (" +
	    "content_downloaded INTEGER NOT NULL DEFAULT 0, " +
	    "content_type TEXT NOT NULL, " +
	    "current_message INTEGER NOT NULL DEFAULT 1, " +
	    "email_account TEXT NOT NULL, " +
	    "folder_name TEXT NOT NULL, " +
	    "from_email_account TEXT NOT NULL, " +
	    "from_name TEXT NOT NULL, " +
	    "has_been_read INTEGER NOT NULL DEFAULT 0, " +
	    "message_html TEXT, " +
	    "message_plain TEXT, " +
	    "message_raw TEXT, " +
	    "received_date TEXT NOT NULL, " +
	    "received_date_unix_epoch BIGINT NOT NULL, " +
	    "selected INTEGER NOT NULL DEFAULT 0, " +
	    "sent_date TEXT NOT NULL, " +
	    "subject TEXT NOT NULL, " +
	    "to_folder_name TEXT NOT NULL, " +
	    "uid BIGINT NOT NULL, " +
	    "FOREIGN KEY (email_account) REFERENCES " +
	    "email_accounts (email_account) ON DELETE CASCADE, " +
	    "PRIMARY KEY (email_account, " +
	    "folder_name, " +
	    "uid))";

	try
	{
	    db.execSQL(str);
	}
	catch(Exception exception)
	{
	}

	str = "CREATE TABLE IF NOT EXISTS messages_attachments (" +
	    "attachment BLOB NOT NULL, " +
	    "attachment_id TEXT NOT NULL, " +
	    "attachment_name TEXT NOT NULL, " +
	    "email_account TEXT NOT NULL, " +
	    "folder_name TEXT NOT NULL, " +
	    "message_uid BIGINT NOT NULL, " +
	    "FOREIGN KEY (email_account) REFERENCES " +
	    "email_accounts (email_account) ON DELETE CASCADE, " +
	    "PRIMARY KEY (attachment_id, " +
	    "attachment_name, " +
	    "email_account, " +
	    "folder_name, " +
	    "message_uid))";

	try
	{
	    db.execSQL(str);
	}
	catch(Exception exception)
	{
	}

	str = "CREATE INDEX IF NOT EXISTS messages_received_date_unix_epoch " +
	    "ON messages (received_date_unix_epoch)";

	try
	{
	    db.execSQL(str);
	}
	catch(Exception exception)
	{
	}

	str = "CREATE TABLE IF NOT EXISTS messages_recipients (" +
	    "email_account TEXT NOT NULL, " +
	    "folder_name TEXT NOT NULL, " +
	    "message_uid BIGINT NOT NULL, " +
	    "recipient_email_account TEXT NOT NULL, " +
	    "recipient_name TEXT NOT NULL, " +
	    "recipient_type TEXT NOT NULL, " +
	    "FOREIGN KEY (email_account) REFERENCES " +
	    "email_accounts (email_account) ON DELETE CASCADE, " +
	    "PRIMARY KEY (email_account, " +
	    "folder_name, " +
	    "message_uid, " +
	    "recipient_email_account, " +
	    "recipient_type))";

	try
	{
	    db.execSQL(str);
	}
	catch(Exception exception)
	{
	}

	str = "CREATE TABLE IF NOT EXISTS open_pgp (" +
	    "function TEXT NOT NULL, " + // encryption, signature.
	    "private_key TEXT NOT NULL, " +
	    "private_key_digest TEXT NOT NULL, " +
	    "public_key TEXT NOT NULL, " +
	    "public_key_digest TEXT NOT NULL, " +
	    "PRIMARY KEY (function, private_key_digest, public_key_digest))";

	try
	{
	    db.execSQL(str);
	}
	catch(Exception exception)
	{
	}

	str = "CREATE TABLE IF NOT EXISTS settings (" +
	    "key_field TEXT NOT NULL PRIMARY KEY, " +
	    "value TEXT NOT NULL)";

	try
	{
	    db.execSQL(str);
	}
	catch(Exception exception)
	{
	}
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int old_version, int new_version)
    {
        onCreate(db);
    }

    public void purge_dangling()
    {
	if(m_db == null)
	    return;

	m_db.beginTransactionNonExclusive();

	try
	{
	    String strings[] = new String[]
	    {
		"DELETE FROM contacts WHERE email_account NOT IN " +
		"(SELECT email_account FROM email_accounts)",
		"DELETE FROM folders WHERE email_account NOT IN " +
		"(SELECT email_account FROM email_accounts)",
		"DELETE FROM messages WHERE email_account NOT IN " +
		"(SELECT email_account FROM email_accounts)",
		"DELETE FROM messages_attachments WHERE email_account NOT IN " +
		"(SELECT email_account FROM email_accounts)",
		"DELETE FROM messages_recipients WHERE email_account NOT IN " +
		"(SELECT email_account FROM email_accounts)"
	    };

	    for(String string : strings)
		try
		{
		    m_db.execSQL(string);
		}
		catch(Exception exception)
		{
		}

	    m_db.setTransactionSuccessful();
	}
	catch(Exception exception)
	{
	}
	finally
	{
	    m_db.endTransaction();
	}
    }

    public void select_all_messages(final Lettera lettera,
				    final MessagesAdapter messages_adapter,
				    final String email_account,
				    final String folder_name,
				    final boolean selected)
    {
	if(m_db == null)
	    return;

	Thread thread = new Thread(new Runnable()
	{
	    @Override
	    public void run()
	    {
		m_db.beginTransactionNonExclusive();

		try
		{
		    SQLiteStatement sqlite_statement = null;

		    sqlite_statement = m_db.compileStatement
			("UPDATE messages SET selected = ? " +
			 "WHERE email_account = ? AND " +
			 "LOWER(to_folder_name) = LOWER(?)");
		    sqlite_statement.bindLong(1, selected ? 1 : 0);
		    sqlite_statement.bindString(2, email_account);
		    sqlite_statement.bindString(3, folder_name);
		    sqlite_statement.execute();
		    m_db.setTransactionSuccessful();
		}
		catch(Exception exception)
		{
		}
		finally
		{
		    m_db.endTransaction();
		}

		if(messages_adapter != null)
		    lettera.runOnUiThread(new Runnable()
		    {
			@Override
			public void run()
			{
			    lettera.prepare_current_folder_text(folder_name);
			    messages_adapter.notifyDataSetChanged();
			}
		    });
	    }
	});

	thread.start();
    }

    public void set_message_read(String email_account,
				 String folder_name,
				 boolean has_been_read,
				 long uid)
    {
	if(m_db == null)
	    return;

	m_db.beginTransactionNonExclusive();

	try
	{
	    SQLiteStatement sqlite_statement = null;

	    sqlite_statement = m_db.compileStatement
		("UPDATE messages SET has_been_read = ? " +
		 "WHERE email_account = ? AND " +
		 "LOWER(to_folder_name) = LOWER(?) AND " +
		 "uid = ?");
	    sqlite_statement.bindLong(1, has_been_read ? 1 : 0);
	    sqlite_statement.bindString(2, email_account);
	    sqlite_statement.bindString(3, folder_name);
	    sqlite_statement.bindLong(4, uid);
	    sqlite_statement.execute();
	    m_db.setTransactionSuccessful();
	}
	catch(Exception exception)
	{
	}
	finally
	{
	    m_db.endTransaction();
	}

	synchronized(m_read_message_cursor_mutex)
	{
	    if(m_read_message_cursor_email_account.equals(email_account) &&
	       m_read_message_cursor_folder_name.equals(folder_name))
		if(m_read_message_cursor != null)
		{
		    m_read_message_cursor.close();
		    m_read_message_cursor = null;
		}
	}
    }

    public void set_message_selected(String email_account,
				     String folder_name,
				     boolean selected,
				     long uid)
    {
	if(m_db == null)
	    return;

	m_db.beginTransactionNonExclusive();

	try
	{
	    SQLiteStatement sqlite_statement = null;

	    sqlite_statement = m_db.compileStatement
		("UPDATE messages SET selected = ? " +
		 "WHERE email_account = ? AND " +
		 "LOWER(to_folder_name) = LOWER(?) AND " +
		 "uid = ?");
	    sqlite_statement.bindLong(1, selected ? 1 : 0);
	    sqlite_statement.bindString(2, email_account);
	    sqlite_statement.bindString(3, folder_name);
	    sqlite_statement.bindLong(4, uid);
	    sqlite_statement.execute();
	    m_db.setTransactionSuccessful();
	}
	catch(Exception exception)
	{
	}
	finally
	{
	    m_db.endTransaction();
	}
    }

    public void set_messages_unread(final Lettera lettera,
				    final MessagesAdapter messages_adapter,
				    final String email_account,
				    final String folder_name)
    {
	if(m_db == null)
	    return;

	Thread thread = new Thread(new Runnable()
	{
	    @Override
	    public void run()
	    {
		m_db.beginTransactionNonExclusive();

		try
		{
		    SQLiteStatement sqlite_statement = null;

		    sqlite_statement = m_db.compileStatement
			("UPDATE messages SET has_been_read = 0 " +
			 "WHERE email_account = ? AND " +
			 "LOWER(to_folder_name) = LOWER(?) AND " +
			 "selected = 1");
		    sqlite_statement.bindString(1, email_account);
		    sqlite_statement.bindString(2, folder_name);
		    sqlite_statement.execute();
		    m_db.setTransactionSuccessful();
		}
		catch(Exception exception)
		{
		}
		finally
		{
		    m_db.endTransaction();
		}

		synchronized(m_read_message_cursor_mutex)
		{
		    if(m_read_message_cursor != null &&
		       m_read_message_cursor_email_account.
		       equals(email_account) &&
		       m_read_message_cursor_folder_name.equals(folder_name))
		    {
			m_read_message_cursor.close();
			m_read_message_cursor = null;
		    }
		}

		if(messages_adapter != null)
		    lettera.runOnUiThread(new Runnable()
		    {
			@Override
			public void run()
			{
			    messages_adapter.notifyDataSetChanged();
			}
		    });
	    }
	});

	thread.start();
    }

    public void write_folders(ArrayList<FolderElement> array_list,
			      String email_account)
    {
	if(array_list == null || array_list.isEmpty() || m_db == null)
	    return;

	m_db.beginTransactionNonExclusive();

	try
	{
	    ContentValues content_values = new ContentValues();

	    content_values.put("current_folder", 0);
	    m_db.update("folders",
			content_values,
			"email_account = ?",
			new String[] {email_account});

	    {
		/*
		** Artificial separator.
		*/

		FolderElement folder_element = new FolderElement();

		folder_element.m_email_account = email_account;
		folder_element.m_is_regular_folder = 1;
		folder_element.m_name = "ZZZZZ";
		array_list.add(folder_element);
	    }

	    boolean insert_separator = false;

	    for(FolderElement folder_element : array_list)
	    {
		if(folder_element == null)
		    continue;

		String name = folder_element.m_name.toLowerCase();
		int is_regular_folder = 2;

		if(name.contains("draft"))
		    is_regular_folder = 0;
		else if(name.contains("important"))
		    is_regular_folder = 0;
		else if(name.contains("inbox"))
		    is_regular_folder = 0;
		else if(name.contains("sent"))
		    is_regular_folder = 0;
		else if(name.contains("spam"))
		    is_regular_folder = 0;
		else if(name.contains("star"))
		    is_regular_folder = 0;
		else if(name.contains("trash"))
		    is_regular_folder = 0;
		else
		    is_regular_folder = folder_element.m_is_regular_folder;

		if(is_regular_folder == 0)
		    insert_separator = true;
		else if(is_regular_folder == 1)
		    if(!insert_separator)
			continue;

		m_db.execSQL
		    ("REPLACE INTO folders (" +
		     "current_folder, " +
		     "email_account, " +
		     "full_name, " +
		     "is_regular_folder, " +
		     "name) VALUES " +
		     "(?, ?, ?, ?, ?)",
		     new String[] {String.valueOf(1),
				   email_account,
				   folder_element.m_full_name,
				   String.valueOf(is_regular_folder),
				   folder_element.m_name});
	    }

	    m_db.delete("folders",
			"current_folder = 0 AND email_account = ?",
			new String[] {email_account});
	    m_db.setTransactionSuccessful();
	}
	catch(Exception exception)
	{
	}
	finally
	{
	    m_db.endTransaction();
	}
    }

    public void write_messages(AtomicBoolean interrupt,
			       IMAPFolder folder,
			       String email_account,
			       boolean enable_database_transaction)
    {
	if(folder == null || m_db == null)
	    return;

	try
	{
	    if((folder.getType() & Folder.HOLDS_MESSAGES) == 0)
		return;
	}
	catch(Exception exception)
	{
	}

	try
	{
	    folder.open(Folder.READ_ONLY);
	}
	catch(Exception exception)
	{
	    Log.e("write_messages()",
		  exception.getMessage() + " (" + folder.getName() + ")");
	    return;
	}

	Message messages[] = null;

	try
	{
	    messages = folder.getMessages();

	    FetchProfile fetch_profile = new FetchProfile();

	    fetch_profile.add(FetchProfile.Item.ENVELOPE);
	    fetch_profile.add(UIDFolder.FetchProfileItem.UID);
	    folder.fetch(messages, fetch_profile);
	}
	catch(Exception exception1)
	{
	    Log.e("write_messages()",
		  exception1.getMessage() + " (" + folder.getName() + ")");

	    try
	    {
		folder.close();
	    }
	    catch(Exception exception2)
	    {
	    }

	    return;
	}

	boolean current_folder = false;
	boolean refresh_cursor = true;
	int count = 0;

	synchronized(m_read_message_cursor_mutex)
	{
	    current_folder = m_read_message_cursor_email_account.
		equals(email_account) &&
		m_read_message_cursor_folder_name.equals(folder.getName());
	}

	if(current_folder)
	    count = message_count(email_account, folder.getName());

	if(enable_database_transaction)
	    m_db.beginTransactionNonExclusive();

	try
	{
	    ContentValues content_values = new ContentValues();

	    /*
	    ** Messages on the server may be deleted. If the local
	    ** messages do not exist on the server,
	    ** delete their local representations.
	    */

	    content_values.put("current_message", 0);
	    m_db.update("messages",
			content_values,
			"email_account = ? AND " +
			"LOWER(folder_name) = LOWER(?)",
			new String[] {email_account, folder.getName()});

	    SQLiteStatement sqlite_statement = m_db.compileStatement
		("REPLACE INTO messages (" +
		 "content_downloaded, " +
		 "content_type, " +
		 "current_message, " +
		 "email_account, " +
		 "folder_name, " +
		 "from_email_account, " +
		 "from_name, " +
		 "has_been_read, " +
		 "message_html, " +
		 "message_plain, " +
		 "message_raw, " +
		 "received_date, " +
		 "received_date_unix_epoch, " +
		 "selected, " +
		 "sent_date, " +
		 "subject, " +
		 "to_folder_name, " +
		 "uid) VALUES " +
		 "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
	    long start_time = 0;

	    for(Message message : messages)
	    {
		if(message == null)
		    continue;

		Cursor cursor = null;
		String to_folder_name = folder.getName();
		int has_been_read = 0;
		int selected = 0;
		long message_uid = 0;

		try
		{
		    message_uid = folder.getUID(message);
		    cursor = m_db.rawQuery
			("SELECT content_downloaded, " + // 0
			 "has_been_read, " +             // 1
			 "selected, " +                  // 2
			 "to_folder_name, " +            // 3
			 "OID " +                        // 4
			 "FROM messages WHERE email_account = ? AND " +
			 "LOWER(folder_name) = LOWER(?) AND " +
			 "uid = ?",
			 new String[] {email_account,
				       folder.getName(),
				       String.valueOf(message_uid)});

		    if(cursor != null && cursor.moveToFirst())
		    {
			if(cursor.getInt(0) == 1)
			{
			    content_values.clear();
			    content_values.put("current_message", 1);

			    if(m_db.
			       update("messages",
				      content_values,
				      "OID = ?",
				      new String[] {String.
						    valueOf(cursor.
							    getLong(4))}) > 0)
			    {
				cursor.close();
				refresh_cursor = true;
				continue;
			    }
			}

			has_been_read = cursor.getInt(1);
			selected = cursor.getInt(2);
			to_folder_name = cursor.getString(3);
		    }
		}
		catch(Exception exception)
		{
		}
		finally
		{
		    if(cursor != null)
			cursor.close();
		}

		if(start_time == 0)
		    start_time = System.currentTimeMillis();

		if(System.currentTimeMillis() - start_time >
		   MESSAGE_DOWNLOAD_TIME)
		    break;

		try
		{
		    content_values.clear();
		    content_values.put("current_message", 1);
		    content_values.put("email_account", email_account);
		    content_values.put
			("folder_name", folder.getName().toLowerCase());

		    if(message.getFrom() != null &&
		       message.getFrom().length != 0)
		    {
			InternetAddress internet_address = (InternetAddress)
			    message.getFrom()[0];

			if(internet_address != null)
			{
			    if(internet_address.getAddress() == null ||
			       internet_address.getAddress().isEmpty())
				content_values.put
				    ("from_email_account", UNKNOWN_EMAIL);
			    else
				content_values.put
				    ("from_email_account",
				     internet_address.getAddress());

			    if(internet_address.getPersonal() == null ||
			       internet_address.getPersonal().isEmpty())
				content_values.put
				    ("from_name",
				     content_values.
				     getAsString("from_email_account"));
			    else
				content_values.put
				    ("from_name",
				     internet_address.getPersonal());
			}
			else
			{
			    content_values.put
				("from_email_account", UNKNOWN_EMAIL);
			    content_values.put("from_name", "(unknown)");
			}
		    }
		    else
		    {
			content_values.put("from_email_account", UNKNOWN_EMAIL);
			content_values.put("from_name", "(unknown)");
		    }

		    content_values.put("has_been_read", has_been_read);

		    if(message.getContentType() == null ||
		       message.isMimeType("text/plain"))
		    {
			content_values.put("content_downloaded", 1);
			content_values.put("content_type", "text/plain");

			if(message.getContent() != null &&
			   message.getContent().toString() != null &&
			   message.getContent().toString().trim().length() > 0)
			{
			    content_values.put
				("message_html",
				 message.getContent().toString().trim());
			    content_values.put
				("message_plain",
				 message.getContent().toString().trim());
			    content_values.put
				("message_raw",
				 message.getContent().toString().trim());
			}
			else
			{
			    content_values.put("message_html", " ");
			    content_values.put("message_plain", " ");
			    content_values.put("message_raw", " ");
			}
		    }
		    else
		    {
			content_values.put("content_downloaded", 1);

			String string = Mail.multipart
			    (message, "text/html").trim();

			if(string.isEmpty())
			    content_values.put("message_html", " ");
			else
			{
			    content_values.put("content_type", "text/html");
			    content_values.put("message_html", string);
			}

			string = Mail.multipart(message, "text/plain").trim();

			if(string.isEmpty())
			    content_values.put("message_plain", " ");
			else
			    content_values.put("message_plain", string);

			string = Mail.multipart(message, "").trim();

			if(string.isEmpty())
			    content_values.put("message_raw", " ");
			else
			    content_values.put("message_raw", string);

			if(!content_values.containsKey("content_type"))
			    content_values.put("content_type", "text/plain");
		    }

		    if(message.getReceivedDate() != null)
		    {
			content_values.put
			    ("received_date",
			     message.getReceivedDate().toString());
			content_values.put
			    ("received_date_unix_epoch",
			     message.getReceivedDate().getTime());
		    }
		    else
		    {
			content_values.put("received_date", "01/01/1970");
			content_values.put("received_date_unix_epoch", 0);
		    }

		    content_values.put("selected", selected);

		    if(message.getSentDate() != null)
			content_values.put
			    ("sent_date", message.getSentDate().toString());
		    else
			content_values.put("sent_date", "01/01/1970");

		    if(message.getSubject() != null)
			content_values.put
			    ("subject", message.getSubject().trim());
		    else
			content_values.put("subject", "(no subject)");

		    content_values.put("uid", message_uid);

		    int i = 1;

		    sqlite_statement.bindLong
			(i++, content_values.getAsLong("content_downloaded"));
		    sqlite_statement.bindString
			(i++, content_values.getAsString("content_type"));
		    sqlite_statement.bindLong
			(i++, content_values.getAsLong("current_message"));
		    sqlite_statement.bindString
			(i++, content_values.getAsString("email_account"));
		    sqlite_statement.bindString
			(i++, content_values.getAsString("folder_name"));
		    sqlite_statement.bindString
			(i++, content_values.getAsString("from_email_account"));
		    sqlite_statement.bindString
			(i++, content_values.getAsString("from_name"));
		    sqlite_statement.bindLong
			(i++, content_values.getAsLong("has_been_read"));
		    sqlite_statement.bindString
			(i++, content_values.getAsString("message_html"));
		    sqlite_statement.bindString
			(i++, content_values.getAsString("message_plain"));
		    sqlite_statement.bindString
			(i++, content_values.getAsString("message_raw"));
		    sqlite_statement.bindString
			(i++, content_values.getAsString("received_date"));
		    sqlite_statement.bindLong
			(i++,
			 content_values.getAsLong("received_date_unix_epoch"));
		    sqlite_statement.bindLong
			(i++, content_values.getAsLong("selected"));
		    sqlite_statement.bindString
			(i++, content_values.getAsString("sent_date"));
		    sqlite_statement.bindString
			(i++, content_values.getAsString("subject"));
		    sqlite_statement.bindString(i++, to_folder_name);
		    sqlite_statement.bindLong
			(i++, content_values.getAsLong("uid"));
		    sqlite_statement.execute();
		    sqlite_statement.clearBindings();

		    if(message.getRecipients(Message.RecipientType.BCC) != null)
			for(Address address :
			    message.getRecipients(Message.RecipientType.BCC))
			    write_recipient((InternetAddress) address,
					    email_account,
					    folder.getName(),
					    "BCC",
					    message_uid);

		    if(message.getRecipients(Message.RecipientType.CC) != null)
			for(Address address :
			    message.getRecipients(Message.RecipientType.CC))
			    write_recipient((InternetAddress) address,
					    email_account,
					    folder.getName(),
					    "CC",
					    message_uid);

		    if(message.getRecipients(Message.RecipientType.TO) != null)
			for(Address address :
			    message.getRecipients(Message.RecipientType.TO))
			    write_recipient((InternetAddress) address,
					    email_account,
					    folder.getName(),
					    "TO",
					    message_uid);
		}
		catch(Exception exception)
		{
		}

		if(interrupt.get())
		    break;
	    }

	    m_db.delete("messages",
			"current_message = 0 AND " +
			"email_account = ? AND " +
			"LOWER(folder_name) = LOWER(?)",
			new String[] {email_account, folder.getName()});

	    if(enable_database_transaction)
		m_db.setTransactionSuccessful();
	}
	catch(Exception exception)
	{
	}
	finally
	{
	    if(enable_database_transaction)
		m_db.endTransaction();
	}

	if(current_folder)
	    if(count != message_count(email_account, folder.getName()) ||
	       refresh_cursor)
		synchronized(m_read_message_cursor_mutex)
		{
		    if(m_read_message_cursor != null)
			m_read_message_cursor.close();

		    m_read_message_cursor = null;
		}

	try
	{
	    folder.close();
	}
	catch(Exception exception)
	{
	}
    }
}
