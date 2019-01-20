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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Windows
{
    public static void show_dialog(Context context, String text, String title)
    {
	if(context == null ||
	   text == null ||
	   text.trim().isEmpty() ||
	   title == null ||
	   title.trim().isEmpty())
	    return;

	try
	{
	    if(((Activity) context).isFinishing())
		return;

	    AlertDialog alert_dialog = new AlertDialog.Builder
		(context).create();

	    alert_dialog.setButton
		(AlertDialog.BUTTON_NEUTRAL,
		 "Close",
		 new DialogInterface.OnClickListener()
		 {
		     @Override
		     public void onClick(DialogInterface dialog, int which)
		     {
			 try
			 {
			     dialog.dismiss();
			 }
			 catch(Exception exception)
			 {
			 }
		     }
		 });
	    alert_dialog.setMessage(text);
	    alert_dialog.setTitle(title);
	    alert_dialog.show();
	}
	catch(Exception exception)
	{
	}
    }

    public static void show_progress_dialog(Context context,
					    Dialog dialog,
					    String text,
					    final AtomicBoolean interrupt)
    {
	if(context == null ||
	   dialog == null ||
	   text == null ||
	   text.trim().isEmpty())
	    return;

	try
	{
	    if(((Activity) context).isFinishing())
		return;

	    LayoutInflater inflater = (LayoutInflater) context.getSystemService
		(Context.LAYOUT_INFLATER_SERVICE);
	    View view = inflater.inflate(R.layout.progress, null);

	    if(interrupt != null)
		((Button) view.findViewById(R.id.interrupt)).
		    setOnClickListener(new View.OnClickListener()
	        {
		    @Override
		    public void onClick(View view)
		    {
			((Button) view).setText("Interrupting...");
			interrupt.set(true);
			view.findViewById(R.id.interrupt).setEnabled(false);
		    }
		});

	    ((Button) view.findViewById(R.id.interrupt)).setAllCaps(false);
	    ((Button) view.findViewById(R.id.interrupt)).setTextColor
		(Lettera.text_color());
	    ((Button) view.findViewById(R.id.interrupt)).setVisibility
		(interrupt == null ? View.GONE : View.VISIBLE);
	    ((TextView) view.findViewById(R.id.text)).setText(text);
	    ((TextView) view.findViewById(R.id.text)).setTextColor
		(Lettera.text_color());
	    dialog.setCancelable(false);
	    dialog.setContentView(view);
	    dialog.show();
	    view.setBackgroundColor(Lettera.background_color());
	}
	catch(Exception exception_1)
	{
	    try
	    {
		dialog.dismiss();
	    }
	    catch(Exception exception_2)
	    {
	    }
	}
    }

    public static void show_prompt_dialog
	(Context context,
	 DialogInterface.OnCancelListener cancel_listener,
	 String prompt,
	 final AtomicBoolean confirmed)
    {
	if(context == null ||
	   cancel_listener == null ||
	   prompt == null ||
	   prompt.trim().isEmpty())
	    return;

	try
	{
	    if(((Activity) context).isFinishing())
		return;

	    AlertDialog alert_dialog = new AlertDialog.Builder
		(context).create();

	    alert_dialog.setButton
		(AlertDialog.BUTTON_NEGATIVE, "No",
		 new DialogInterface.OnClickListener()
		 {
		     public void onClick(DialogInterface dialog, int which)
		     {
			 try
			 {
			     dialog.dismiss();
			 }
			 catch(Exception exception)
			 {
			 }
		     }
		 });
	    alert_dialog.setButton
		(AlertDialog.BUTTON_POSITIVE, "Yes",
		 new DialogInterface.OnClickListener()
		 {
		     public void onClick(DialogInterface dialog, int which)
		     {
			 try
			 {
			     if(confirmed != null)
				 confirmed.set(true);

			     dialog.cancel();
			 }
			 catch(Exception exception)
			 {
			 }
		     }
		 });
	    alert_dialog.setMessage(prompt);
	    alert_dialog.setOnCancelListener(cancel_listener);
	    alert_dialog.setTitle("Confirmation");
	    alert_dialog.show();
	}
	catch(Exception exception)
	{
	}
    }
}
