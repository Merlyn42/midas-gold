package havocx42;

import java.awt.EventQueue;
import java.awt.Toolkit;
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
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import pfaeff.IDChanger;

public class ActionEngine implements ActionListener {
	RunConfiguration config;
	public IDChanger ui;
	private static Logger logger = Logger.getLogger(ActionEngine.class.getName());
	
	public ActionEngine(RunConfiguration config) {
		super();
		this.config = config;
	}

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
			ui.loadTranslationsFromFile(patch);
		}

		// Add ID
		// new version adds user stupidity resistance II
		if ("addID".equals(e.getActionCommand())) {
			try {
				String currentSource = ui.getNewSourceID();
				String currentTarget = ui.getNewTargetID();

				TranslationRecord tr = TranslationRecordFactory.createTranslationRecord(currentSource, currentTarget);
				if (tr != null) {
					ui.addTranslation(tr);
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
				ui.removeTranslation(ui.li_ID.getSelectedIndices()[i]);
			}
		}
		
		// Start
		if ("start".equals(e.getActionCommand())) {
			String message = config.verify();
			if(message!=null){
				ui.showMessage(message, "Warning", JOptionPane.WARNING_MESSAGE);
			}

		}
		return;
	}
	
	private static void initRootLogger() throws SecurityException, IOException {

		FileHandler fileHandler;
		fileHandler = new FileHandler("midasLog.%u.%g.log", 1024 * 1024, 3, true);
		fileHandler.setLevel(Level.CONFIG);
		Logger rootLogger = Logger.getLogger("");
		Handler[] handlers = rootLogger.getHandlers();
		for (Handler handler : handlers) {
			handler.setLevel(Level.INFO);
		}
		rootLogger.setLevel(Level.CONFIG);
		rootLogger.addHandler(fileHandler);
	}
	
	public static void main(String[] args) {
		try {
			initRootLogger();
		} catch (SecurityException | IOException e) {
			logger.log(Level.WARNING, "Unable to create log File", e);
			return;
		}
		EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
		queue.push(new EventQueueProxy());

		try {
			// Use system specific look and feel
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			logger.log(Level.WARNING, "Unable to set look and feel", e);
		}
		try {
			RunConfiguration config = new RunConfiguration();
			ActionEngine engine = new ActionEngine(config);
			IDChanger frame = new IDChanger("mIDas *GOLD* V0.2.5",engine);
			
			

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Runtime Exception", e);
		}

		logger.config("System Look and Feel:"
				+ UIManager.getSystemLookAndFeelClassName().toString());

	}



}
