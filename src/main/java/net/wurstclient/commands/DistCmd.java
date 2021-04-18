/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.util.math.BlockPos;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;

public class DistCmd extends Command {

    int fx;
    int fz;
    int tx;
    int tz;

    public DistCmd() {
        super("dist", "Gets Distance between 2 Coordinates\n ", ".dist <x> <z> <x> <z>", "Convert Overworld coordinates To Nether \ncoordinates .dist cvt <x> <z>");
    }

    @Override
    public void call(String[] args) throws CmdException {
        if (args.length < 1)
            throw new CmdError("Enter Command Properly");

        if (("cvt".equalsIgnoreCase(args[0]))) {
            try {
                int cvtX = Integer.parseInt(args[1]);
                int cvtZ = Integer.parseInt(args[2]);
                ChatUtils.message("Nether cords For " + cvtX + " " + cvtZ + " is " + cvtX / 8 + " " + cvtZ / 8);

            } catch (Exception e) {
                throw new CmdError("Invalid Coordinates");
            }

        } else {

            if (args.length <= 3) {
                throw new CmdError("Invalid Coordinates");
            }

            if (args[0].equals("~")) {
                BlockPos playerPos = new BlockPos(MC.player.getPos());
                fx = Math.round(playerPos.getX());
                fz = Math.round(playerPos.getZ());

            } else {
                try {
                    fx = Integer.parseInt(args[0]);
                    fz = Integer.parseInt(args[1]);
                } catch (Exception e) {
                    throw new CmdError("Coordinates should be numbers only");
                }
            }
            if (args[2].equals("~")) {
                BlockPos playerPos = new BlockPos(MC.player.getPos());
                tx = Math.round(playerPos.getX());
                tz = Math.round(playerPos.getZ());

            } else {
                try {
                    tx = Integer.parseInt(args[2]);
                    tz = Integer.parseInt(args[3]);

                } catch (Exception e) {
                    throw new CmdError("Coordinates should be numbers only");
                }
            }
            double dist;
            try {
                dist = Math.sqrt((tx - fx) * (tx - fx) + (tz - fz) * (tz - fz));
                int FinalDist = (int) Math.round(dist) + 1;
                ChatUtils.message("Distance From " + Math.round(fx) + " " + Math.round(fz) + " To " + Math.round(tx) + " " + Math.round(tz) + " is " + FinalDist + " Blocks Away");
            } catch (Exception e) {
                ChatUtils.message("Invalid Coordinates");
            }

        }
    }
}


