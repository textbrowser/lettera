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

import android.content.res.ColorStateList;
import android.view.ViewGroup;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import java.util.Calendar;
import java.util.Date;

public abstract class Utilities
{
    public static String bytes_to_hex(byte bytes[])
    {
	if(bytes == null || bytes.length <= 0)
	    return "";

	try
	{
	    StringBuilder stringBuilder = new StringBuilder();

	    for(byte b : bytes)
		stringBuilder.append(String.format("%02x", b));

	    return stringBuilder.toString();
	}
	catch(Exception exception)
	{
	    return "";
	}
    }

    public static String formatted_email_date(Date date)
    {
	if(date == null)
	    return "";

	StringBuffer string_buffer = new StringBuffer();

	try
	{
	    Calendar calendar = Calendar.getInstance();
	    Calendar today = Calendar.getInstance();

	    calendar.setTime(date);
	    today.set(Calendar.HOUR_OF_DAY, 0);
	    today.set(Calendar.MILLISECOND, 0);
	    today.set(Calendar.MINUTE, 0);
	    today.set(Calendar.SECOND, 0);

	    if(date.after(today.getTime()))
	    {
		string_buffer.append
		    (calendar.get(Calendar.HOUR)  == 0 ?
		     12 : calendar.get(Calendar.HOUR));
		string_buffer.append(":");
		string_buffer.append
		    (calendar.get(Calendar.MINUTE) < 10 ?
		     "0" + calendar.get(Calendar.MINUTE) :
		     calendar.get(Calendar.MINUTE));
		string_buffer.append(" ");
		string_buffer.append
		    (calendar.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM");
	    }
	    else if(calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR))
	    {
		switch(calendar.get(Calendar.MONTH))
		{
		case Calendar.APRIL:
		    string_buffer.append("Apr ");
		    break;
		case Calendar.AUGUST:
		    string_buffer.append("Aug ");
		    break;
		case Calendar.DECEMBER:
		    string_buffer.append("Dec ");
		    break;
		case Calendar.FEBRUARY:
		    string_buffer.append("Feb ");
		    break;
		case Calendar.JANUARY:
		    string_buffer.append("Jan ");
		    break;
		case Calendar.JULY:
		    string_buffer.append("Jul ");
		    break;
		case Calendar.JUNE:
		    string_buffer.append("Jun ");
		    break;
		case Calendar.MARCH:
		    string_buffer.append("Mar ");
		    break;
		case Calendar.MAY:
		    string_buffer.append("May ");
		    break;
		case Calendar.SEPTEMBER:
		    string_buffer.append("Sep ");
		    break;
		case Calendar.OCTOBER:
		    string_buffer.append("Oct ");
		    break;
		case Calendar.NOVEMBER:
		    string_buffer.append("Nov ");
		    break;
		default:
		    break;
		}

		string_buffer.append(calendar.get(Calendar.DAY_OF_MONTH));
	    }
	    else
	    {
		string_buffer.append(calendar.get(Calendar.MONTH) + 1);
		string_buffer.append("/");
		string_buffer.append(calendar.get(Calendar.DAY_OF_MONTH));
		string_buffer.append("/");
		string_buffer.append(calendar.get(Calendar.YEAR));
	    }
	}
	catch(Exception exception)
	{
	}

	return string_buffer.toString();
    }

    public static void color_checkbox(CheckBox checkbox,
				      int background_color,
				      int divider_color,
				      int text_color)
    {
	if(checkbox == null)
	    return;

	int colors[] = new int[]
	{
	    text_color, divider_color, text_color, text_color
	};
	int states[][] = new int[][]
	{
	    new int[] { android.R.attr.state_enabled},
	    new int[] {-android.R.attr.state_enabled},
	    new int[] {-android.R.attr.state_checked},
	    new int[] { android.R.attr.state_pressed}
	};

	ColorStateList color_state_list = new ColorStateList(states, colors);

	checkbox.setButtonTintList(color_state_list);
    }

    public static void color_children(View view,
				      int background_color,
				      int divider_color,
				      int text_color)
    {
	if(view == null)
	    return;
	else if(!(view instanceof ViewGroup))
	{
	    if(view instanceof Button)
	    {
		((Button) view).setTextColor(text_color);

		if(view instanceof CheckBox)
		    color_checkbox((CheckBox) view,
				   background_color,
				   divider_color,
				   text_color);
	    }
	    else if(view instanceof TextView)
	    {
		((TextView) view).setHintTextColor(text_color);
		((TextView) view).setTextColor(text_color);
	    }

	    return;
	}

	ViewGroup view_group = (ViewGroup) view;
	int count = view_group.getChildCount();

	for(int i = 0; i < count; i++)
	{
	    View child = view_group.getChildAt(i);

	    color_children(child, background_color, divider_color, text_color);
	}
    }
}
