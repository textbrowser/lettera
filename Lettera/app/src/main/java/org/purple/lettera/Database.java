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
import java.util.ArrayList;

public class Database extends SQLiteOpenHelper
{
    private SQLiteDatabase m_db = null;
    private final static String DATABASE_NAME = "lettera.db";
    private final static int DATABASE_VERSION = 1;
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
		 "OID " +                      // 9
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
			    cursor.getInt(i) == 0 ? false : true;
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

    public String save_email(ContentValues content_values)
    {
	if(content_values == null || content_values.size() == 0)
	    return "empty container";
	else if(m_db == null)
	    return "m_db is null";

	m_db.beginTransactionNonExclusive();

	try
	{
	    m_db.insertOrThrow("email_accounts", null, content_values);
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

    public static synchronized Database getInstance()
    {
	return s_instance; // Should never be null.
    }

    public static synchronized Database getInstance(Context context)
    {
	if(s_instance == null)
	    s_instance = new Database(context.getApplicationContext());

	return s_instance;
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

	str = "CREATE TABLE IF NOT EXISTS email_accounts (" +
	    "delete_on_server INTEGER DEFAULT 0 NOT NULL, " +
	    "email_account TEXT NOT NULL PRIMARY KEY, " +
	    "in_address TEXT NOT NULL, " +
	    "in_password TEXT NOT NULL, " +
	    "in_port INTEGER NOT NULL, " +
	    "out_address TEXT NOT NULL, " +
	    "out_email TEXT NOT NULL, " +
	    "out_password TEXT NOT NULL, " +
	    "out_port INTEGER NOT NULL)";

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
