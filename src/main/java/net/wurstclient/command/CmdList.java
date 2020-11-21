/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.command;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.TreeMap;

import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.wurstclient.commands.*;

public final class CmdList
{
	public final AddAltCmd addAltCmd = new AddAltCmd();
	public final AnnoyCmd annoyCmd = new AnnoyCmd();
	public final AuthorCmd authorCmd = new AuthorCmd();
	public final BindCmd bindCmd = new BindCmd();
	public final BindsCmd bindsCmd = new BindsCmd();
	public final BlinkCmd blinkCmd = new BlinkCmd();
	public final ClearCmd clearCmd = new ClearCmd();
	public final CopyItemCmd copyitemCmd = new CopyItemCmd();
	public final DamageCmd damageCmd = new DamageCmd();
	public final DigCmd digCmd = new DigCmd();
	public final DropCmd dropCmd = new DropCmd();
	public final EnabledHaxCmd enabledHaxCmd = new EnabledHaxCmd();
	public final EnchantCmd enchantCmd = new EnchantCmd();
	public final ExcavateCmd excavateCmd = new ExcavateCmd();
	public final FeaturesCmd featuresCmd = new FeaturesCmd();
	public final FollowCmd followCmd = new FollowCmd();
	public final FriendsCmd friendsCmd = new FriendsCmd();
	public final GetPosCmd getPosCmd = new GetPosCmd();
	public final GiveCmd giveCmd = new GiveCmd();
	public final GmCmd gmCmd = new GmCmd();
	public final GoToCmd goToCmd = new GoToCmd();
	public final HelpCmd helpCmd = new HelpCmd();
	public final InvseeCmd invseeCmd = new InvseeCmd();
	public final IpCmd ipCmd = new IpCmd();
	public final JumpCmd jumpCmd = new JumpCmd();
	public final LeaveCmd leaveCmd = new LeaveCmd();
	public final ModifyCmd modifyCmd = new ModifyCmd();
	public final PathCmd pathCmd = new PathCmd();
	public final PotionCmd potionCmd = new PotionCmd();
	public final ProtectCmd protectCmd = new ProtectCmd();
	public final RenameCmd renameCmd = new RenameCmd();
	public final RepairCmd repairCmd = new RepairCmd();
	public final RvCmd rvCmd = new RvCmd();
	public final SvCmd svCmd = new SvCmd();
	public final SayCmd sayCmd = new SayCmd();
	public final SetCheckboxCmd setCheckboxCmd = new SetCheckboxCmd();
	public final SetModeCmd setModeCmd = new SetModeCmd();
	public final SetSliderCmd setSliderCmd = new SetSliderCmd();
	public final SettingsCmd settingsCmd = new SettingsCmd();
	public final TacoCmd tacoCmd = new TacoCmd();
	public final TCmd tCmd = new TCmd();
	public final TooManyHaxCmd tooManyHaxCmd = new TooManyHaxCmd();
	public final TpCmd tpCmd = new TpCmd();
	public final UnbindCmd unbindCmd = new UnbindCmd();
	public final VClipCmd vClipCmd = new VClipCmd();
	public final ViewNbtCmd viewNbtCmd = new ViewNbtCmd();
	
	private final TreeMap<String, Command> cmds =
		new TreeMap<>((o1, o2) -> o1.compareToIgnoreCase(o2));
	
	public CmdList()
	{
		try
		{
			for(Field field : CmdList.class.getDeclaredFields())
			{
				if(!field.getName().endsWith("Cmd"))
					continue;
				
				Command cmd = (Command)field.get(this);
				cmds.put(cmd.getName(), cmd);
			}
			
		}catch(Exception e)
		{
			String message = "Initializing Wurst commands";
			CrashReport report = CrashReport.create(e, message);
			throw new CrashException(report);
		}
	}
	
	public Command getCmdByName(String name)
	{
		return cmds.get("." + name);
	}
	
	public Collection<Command> getAllCmds()
	{
		return cmds.values();
	}
	
	public int countCmds()
	{
		return cmds.size();
	}
}
