/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.serverfinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import net.minecraft.client.gui.widget.*;
import net.minecraft.client.option.*;
import net.minecraft.util.Util;
import net.wurstclient.util.CyclingButtonWidget;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IMultiplayerScreen;
import net.wurstclient.util.MathUtils;
import net.wurstclient.core.MCScreen;

public class ServerFinderScreen extends MCScreen implements IServerFinderDoneListener {
    private final MultiplayerScreen prevScreen;
    private TextFieldWidget ipBox;
    private TextFieldWidget versionBox;
    private TextFieldWidget maxThreadsBox;
    private TextFieldWidget targetCheckedBox;
    private ButtonWidget searchButton;
    private CyclingButtonWidget scanPortsButton;
    private CyclingButtonWidget searchDirectionButton;

    private ServerFinderState state;
    private int maxThreads;
    private volatile int numActiveThreads;
    private volatile int checked;
    private volatile int working;

    private int targetChecked = 512;

    private final Stack<String> ipsToPing = new Stack<>();

    private final Object serverFinderLock = new Object();

    public static ServerFinderScreen instance = null;
    private static int searchNumber = 0;

    private ArrayList<String> versionFilters = new ArrayList<>();
    private boolean scanPorts = true;

    private String saveToFileMessage = null;

    public ServerFinderScreen(MultiplayerScreen prevMultiplayerMenu) {
        super(new LiteralText(""));
        newSearch();
        instance = this;
        prevScreen = prevMultiplayerMenu;
    }

    private void newSearch() {
        searchNumber = (searchNumber + 1) % 1000;
    }

    public void incrementTargetChecked(int amount) {
        synchronized (serverFinderLock) {
            if (state != ServerFinderState.CANCELLED)
                checked += amount;
        }
    }

    public ServerFinderState getState() {
        return state;
    }

    private void saveToFile() {
        if (WurstClient.INSTANCE == null || prevScreen == null)
            return;

        int newIPs = 0;

        Path wurstFolder = WurstClient.INSTANCE.getWurstFolder();
        if (wurstFolder == null)
            return;

        Path filePath = wurstFolder.resolve("servers.txt");
        File serverFile = filePath.toFile();
        HashSet<IPAddress> hashedIPs = new HashSet<>();
        if (serverFile.exists()) {
            try {
                List<String> ips = Files.readAllLines(filePath);
                for (String ip : ips) {
                    IPAddress parsedIP = IPAddress.fromText(ip);
                    if (parsedIP != null)
                        hashedIPs.add(parsedIP);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ServerList servers = prevScreen.getServerList();
        for (int i = 0; i < servers.size(); i++) {
            ServerInfo info = servers.get(i);
            IPAddress addr = IPAddress.fromText(info.address);
            if (addr != null && hashedIPs.add(addr))
                newIPs++;
        }

        StringBuilder fileOutput = new StringBuilder();
        for (IPAddress ip : hashedIPs) {
            String stringIP = ip.toString();
            if (stringIP != null)
                fileOutput.append(stringIP).append("\n");
        }
        try (PrintWriter pw = new PrintWriter(filePath.toFile())) {
            pw.print(fileOutput);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        saveToFileMessage = "\u00a76Saved " + newIPs + " new IP" + (newIPs == 1 ? "" : "s");
    }

    @Override
    public void init() {
        ipBox = new TextFieldWidget(textRenderer, width / 2 - 100, 90, 200, 20, new LiteralText(""));
        ipBox.setMaxLength(200);
        ipBox.setTextFieldFocused(true);
        addDrawableChild(ipBox);
        setInitialFocus(ipBox);
        maxThreadsBox = new TextFieldWidget(textRenderer, width / 2 - 30, 115, 30, 10, new LiteralText(""));
        maxThreadsBox.setMaxLength(4);
        maxThreadsBox.setText("128");
        addDrawableChild(maxThreadsBox);
        targetCheckedBox = new TextFieldWidget(textRenderer, width / 2 + 100 - 40, 115, 40, 10, new LiteralText(""));
        targetCheckedBox.setMaxLength(5);
        targetCheckedBox.setText("1337");
        addDrawableChild(targetCheckedBox);
        state = ServerFinderState.NOT_RUNNING;
        ArrayList<CyclingButtonWidget.CycleButtonEntry> scanPortsOptions = new ArrayList<CyclingButtonWidget.CycleButtonEntry>();
        scanPortsOptions.add(new CyclingButtonWidget.CycleButtonEntry("Extra Ports: Yes", "YES"));
        scanPortsOptions.add(new CyclingButtonWidget.CycleButtonEntry("Extra Ports: No", "NO"));
        scanPortsButton = new CyclingButtonWidget(width / 2 - 100, 130, 98, 20, scanPortsOptions, Optional.empty());
        addDrawableChild(scanPortsButton);
        ArrayList<CyclingButtonWidget.CycleButtonEntry> searchDirectionOptions = new ArrayList<CyclingButtonWidget.CycleButtonEntry>();
        searchDirectionOptions.add(new CyclingButtonWidget.CycleButtonEntry("Search Forwards", "FORWARD"));
        searchDirectionOptions.add(new CyclingButtonWidget.CycleButtonEntry("Search Backwards", "BACK"));
        searchDirectionButton = new CyclingButtonWidget(width / 2, 130, 98, 20, searchDirectionOptions, Optional.empty());
        addDrawableChild(searchDirectionButton);

		versionBox = new TextFieldWidget(textRenderer, width / 2 - 100, 185, 200, 20, new LiteralText(""));
		versionBox.setMaxLength(200);
		addDrawableChild(versionBox);
		addDrawableChild(searchButton =
				new ButtonWidget(width / 2 - 100, 210, 200, 20,
						new LiteralText("Search"), b -> searchOrCancel()));

		addDrawableChild(new ButtonWidget(width / 2 - 100, 230, 98, 20, new LiteralText("Tutorial"),
				b -> Util.getOperatingSystem()
						.open("https://www.wurstclient.net/wiki/Special_Features/Server_Finder/")));

		addDrawableChild(new ButtonWidget(width / 2 + 2, 230, 98, 20, new LiteralText("Save to File"),
				b -> saveToFile()));

        addDrawableChild(new ButtonWidget(width / 2 - 100, 250, 200, 20, new LiteralText("Back"),
                b -> WurstClient.setScreen(prevScreen)));

    }

    private void searchOrCancel() {
        if (state.isRunning()) {
            state = ServerFinderState.CANCELLED;
            return;
        }

        state = ServerFinderState.RESOLVING;
        maxThreads = Integer.parseInt(maxThreadsBox.getText());
        targetChecked = Integer.parseInt(targetCheckedBox.getText());
        scanPorts = scanPortsButton.getSelectedValue().equals("YES");
        saveToFileMessage = null;
        ipsToPing.clear();
        numActiveThreads = 0;
        checked = 0;
        working = 0;

        newSearch();
        parseVersionFilters();

        findServers();
    }

    private void parseVersionFilters() {
        String filter = versionBox.getText();
        String[] versions = filter.split(";");
        if (versionFilters == null) {
            versionFilters = new ArrayList<>();
        }
        versionFilters.clear();
        for (String version : versions) {
            String trimmed = version.trim();
            if (trimmed.length() > 0)
                versionFilters.add(version.trim());
        }
    }

    private static int getIntFromInetAddress(InetAddress addr) {
        byte[] a = addr.getAddress();
        return ((a[0] & 0xFF) << 24) | ((a[1] & 0xFF) << 16) | ((a[2] & 0xFF) << 8) | (a[3] & 0xFF);
    }

    private static InetAddress getInetAddressFromInt(int value) throws UnknownHostException {
        StringBuffer ip_address_string = new StringBuffer();
        for (int i = 0; i < 4; i++) {
            ip_address_string.append(0xff & value >> 24);
            value <<= 8;
            if (i != 4 - 1)
                ip_address_string.append('.');
        }
        return InetAddress.getByName(ip_address_string.toString());
    }

    private static InetAddress getPreviousIPV4Address(InetAddress ip) {
        try {
            return getInetAddressFromInt(getIntFromInetAddress(ip) - 1);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static InetAddress getNextIPV4Address(InetAddress ip) {
        try {
            return getInetAddressFromInt(getIntFromInetAddress(ip) + 1);
        } catch (UnknownHostException e) {
            return null;
        }
    }


    private void findServers() {

        try {
            InetAddress addr = InetAddress.getByName(ipBox.getText().split(":")[0].trim());

            state = ServerFinderState.SEARCHING;
            ipsToPing.push(addr.getHostAddress());
            for (int x = 0; x < targetChecked - 1; x++) {
                if (searchDirectionButton.getSelectedValue().equals("FORWARD")) {
                    addr = getNextIPV4Address(addr);
                } else {
                    addr = getPreviousIPV4Address(addr);
                }
                if (addr != null) {
                    ipsToPing.push(addr.getHostAddress());
                }
            }
            System.out.println("IPS TO PING: " + ipsToPing.size());
            for (String ip : ipsToPing) {
                System.out.println(ip);
            }
            while (numActiveThreads < maxThreads && pingNewIP()) {
            }

        } catch (UnknownHostException e) {
            state = ServerFinderState.UNKNOWN_HOST;

        } catch (Exception e) {
            e.printStackTrace();
            state = ServerFinderState.ERROR;
        }
    }

    private boolean pingNewIP() {
        synchronized (serverFinderLock) {
            if (ipsToPing.size() > 0) {
                String ip = ipsToPing.pop();
                WurstServerPinger pinger = new WurstServerPinger(scanPorts, searchNumber);
                checked++;
                pinger.addServerFinderDoneListener(this);
                pinger.ping(ip);
                numActiveThreads++;
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        ipBox.tick();
        versionBox.tick();

        searchButton.setMessage(new LiteralText(state.isRunning() ? "Cancel" : "Search"));
        ipBox.active = !state.isRunning();
        versionBox.active = !state.isRunning();
        maxThreadsBox.active = !state.isRunning();

        searchButton.active = MathUtils.isInteger(maxThreadsBox.getText()) && !ipBox.getText().isEmpty();
    }

    private boolean isServerInList(String ip) {
        for (int i = 0; i < prevScreen.getServerList().size(); i++)
            if (prevScreen.getServerList().get(i).address.equals(ip))
                return true;

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int int_3) {
        if (keyCode == GLFW.GLFW_KEY_ENTER)
            searchButton.onPress();

        return super.keyPressed(keyCode, scanCode, int_3);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack);

        drawCenteredText(matrixStack, textRenderer, "Server Finder",
                width / 2, 20, 16777215);
        drawCenteredText(matrixStack, textRenderer,
                "This will search for servers with similar IPs",
                width / 2, 40, 10526880);
        drawCenteredText(matrixStack, textRenderer,
                "to the IP you type into the field below.",
                width / 2, 50, 10526880);
        drawCenteredText(matrixStack, textRenderer,
                "The servers it finds will be added to your server list.",
                width / 2, 60, 10526880);

        drawStringWithShadow(matrixStack, textRenderer, "Server address:",
                width / 2 - 100, 80, 10526880);
        ipBox.render(matrixStack, mouseX, mouseY, partialTicks);
        drawCenteredText(matrixStack, textRenderer, saveToFileMessage == null ? state.toString() : saveToFileMessage,
                width / 2, 30, 10526880);

        drawStringWithShadow(matrixStack, textRenderer, "Max. Threads:",
                width / 2 - 100, 115, 10526880);
        maxThreadsBox.render(matrixStack, mouseX, mouseY, partialTicks);
        drawStringWithShadow(matrixStack, textRenderer, "Max. IPs:",
                width / 2 + 3, 115, 10526880);
        targetCheckedBox.render(matrixStack, mouseX, mouseY, partialTicks);
        drawStringWithShadow(matrixStack, textRenderer, "Checking: " + checked + " / " + targetCheckedBox.getText(),
                width / 2 - 100, 155, 10526880);

        drawStringWithShadow(matrixStack, textRenderer, "Found Working: " + working,
                width / 2 - 100, 165, 10526880);

        drawStringWithShadow(matrixStack, textRenderer, "Filter by version. Example: 1.18.1;1.18.2",
                width / 2 - 100, 175, 10526880);
        versionBox.render(matrixStack, mouseX, mouseY, partialTicks);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void close() {
        state = ServerFinderState.CANCELLED;
        super.close();
    }

    enum ServerFinderState {
        NOT_RUNNING(""), SEARCHING("\u00a72Searching..."), RESOLVING("\u00a72Resolving..."),
        UNKNOWN_HOST("\u00a74Unknown Host!"), CANCELLED("\u00a74Cancelled!"), DONE("\u00a72Done!"),
        ERROR("\u00a74An error occurred!");

        private final String name;

        private ServerFinderState(String name) {
            this.name = name;
        }

        public boolean isRunning() {
            return this == SEARCHING || this == RESOLVING;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static int getSearchNumber() {
        return searchNumber;
    }

    private boolean filterPass(WurstServerInfo info) {
        if (info == null)
            return false;
        int playerCountFilter = 0;
        if (info.playerCount < playerCountFilter)
            return false;
        for (String version : versionFilters) {
            if (info.version != null && info.version.contains(version)) {
                return true;
            }
        }
        return versionFilters.isEmpty();
    }

    @Override
    public void onServerDone(WurstServerPinger pinger) {
        if (state == ServerFinderState.CANCELLED || pinger == null || pinger.getSearchNumber() != searchNumber)
            return;
        if (pinger.isWorking()) {
            if (!isServerInList(pinger.getServerIP()) && filterPass(pinger.getServerInfo())) {
                synchronized (serverFinderLock) {
                    working++;
                    prevScreen.getServerList().add(new ServerInfo("Grief me #" + working, pinger.getServerIP(), false));
                    prevScreen.getServerList().saveFile();
                    ((IMultiplayerScreen) prevScreen).getServerListSelector().setSelected(null);
                    ((IMultiplayerScreen) prevScreen).getServerListSelector().setServers(prevScreen.getServerList());
                }
            }
        }
        synchronized (serverFinderLock) {
            numActiveThreads--;
        }
        while (numActiveThreads < maxThreads && pingNewIP()) {

        }
        synchronized (serverFinderLock) {
            if (numActiveThreads == 0) {
                state = ServerFinderState.DONE;
            }
        }
    }

    @Override
    public void onServerFailed(WurstServerPinger pinger) {
        if (state == ServerFinderState.CANCELLED || pinger == null || pinger.getSearchNumber() != searchNumber)
            return;
        synchronized (serverFinderLock) {
            numActiveThreads--;
        }
        while (numActiveThreads < maxThreads && pingNewIP()) {

        }
        synchronized (serverFinderLock) {
            if (numActiveThreads == 0) {
                state = ServerFinderState.DONE;
            }
        }
    }
}

