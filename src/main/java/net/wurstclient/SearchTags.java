/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds search tags to a Wurst feature so that it can be found through the
 * search bar in the Navigator GUI.
 *
 * <p>
 * Navigator can already find features by their name and description, so
 * repeating that information in the search tags is pointless. However, names
 * and descriptions of settings are not used by Navigator, so repeating those
 * can make sense if people are likely to search for them.
 *
 * <p>
 * Navigator is not case-sensitive, so for example "NukerLegit" and "nukerlegit"
 * are treated the same. However, Navigator struggles with spaces and the order
 * of words, for example "NukerLegit", "Nuker Legit" and "Legit Nuker" are all
 * treated differently.
 *
 * <p>
 * By convention, search tags with spaces should be written in lower case and
 * search tags without spaces should be written in camel case. For example,
 * "NukerLegit" and "nuker legit". This is not enforced, but it makes the
 * code easier to read.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SearchTags
{
	String[] value();
}
