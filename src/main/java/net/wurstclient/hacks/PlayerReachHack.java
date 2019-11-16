/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;
 
 
import net.wurstclient.Category;
import net.wurstclient.SearchTags; 
import net.wurstclient.hack.Hack; 

@SearchTags({ "PlayerReachHack", "Reach Hack" })
public final class PlayerReachHack extends Hack  {

    
    public PlayerReachHack() {
        super("PlayerReachHack", "Set Max Place Distance to 7 Blocks Away.\n"
        +"Set Max Break Distance to 9 Blocks Away.\n"
        +"\n"
        +"Created by Dj-jom2x");
        setCategory(Category.BLOCKS); 
    }

    @Override
    public void onEnable() { 
        IMC.getInteractionManager().setReachDistance(12f);
        IMC.getInteractionManager().setHasExtendedReach(true);
    }

    @Override
    public void onDisable() { 
        IMC.getInteractionManager().setReachDistance(4.5f);
        IMC.getInteractionManager().setHasExtendedReach(false);
    }

    
}
