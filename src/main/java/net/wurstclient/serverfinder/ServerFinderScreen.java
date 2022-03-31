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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Util;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IMultiplayerScreen;
import net.wurstclient.util.MathUtils;

public class ServerFinderScreen extends Screen implements IServerFinderDoneListener {
	private MultiplayerScreen prevScreen;

	private TextFieldWidget ipBox;
	private TextFieldWidget versionBox;
	private TextFieldWidget maxThreadsBox;
	private TextFieldWidget targetCheckedBox;
	private ButtonWidget searchButton;

	private ServerFinderState state;
	private int maxThreads;
	private volatile int numActiveThreads;
	private volatile int checked;
	private volatile int working;

	private int targetChecked = 512;

	private Stack<String> ipsToPing = new Stack<>();

	private final Object serverFinderLock = new Object();
	
	public static ServerFinderScreen instance = null;
	private static int searchNumber = 0;
	
	private ArrayList<String> versionFilters = new ArrayList<>();
	private int playerCountFilter = 0;
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
		synchronized(serverFinderLock) {
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
				for (String ip: ips) {
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
		
		String fileOutput = "";
		for (IPAddress ip : hashedIPs) {
			String stringIP = ip.toString();
			if (stringIP != null)
				fileOutput += stringIP + "\n";
		}
		try (PrintWriter pw = new PrintWriter(filePath.toFile())) {
			pw.print(fileOutput);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		saveToFileMessage = "\u00a76Saved " + newIPs + " new IP" + (newIPs == 1 ? "" : "s");
	}

	@Override
	public void init()
	{
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
		addDrawableChild(new ButtonWidget(width / 2 - 100, 130, 200, 20, new LiteralText("Scan Ports: " + (scanPorts ? "Yes" : "No")),
				b -> b.setMessage(new LiteralText("Scan Ports: " + ((scanPorts = !scanPorts) ? "Yes" : "No")))));

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
				b -> client.setScreen(prevScreen)));

	}

	private void searchOrCancel() {
		if (state.isRunning()) {
			state = ServerFinderState.CANCELLED;
			return;
		}

		state = ServerFinderState.RESOLVING;
		maxThreads = Integer.parseInt(maxThreadsBox.getText());
		targetChecked = Integer.parseInt(targetCheckedBox.getText());
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

	private void findServers() {
		try {
			InetAddress addr = InetAddress.getByName(ipBox.getText().split(":")[0].trim());

			int[] ipParts = new int[4];
			for (int i = 0; i < 4; i++)
				ipParts[i] = addr.getAddress()[i] & 0xff;

			state = ServerFinderState.SEARCHING;
			int[] changes = { 0, 1, -1, 2, -2, 3, -3 };
			for (int change : changes)
				for (int i2 = 0; i2 <= 255; i2++) {
					if (state == ServerFinderState.CANCELLED)
						return;

					int[] ipParts2 = ipParts.clone();
					ipParts2[2] = ipParts[2] + change & 0xff;
					ipParts2[3] = i2;
					String ip = ipParts2[0] + "." + ipParts2[1] + "." + ipParts2[2] + "." + ipParts2[3];

					ipsToPing.push(ip);
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
				width / 2,20, 16777215);
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
		drawStringWithShadow(matrixStack, textRenderer, "Max. Check:",
				width / 2 + 3, 115, 10526880);
		targetCheckedBox.render(matrixStack, mouseX, mouseY, partialTicks);
		drawStringWithShadow(matrixStack, textRenderer, "Checked: " + checked + " / " + targetCheckedBox.getText(),
				width / 2 - 100, 155,10526880);

		drawStringWithShadow(matrixStack, textRenderer, "Found Working: " + working,
				width / 2 - 100, 165, 10526880);

		drawStringWithShadow(matrixStack, textRenderer, "Filter by version. Example: 1.18.1;1.18.2",
				width / 2 - 100, 175, 10526880);
		versionBox.render(matrixStack, mouseX, mouseY, partialTicks);

		super.render(matrixStack, mouseX, mouseY, partialTicks);
	}

	@Override
	public void close()
	{
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
		synchronized (serverFinderLock) {
			checked++;
			numActiveThreads--;
		}
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
		while (numActiveThreads < maxThreads && pingNewIP());
		synchronized (serverFinderLock) {
			if (checked >= targetChecked) {
				state = ServerFinderState.DONE;
			}
		}
	}

	@Override
	public void onServerFailed(WurstServerPinger pinger) {
		if (state == ServerFinderState.CANCELLED || pinger == null || pinger.getSearchNumber() != searchNumber)
			return;
		synchronized (serverFinderLock) {
			checked++;
			numActiveThreads--;
		}
		while (numActiveThreads < maxThreads && pingNewIP());
		synchronized (serverFinderLock) {
			if (checked >= targetChecked) {
				state = ServerFinderState.DONE;
			}
		}
	}
}
