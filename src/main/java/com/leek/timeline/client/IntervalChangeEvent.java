/**
 * 
 */
package com.leek.timeline.client;

import java.util.Date;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Fired when interval changed.
 * @author <a href="mailto:andrei.cojocaru@hansenhof.de">Andrei Cojocaru</a>
 */
public class IntervalChangeEvent extends GwtEvent<IntervalChangeEvent.Handler>
{
	/**
	 * Event type.
	 */
	public static final GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();
	
	/**
	 * Interval start date.
	 */
	private Date start = null;
	/**
	 * Interval end date.
	 */
	private Date end = null;
	
	/**
	 * Constructor provided with new interval endpoints.
	 * @param start interval start date
	 * @param end interval end date
	 */
	public IntervalChangeEvent(Date start, Date end)
	{
		this.start = start;
		this.end = end;
	}

	/**
	 * @author <a href="mailto:andrei.cojocaru@hansenhof.de">Andrei Cojocaru</a>
	 *
	 */
	public interface Handler extends EventHandler
	{
		void onIntervalChange(IntervalChangeEvent event);
	}

	@Override
	public com.google.gwt.event.shared.GwtEvent.Type<Handler> getAssociatedType()
	{
		return TYPE;
	}

	@Override
	protected void dispatch(Handler handler)
	{
		handler.onIntervalChange(this);
	}

	/**
	 * @return the start
	 */
	public Date getStart()
	{
		return start;
	}

	/**
	 * @return the end
	 */
	public Date getEnd()
	{
		return end;
	}
}
