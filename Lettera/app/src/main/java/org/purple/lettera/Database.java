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
import android.util.Base64;
import java.security.KeyPair;
import java.util.ArrayList;

public class Database extends SQLiteOpenHelper
{
    private SQLiteDatabase m_db = null;
    private final static String DATABASE_NAME = "lettera.db";
    private final static int DATABASE_VERSION = 4;
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

    public ArrayList<String> email_account_names()
    {
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

    public EmailElement email_element(String account)
    {
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
		 new String[] {account});

	    if(cursor != null && cursor.moveToFirst())
	    {
		EmailElement email_element = new EmailElement();

		for(int i = 0; i < cursor.getColumnCount(); i++)
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

    public SettingsElement settings_element(String key)
    {
	Cursor cursor = null;

	try
	{
	    cursor = m_db.rawQuery
		("SELECT key_field, value FROM settings WHERE key_field = ?",
		 new String[] {key});

	    if(cursor != null && cursor.moveToFirst())
	    {
		SettingsElement settings_element = new SettingsElement();

		for(int i = 0; i < cursor.getColumnCount(); i++)
		    switch(i)
		    {
		    case 0:
			settings_element.m_key = key;
			break;
		    case 1:
			settings_element.m_value = cursor.getString(i).trim();
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

    public String save_email(ContentValues content_values)
    {
	if(content_values == null || content_values.size() == 0)
	    return "empty container on save_email()";
	else if(m_db == null)
	    return "m_db is null on save_email()";

	m_db.beginTransactionNonExclusive();

	try
	{
	    m_db.execSQL
		("REPLACE INTO email_accounts (" +
		 "delete_on_server, " +
		 "email_account, " +
		 "in_address, " +
		 "in_password, " +
		 "in_port, " +
		 "out_address, " +
		 "out_email, " +
		 "out_password, " +
		 "out_port, " +
		 "proxy_address, " +
		 "proxy_password, " +
		 "proxy_port, " +
		 "proxy_type, " +
		 "proxy_user) VALUES " +
		 "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
		 new String[] {content_values.getAsString("delete_on_server"),
			       content_values.getAsString("email_account"),
			       content_values.getAsString("in_address"),
			       content_values.getAsString("in_password"),
			       content_values.getAsString("in_port"),
			       content_values.getAsString("out_address"),
			       content_values.getAsString("out_email"),
			       content_values.getAsString("out_password"),
			       content_values.getAsString("out_port"),
			       content_values.getAsString("proxy_address"),
			       content_values.getAsString("proxy_password"),
			       content_values.getAsString("proxy_port"),
			       content_values.getAsString("proxy_type"),
			       content_values.getAsString("proxy_user")});
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

    public String save_setting(ContentValues content_values)
    {
	if(content_values == null || content_values.size() == 0)
	    return "empty container on save_setting()";
	else if(m_db == null)
	    return "m_db is null on save_setting()";

	m_db.beginTransactionNonExclusive();

	try
	{
	    m_db.execSQL
		("REPLACE INTO settings (key_field, value) VALUES (?, ?)",
		 new String[] {content_values.getAsString("key"),
			       content_values.getAsString("value")});
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

    public boolean delete_email_account(String account)
    {
	if(m_db == null)
	    return false;

	boolean ok = false;

	m_db.beginTransactionNonExclusive();

	try
	{
	    ok = m_db.delete
		("email_accounts",
		 "email_account = ?",
		 new String[] {account}) > 0;
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

    public boolean save_pgp_key_pair(KeyPair key_pair, String function)
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

    public static synchronized Database get_instance()
    {
	return s_instance; // Should never be null.
    }

    public static synchronized Database get_instance(Context context)
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
		 "open_pgp",
		 "settings"};

	    for(int i = 0; i < tables.length; i++)
		try
		{
		    m_db.delete(tables[i], null, null);
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
	    db.execSQL("PRAGMA secure_delete = True", null);
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
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        onCreate(db);
    }
}
