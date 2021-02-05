/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
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
	public AddAltCmd addAltCmd;
	public AnnoyCmd annoyCmd;
	public AuthorCmd authorCmd;
	public BindCmd bindCmd;
	public BindsCmd bindsCmd;
	public BlinkCmd blinkCmd;
	public ClearCmd clearCmd;
	public CopyItemCmd copyitemCmd;
	public DamageCmd damageCmd;
	public DigCmd digCmd;
	public DropCmd dropCmd;
	public EnabledHaxCmd enabledHaxCmd;
	public EnchantCmd enchantCmd;
	public ExcavateCmd excavateCmd;
	public FeaturesCmd featuresCmd;
	public FollowCmd followCmd;
	public FriendsCmd friendsCmd;
	public GetPosCmd getPosCmd;
	public GiveCmd giveCmd;
	public GmCmd gmCmd;
	public GoToCmd goToCmd;
	public HelpCmd helpCmd;
	public InvseeCmd invseeCmd;
	public IpCmd ipCmd;
	public JumpCmd jumpCmd;
	public LeaveCmd leaveCmd;
	public ModifyCmd modifyCmd;
	public PathCmd pathCmd;
	public PotionCmd potionCmd;
	public PrefixCmd prefixCmd;
	public ProtectCmd protectCmd;
	public RenameCmd renameCmd;
	public RepairCmd repairCmd;
	public RvCmd rvCmd;
	public SvCmd svCmd;
	public SayCmd sayCmd;
	public SetCheckboxCmd setCheckboxCmd;
	public SetModeCmd setModeCmd;
	public SetSliderCmd setSliderCmd;
	public SettingsCmd settingsCmd;
	public TacoCmd tacoCmd;
	public TCmd tCmd;
	public TooManyHaxCmd tooManyHaxCmd;
	public TpCmd tpCmd;
	public UnbindCmd unbindCmd;
	public VClipCmd vClipCmd;
	public ViewNbtCmd viewNbtCmd;

	private TreeMap<String, Command> cmds;

	public void newCmds(){
		addAltCmd = new AddAltCmd();
		annoyCmd = new AnnoyCmd();
		authorCmd = new AuthorCmd();
		bindCmd = new BindCmd();
		bindsCmd = new BindsCmd();
		blinkCmd = new BlinkCmd();
		clearCmd = new ClearCmd();
		copyitemCmd = new CopyItemCmd();
		damageCmd = new DamageCmd();
		digCmd = new DigCmd();
		dropCmd = new DropCmd();
		enabledHaxCmd = new EnabledHaxCmd();
		enchantCmd = new EnchantCmd();
		excavateCmd = new ExcavateCmd();
		featuresCmd = new FeaturesCmd();
		followCmd = new FollowCmd();
		friendsCmd = new FriendsCmd();
		getPosCmd = new GetPosCmd();
		giveCmd = new GiveCmd();
		gmCmd = new GmCmd();
		goToCmd = new GoToCmd();
		helpCmd = new HelpCmd();
		invseeCmd = new InvseeCmd();
		ipCmd = new IpCmd();
		jumpCmd = new JumpCmd();
		leaveCmd = new LeaveCmd();
		modifyCmd = new ModifyCmd();
		pathCmd = new PathCmd();
		potionCmd = new PotionCmd();
		prefixCmd = new PrefixCmd();
		protectCmd = new ProtectCmd();
		renameCmd = new RenameCmd();
		repairCmd = new RepairCmd();
		rvCmd = new RvCmd();
		svCmd = new SvCmd();
		sayCmd = new SayCmd();
		setCheckboxCmd = new SetCheckboxCmd();
		setModeCmd = new SetModeCmd();
		setSliderCmd = new SetSliderCmd();
		settingsCmd = new SettingsCmd();
		tacoCmd = new TacoCmd();
		tCmd = new TCmd();
	 	tooManyHaxCmd = new TooManyHaxCmd();
		tpCmd = new TpCmd();
		unbindCmd = new UnbindCmd();
 		vClipCmd = new VClipCmd();
	 	viewNbtCmd = new ViewNbtCmd();

		cmds = new TreeMap<>((o1, o2) -> o1.compareToIgnoreCase(o2));

		try
		{
			for(Field field : CmdList.class.getDeclaredFields())
			{
				if(!field.getName().endsWith("Cmd"))
					continue;

				Command cmd = (Command)field.get(this);
				cmds.put(cmd.getName().substring(CmdProcessor.getPrefix().length()), cmd);
			}

		}catch(Exception e)
		{
			String message = "Initializing Wurst commands";
			CrashReport report = CrashReport.create(e, message);
			throw new CrashException(report);
		}
	}
	
	public CmdList()
	{
		newCmds();
	}
	
	public Command getCmdByName(String name)
	{
		return cmds.get(name);
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
