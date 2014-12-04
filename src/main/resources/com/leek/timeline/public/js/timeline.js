
var MILLIS_PER_SECOND = 1000;
var MILLIS_PER_MINUTE = 60000;
var MILLIS_PER_HOUR = 3600000;
var MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;

/**
 * 
 * @param startTime
 * @param endTime
 * @param timeZone
 * @param scale
 * @returns {Array of Date}
 */
function computeTicks(startTime, endTime, timeZone, scale) {
	var ticks;
	
	switch(scale)
	{
		case "SECOND":
			var seconds = intervalInSeconds(startTime, endTime);
			var step = 1 + Math.floor(seconds / 10);
			var steps = [1, 2, 5, 10, 15, 20, 30];
			var delta = findStep(steps, step) * MILLIS_PER_SECOND;
			
			var startDate = createDate(startTime, timeZone);
			startDate.setSeconds(0, 0);
			
			ticks = generateTicks(startDate, startTime, endTime, timeZone, delta);
			break;
		case "MINUTE":
			var minutes = intervalInMinutes(startTime, endTime);
			var step = 1 + Math.floor(minutes / 10);
			var steps = [1, 2, 5, 10, 20, 30];
			var delta = findStep(steps, step) * MILLIS_PER_MINUTE;
			
			var startDate = createDate(startTime, timeZone);
			startDate.setMinutes(0, 0, 0);
			
			ticks = generateTicks(startDate, startTime, endTime, timeZone, delta);
			break;
		case "HOUR":
			var hours = intervalInHours(startTime, endTime);
			var step = 1 + Math.floor(hours / 10);
			var steps = [1, 2, 6, 12];
			var delta = findStep(steps, step) * MILLIS_PER_HOUR;
			
			var startDate = createDate(startTime, timeZone);
			startDate.setHours(0, 0, 0, 0);
			
			ticks = [];
			var date = startDate.getDate();	// day in month
			var currentTime = startDate.getTime();
			while (currentTime < endTime) {
				if (currentTime >= startTime) {
					currentDate = createDate(currentTime, timeZone);
					if (currentDate.getDate() != date) {	// day changed, ensure tick is fronteer:
						currentDate.setHours(0, 0, 0, 0);
						date = currentDate.getDate();
					}
					ticks.push(currentDate);
				}
				currentTime = currentTime + delta;
			}
			break;
		case "DAY":
			var days = intervalInDays(startTime, endTime);
			var step = 1 + Math.floor(days / 10);
			var steps = [1, 2, 5, 10, 15];
			step = findStep(steps, step);

			var startDate = createDate(startTime, timeZone);
			startDate.setDate(1);
			startDate.setHours(0, 0, 0, 0);
			
			ticks = [];
			var currentTime = startDate.getTime();
			while (currentTime <= endTime) {
				if (currentTime >= startTime)
					ticks.push(createDate(currentTime, timeZone));

				currentDate = createDate(currentTime, timeZone);
				currentDate.setDate(currentDate.getDate() + step);
				currentDate.setHours(0, 0, 0, 0);
				currentTime = currentDate.getTime();
			}
			break;
		case "MONTH":
			var startDate = createDate(startTime, timeZone);
			var endDate = createDate(endTime, timeZone);
			var months = 12 * (endDate.getFullYear() - startDate.getFullYear()) - startDate.getMonth() + endDate.getMonth();
			
			var step = 1 + months / 10;
			var steps = [1, 2, 3, 4, 6, 12];
			var step = findStep(steps, step);
			
			startDate.setMonth(0, 1);
			startDate.setHours(0, 0, 0, 0);
			
			ticks = [];
			var currentTime = startDate.getTime();
			var currentDate = createDate(currentTime, timeZone);
			while (currentTime < endTime) {
				if (currentTime > startTime)
					ticks.push(createDate(currentTime, timeZone));
				currentDate.setMonth(currentDate.getMonth() + step);
				currentTime = currentDate.getTime();
			}
			break;
		case "YEAR":
			var startDate = createDate(startTime, timeZone);
			var endDate = createDate(endTime, timeZone);
			var years = endDate.getFullYear() - startDate.getFullYear();
			var step = Math.ceil(years / 10);
			
			startDate.setMonth(0, 1);
			startDate.setHours(0, 0, 0, 0);

			ticks = [];
			var currentTime = startDate.getTime();
			var currentDate = createDate(currentTime, timeZone);
			while (currentTime < endTime) {
				if (currentTime > startTime)
					ticks.push(createDate(currentTime, timeZone));
				currentDate.setFullYear(currentDate.getFullYear() + delta);
				currentTime = currentDate.getTime();
			}
			break;
	}
	
	return ticks;
}

function findStep(steps, value) {
	var found = false;
	var i = 0;
	while (i < steps.length && !found) {
		found = value <= steps[i];
		i++;
	}
	return steps[i - 1];
}

/**
 * Generates equidistant ticks in a given interval.
 * @param start start point to generate from
 * @param min {Number or Date} interval start date
 * @param max {Number or Date} interval end date
 * @param timeZone {string} optional timezone
 * @param delta {number} step in millis
 */
function generateTicks(start, min, max, timeZone, delta) {
	var ticks = [];
	
	var minTime = typeof min == "number" ? min : min.getTime();
	var maxTime = typeof max == "number" ? max : max.getTime();
	var currentTime = typeof start == "number" ? start : start.getTime();
	
	while (currentTime < maxTime) {
		if (currentTime >= minTime)
			ticks.push(createDate(currentTime, timeZone));
		currentTime = currentTime + delta;
	}
		
	return ticks;
}

/**
 * Builds a Date object. If timezoneJS is in use and a timeZone is provided,
 * then a timezone-aware Date will be returned.
 * @param time {Number} milliseconds
 * @param timeZone {string} timezone info, or null
 * @returns {Date}
 */
function createDate(time, timeZone) {
	if (typeof timezoneJS != "undefined" && timeZone)
		return new timezoneJS.Date(time, timeZone);
	return new Date(time);
}

/**
 * Determines whether the given tick is a "time window fronteer" for the given scale.
 * Examples:
 * if scale is MONTH, fronteers are all timestamps on January 1st, 00:00:00 (year changes)
 * if scale is MINUTE, fronteers are all timesamps where hour is 00 (hour changes)
 * if scale is SECOND, fronteers are all timestamps where second is 00 (minute changes)
 * @param tick {Date} the tick
 * @param scale display scale
 */
function isFronteer(tick, scale) {
	switch(scale) {
	case "YEAR":
		return false;
	case "MONTH":
		return tick.getMonth() == 0 && tick.getDate() == 1;
	case "DAY":
		return tick.getDate() == 1;
	case "HOUR":
//		return tick.getHours() == 0;	// same as MINUTE
	case "MINUTE":
		return tick.getHours() == 0 && tick.getMinutes() == 0;
	case "SECOND":
		return tick.getSeconds() == 0;
	}
}

/**
 * @param start {Number or Date} start time, as millis or Date
 * @param end {Number or Date} end time, as millis or Date
 * @returns {Number} interval in millis
 */
function intervalInMillis(start, end) {
	var startTime = typeof start == "number" ? start : start.getTime();
	var endTime = typeof end == "number" ? end : end.getTime();
	return Math.abs(startTime - endTime);
}

/**
 * @param start {Number or Date} start time, as millis or Date
 * @param end {Number or Date} end time, as millis or Date
 * @returns {Number} interval in seconds
 */
function intervalInSeconds(start, end) {
	return Math.floor(intervalInMillis(start, end) / MILLIS_PER_SECOND);
}

/**
 * @param start {Number or Date} start time, as millis or Date
 * @param end {Number or Date} end time, as millis or Date
 * @returns {Number} interval in minutes
 */
function intervalInMinutes(start, end) {
	return Math.floor(intervalInMillis(start, end) / MILLIS_PER_MINUTE);
}

/**
 * @param start {Number or Date} start time, as millis or Date
 * @param end {Number or Date} end time, as millis or Date
 * @returns {Number} interval in hours
 */
function intervalInHours(start, end) {
	return Math.floor(intervalInMillis(start, end) / MILLIS_PER_HOUR);
}

/**
 * @param start {Number or Date} start time, as millis or Date
 * @param end {Number or Date} end time, as millis or Date
 * @returns {Number} interval in days
 */
function intervalInDays(start, end) {
	return Math.floor(intervalInMillis(start, end) / MILLIS_PER_DAY);
}

function formatYYYY(date) {
	return date.getFullYear().toString();
}

function formatYYYYMM(date) {
	return date.getFullYear() + " - " + formatMM(date);
}

function formatYYYYMMDD(date) {
	return formatYYYYMM(date) + " - " + formatDD(date);
}

function formatYYYYMMDDHHmm(date) {
	return formatYYYYMMDD(date) + " " + formatHHmm(date);
}

function formatYYYYMMDDHHmmss(date) {
	return formatYYYYMMDD(date) + " " + formatHHmmss(date);
}

function formatMM(date) {
	return format2Digits(date.getMonth() + 1);
}

function formatMMDD(date) {
	return formatMM(date) + " - " + formatDD(date);
}

function formatMMDDHH(date) {
	return formatMMDD(date) + " " + formatHH(date);
}

function formatMMDDHHmm(date) {
	return formatMMDD(date) + " " + formatHHmm(date);
}

function formatDD(date) {
	return format2Digits(date.getDate());
}

function formatDDHHmm(date) {
	return formatDD(date) + " " + formatHHmm(date);
}

function formatHH(date) {
	return format2Digits(date.getHours());
}

function formatHHmm(date) {
	return formatHH(date) + ":" + formatmm(date);
}

function formatHHmmss(date) {
	return formatHH(date) + ":" + formatmm(date) + ":" + formatss(date);
}

function formatmm(date) {
	return format2Digits(date.getMinutes());
}

function formatss(date) {
	return format2Digits(date.getSeconds());
}

function format2Digits(n) {
	if (n < 10)
		return "0" + n;
	return n.toString();
}
