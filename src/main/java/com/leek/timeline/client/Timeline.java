/**
 * 
 */
package com.leek.timeline.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.Context2d.TextBaseline;
import com.google.gwt.canvas.dom.client.TextMetrics;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsDate;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.RootPanel;
import com.leek.timeline.client.IntervalChangeEvent.Handler;

/**
 * @author <a href="mailto:andrei.cojocaru@hansenhof.de">Andrei Cojocaru</a>
 */
public class Timeline implements IsWidget, RequiresResize, FiresTimelineEvents
{

	/**
	 * Event handlers manager.
	 */
	private HandlerManager handlerManager = new HandlerManager(this);
	
	private static final int ROW_HEIGHT = 16;
	private static final int ROWS_SPACING = 2;
	private static final int LABEL_HEIGHT = 15;
	private static final int SPACING = 4;
	/**
	 * Space between the viewport and the right side of the widget.
	 */
	private static final int RIGHT_SPACING = 210;
	private static final int TICK_HEIGHT = 7;
	private static final int HALF_TICK = TICK_HEIGHT / 2;
	private static final int ROW_HEADING_WIDTH = 120;
	private static final int HEADING_ICON_OFFSET_X = SPACING;
	private static final int HEADING_ICON_SIZE = ROW_HEIGHT;
	private static final int HEADING_TEXT_MAX_WIDTH = ROW_HEADING_WIDTH - SPACING - HEADING_ICON_SIZE;
	private static final int HEADING_TEXT_OFFSET_X = HEADING_ICON_OFFSET_X + HEADING_ICON_SIZE + SPACING;
	private static final int VIEWPORT_OFFSET_X = SPACING + ROW_HEADING_WIDTH + SPACING;
	private static final int VIEWPORT_OFFSET_Y = SPACING + LABEL_HEIGHT + SPACING;
	
	private static final long MAX_INTERVAL = 10 * 365 * TimeUtils.MILLIS_PER_DAY;
	private static final long MIN_INTERVAL = 5 * TimeUtils.MILLIS_PER_SECOND;
	private static final Comparator<? super TimeBlock> TIME_BLOCK_COMPARATOR = 
		new Comparator<TimeBlock>()
		{
			@Override
			public int compare(TimeBlock arg0, TimeBlock arg1)
			{
				int diff = arg0.getStart().compareTo(arg1.getStart());
				if (diff == 0)
					diff = arg0.getEnd().compareTo(arg1.getEnd());
				return diff;
			}
		};
	
	private List<String> rowIds = new ArrayList<>();
	private Map<String, String> rowIcons = new HashMap<>();
	private Map<String, String> rowHeadings = new HashMap<>();
	private Map<String, SortedSet<TimeBlock>> rowTimeBlocks = new HashMap<>();
	
	private String timeZone = null;
	
	/**
	 * Visible interval start date.
	 */
	private Date startDate = null;
	
	/**
	 * Visible interval end date.
	 */
	private Date endDate = null;
	
	/**
	 * If <code>false</code>, will not render interval information in the top-left corner.
	 */
	boolean renderInterval = true;
	/**
	 * If <code>false</code>, will not render a popup.
	 */
	boolean renderPopup = true;
	
	/**
	 * The display 'scale'. It determines the dates format:
	 * <ul>
	 * <li>YEAR: <code>yyyy</code></li>
	 * <li>MONTH: <code>yyyy-MM</code></li>
	 * </ul>
	 * @author <a href="mailto:andrei.cojocaru@hansenhof.de">Andrei Cojocaru</a>
	 */
	protected enum Scale {
		YEAR,
		MONTH,
		DAY,
		HOUR,
		MINUTE,
		SECOND;
	}
	private Scale scale = null;
	
	Canvas canvas = null;
	
	private Context2d context;
	
	/**
	 * Mouse position to display the cursor line at.
	 * May be <code>null</code>; in such case, no cursor line will be displayed.<br/>
	 * Value is relative to canvas border.
	 */
	private Integer mouseMoveX = null;
	/**
	 * Mouse position to display the cursor line at.
	 * May be <code>null</code>; in such case, no cursor line will be displayed.<br/>
	 * Value is relative to canvas border.
	 */
	private Integer mouseMoveY = null;
	/**
	 * Time representing the current mouse position.
	 */
	private Long mouseMoveTime = null;
	
	/**
	 * X offset of last drag.
	 */
	private Integer dragX = null;

	/**
	 * ID of the row being hovered by mouse.
	 */
	private String mouseRow = null;
	
	public Timeline()
	{
		if (!Canvas.isSupported())
			throw new UnsupportedOperationException("Canvas is not supported");
		
		canvas = Canvas.createIfSupported();
		context = canvas.getContext2d();
		
		// Draw on attach (no size before that):
		canvas.addAttachHandler(new AttachEvent.Handler()
		{
			@Override
			public void onAttachOrDetach(AttachEvent event)
			{
				if (event.isAttached())
					redraw();
			}
		});
		
		canvas.addMouseWheelHandler(new MouseWheelHandler()
		{
			@Override
			public void onMouseWheel(MouseWheelEvent event)
			{
				if (mouseMoveTime == null)
					return;
				
				long oldInterval = TimeUtils.intervalInMillis(startDate, endDate);
				
				long newInterval = event.isNorth() ? (long) (oldInterval * .5) : (long) (oldInterval * 2);
				
				if (newInterval < MIN_INTERVAL)
					newInterval = MIN_INTERVAL;
				
				if (newInterval > MAX_INTERVAL)
					newInterval = MAX_INTERVAL;
				
				int mouseOffsetX = mouseMoveX - VIEWPORT_OFFSET_X;
				long mouseOffsetMillis = newInterval * mouseOffsetX / getViewportWidth();
				startDate = new Date(mouseMoveTime - mouseOffsetMillis);
				endDate = new Date(startDate.getTime() + newInterval);
				
				redraw();
				
				fireIntervalChangeEvent();
			}
		});
		canvas.addMouseMoveHandler(new MouseMoveHandler()
		{
			@Override
			public void onMouseMove(MouseMoveEvent event)
			{
//				Date dStart = new Date();

				int rowsCount = rowIds.size();
				
				int maxX = getViewportMaxX()
					, maxY = VIEWPORT_OFFSET_Y + rowsCount * (SPACING + ROW_HEIGHT);
				
				mouseMoveX = null;
				mouseMoveY = null;
				mouseMoveTime = null;
				mouseRow = null;
				
				if (event.getX() >= VIEWPORT_OFFSET_X 
					&& event.getX() <= maxX
					&& event.getY() >= VIEWPORT_OFFSET_Y
					&& event.getY() <= maxY)
				{
					mouseMoveX = event.getX();
					mouseMoveY = event.getY();
					
					int rowIndex = (mouseMoveY - VIEWPORT_OFFSET_Y) / (SPACING + ROW_HEIGHT);
					int rowStart = VIEWPORT_OFFSET_Y + rowIndex * (SPACING + ROW_HEIGHT) + SPACING;
					
					if (event.getY() >= rowStart)
					{	// Mouse is over a row
						mouseMoveTime = getTimeFromX(mouseMoveX);
						mouseRow = rowIds.get(rowIndex);
					}
					// Otherwise it's over a spacing.
				}
				
				// DnD management:
				if (dragX != null)
				{
					if (event.getX() < VIEWPORT_OFFSET_X || event.getX() > getViewportMaxX()
						|| event.getY() < VIEWPORT_OFFSET_Y || event.getY() > getViewportMaxY())
					{
						// mouse outside the viewport; drag ended
						dragX = null;
					}
					else
					{
						// we're dragging
						int deltaPx = dragX - event.getX();
						long deltaMillis = TimeUtils.intervalInMillis(startDate, endDate) * deltaPx / getViewportWidth();
						// we modify the interval with setInterval(), which will fire an IntervalChangeEvent.
						setInterval(
								new Date(startDate.getTime() + deltaMillis), 
								new Date(endDate.getTime() + deltaMillis), 
								false);
						dragX = event.getX();
					}
				}
				
				redraw();
				
//				GWT.log("Firing BrowseEvent");
				handlerManager.fireEvent(new BrowseEvent(mouseRow, mouseMoveTime == null ? null : new Date(mouseMoveTime)));
//				GWT.log("onmousemove processed in " + TimeUtils.intervalInMillis(dStart, new Date()));
			}
		});
		canvas.addMouseDownHandler(new MouseDownHandler()
		{
			@Override
			public void onMouseDown(MouseDownEvent event)
			{
				// start drag state if in bounds:
				if (event.getX() >= VIEWPORT_OFFSET_X && event.getY() <= getViewportMaxX()
					&& event.getY() >= VIEWPORT_OFFSET_Y && event.getY() <= getViewportMaxY())
				{
					dragX = event.getX();
				}
			}
		});
		canvas.addMouseUpHandler(new MouseUpHandler()
		{
			@Override
			public void onMouseUp(MouseUpEvent event)
			{
				// stop drag state:
				dragX = null;
			}
		});
	}
	
	private TimeBlock findBlock(String rowId, long millis)
	{
		Iterator<TimeBlock> it = rowTimeBlocks.get(rowId).iterator();
		boolean stop = false;
		TimeBlock block = null;
		while (it.hasNext() && block == null && !stop)
		{
			TimeBlock current = it.next();
			if (millis >= current.getStart().getTime() && millis <= current.getEnd().getTime())
				block = current;
			else if (millis < current.getStart().getTime())	// stop because the collection is ordered
				stop = true;
		}
		return block;
	}
	
	private int getViewportMaxX()
	{
		return canvas.getCoordinateSpaceWidth() - RIGHT_SPACING;
	}
	
	private int getViewportMaxY()
	{
		return canvas.getCoordinateSpaceHeight()
				- SPACING - LABEL_HEIGHT 	// 'fronteer' label
				- SPACING - LABEL_HEIGHT	// 'tick' label
				- SPACING - HALF_TICK;
	}
	
	/**
	 * @return current viewport width.
	 */
	private int getViewportWidth()
	{
		return getViewportMaxX() - VIEWPORT_OFFSET_X;
	}
	
	/**
	 * @return current viewport height.
	 */
	private int getViewportHeight()
	{
		return canvas.getCoordinateSpaceHeight() 
				- VIEWPORT_OFFSET_Y
				- SPACING - LABEL_HEIGHT	// fronteer label
				- SPACING - LABEL_HEIGHT	// tick label
				- SPACING - HALF_TICK;
	}
	
	/**
	 * Redraws the component.
	 */
	public void redraw()
	{
		canvas.setCoordinateSpaceHeight(canvas.getOffsetHeight());
		canvas.setCoordinateSpaceWidth(canvas.getOffsetWidth());
		
		drawBackground();
		
		if (renderInterval)
			drawInterval();
		
		drawViewportBorders();
		drawTimeAxis();
		drawRows();
		
		// Eventually draw cursor:
		if (mouseMoveTime != null)
			drawCursor();
		
		// Eventually draw popup:
		if (renderPopup && mouseMoveTime != null)
			drawPopup();
	}

	private void drawCursor()
	{
		context.beginPath();
		context.setStrokeStyle("blue");
		context.moveTo(mouseMoveX, VIEWPORT_OFFSET_Y);
		context.lineTo(mouseMoveX, getViewportMaxY());
		context.stroke();
	}

	private void drawPopup()
	{
		String date = jsGetDateTimeFormat(mouseMoveTime, timeZone);
		TextMetrics dateMeasure = context.measureText(date);
		int x = mouseMoveX + SPACING
			, y = mouseMoveY - SPACING
			, w = (int) (SPACING + dateMeasure.getWidth() + SPACING)
			, h = SPACING + LABEL_HEIGHT + SPACING;
		// popup background:
		context.setFillStyle("white");
		context.fillRect(x, y, w, h);
		// popup border:
		TimeBlock mouseBlock = findBlock(mouseRow, mouseMoveTime);
		String color = mouseBlock == null ? "gray" : mouseBlock.getColor();
		context.setLineWidth(2.0);
		context.setStrokeStyle(color);
		context.strokeRect(x, y, w, h);
		// text:
		context.setFillStyle("black");
		context.setTextBaseline(TextBaseline.TOP);
		context.fillText(date, x + SPACING, y + SPACING + 3);
	}
	
	/**
	 * Sets the icon for a row. Does not redraw the widget.
	 * @param rowId row identifier
	 * @param url icon URL.
	 */
	public void setRowIcon(String rowId, String url)
	{
		ensureHasRowId(rowId);
		
		rowIcons.put(rowId, url);
	}
	
	/**
	 * Sets the label for a row. Does not redraw the widget.
	 * @param rowId row identifier
	 * @param text row label
	 */
	public void setRowHeading(String rowId, String text)
	{
		ensureHasRowId(rowId);
		
		rowHeadings.put(rowId, text);
	}
	
	private void drawViewportBorders()
	{
		context.beginPath();
		context.setStrokeStyle("rgba(0,0,0,0.5)");
		context.strokeRect(VIEWPORT_OFFSET_X, VIEWPORT_OFFSET_Y, 
							getViewportWidth(), getViewportHeight());
	}
	
	public int getPreferredHeight(int rowCount)
	{
		return VIEWPORT_OFFSET_Y
				+ (SPACING + ROW_HEIGHT) * rowCount	// rows + their spacing on top
				+ SPACING	// spacing under latest row
				+ TICK_HEIGHT
				+ SPACING + LABEL_HEIGHT	// tick label
				+ SPACING + LABEL_HEIGHT	// fronteer label
				+ SPACING;
				
	}
	
	private void drawRows()
	{
		if (startDate == null || endDate == null)
			return;
		
		int viewportWidthPx = getViewportWidth();
		long viewportWidthMillis = TimeUtils.intervalInMillis(startDate, endDate);
		
		for (int i = 0; i < rowIds.size(); i++)
		{
			String rowId = rowIds.get(i);
			
			final int offsetY = 
					VIEWPORT_OFFSET_Y
					+ i * (SPACING + ROW_HEIGHT)
					+ SPACING;
					// This is the way of drawing them bottom-top:
//					canvas.getCoordinateSpaceHeight() 
//					- SPACING - LABEL_HEIGHT	// 'fronteer' label
//					- SPACING - LABEL_HEIGHT	// 'tick' label
//					- SPACING - TICK_HEIGHT	// tick indicator
//					- (rowIds.size() - i) * ROWS_SPACING - (rowIds.size() - i) * ROW_HEIGHT;
			
			// Gray background:
			context.setFillStyle("rgba(150, 150, 150, 0.8)");
			context.fillRect(SPACING, offsetY, ROW_HEADING_WIDTH + SPACING + viewportWidthPx, ROW_HEIGHT);
			
			// icon:
			if (rowIcons.containsKey(rowId))
			{
				String url = rowIcons.get(rowId);
				final Image image = new Image();
				image.setVisible(false);
				image.addLoadHandler(new LoadHandler()
				{
					@Override
					public void onLoad(LoadEvent event)
					{
						context.drawImage((ImageElement) image.getElement().cast(), 
								HEADING_ICON_OFFSET_X, offsetY, ROW_HEIGHT, ROW_HEIGHT);
						RootPanel.get().remove(image);
					}
				});
				RootPanel.get().add(image);
				image.setUrl(url);
			}
			
			// text:
			int textOffsetY = offsetY + (ROW_HEIGHT - 11) / 2;
			if (rowHeadings.containsKey(rowId))
			{
				String text = rowHeadings.get(rowId);
//				context.setFont("normal normal normal normal medium 16px serif");
				context.setFont("11px serif");
				context.setTextBaseline(TextBaseline.TOP);
				context.setFillStyle("black");
				context.fillText(text, HEADING_TEXT_OFFSET_X, textOffsetY, HEADING_TEXT_MAX_WIDTH);
				context.setStrokeStyle("black");
				context.strokeText(text, HEADING_TEXT_OFFSET_X, textOffsetY, HEADING_TEXT_MAX_WIDTH);
			}
			
			// blocks:
			if (rowTimeBlocks.containsKey(rowId))
			{
				Iterator<TimeBlock> it = rowTimeBlocks.get(rowId).iterator();
				boolean stop = false;
				while (!stop && it.hasNext())
				{
					TimeBlock block = it.next();
					
					if (!block.getEnd().before(startDate)
						&& !block.getStart().after(endDate))
					{
						Date blockStart = block.getStart().before(startDate) ? startDate : block.getStart()
								, blockEnd = block.getEnd().after(endDate) ? endDate : block.getEnd();
							
							long timeBlockWidthMillis = TimeUtils.intervalInMillis(blockStart, blockEnd);
							int timeBlockWidthPx = (int) (((double) timeBlockWidthMillis / viewportWidthMillis) * viewportWidthPx);
							if (timeBlockWidthPx < 1)
								timeBlockWidthPx = 1;
							
							int offsetPx = getXFromTime(blockStart.getTime());
							
							if (block.getColor() != null)
								context.setFillStyle(block.getColor());
							context.fillRect(offsetPx, offsetY, timeBlockWidthPx, ROW_HEIGHT);
					}
					
					// stop condition (blocks structure is sorted):
					stop = block.getStart().after(endDate);
				}
			}
		}
	}

	/**
	 * @param offsetPx X coordinate in the Canvas.
	 * @return
	 */
	private long getTimeFromX(int offsetPx)
	{
		offsetPx -= VIEWPORT_OFFSET_X;
		long offsetMillis = TimeUtils.intervalInMillis(startDate, endDate);
		return startDate.getTime() + offsetMillis * offsetPx / getViewportWidth();
	}
	
	/**
	 * @param millis time in millis
	 * @return X offset for the given time in millis
	 */
	private int getXFromTime(long millis)
	{
		long deltaMillis = millis - startDate.getTime()
			, widthMillis = TimeUtils.intervalInMillis(startDate, endDate);
		
		return VIEWPORT_OFFSET_X + (int) ((double) deltaMillis * getViewportWidth() / widthMillis);
	}
	
	private void drawInterval()
	{
		if (startDate == null || endDate == null)
			return;
		
		StringBuilder sb = new StringBuilder();
		sb.append(jsGetDateTimeFormat((double) startDate.getTime(), timeZone))
			.append(" - ")
			.append(jsGetDateTimeFormat((double) endDate.getTime(), timeZone));
		
		context.setStrokeStyle("black");
		context.setTextBaseline(TextBaseline.TOP);
		context.strokeText(sb.toString(), SPACING, SPACING);
	}
	
	private native String jsGetDateTimeFormat(double time, String timezone)
	/*-{
		var date = $wnd.createDate(time, timezone);
		
		return $wnd.formatYYYYMMDDHHmmss(date);
	}-*/;

	private void drawTimeAxis()
	{
		if (startDate == null || endDate == null)
			return;
		
		int heightPx = canvas.getCoordinateSpaceHeight();
		
		long delta = TimeUtils.intervalInMillis(startDate, endDate);
		resolveScale(delta);
		String scaleString = scale.toString();
		
		// Draw the time axis:
		int viewportWidth = getViewportWidth()
			, timeAxisOffsetY = heightPx 
								- SPACING - LABEL_HEIGHT 	// 'fronteer' label
								- SPACING - LABEL_HEIGHT	// 'tick' label
								- SPACING - HALF_TICK;
		context.beginPath();
		context.setLineWidth(1.0);
		context.setStrokeStyle("rgba(0,0,0,1.0)");
		context.moveTo(VIEWPORT_OFFSET_X, timeAxisOffsetY);
		context.lineTo(VIEWPORT_OFFSET_X + viewportWidth, timeAxisOffsetY);
		context.stroke();
		
		// Draw the ticks:
		context.setLineWidth(0.25);
		context.setStrokeStyle("black");
		context.setTextBaseline(TextBaseline.BOTTOM);
		
		JsArray<JsDate> ticks = jsComputeTicks(
			(double) startDate.getTime(), 
			(double) endDate.getTime(),
			this.timeZone,
			scaleString);
		
		int yTick = heightPx 
						- SPACING - LABEL_HEIGHT  	// 'fronteer' label
						- SPACING - LABEL_HEIGHT 	// 'tick' label
						- SPACING - TICK_HEIGHT
			, yTickLabel = heightPx - SPACING - LABEL_HEIGHT - SPACING - LABEL_HEIGHT
			, yFronteerLabel = heightPx - SPACING - LABEL_HEIGHT;
		
		for (int i = 0; i < ticks.length(); i++)
		{
			JsDate tick = ticks.get(i);
			int tickOffsetX = getXFromTime((long) tick.getTime())
				, labelOffsetX = tickOffsetX + SPACING;
			
			boolean isFronteer = jsIsFronteer(tick, scaleString);
			
			// the vertical line:
			context.beginPath();
			if (isFronteer)
			{
				context.setStrokeStyle("rgba(0, 0, 0, 0.9)");
				context.moveTo(tickOffsetX, VIEWPORT_OFFSET_Y);
				context.lineTo(tickOffsetX, yFronteerLabel);
			}
			else
			{
				context.setStrokeStyle("rgba(0, 0, 0, 1.0");
				context.moveTo(tickOffsetX, yTick);
				context.lineTo(tickOffsetX, yTick + TICK_HEIGHT);
			}
			context.stroke();
			// the tick label:
			String tickLabel = jsGetTickLabel(tick, scaleString);
			context.setTextBaseline(TextBaseline.TOP);
			context.setFillStyle("black");
			context.fillText(tickLabel, labelOffsetX, yTickLabel);
			context.setStrokeStyle("black");
			context.strokeText(tickLabel, labelOffsetX, yTickLabel);
			// the fronteer label:
			if (isFronteer)
			{
				String fronteerLabel = jsGetFronteerLabel(tick, scaleString);
				context.setTextBaseline(TextBaseline.TOP);
				context.setFillStyle("black");
				context.fillText(fronteerLabel, labelOffsetX, yFronteerLabel);
				context.setStrokeStyle("black");
				context.strokeText(fronteerLabel, labelOffsetX, yFronteerLabel);
			}
		}
	}
	
	private native boolean jsIsFronteer(JsDate tick, String scale)
	/*-{
		return $wnd.isFronteer(tick, scale);
	}-*/;
	
	private native String jsGetTickLabel(JsDate tick, String scale)
	/*-{
		var label;
		switch(scale) {
			case "YEAR":
				label = $wnd.formatYYYY(tick);
				break;
			case "MONTH":
				label = $wnd.formatMM(tick);
				break;
			case "DAY":
				label = $wnd.formatDD(tick);
				break;
			case "HOUR":
				label = $wnd.formatHHmm(tick);
				break;
			case "MINUTE":
				label = $wnd.formatHHmm(tick);
				break;
			case "SECOND":
				label = $wnd.formatss(tick);
				break;
		}
		return label;
	}-*/;

	private native String jsGetFronteerLabel(JsDate tick, String scale)
	/*-{
		var label;
		switch(scale) {
			case "YEAR":
				label = ""; // no fronteers when displaying years
				break;
			case "MONTH":
				label = $wnd.formatYYYY(tick);
				break;
			case "DAY":
				label = $wnd.formatYYYYMM(tick);
				break;
			case "HOUR":
				label = $wnd.formatYYYYMMDD(tick);
				break;
			case "MINUTE":
//				label = $wnd.formatHH(tick);
				label = "";
				break;
			case "SECOND":
				label = $wnd.formatHHmm(tick);
				break;
		}
		return label;
	}-*/;
	
	private String getAxisLabel(Date tick)
	{
		String format = null;
		switch(scale)
		{
		case YEAR:
			format = "yyyy";
			break;
		case MONTH:
			format = "yyyy-MM";
			break;
		case DAY:
			format = "yyyy-MM-dd";
			break;
		case HOUR:
			format = "HH:00";
			break;
		case MINUTE:
			format = "HH:mm";
			break;
		case SECOND:
			format = "mm:ss";
			break;
		}
		
		DateTimeFormat dtf = DateTimeFormat.getFormat(format);
		return dtf.format(tick);
	}
	
	private native JsArray<JsDate> jsComputeTicks(double startTime, double endTime, String timeZone, String scale)
	/*-{
		return $wnd.computeTicks(startTime, endTime, timeZone, scale);
	}-*/;

	private void resolveScale(long delta)
	{
		if (delta < 3 * TimeUtils.MILLIS_PER_MINUTE)	// 2 minutes
			scale = Scale.SECOND;
		else if (delta < 3 * TimeUtils.MILLIS_PER_HOUR)	// 2 hours
			scale = Scale.MINUTE;
		else if (delta < 3 * TimeUtils.MILLIS_PER_DAY)	// 2 days
			scale = Scale.HOUR;
		else if (delta < 40 * TimeUtils.MILLIS_PER_DAY)	// ~1.5 month
			scale = Scale.DAY;
		else if (delta < 1200 * TimeUtils.MILLIS_PER_DAY)	// ~2 years
			scale = Scale.MONTH;
		else
			scale = Scale.YEAR;
	}

	private void drawBackground()
	{
		// draw background:
		context.setFillStyle("#ffffff");
		
		int width = canvas.getCoordinateSpaceWidth()
			, height = canvas.getCoordinateSpaceHeight();
		
		context.fillRect(0, 0, width, height);
	}
	
	public void clear()
	{
		rowHeadings.clear();
		rowIds.clear();
		rowIcons.clear();
		rowTimeBlocks.clear();
		
		mouseRow = null;
		mouseMoveTime = null;
		mouseMoveX = null;
		mouseMoveY = null;
		
		redraw();
	}
	
	public void addTimeBlock(TimeBlock timeBlock, boolean redraw)
	{
		addTimeBlock(null, timeBlock, redraw);
	}
	
	public void addTimeBlock(String rowId, TimeBlock timeBlock, boolean redraw)
	{
		ensureHasRowId(rowId);
		
		if (!rowTimeBlocks.containsKey(rowId))
			rowTimeBlocks.put(rowId, new TreeSet<TimeBlock>(TIME_BLOCK_COMPARATOR));
		
		rowTimeBlocks.get(rowId).add(timeBlock);
		
		if (redraw && canvas.isAttached())
			redraw();
	}
	
	/**
	 * Eventually adds the given row ID into {@link #rowIds}.
	 * <code>null</code> values are translated into the string <code>"default"</code>.
	 * @param rowId row ID. May be <code>null</code>.
	 */
	private void ensureHasRowId(String rowId)
	{
		if (rowId == null)
			rowId = "default";
		
		if (!rowIds.contains(rowId))
			rowIds.add(rowId);
	}

	@Override
	public Canvas asWidget()
	{
		return canvas;
	}

	/**
	 * @param timezone the timezone to set
	 */
	public void setTimezone(String timezone)
	{
		setTimezone(timezone, true);
	}

	/**
	 * @param timezone the timezone to set
	 */
	public void setTimezone(String timezone, boolean redraw)
	{
		this.timeZone = timezone;
		
		if (redraw && canvas.isAttached())
			redraw();
	}
	
	/**
	 * Modifies the visible interval. Redraws the timeline.
	 * @param start start date
	 * @param end end date
	 */
	public void setInterval(Date start, Date end)
	{
		setInterval(start, end, true);
	}
	
	/**
	 * Modifies the visible interval.
	 * @param start start date
	 * @param end end date
	 * @param redraw if this value is <code>true</code> and the canvas is attached, redraws the widget 
	 */
	public void setInterval(Date start, Date end, boolean redraw)
	{
		this.startDate = start;
		this.endDate = end;
		
		if (redraw && canvas.isAttached())
			redraw();
		
		fireIntervalChangeEvent();
	}

	public Date getStartDate()
	{
		return startDate;
	}
	
	public Date getEndDate()
	{
		return endDate;
	}
	
	@Override
	public void onResize()
	{
		redraw();
	}
	
	public void setRenderPopup(boolean renderPopup)
	{
		this.renderPopup = renderPopup;
	}
	
	/**
	 * Indicates whether to render some text representing the interval, in the top-left corner of the canvas.
	 * Does not redraw the widget.
	 * @param render if <code>true</code>, will render text representing the interval.
	 */
	public void setRenderInterval(boolean render)
	{
		this.renderInterval = render;
	}

	/**
	 * Fires an {@link IntervalChangeEvent} with current interval endpoints {@link #startDate} and {@link #endDate}.
	 */
	private void fireIntervalChangeEvent()
	{
		handlerManager.fireEvent(new IntervalChangeEvent(startDate, endDate));
	}
	
	@Override
	public HandlerRegistration addIntervalChangeEventHandler(Handler handler)
	{
		return handlerManager.addHandler(IntervalChangeEvent.TYPE, handler);
	}

	@Override
	public HandlerRegistration addBrowseEventHandler(BrowseEvent.Handler handler)
	{
		return handlerManager.addHandler(BrowseEvent.TYPE, handler);
	}
}
