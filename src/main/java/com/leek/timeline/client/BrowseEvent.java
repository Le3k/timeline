/**
 * 
 */
package com.leek.timeline.client;

import java.util.Date;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Fired when User is hovering <i>the region defined by the Timeline axes</i>.
 * If mouse is over a row, then the values {@link #rowId} and {@link #date} are provided.
 * Otherwise, they are <code>null</code>.
 * 
 * @author <a href="mailto:andrei.cojocaru@hansenhof.de">Andrei Cojocaru</a>
 */
public class BrowseEvent extends GwtEvent<BrowseEvent.Handler>
{
	/**
	 * Event type.
	 */
	public static final Type<Handler> TYPE = new Type<>();

	/**
	 * ID of the row being browsed.
	 */
	private String rowId = null;
	
	/**
	 * Date being browsed.
	 */
	private Date date = null;
	
	/**
	 * Constructor provided with the browsing coordinates
	 * @param rowId ID of the row being browsed.
	 * @param date date being browsed.
	 */
	public BrowseEvent(String rowId, Date date)
	{
		this.rowId = rowId;
		this.date = date;
	}
	
	/**
	 * @author <a href="mailto:andrei.cojocaru@hansenhof.de">Andrei Cojocaru</a>
	 */
	public interface Handler extends EventHandler
	{
		void onBrowse(BrowseEvent event);
	}

	@Override
	public com.google.gwt.event.shared.GwtEvent.Type<Handler> getAssociatedType()
	{
		return TYPE;
	}

	@Override
	protected void dispatch(Handler handler)
	{
		handler.onBrowse(this);
	}

	/**
	 * @return the rowId
	 */
	public String getRowId()
	{
		return rowId;
	}

	/**
	 * @return the date
	 */
	public Date getDate()
	{
		return date;
	}
}
