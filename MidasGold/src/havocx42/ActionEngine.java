package havocx42;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import pfaeff.IDChanger;

public class ActionEngine implements ActionListener {
	RunConfiguration config;
	IDChanger ui;
	private static Logger logger = Logger.getLogger(ActionEngine.class.getName());

	@Override
	public void actionPerformed(ActionEvent e) {
		// something changed - reset progress bars
		//TODO status.pb_file.setValue(0);
		//TODO status.pb_chunk.setValue(0);

		// Open configure plugins screen
		if ("openPlugins".equals(e.getActionCommand())) {
			ConfigurePluginsUI configurePluginsUI = new ConfigurePluginsUI(config.pluginLoader);
		}

		// Open Save Folder
		if ("openFolder".equals(e.getActionCommand())) {
			ui.addWorld();
		}

		// new version open a patch file
		if ("openPatch".equals(e.getActionCommand())) {
			File patch = ui.loadPatch();
			config.loadTranslationsFromFile(patch);
		}

		// Add ID
		// new version adds user stupidity resistance II
		if ("addID".equals(e.getActionCommand())) {
			try {
				String currentSource = ui.getNewSourceID();
				String currentTarget = ui.getNewTargetID();

				TranslationRecord tr = TranslationRecordFactory.createTranslationRecord(currentSource, currentTarget);
				if (tr != null) {
					config.addTranslation(tr);
				} else {
					ui.showMessage("That's not how you format translations" + System.getProperty("line.separator")
							+ "example:" + System.getProperty("line.separator") + "1 stone" + System.getProperty("line.separator")
							+ "3 dirt", "Error", JOptionPane.ERROR_MESSAGE);
				}
			} catch (NumberFormatException badinput) {
				ui.showMessage("That's not how you format translations" + System.getProperty("line.separator")
						+ "example:" + System.getProperty("line.separator") + "1 stone -> 3 dirt", "Error", JOptionPane.ERROR_MESSAGE);
			} catch (TranslationExistsException ex){
				ui.showMessage(ex.getMessage(), "Information", JOptionPane.INFORMATION_MESSAGE);
			}
		}

		// Remove ID
		if ("removeID".equals(e.getActionCommand())) {
			for (int i = ui.li_ID.getSelectedIndices().length - 1; i >= 0; i--) {
				config.removeTranslation(ui.li_ID.getSelectedIndices()[i]);
			}
		}
		
		// Start
		if ("start".equals(e.getActionCommand())) {
			// World
			int worldIndex = ui.getWorldIndex();


			if (worldIndex < 0) {
				return;
			}
			
			String message = config.verify();
			if(message!=null){
				ui.showMessage(message, "Warning", JOptionPane.WARNING_MESSAGE);
			}

			final HashMap<BlockUID, BlockUID> translations = config.getTranslations();

			final World world;
			try {
				world = new World(config.worlds.get(worldIndex));

				SwingWorker worker = new SwingWorker() {
					
					@Override
					protected Object doInBackground() throws Exception {
						world.convert(ui.status, translations, config.pluginLoader);
						return null;
					}
				};
				ui.showMessage("Done in " + duration + "ms" + System.getProperty("line.separator") + ui.status.changedPlaced
						+ " placed blocks changed." + System.getProperty("line.separator") + ui.status.changedPlayer
						+ " blocks in player inventories changed." + System.getProperty("line.separator") + ui.status.changedChest
						+ " blocks in entity inventories changed.", "Information", JOptionPane.INFORMATION_MESSAGE);
			}

				// worker.addPropertyChangeListener(this);
				worker.execute();
			} catch (IOException e1) {
				logger.log(Level.WARNING, "Unable to open world, are you sure you have selected a save?");
			}
		}
		return;
	}

}
