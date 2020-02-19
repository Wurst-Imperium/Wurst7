/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager;

import java.awt.Component;
import java.awt.HeadlessException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.wurstclient.util.SwingUtils;

public final class ImportAltsFileChooser extends JFileChooser
{
	public static void main(String[] args)
	{
		SwingUtils.setLookAndFeel();
		JFileChooser fileChooser = new ImportAltsFileChooser(new File(args[0]));
		
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.addChoosableFileFilter(
			new FileNameExtensionFilter("TXT file (username:password)", "txt"));
		
		if(fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
			return;
		
		File file = fileChooser.getSelectedFile();
		try
		{
			for(String line : Files.readAllLines(file.toPath()))
				System.out.println(line);
			
		}catch(IOException e)
		{
			e.printStackTrace();
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			String message = writer.toString();
			JOptionPane.showMessageDialog(fileChooser, message, "Error",
				JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public ImportAltsFileChooser(File currentDirectory)
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
