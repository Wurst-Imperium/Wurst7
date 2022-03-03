/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.serverfinder;

import net.wurstclient.util.SwingUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public final class ImportServersFileChooser extends JFileChooser
{
	public static void main(String[] args)
	{
		SwingUtils.setLookAndFeel();
		JFileChooser fileChooser = new ImportServersFileChooser(new File(args[0]));
		
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("TXT file", "txt"));
		fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("LIST file", "list"));
		fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("SERVERS file", "servers"));
		
		if(fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
			return;
		
		String path = fileChooser.getSelectedFile().getAbsolutePath();
		System.out.println(path);
	}
	
	public ImportServersFileChooser(File currentDirectory)
	{
		super(currentDirectory);
	}
	
	@Override
	protected JDialog createDialog(Component parent) throws HeadlessException
	{
		JDialog dialog = super.createDialog(parent);
		dialog.setAlwaysOnTop(true);
		return dialog;
	}
	
}
