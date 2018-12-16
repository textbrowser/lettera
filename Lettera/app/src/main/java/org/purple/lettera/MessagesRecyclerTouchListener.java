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

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class MessagesRecyclerTouchListener
    implements RecyclerView.OnItemTouchListener
{
    private ClickListener m_click_listener = null;
    private GestureDetector m_gesture_detector = null;

    public interface ClickListener
    {
	void onClick(View view, int position);
	void onLongClick(View view, int position);
    }

    public MessagesRecyclerTouchListener(Context context,
					 final RecyclerView recycler_view,
					 final ClickListener click_listener)
    {
	m_click_listener = click_listener;
	m_gesture_detector = new GestureDetector
	    (context, new GestureDetector.SimpleOnGestureListener()
	    {
		@Override
		public boolean onSingleTapUp(MotionEvent motion_event)
		{
		    return true;
		}

		@Override
		public void onLongPress(MotionEvent motion_event)
		{
		    View view = recycler_view.findChildViewUnder
			(motion_event.getX(), motion_event.getY());

		    if(m_click_listener != null && view != null)
			m_click_listener.onLongClick
			    (view, recycler_view.getChildAdapterPosition(view));
		}
	    });
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView recycler_view,
					 MotionEvent motion_event)
    {
	View view = recycler_view.findChildViewUnder
	    (motion_event.getX(), motion_event.getY());

	if(m_click_listener != null &&
	   m_gesture_detector.onTouchEvent(motion_event) &&
	   view != null)
	    m_click_listener.onClick
		(view, recycler_view.getChildAdapterPosition(view));

	return false;
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent
	(boolean disallow_intercept)
    {
    }

    @Override
    public void onTouchEvent(RecyclerView recycler_view,
			     MotionEvent motion_event)
    {
    }
}
