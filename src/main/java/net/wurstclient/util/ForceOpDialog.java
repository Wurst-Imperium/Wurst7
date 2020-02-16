/*
 * Copyright (C) 2014 - 2020 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;

import javax.swing.*;

public class ForceOpDialog extends JDialog
{
	public static void main(String[] args)
	{
		SwingUtils.setLookAndFeel();
		new ForceOpDialog();
	}
	
	private final ArrayList<Component> components = new ArrayList<>();
	
	public ForceOpDialog()
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
		
		addLabel("Username: error", 4, 140);
		addLabel("Passwords: error", 4, 160);
		addLabel("Estimated time: error", 4, 180);
		addLabel("Attempts: error", 4, 200);
		addStartButton();
		
		loadPWList();
		update();
		setVisible(true);
		toFront();
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
		
		JButton bTXTList = new JButton("browse");
		bTXTList.setLocation(rbTXTList.getX() + rbTXTList.getWidth() + 4, 24);
		bTXTList.setSize(bTXTList.getPreferredSize());
		bTXTList.setEnabled(rbTXTList.isSelected());
		add(bTXTList);
	}
	
	private void addHowToUseButton()
	{
		JButton bHowTo = new JButton("How to use");
		bHowTo.setFont(new Font(bHowTo.getFont().getName(), Font.BOLD, 16));
		bHowTo.setSize(bHowTo.getPreferredSize());
		bHowTo.setLocation(506 - bHowTo.getWidth() - 32, 12);
		add(bHowTo);
	}
	
	private void addDelaySelector()
	{
		JLabel lDelay1 = addLabel("Delay between attempts:", 4, 84);
		
		JSpinner spDelay = new JSpinner();
		spDelay.setToolTipText("<html>"
			+ "50ms: Fastest, doesn't bypass AntiSpam plugins<br>"
			+ "1000ms: Recommended, bypasses most AntiSpam plugins<br>"
			+ "10000ms: Slowest, bypasses all AntiSpam plugins" + "</html>");
		spDelay.setModel(new SpinnerNumberModel(1000, 50, 10000, 50));
		spDelay.setLocation(lDelay1.getX() + lDelay1.getWidth() + 4, 84);
		spDelay.setSize(60, (int)spDelay.getPreferredSize().getHeight());
		add(spDelay);
		
		addLabel("ms", spDelay.getX() + spDelay.getWidth() + 4, 84);
	}
	
	private void addDontWaitCheckbox()
	{
		JCheckBox cb = new JCheckBox("<html>Don't wait for "
			+ "\"<span style=\"color: red;\"><b>Wrong password!</b></span>\" "
			+ "messages</html>", false);
		cb.setToolTipText("Increases the speed but can cause inaccuracy.");
		cb.setLocation(4, 104);
		cb.setSize(cb.getPreferredSize());
		add(cb);
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
		JLabel label = new JLabel(text);
		label.setLocation(x, y);
		label.setSize(label.getPreferredSize());
		
		add(label);
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
	
	private void loadPWList()
	{
		// TODO
	}
	
	private void startForceOP()
	{
		components.forEach(c -> c.setEnabled(false));
		System.out.println("start");
	}
	
	private void update()
	{
		// TODO
	}
}
