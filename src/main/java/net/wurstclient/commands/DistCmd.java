package net.wurstclient.commands;

import net.minecraft.util.math.BlockPos;
import net.wurstclient.command.CmdException;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import java.lang.Math;


public class DistCmd extends Command {


    public DistCmd() {
        super("dist", "Gets Distance between 2 Cordinates\nYou can convert Overworld Cords To Nether cords With cvt:<x> <z>\n ", ".dist <x> <z>:<x> <z>", ".dist cvt:<x> <z>");
    }

    @Override
    public void call(String[] args) throws CmdException {

        if(args.length < 1)
            throw new CmdError();

        String Allcords = String.join(" ", args);
        if(!Allcords.contains(":"))
            throw new CmdError("Separate Cordinates With : ");

        if (Allcords.startsWith("cvt")){

            String[] arrOfSTR = Allcords.split(":", 0); Allcords.split(":",0);
            String cordsWithSpace = arrOfSTR[1];
            String[] Cordsarray = cordsWithSpace.split(" ",0);
            if (Cordsarray.length < 2)
                throw new CmdError("Provide Cordinates Properly");
            int cvtX;
            int cvtZ;
            cvtX = Integer.parseInt(Cordsarray[0]);
            cvtZ = Integer.parseInt(Cordsarray[1]);

            int netherX =  cvtX / 8;
            int netherZ = cvtZ / 8;
            ChatUtils.message("Neter cords For "+ cvtX + " "+ cvtZ + " is " + netherX + " " + netherZ);
        }else {
            String[] arrOfStr = Allcords.split(":", 0); Allcords.split(":",0);
            String FromCord;
            String ToCord;
            FromCord = arrOfStr[0];
            ToCord = arrOfStr[1];
            String[] FromCordStrip = FromCord.split(" ",0);
            String[] ToCordStrip = ToCord.split(" ", 0);
            if (FromCordStrip.length < 2 || ToCordStrip.length < 2)
                throw new CmdError("Provide Cordinates Properly");

            int fx;
            int fz;
            int tx;
            int tz;
            if (FromCordStrip[0].equals("~")){
                BlockPos playerPos = new BlockPos(MC.player.getPos());
                fx = playerPos.getX();
                fz = playerPos.getZ();

            }else{
                fx = Integer.parseInt(FromCordStrip[0]);
                fz = Integer.parseInt(FromCordStrip[1]);
            }
            if (ToCordStrip[0].equals("~")){
                BlockPos playerPos = new BlockPos(MC.player.getPos());
                tx = playerPos.getX();
                tz = playerPos.getZ();

            }else{
                tx = Integer.parseInt(ToCordStrip[0]);
                tz = Integer.parseInt(ToCordStrip[1]);

            }
            double dist;
            dist=Math.sqrt((tx-fx)*(tx-fx) + (tz-fz)*(tz-fz));

            int FinalDist = (int) Math.round(dist) + 1;

            ChatUtils.message("Distance From " + Math.round(fx) + " " + Math.round(fz) +" To "+ Math.round(tx) +" "+ Math.round(tz)+ " is "  + FinalDist +  " Blocks Away");

        }



    }

}


