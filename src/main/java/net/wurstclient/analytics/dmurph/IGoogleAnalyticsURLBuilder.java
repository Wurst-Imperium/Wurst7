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
 * Created at Jul 20, 2010, 4:56:30 AM
 */
package net.wurstclient.analytics.dmurph;

/**
 * URL builder for the tracking requests. Interfaced for supporting future
 * versions.
 *
 * @author Daniel Murphy
 *
 */
public interface IGoogleAnalyticsURLBuilder
{
	
	/**
	 * Reset the session cookie.
	 */
	public void resetSession();
	
	/**
	 * Gets the version for this builder.
	 *
	 * @return
	 */
	public String getGoogleAnalyticsVersion();
	
	/**
	 * Build the url request from the data.
	 *
	 * @param argData
	 * @return
	 */
	public String buildURL(AnalyticsRequestData argData);
}
