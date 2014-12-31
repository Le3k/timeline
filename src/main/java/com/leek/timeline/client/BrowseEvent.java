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
	 * Date being browsed. May be <code>null</code> if no date is being browsed.
	 */
	private Date date = null;

	/**
	 * Stats being browsed. May be <code>null</code> if no stats are being browsed.
	 */
	private Float statsPercent = null;
	
	/**
	 * mouse X position in screen.
	 */
	private int clientY;

	/**
	 * mouse Y position in screen.
	 */
	private int clientX;
	
	/**
	 * Constructor provided with the browsing coordinates
	 * @param x mouse X position in screen.
	 * @param y mouse Y position in screen.
	 * @param rowId ID of the row being browsed.
	 * @param date date being browsed. May be <code>null</code>.
	 * @param statsPercent percent being browsed. May be <code>null</code>.
	 */
	public BrowseEvent(int x, int y, String rowId, Date date, Float statsPercent)
	{
		this.clientX = x;
		this.clientY = y;
		this.rowId = rowId;
		this.date = date;
		this.statsPercent = statsPercent;
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

	/**
	 * @return the screenY
	 */
	public int getClientY()
	{
		return clientY;
	}

	/**
	 * @return the screenX
	 */
	public int getClientX()
	{
		return clientX;
	}

	/**
	 * @return the statsPercent
	 */
	public Float getStatsPercent()
	{
		return statsPercent;
	}
}
