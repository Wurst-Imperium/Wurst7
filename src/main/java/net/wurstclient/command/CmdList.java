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
	public DupeCmd dupeCmd;
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
	
	public CmdList()
	{
		newCmds();
	}

	public void newCmds(){
    AddAltCmd addAltCmd = new AddAltCmd();
    AnnoyCmd annoyCmd = new AnnoyCmd();
    AuthorCmd authorCmd = new AuthorCmd();
    BindCmd bindCmd = new BindCmd();
    BindsCmd bindsCmd = new BindsCmd();
    BlinkCmd blinkCmd = new BlinkCmd();
    ClearCmd clearCmd = new ClearCmd();
    CopyItemCmd copyitemCmd = new CopyItemCmd();
    DamageCmd damageCmd = new DamageCmd();
    DigCmd digCmd = new DigCmd();
    DropCmd dropCmd = new DropCmd();
    DupeCmd dupeCmd = new DupeCmd();
    EnabledHaxCmd enabledHaxCmd = new EnabledHaxCmd();
    EnchantCmd enchantCmd = new EnchantCmd();
    ExcavateCmd excavateCmd = new ExcavateCmd();
    FeaturesCmd featuresCmd = new FeaturesCmd();
    FollowCmd followCmd = new FollowCmd();
    FriendsCmd friendsCmd = new FriendsCmd();
    GetPosCmd getPosCmd = new GetPosCmd();
    GiveCmd giveCmd = new GiveCmd();
    GmCmd gmCmd = new GmCmd();
    GoToCmd goToCmd = new GoToCmd();
    HelpCmd helpCmd = new HelpCmd();
    InvseeCmd invseeCmd = new InvseeCmd();
    IpCmd ipCmd = new IpCmd();
    JumpCmd jumpCmd = new JumpCmd();
    LeaveCmd leaveCmd = new LeaveCmd();
    ModifyCmd modifyCmd = new ModifyCmd();
    PathCmd pathCmd = new PathCmd();
    PotionCmd potionCmd = new PotionCmd();
    ProtectCmd protectCmd = new ProtectCmd();
    RenameCmd renameCmd = new RenameCmd();
    RepairCmd repairCmd = new RepairCmd();
    RvCmd rvCmd = new RvCmd();
    SvCmd svCmd = new SvCmd();
    SayCmd sayCmd = new SayCmd();
    SetCheckboxCmd setCheckboxCmd = new SetCheckboxCmd();
    SetModeCmd setModeCmd = new SetModeCmd();
    SetSliderCmd setSliderCmd = new SetSliderCmd();
    SettingsCmd settingsCmd = new SettingsCmd();
    TacoCmd tacoCmd = new TacoCmd();
    TCmd tCmd = new TCmd();
    TooManyHaxCmd tooManyHaxCmd = new TooManyHaxCmd();
    TpCmd tpCmd = new TpCmd();
    UnbindCmd unbindCmd = new UnbindCmd();
    VClipCmd vClipCmd = new VClipCmd();
    ViewNbtCmd viewNbtCmd = new ViewNbtCmd();

		cmds = new TreeMap<>((o1, o2) -> o1.compareToIgnoreCase(o2));

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
		return cmds.get(CmdProcessor.getPrefix() + name);
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
