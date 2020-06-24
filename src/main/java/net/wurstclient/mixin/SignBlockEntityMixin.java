/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.ISignBlockEntity;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntity
	implements ISignBlockEntity
{
	@Shadow
	@Final
	private Text[] text;
	
	public SignBlockEntityMixin(WurstClient wurst, BlockEntityType<?> type)
	{
		super(type);
	}
	
	@Override
	public Text getTextOnRow(int row)
	{
		return text[row];
	}
}
