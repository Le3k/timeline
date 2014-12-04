/**
 * 
 */
package com.leek.timeline.client;

import java.util.Date;

/**
 * @author <a href="mailto:andrei.cojocaru@hansenhof.de">Andrei Cojocaru</a>
 *
 */
public final class TimeUtils
{

	private TimeUtils() {}
	
	public static final long MILLIS_PER_SECOND = 1000;
	public static final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;
	public static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;
	public static final long MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;
	
	public static long intervalInMillis(Date start, Date end)
	{
		return intervalInMillis(start.getTime(), end.getTime());
	}
	
	public static long intervalInMillis(long start, long end)
	{
		return (long) jsIntervalInMillis(start, end);
	}
	
	private static native double jsIntervalInMillis(double start, double end)
	/*-{
		return $wnd.intervalInMillis(start, end);
	}-*/;
	
	public static final long intervalInSeconds(Date start, Date end)
	{
		return (long) jsIntervalInSeconds(start.getTime(), end.getTime());
	}
	
	private static native double jsIntervalInSeconds(double start, double end)
	/*-{
		return $wnd.intervalInSeconds(start, end);
	}-*/;

	public static long intervalInMinutes(Date startDate, Date endDate)
	{
		return intervalInMillis(startDate, endDate) / MILLIS_PER_MINUTE;
	}

	public static long intervalInHours(Date startDate, Date endDate)
	{
		return intervalInMillis(startDate, endDate) / MILLIS_PER_HOUR;
	}

	public static long intervalInDays(Date startDate, Date endDate)
	{
		return intervalInMillis(startDate, endDate) / MILLIS_PER_DAY;
	}
}
