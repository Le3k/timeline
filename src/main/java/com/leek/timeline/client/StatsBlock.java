/**
 * 
 */
package com.leek.timeline.client;

/**
 * @author <a href="mailto:andrei.cojocaru@hansenhof.de">Andrei Cojocaru</a>
 *
 */
public class StatsBlock
{

	/**
	 * Part of a total, as a value in interval 0-1.
	 */
	private float percent;
	
	/**
	 * Block color;
	 */
	private String color;

	public StatsBlock(float percent, String color)
	{
		this.percent = percent;
		this.color = color;
	}

	/**
	 * @return the percent
	 */
	public float getPercent()
	{
		return percent;
	}

	/**
	 * @param percent the percent to set
	 */
	public void setPercent(float percent)
	{
		this.percent = percent;
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
