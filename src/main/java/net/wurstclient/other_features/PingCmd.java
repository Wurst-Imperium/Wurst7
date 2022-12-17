package net.wurstclient.other_features;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.MinecraftClientGame;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.report.log.ReceivedMessage;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.StatisticsS2CPacket;
import net.minecraft.util.profiling.jfr.event.PacketEvent;
import net.wurstclient.Category;
import net.wurstclient.DontBlock;
import net.wurstclient.SearchTags;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.events.GUIRenderListener;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.serverfinder.WurstServerPinger;
import net.wurstclient.util.ChatUtils;

import java.util.Objects;

@DontBlock
@SearchTags({"ping", "delay", "connection"})
public class PingCmd extends Command implements UpdateListener, PacketInputListener {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    long lastPingAt = -1L;
    double pingCache = -1.0;
    boolean invoked = false;


    public PingCmd() {
        super("ping", "Pings the server and shows the result in chat.\n" +
                "Can be used to check your connection.");
        setCategory(Category.OTHER);
    }

    public PingCmd(String name, String description, String... syntax) {
        super(name, description, syntax);
    }

    @Override
    public void onUpdate() {


    }

    @Override
    public void call(String[] args) throws CmdException {
        if (args.length != 0) {
            throw new CmdSyntaxError("Ping doesn't need arguments!");
        }


        EVENTS.add(PacketInputListener.class, this);
        EVENTS.add(UpdateListener.class, this);

        invoked = true;
        sendPacket();



}

    @Override
    public String getPrimaryAction() {
        return "Ping the server";
    }


    @Override
    public void doPrimaryAction() {
        WURST.getCmdProcessor().process("ping");
    }

    public void sendPacket() {
        if (lastPingAt > 0) {
            if (invoked) {
                ChatUtils.message("§cAlready pinging!");
                return;
            }
        }
        Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS));

        lastPingAt = System.nanoTime();

    }


    @Override
    public void onReceivedPacket(PacketInputEvent event) {
        if (lastPingAt > 0) {
            if (event.getPacket() instanceof GameJoinS2CPacket) {
                lastPingAt = -1L;
                invoked = false;
            } else if (event.getPacket() instanceof StatisticsS2CPacket) {
                double diff = (Math.abs(System.nanoTime() - lastPingAt) / 1_000_000.0);
                lastPingAt *= -1;
                pingCache = diff;
                if (invoked) {
                    invoked = false;
                    diff = Math.round(diff);
                    ChatUtils.message("§" + (diff < 50.0 ? "a" : (diff < 100.0 ? "2" : (diff < 149.0 ? "e" : (diff < 249.0 ? "6" : "c")))) + diff + " §7ms");


                }
            }
        }
    }
}

