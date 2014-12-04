/**
 * 
 */
package com.leek.timeline.client;

import java.util.Date;

/**
 * @author <a href="mailto:andrei.cojocaru@hansenhof.de">Andrei Cojocaru</a>
 *
 */
public class TimeBlock
{

	private Date start;
	private Date end;
	
	private String color;

	/**
	 * @return the start
	 */
	public Date getStart()
	{
		return start;
	}

	/**
	 * @param start the start to set
	 */
	public void setStart(Date start)
	{
		this.start = start;
	}

	/**
	 * @return the end
	 */
	public Date getEnd()
	{
		return end;
	}

	/**
	 * @param end the end to set
	 */
	public void setEnd(Date end)
	{
		this.end = end;
	}

	/**
	 * @return the color
	 */
	public String getColor()
	{
		return color;
	}

	/**
	 * @param color the color to set
	 */
	public void setColor(String color)
	{
		this.color = color;
	}
}
