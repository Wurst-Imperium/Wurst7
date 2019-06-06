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
 * Created at Jul 22, 2010, 11:37:36 PM
 */
package net.wurstclient.analytics.dmurph;

/**
 * Data that is client-specific, and should be common for all tracking requests.
 * For convenience most of this data is populated automatically by
 * {@link #populateFromSystem()}.
 *
 * @author Daniel Murphy
 *
 */
public class AnalyticsConfigData
{
	
	private final String trackingCode;
	private String encoding = "UTF-8";
	private String screenResolution = null;
	private String colorDepth = null;
	private String userLanguage = null;
	private String flashVersion = null;
	private String userAgent = null;
	private VisitorData visitorData;
	
	/**
	 * constructs with the tracking code using the provided visitor data.
	 *
	 * @param argTrackingCode
	 */
	public AnalyticsConfigData(String argTrackingCode, VisitorData visitorData)
	{
		if(argTrackingCode == null)
			throw new RuntimeException("Tracking code cannot be null");
		trackingCode = argTrackingCode;
		this.visitorData = visitorData;
	}
	
	/**
	 * @return the colorDepth
	 */
	public String getColorDepth()
	{
		return colorDepth;
	}
	
	/**
	 * @return the encoding
	 */
	public String getEncoding()
	{
		return encoding;
	}
	
	/**
	 * @return the flashVersion
	 */
	public String getFlashVersion()
	{
		return flashVersion;
	}
	
	/**
	 * @return the screenResolution
	 */
	public String getScreenResolution()
	{
		return screenResolution;
	}
	
	/**
	 * @return the trackingCode
	 */
	public String getTrackingCode()
	{
		return trackingCode;
	}
	
	/**
	 * @return the userLanguage
	 */
	public String getUserLanguage()
	{
		return userLanguage;
	}
	
	/**
	 * @return the user agent used for the network requests
	 */
	public String getUserAgent()
	{
		return userAgent;
	}
	
	/**
	 * @return the visitor data, used to track unique visitors
	 */
	public VisitorData getVisitorData()
	{
		return visitorData;
	}
	
	public void setVisitorData(VisitorData visitorData)
	{
		this.visitorData = visitorData;
	}
	
	/**
	 * Sets the color depth of the user. like 32 bit.
	 *
	 * @param argColorDepth
	 */
	public void setColorDepth(String argColorDepth)
	{
		colorDepth = argColorDepth;
	}
	
	/**
	 * Sets the character encoding of the client. like UTF-8
	 *
	 * @param argEncoding
	 *            the encoding to set
	 */
	public void setEncoding(String argEncoding)
	{
		encoding = argEncoding;
	}
	
	/**
	 * Sets the flash version of the client, like "9.0 r24"
	 *
	 * @param argFlashVersion
	 *            the flashVersion to set
	 */
	public void setFlashVersion(String argFlashVersion)
	{
		flashVersion = argFlashVersion;
	}
	
	/**
	 * Sets the screen resolution, like "1280x800".
	 *
	 * @param argScreenResolution
	 *            the screenResolution to set
	 */
	public void setScreenResolution(String argScreenResolution)
	{
		screenResolution = argScreenResolution;
	}
	
	/**
	 * Sets the user language, like "EN-us"
	 *
	 * @param argUserLanguage
	 *            the userLanguage to set
	 */
	public void setUserLanguage(String argUserLanguage)
	{
		userLanguage = argUserLanguage;
	}
	
	public void setUserAgent(String userAgent)
	{
		this.userAgent = userAgent;
	}
}
