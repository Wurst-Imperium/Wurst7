/*
 * Copyright (C) 2014 - 2020 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ForceOpDialog extends JDialog
{
	public static void main(String[] args)
	{
		SwingUtils.setLookAndFeel();
		new ForceOpDialog(args[0]);
	}
	
	private final ArrayList<Component> components = new ArrayList<>();
	
	private JSpinner spDelay;
	private JCheckBox cbDontWait;
	
	private JLabel lPasswords;
	private JLabel lTime;
	private JLabel lAttempts;
	
	private int numPW = 50;
	private int lastPW = -1;
	
	public ForceOpDialog(String username)
	{
		super((JFrame)null, "ForceOP", false);
		setAlwaysOnTop(true);
		setSize(512, 248);
		setResizable(false);
		setLocationRelativeTo(null);
		setLayout(null);
		SwingUtils.setExitOnClose(this);
		
		addLabel("Password list", 4, 4);
		addPwListSelector();
		addHowToUseButton();
		
		addSeparator(4, 56, 498, 4);
		
		addLabel("Speed", 4, 64);
		addDelaySelector();
		addDontWaitCheckbox();
		
		addSeparator(4, 132, 498, 4);
		
		addLabel("Username: " + username, 4, 140);
		lPasswords = addLabel("Passwords: error", 4, 160);
		lTime = addPersistentLabel("Estimated time: error", 4, 180);
		lAttempts = addPersistentLabel("Attempts: error", 4, 200);
		addStartButton();
		
		updateNumPasswords();
		setVisible(true);
		toFront();
		
		new Thread(() -> handleDialogInput(), "ForceOP dialog input").start();
	}
	
	private void handleDialogInput()
	{
		try(BufferedReader bf = new BufferedReader(
			new InputStreamReader(System.in, StandardCharsets.UTF_8)))
		{
			for(String line = ""; (line = bf.readLine()) != null;)
				messageFromWurst(line);
			
		}catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private void messageFromWurst(String line)
	{
		if(line.startsWith("numPW "))
		{
			numPW = Integer.parseInt(line.substring(6));
			updateNumPasswords();
			return;
		}
		
		if(line.startsWith("index "))
		{
			lastPW = Integer.parseInt(line.substring(6));
			updateTimeLabel();
			updateAttemptsLabel();
		}
	}
	
	private void addPwListSelector()
	{
		JRadioButton rbDefaultList = new JRadioButton("default", true);
		rbDefaultList.setLocation(4, 24);
		rbDefaultList.setSize(rbDefaultList.getPreferredSize());
		add(rbDefaultList);
		
		JRadioButton rbTXTList = new JRadioButton("TXT file", false);
		rbTXTList.setLocation(
			rbDefaultList.getX() + rbDefaultList.getWidth() + 4, 24);
		rbTXTList.setSize(rbTXTList.getPreferredSize());
		add(rbTXTList);
		
		ButtonGroup rbGroup = new ButtonGroup();
		rbGroup.add(rbDefaultList);
		rbGroup.add(rbTXTList);
		
		JButton bBrowse = new JButton("browse");
		bBrowse.setLocation(rbTXTList.getX() + rbTXTList.getWidth() + 4, 24);
		bBrowse.setSize(bBrowse.getPreferredSize());
		bBrowse.setEnabled(rbTXTList.isSelected());
		bBrowse.addActionListener(e -> browsePwList());
		add(bBrowse);
		
		rbDefaultList.addActionListener(e -> {
			bBrowse.setEnabled(false);
			System.out.println("list default");
		});
		
		rbTXTList.addActionListener(e -> {
			bBrowse.setEnabled(true);
		});
	}
	
	private void browsePwList()
	{
		JFileChooser fsTXTList = new JFileChooser();
		fsTXTList.setAcceptAllFileFilterUsed(false);
		fsTXTList.addChoosableFileFilter(
			new FileNameExtensionFilter("TXT files", new String[]{"txt"}));
		fsTXTList.setFileSelectionMode(JFileChooser.FILES_ONLY);
		
		int action = fsTXTList.showOpenDialog(this);
		if(action != JFileChooser.APPROVE_OPTION)
			return;
		
		if(!fsTXTList.getSelectedFile().exists())
		{
			JOptionPane.showMessageDialog(this, "File does not exist!", "Error",
				JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		String pwList = fsTXTList.getSelectedFile().getPath();
		System.out.println("list " + pwList);
	}
	
	private void addHowToUseButton()
	{
		JButton bHowTo = new JButton("How to use");
		bHowTo.setFont(new Font(bHowTo.getFont().getName(), Font.BOLD, 16));
		bHowTo.setSize(bHowTo.getPreferredSize());
		bHowTo.setLocation(506 - bHowTo.getWidth() - 32, 12);
		bHowTo.addActionListener(e -> openHowToUseLink());
		add(bHowTo);
	}
	
	private void openHowToUseLink()
	{
		try
		{
			String howToLink =
				"https://www.wurstclient.net/Mods/Force_OP_(AuthMeCracker)/";
			
			Desktop.getDesktop().browse(URI.create(howToLink));
			
		}catch(IOException e2)
		{
			throw new RuntimeException(e2);
		}
	}
	
	private void addDelaySelector()
	{
		JLabel lDelay1 = addLabel("Delay between attempts:", 4, 84);
		
		spDelay = new JSpinner();
		spDelay.setToolTipText("<html>"
			+ "50ms: Fastest, doesn't bypass AntiSpam plugins<br>"
			+ "1000ms: Recommended, bypasses most AntiSpam plugins<br>"
			+ "10000ms: Slowest, bypasses all AntiSpam plugins" + "</html>");
		spDelay.setModel(new SpinnerNumberModel(1000, 50, 10000, 50));
		spDelay.setLocation(lDelay1.getX() + lDelay1.getWidth() + 4, 84);
		spDelay.setSize(60, (int)spDelay.getPreferredSize().getHeight());
		spDelay.addChangeListener(e -> updateTimeLabel());
		add(spDelay);
		
		addLabel("ms", spDelay.getX() + spDelay.getWidth() + 4, 84);
	}
	
	private void addDontWaitCheckbox()
	{
		cbDontWait = new JCheckBox("<html>Don't wait for "
			+ "\"<span style=\"color: red;\"><b>Wrong password!</b></span>\" "
			+ "messages</html>", false);
		cbDontWait
			.setToolTipText("Increases the speed but can cause inaccuracy.");
		cbDontWait.setLocation(4, 104);
		cbDontWait.setSize(cbDontWait.getPreferredSize());
		cbDontWait.addActionListener(e -> updateTimeLabel());
		add(cbDontWait);
	}
	
	private void addStartButton()
	{
		JButton bStart = new JButton("Start");
		bStart.setFont(new Font(bStart.getFont().getName(), Font.BOLD, 18));
		bStart.setLocation(506 - 192 - 12, 144);
		bStart.setSize(192, 66);
		bStart.addActionListener(e -> startForceOP());
		add(bStart);
	}
	
	private JLabel addLabel(String text, int x, int y)
	{
		JLabel label = makeLabel(text, x, y);
		add(label);
		return label;
	}
	
	/**
	 * Adds a label that won't be disabled when the Start button is pressed.
	 */
	private JLabel addPersistentLabel(String text, int x, int y)
	{
		JLabel label = makeLabel(text, x, y);
		super.add(label);
		return label;
	}
	
	private JLabel makeLabel(String text, int x, int y)
	{
		JLabel label = new JLabel(text);
		label.setLocation(x, y);
		label.setSize(label.getPreferredSize());
		return label;
	}
	
	private void addSeparator(int x, int y, int width, int height)
	{
		JSeparator sepSpeedStart = new JSeparator();
		sepSpeedStart.setLocation(x, y);
		sepSpeedStart.setSize(width, height);
		add(sepSpeedStart);
	}
	
	@Override
	public Component add(Component comp)
	{
		components.add(comp);
		return super.add(comp);
	}
	
	private void updateNumPasswords()
	{
		updatePasswordsLabel();
		updateTimeLabel();
		updateAttemptsLabel();
	}
	
	private void updatePasswordsLabel()
	{
		lPasswords.setText("Passwords: " + numPW);
		lPasswords.setSize(lPasswords.getPreferredSize());
	}
	
	private void updateTimeLabel()
	{
		int remainingPW = numPW - (lastPW + 1);
		long timeMS = remainingPW * (int)spDelay.getValue();
		
		// AutoReconnect time (5s every 30s)
		timeMS += (int)(timeMS / 30000 * 5000);
		
		// "wrong password" wait time (estimated 50ms per password)
		// actual value varies with lag, which cannot be predicted
		if(!cbDontWait.isSelected())
			timeMS += remainingPW * 50;
		
		String timeString = getTimeString(timeMS);
		
		lTime.setText("Estimated time: " + timeString);
		lTime.setSize(lTime.getPreferredSize());
	}
	
	private String getTimeString(long ms)
	{
		TimeUnit uDays = TimeUnit.DAYS;
		TimeUnit uHours = TimeUnit.HOURS;
		TimeUnit uMin = TimeUnit.MINUTES;
		TimeUnit uMS = TimeUnit.MILLISECONDS;
		
		long days = uMS.toDays(ms);
		long hours = uMS.toHours(ms) - uDays.toHours(days);
		long minutes = uMS.toMinutes(ms) - uHours.toMinutes(uMS.toHours(ms));
		long seconds = uMS.toSeconds(ms) - uMin.toSeconds(uMS.toMinutes(ms));
		
		return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
	}
	
	private void updateAttemptsLabel()
	{
		lAttempts.setText("Attempts: " + (lastPW + 1) + "/" + numPW);
		lAttempts.setSize(lAttempts.getPreferredSize());
	}
	
	private void startForceOP()
	{
		components.forEach(c -> c.setEnabled(false));
		
		int delay = (int)spDelay.getValue();
		boolean waitForMsg = !cbDontWait.isSelected();
		System.out.println("start " + delay + " " + waitForMsg);
	}
}
