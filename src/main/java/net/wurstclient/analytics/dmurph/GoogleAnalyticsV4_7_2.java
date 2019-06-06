/**
 * Copyright (c) 2010 Daniel Murphy
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
/**
 * Created at Jul 20, 2010, 4:39:49 AM
 */
package net.wurstclient.analytics.dmurph;

import java.util.Random;

/**
 * http://code.google.com/apis/analytics/docs/tracking/gaTrackingTroubleshooting
 * .html#gifParameters
 *
 * @author Daniel Murphy
 *
 */
public class GoogleAnalyticsV4_7_2 implements IGoogleAnalyticsURLBuilder
{
	public static final String URL_PREFIX =
		"http://www.google-analytics.com/__utm.gif";
	
	private AnalyticsConfigData config;
	private Random random = new Random((long)(Math.random() * Long.MAX_VALUE));
	
	public GoogleAnalyticsV4_7_2(AnalyticsConfigData argConfig)
	{
		config = argConfig;
	}
	
	/**
	 * @see net.wurstclient.analytics.dmurph.IGoogleAnalyticsURLBuilder#getGoogleAnalyticsVersion()
	 */
	@Override
	public String getGoogleAnalyticsVersion()
	{
		return "4.7.2";
	}
	
	/**
	 * @see net.wurstclient.analytics.dmurph.IGoogleAnalyticsURLBuilder#buildURL(net.wurstclient.analytics.dmurph.AnalyticsRequestData)
	 */
	@Override
	public String buildURL(AnalyticsRequestData argData)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(URL_PREFIX);
		
		System.currentTimeMillis();
		
		sb.append("?utmwv=" + getGoogleAnalyticsVersion()); // version
		sb.append("&utmn=" + random.nextInt()); // random int so no caching
		
		if(argData.getHostName() != null)
			sb.append("&utmhn=" + getURIString(argData.getHostName())); // hostname
			
		if(argData.getEventAction() != null
			&& argData.getEventCategory() != null)
		{
			sb.append("&utmt=event");
			String category = getURIString(argData.getEventCategory());
			String action = getURIString(argData.getEventAction());
			
			sb.append("&utme=5(" + category + "*" + action);
			
			if(argData.getEventLabel() != null)
				sb.append("*" + getURIString(argData.getEventLabel()));
			sb.append(")");
			
			if(argData.getEventValue() != null)
				sb.append("(" + argData.getEventValue() + ")");
		}else if(argData.getEventAction() != null
			|| argData.getEventCategory() != null)
			throw new IllegalArgumentException(
				"Event tracking must have both a category and an action");
		
		if(config.getEncoding() != null)
			sb.append("&utmcs=" + getURIString(config.getEncoding())); // encoding
		else
			sb.append("&utmcs=-");
		if(config.getScreenResolution() != null)
			sb.append("&utmsr=" + getURIString(config.getScreenResolution())); // screen
		// resolution
		if(config.getColorDepth() != null)
			sb.append("&utmsc=" + getURIString(config.getColorDepth())); // color
		// depth
		if(config.getUserLanguage() != null)
			sb.append("&utmul=" + getURIString(config.getUserLanguage())); // language
		sb.append("&utmje=1"); // java enabled (probably)
		
		if(config.getFlashVersion() != null)
			sb.append("&utmfl=" + getURIString(config.getFlashVersion())); // flash
		// version
		
		if(argData.getPageTitle() != null)
			sb.append("&utmdt=" + getURIString(argData.getPageTitle())); // page
		// title
		
		sb.append("&utmhid=" + random.nextInt());
		
		if(argData.getPageURL() != null)
			sb.append("&utmp=" + getURIString(argData.getPageURL())); // page
		// url
		
		sb.append("&utmac=" + config.getTrackingCode()); // tracking code
		
		// cookie data
		// utmccn=(organic)|utmcsr=google|utmctr=snotwuh |utmcmd=organic
		String utmcsr = getURIString(argData.getUtmcsr());
		String utmccn = getURIString(argData.getUtmccn());
		String utmctr = getURIString(argData.getUtmctr());
		String utmcmd = getURIString(argData.getUtmcmd());
		String utmcct = getURIString(argData.getUtmcct());
		
		// yes, this did take a while to figure out
		int hostnameHash = hostnameHash(argData.getHostName());
		int visitorId = config.getVisitorData().getVisitorId();
		long timestampFirst = config.getVisitorData().getTimestampFirst();
		long timestampPrevious = config.getVisitorData().getTimestampPrevious();
		long timestampCurrent = config.getVisitorData().getTimestampCurrent();
		int visits = config.getVisitorData().getVisits();
		
		sb.append("&utmcc=__utma%3D" + hostnameHash + "." + visitorId + "."
			+ timestampFirst + "." + timestampPrevious + "." + timestampCurrent
			+ "." + visits + "%3B%2B__utmz%3D" + hostnameHash + "."
			+ timestampCurrent + ".1.1.utmcsr%3D" + utmcsr + "%7Cutmccn%3D"
			+ utmccn + "%7Cutmcmd%3D" + utmcmd
			+ (utmctr != null ? "%7Cutmctr%3D" + utmctr : "")
			+ (utmcct != null ? "%7Cutmcct%3D" + utmcct : "") + "%3B&gaq=1");
		return sb.toString();
	}
	
	/*
	 * page view url:
	 * http://www.google-analytics.com/__utm.gif
	 * ?utmwv=4.7.2
	 * &utmn=631966530
	 * &utmhn=www.dmurph.com
	 * &utmcs=ISO-8859-1
	 * &utmsr=1280x800
	 * &utmsc=24-bit
	 * &utmul=en-us
	 * &utmje=1
	 * &utmfl=10.1%20r53
	 * &utmdt=Hello
	 * &utmhid=2043994175
	 * &utmr=0
	 * &utmp=%2Ftest%2Ftest.php
	 * &utmac=UA-17109202-5
	 * &utmcc=__utma%3D143101472.2118079581.1279863622.1279863622.1279863622.1%
	 * 3B
	 * %
	 * 2B__utmz%3D143101472.1279863622.1.1.utmcsr%3D(direct)%7Cutmccn%3D(direct)
	 * %7Cutmcmd%3D(none)%3B&gaq=1
	 */
	
	// tracking url:
	/*
	 * http://www.google-analytics.com/__utm.gif
	 * ?utmwv=4.7.2
	 * &utmn=480124034
	 * &utmhn=www.dmurph.com
	 * &utmt=event
	 * &utme=5(Videos*Play)
	 * &utmcs=ISO-8859-1
	 * &utmsr=1280x800
	 * &utmsc=24-bit
	 * &utmul=en-us
	 * &utmje=1
	 * &utmfl=10.1%20r53
	 * &utmdt=Hello
	 * &utmhid=166062212
	 * &utmr=0
	 * &utmp=%2Ftest%2Ftest.php
	 * &utmac=UA-17109202-5
	 * &utmcc=__utma%3D143101472.2118079581.1279863622.1279863622.1279863622.1%
	 * 3B
	 * %
	 * 2B__utmz%3D143101472.1279863622.1.1.utmcsr%3D(direct)%7Cutmccn%3D(direct)
	 * %7Cutmcmd%3D(none)%3B&gaq=1
	 */
	
	private String getURIString(String argString)
	{
		if(argString == null)
			return null;
		return URIEncoder.encodeURI(argString);
	}
	
	private int hostnameHash(String hostname)
	{
		return 999;
	}
	
	/**
	 * @see net.wurstclient.analytics.dmurph.IGoogleAnalyticsURLBuilder#resetSession()
	 */
	@Override
	public void resetSession()
	{
		config.getVisitorData().resetSession();
	}
}
