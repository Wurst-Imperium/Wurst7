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
 * Created on Jul 20, 2010, 4:53:44 AM
 */
package net.wurstclient.analytics.dmurph;

/**
 * Tracking data that is pertinent to each individual tracking
 * request.
 *
 * @author Daniel Murphy
 */
public class AnalyticsRequestData
{
	
	private String pageTitle = null;
	private String hostName = null;
	private String pageURL = null;
	private String eventCategory = null;
	private String eventAction = null;
	private String eventLabel = null;
	private Integer eventValue = null;
	// utmcsr
	// Identifies a search engine, newsletter name, or other source specified in
	// the
	// utm_source query parameter See the "Marketing Campaign Tracking"
	// section for more information about query parameters.
	//
	// utmccn
	// Stores the campaign name or value in the utm_campaign query parameter.
	//
	// utmctr
	// Identifies the keywords used in an organic search or the value in the
	// utm_term query parameter.
	//
	// utmcmd
	// A campaign medium or value of utm_medium query parameter.
	//
	// utmcct
	// Campaign content or the content of a particular ad (used for A/B testing)
	// The value from utm_content query parameter.
	// referal:
	// utmcsr=forums.jinx.com|utmcct=/topic.asp|utmcmd=referral
	// utmcsr=rolwheels.com|utmccn=(referral)|utmcmd=referral|utmcct=/rol_dhuez_wheels.php
	// search:
	// utmcsr=google|utmccn=(organic)|utmcmd=organic|utmctr=rol%20wheels
	
	// utmcsr%3D(direct)%7Cutmccn%D(direct)%7utmcmd%3D(none)
	private String utmcsr = "(direct)";
	private String utmccn = "(direct)";
	private String utmctr = null;
	private String utmcmd = "(none)";
	private String utmcct = null;
	
	public void setReferrer(String argSite, String argPage)
	{
		utmcmd = "referral";
		utmcct = argPage;
		utmccn = "(referral)";
		utmcsr = argSite;
		utmctr = null;
	}
	
	public void setSearchReferrer(String argSearchSource,
		String argSearchKeywords)
	{
		utmcsr = argSearchSource;
		utmctr = argSearchKeywords;
		utmcmd = "organic";
		utmccn = "(organic)";
		utmcct = null;
	}
	
	/**
	 * @return the utmcsr
	 */
	public String getUtmcsr()
	{
		return utmcsr;
	}
	
	/**
	 * @return the utmccn
	 */
	public String getUtmccn()
	{
		return utmccn;
	}
	
	/**
	 * @return the utmctr
	 */
	public String getUtmctr()
	{
		return utmctr;
	}
	
	/**
	 * @return the utmcmd
	 */
	public String getUtmcmd()
	{
		return utmcmd;
	}
	
	/**
	 * @return the utmcct
	 */
	public String getUtmcct()
	{
		return utmcct;
	}
	
	/**
	 * @return the eventAction
	 */
	public String getEventAction()
	{
		return eventAction;
	}
	
	/**
	 * @return the eventCategory
	 */
	public String getEventCategory()
	{
		return eventCategory;
	}
	
	/**
	 * @return the eventLabel
	 */
	public String getEventLabel()
	{
		return eventLabel;
	}
	
	/**
	 * @return the eventValue
	 */
	public Integer getEventValue()
	{
		return eventValue;
	}
	
	/**
	 * @return the hostName
	 */
	public String getHostName()
	{
		return hostName;
	}
	
	/**
	 * @return the contentTitle
	 */
	public String getPageTitle()
	{
		return pageTitle;
	}
	
	/**
	 * @return the pageURL
	 */
	public String getPageURL()
	{
		return pageURL;
	}
	
	/**
	 * Sets the event action, which is required for
	 * tracking events.
	 *
	 * @param argEventAction
	 *            the eventAction to set
	 */
	public void setEventAction(String argEventAction)
	{
		eventAction = argEventAction;
	}
	
	/**
	 * Sets the event category, which is required for
	 * tracking events.
	 *
	 * @param argEventCategory
	 *            the eventCategory to set
	 */
	public void setEventCategory(String argEventCategory)
	{
		eventCategory = argEventCategory;
	}
	
	/**
	 * Sets the event label, which is optional for
	 * tracking events.
	 *
	 * @param argEventLabel
	 *            the eventLabel to set
	 */
	public void setEventLabel(String argEventLabel)
	{
		eventLabel = argEventLabel;
	}
	
	/**
	 * Sets the event value, which is optional for tracking
	 * events.
	 *
	 * @param argEventValue
	 *            the eventValue to set
	 */
	public void setEventValue(Integer argEventValue)
	{
		eventValue = argEventValue;
	}
	
	/**
	 * The host name of the page
	 *
	 * @param argHostName
	 *            the hostName to set
	 */
	public void setHostName(String argHostName)
	{
		hostName = argHostName;
	}
	
	/**
	 * Sets the page title, which will be the Content Title
	 * in Google Analytics
	 *
	 * @param argContentTitle
	 *            the contentTitle to set
	 */
	public void setPageTitle(String argContentTitle)
	{
		pageTitle = argContentTitle;
	}
	
	/**
	 * The page url, which is required. Traditionally
	 * this is of the form "/content/page.html", but you can
	 * put anything here (like "/com/dmurph/test.java").
	 *
	 * @param argPageURL
	 *            the pageURL to set
	 */
	public void setPageURL(String argPageURL)
	{
		pageURL = argPageURL;
	}
}
