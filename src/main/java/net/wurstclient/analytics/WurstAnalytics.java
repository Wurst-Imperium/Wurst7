/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.analytics;

import java.nio.file.Path;

import net.wurstclient.analytics.dmurph.AnalyticsRequestData;

public final class WurstAnalytics
{
	private final String hostname;
	private final WurstAnalyticsTracker tracker;
	private final AnalyticsConfigFile configFile;
	
	public WurstAnalytics(String trackingID, String hostname, Path configFile)
	{
		tracker = new WurstAnalyticsTracker(trackingID);
		this.hostname = hostname;
		this.configFile = new AnalyticsConfigFile(configFile);
		this.configFile.load(tracker);
	}
	
	public boolean isEnabled()
	{
		return tracker.isEnabled();
	}
	
	public void setEnabled(boolean enabled)
	{
		if(!enabled)
			trackEvent("options", "analytics", "disable");
		
		tracker.setEnabled(enabled);
		configFile.save(tracker);
		
		if(enabled)
			trackEvent("options", "analytics", "enable");
	}
	
	public void trackPageView(String url, String title)
	{
		tracker.trackPageView(url, title, hostname);
	}
	
	public void trackPageViewFromReferrer(String url, String title,
		String referrerSite, String referrerPage)
	{
		tracker.trackPageViewFromReferrer(url, title, hostname, referrerSite,
			referrerPage);
	}
	
	public void trackPageViewFromSearch(String url, String title,
		String searchSource, String keywords)
	{
		tracker.trackPageViewFromSearch(url, title, hostname, searchSource,
			keywords);
	}
	
	public void trackEvent(String category, String action)
	{
		tracker.trackEvent(category, action);
	}
	
	public void trackEvent(String category, String action, String label)
	{
		tracker.trackEvent(category, action, label);
	}
	
	public void trackEvent(String category, String action, String label,
		Integer value)
	{
		tracker.trackEvent(category, action, label, value);
	}
	
	public void makeCustomRequest(AnalyticsRequestData data)
	{
		tracker.makeCustomRequest(data);
	}
}
