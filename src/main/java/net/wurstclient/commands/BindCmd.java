/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.wurstclient.command.CmdException;
import net.wurstclient.command.Command;

public final class BindCmd extends Command
{
	public BindCmd()
	{
		super("bind", "通过指令快速设置快捷键", ".bind <key> <hacks>",
			".bind <按键名> <功能名>(增加功能快捷键)",
			".bind <按键名> <指令>(增加指令快捷键)", "需要设置多个[功能/指令]时,\n用 ';'符号分隔,", "需要使用完整功能请用.binds指令");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		WURST.getCmdProcessor().process("binds add " + String.join(" ", args));
	}
}
