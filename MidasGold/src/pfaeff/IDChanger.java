/*
 * Copyright 2011 Kai Rohr 
 *    
 *
 *    This file is part of mIDas.
 *
 *    mIDas is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    mIDas is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with mIDas.  If not, see <http://www.gnu.org/licenses/>.
 */

package pfaeff;

import havocx42.ActionEngine;
import havocx42.BlockUID;
import havocx42.ConfigurePluginsUI;
import havocx42.EventQueueProxy;
import havocx42.FileListCellRenderer;
import havocx42.PluginLoader;
import havocx42.RunConfiguration;
import havocx42.Status;
import havocx42.TranslationExistsException;
import havocx42.TranslationRecord;
import havocx42.TranslationRecordFactory;
import havocx42.World;
import havocx42.logging.PopupHandler;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import region.RegionFile;
import java.awt.Component;
import javax.swing.JSeparator;
import javax.swing.border.EtchedBorder;

/*
 * TODO: Clean up, isolate visualization from logic and data
 */
public class IDChanger extends JFrame {
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 9149749206914440913L;
	private ArrayList<String>	idNames				= new ArrayList<String>();
	private ArrayList<File>		saveGames			= new ArrayList<File>();
	public Status				status				= new Status();
	public ArrayList<File>		worlds				= new ArrayList<File>();
	private ActionEngine		engine;
	public DefaultListModel<TranslationRecord> model = new DefaultListModel<TranslationRecord>();

	// Gui Elements
	private JComboBox			cb_selectSaveGame;
	private JComboBox			cb_selectSourceID;
	private JComboBox			cb_selectTargetID;

	public JList				li_ID;
	public PluginLoader			pluginLoader;

	private static Logger		logger				= Logger.getLogger(IDChanger.class.getName());

	public IDChanger(String title, ActionEngine	engine_in) throws SecurityException, IOException {
		super(title);

		// Init Data
		initLogger();
		initSaveGames();
		initIDNames();
		initPlugins();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		engine = engine_in;
				
		// Add GUI elements
		getContentPane().add(createOpenFilesPanel(), BorderLayout.PAGE_START);
		getContentPane().add(createChooseIDsPanel(), BorderLayout.LINE_START);
		getContentPane().add(createProgressPanel(), BorderLayout.LINE_END);
		pack();

		setLocationRelativeTo(null);
		setResizable(false);
		setVisible(true);
	}

	private void initLogger() {
		Logger rootLogger = Logger.getLogger("");
		PopupHandler popupHandler = new PopupHandler(this);
		rootLogger.addHandler(popupHandler);
	}

	private void initPlugins() {
		try {
			pluginLoader = new PluginLoader();
		} catch (FileNotFoundException e) {
			logger.log(Level.SEVERE, "Unable to load plugins: " + e.getMessage(), e);
			e.printStackTrace();
		}
		pluginLoader.loadPlugins();

	}

	private void initSaveGames() {
		File mcSavePath = getMCSavePath();
		if (mcSavePath.exists()) {
			saveGames = FileTools.getSubDirectories(mcSavePath);
		}
	}

	private void initIDNames() {
		try {
			String path = IDChanger.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			File f = new File((new File(path)).getParent(), "IDNames.txt");
			if (f.exists()) {
				idNames = readFile(f);
			} else {
				logger.info("IDNames.txt does not exist");
			}
		} catch (IOException | URISyntaxException e1) {
			logger.log(Level.WARNING, "Unable to load IDNames.txt", e1);
		}
	}

	public void addWorld() {
		final JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setSelectedFile(new File(getMCSavePath() + "/New World"));
		int returnVal = fc.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File f = fc.getSelectedFile();
			if (isValidSaveGame(f)) {
				if (!saveGames.contains(f)) {
					saveGames.add(f);

					cb_selectSaveGame.addItem(f);
					cb_selectSaveGame.setSelectedIndex(cb_selectSaveGame.getItemCount() - 1);

					// workaround for a bug that occurs when there was no
					// item in the beginning
					if (cb_selectSaveGame.getItemCount() > saveGames.size()) {
						cb_selectSaveGame.removeItemAt(0);
					}

					// System.out.println(saveGames.size());
					// System.out.println(cb_selectSaveGame.getItemCount());

				} else {
					cb_selectSaveGame.setSelectedIndex(saveGames.indexOf(f));
					JOptionPane.showMessageDialog(this, "The selected savegame is already in the list!", "Information",
							JOptionPane.INFORMATION_MESSAGE);
				}
			} else {
				JOptionPane.showMessageDialog(this, "Invalid savegame!", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	public File loadPatch() {
		File result = null;
		final JFileChooser fileChooser = new JFileChooser();
		String path;
		try {
			path = IDChanger.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			fileChooser.setSelectedFile(new File((new File(path)).getParent(), "Patch.txt"));
		} catch (URISyntaxException e1) {
			logger.log(Level.WARNING, "Unable to load Patch file", e1);
		}

		int returnVal = fileChooser.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File patch = fileChooser.getSelectedFile();
			if (patch.exists()) {
				result = patch;
			}
		}
		return result;
	}

	public void showMessage(String message, String title, int messageType) {
		JOptionPane.showMessageDialog(this, message, title, messageType);
	}

	public String getNewSourceID() {
		return (String) cb_selectSourceID.getSelectedItem();
	}

	public String getNewTargetID() {
		return (String) cb_selectTargetID.getSelectedItem();
	}

	public int getWorldIndex() {
		return cb_selectSaveGame.getSelectedIndex();
	}

	private JPanel createProgressPanel() {
		JPanel pnl_progress = new JPanel();
		pnl_progress.setLayout(new BoxLayout(pnl_progress, BoxLayout.PAGE_AXIS));
		pnl_progress.setBorder(BorderFactory.createTitledBorder("Progress"));
		pnl_progress.add(createStartPanel());
		pnl_progress.add(Box.createVerticalStrut(10));
		status.lb_file = new JLabel("Current File:");
		pnl_progress.add(status.lb_file);
		pnl_progress.add(createFileProgressBar());
		status.lb_chunk = new JLabel("Current Chunk:");
		pnl_progress.add(status.lb_chunk);
		pnl_progress.add(createChunkProgressBar());

		pnl_progress.setPreferredSize(new Dimension(400, pnl_progress.getHeight()));

		return pnl_progress;
	}

	private JPanel createStartPanel() {
		JPanel pnl_start = new JPanel(new FlowLayout());
		pnl_start.add(createStartButton());
		// pnl_start.add(createBackupCheckBox());
		return pnl_start;
	}

	private JProgressBar createFileProgressBar() {
		status.pb_file = new JProgressBar(0, 100);
		status.pb_file.setValue(0);
		status.pb_file.setStringPainted(true);
		return status.pb_file;
	}

	private JProgressBar createChunkProgressBar() {
		status.pb_chunk = new JProgressBar(0, 100);
		status.pb_chunk.setValue(0);
		status.pb_chunk.setStringPainted(true);
		return status.pb_chunk;
	}

	// new version changed From to Translations
	private JPanel createChooseIDsPanel() {
		JPanel pnl_chooseIDs = new JPanel();
		pnl_chooseIDs.setBorder(BorderFactory.createTitledBorder("Change IDs"));
		pnl_chooseIDs.setLayout(new BoxLayout(pnl_chooseIDs, BoxLayout.PAGE_AXIS));
		pnl_chooseIDs.add(new JLabel("Translations: "));
		pnl_chooseIDs.add(initIDPane());
		pnl_chooseIDs.add(initSourceIDPanel());
		pnl_chooseIDs.add(new JLabel("To:"));
		pnl_chooseIDs.add(createSelectTargetIDComboBox());

		return pnl_chooseIDs;
	}

	private JPanel initSourceIDPanel() {
		JPanel pnl_sourceID = new JPanel(new FlowLayout());
		pnl_sourceID.add(createSelectSourceIDComboBox());
		pnl_sourceID.add(createAddIDButton());
		pnl_sourceID.add(createRemoveIDButton());
		return pnl_sourceID;
	}

	private JButton createStartButton() {
		JButton btn_start = new JButton("Start");
		btn_start.addActionListener(engine);
		btn_start.setActionCommand("start");
		return btn_start;
	}

	// new version updated buttons
	private JButton createRemoveIDButton() {
		JButton btn_removeID = new JButton("Remove Translation");
		btn_removeID.addActionListener(engine);
		btn_removeID.setActionCommand("removeID");
		return btn_removeID;
	}

	private JButton createAddIDButton() {
		JButton btn_addID = new JButton("Add Translation");
		btn_addID.addActionListener(engine);
		btn_addID.setActionCommand("addID");
		return btn_addID;
	}

	// new version
	private JPanel createOpenFilesPanel() {
		JPanel pnl_openFiles = new JPanel(new FlowLayout());
		pnl_openFiles.setBorder(BorderFactory.createTitledBorder("Setup"));
		{
			JPanel patchPanel = new JPanel();
			patchPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
			pnl_openFiles.add(patchPanel);
			JLabel label = new JLabel("Load patch file:");
			patchPanel.add(label);
			JButton btn_openFile = new JButton("Load");
			patchPanel.add(btn_openFile);
			btn_openFile.addActionListener(engine);
			btn_openFile.setActionCommand("openPatch");
		}
		{
			JPanel savegamePanel = new JPanel();
			savegamePanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
			pnl_openFiles.add(savegamePanel);
			JLabel label = new JLabel("Available savegames:");
			savegamePanel.add(label);

			cb_selectSaveGame = new JComboBox(saveGames.toArray());
			savegamePanel.add(cb_selectSaveGame);
			FileListCellRenderer renderer = new FileListCellRenderer();
			renderer.maxIndex = saveGames.size() - 1;
			cb_selectSaveGame.setRenderer(renderer);
			JButton btn_openFile = new JButton("Add savegame");
			savegamePanel.add(btn_openFile);
			btn_openFile.addActionListener(engine);
			btn_openFile.setActionCommand("openFolder");
			{
				JPanel pluginPanel = new JPanel();
				pluginPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
				pnl_openFiles.add(pluginPanel);
				{
					JButton btnConfigurePlugins = new JButton("Configure Plugins");
					btnConfigurePlugins.setActionCommand("openPlugins");
					btnConfigurePlugins.addActionListener(engine);
					pluginPanel.add(btnConfigurePlugins);
				}
			}

			cb_selectSaveGame.addActionListener(engine);
		}

		return pnl_openFiles;
	}

	private JScrollPane initIDPane() {
		JScrollPane pn_ID = new JScrollPane(initIDTextArea());
		pn_ID.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		return pn_ID;
	}

	private JList initIDTextArea() {
		li_ID = new JList(model);
		return li_ID;
	}
	
	public void addTranslation(TranslationRecord tr)
			throws TranslationExistsException {
		for (int i = 0; i < model.size(); i++) {
			if (model.get(i).source.equals(tr.source)) {
				throw new TranslationExistsException("Source ID " + tr.source
						+ " is already being translated!");
			}
		}
		int index = 0;
		if (model.getSize() > 0) {
			index = model.getSize();
		}
		model.add(index, tr);
		return;
	}
	
	public void removeTranslation(int index){
		model.remove(index);
	}
	
	public void loadTranslationsFromFile(File patch){
		try {
			FileInputStream fstream = new FileInputStream(patch);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(
					new InputStreamReader(in));
			String strLine;
			TranslationRecord tr;
			while ((strLine = br.readLine()) != null) {
				try {
					tr = TranslationRecordFactory
							.createTranslationRecord(strLine);
					if (tr != null) {
						addTranslation(tr);
					} else {
						logger.info("Patch contains an invalid line, no big deal: "
								+ strLine);
					}

				} catch (NumberFormatException e2) {
					// JOptionPane.showMessageDialog(this,
					// "That's not how you format translations \""+strLine+System.getProperty("line.separator")+"example:"+System.getProperty("line.separator")+"1 stone -> 3 dirt",
					// "Error", JOptionPane.ERROR_MESSAGE);
					logger.info("Patch contains an invalid line, no big deal"
							+ strLine);
					continue;
				}

			}
			br.close();
			in.close();
			fstream.close();

		} catch (Exception filewriting) {
			logger.log(Level.WARNING, "Unable to open patch file", filewriting);
		}
	}

	private JComboBox createSelectSourceIDComboBox() {
		if (idNames.size() > 0) {
			cb_selectSourceID = new JComboBox(idNames.toArray());
		} else {
			cb_selectSourceID = new JComboBox();
		}

		CBRenderer renderer = new CBRenderer();
		renderer.maxIndex = idNames.size() - 1;

		cb_selectSourceID.setEditable(true);
		cb_selectSourceID.setRenderer(renderer);

		cb_selectSourceID.addActionListener(new NumberOnlyActionListener(idNames, 0, 32000 - 1));
		return cb_selectSourceID;
	}

	private JComboBox createSelectTargetIDComboBox() {
		if (idNames.size() > 0) {
			cb_selectTargetID = new JComboBox(idNames.toArray());
		} else {
			cb_selectTargetID = new JComboBox();
		}

		CBRenderer renderer = new CBRenderer();
		renderer.maxIndex = idNames.size() - 1;

		cb_selectTargetID.setEditable(true);
		cb_selectTargetID.setRenderer(renderer);

		cb_selectTargetID.addActionListener(new NumberOnlyActionListener(idNames, 0, 32000 - 1));
		return cb_selectTargetID;
	}

	private File getMCSavePath() {
		return new File(getAppDir("minecraft"), "saves");
	}

	public static File getAppDir(String s) {
		String osName = System.getProperty("os.name");
		OperatingSystem operatingSystem = OperatingSystem.resolve(osName);
		OperatingSystemFamily family = OperatingSystemFamily.WINDOWS;
		if (operatingSystem != null) {
			family = operatingSystem.getFamily();
		}
		switch (family) {
		case WINDOWS:
			return new File(System.getenv("appdata"), "." + s);
		case LINUX:
			return new File(System.getProperty("user.home", "."), s);
		case MAC: {
			return new File(System.getProperty("user.home", "."), "Library/Application Support/" + s);
		}
		default:
			return new File(System.getProperty("user.home", "."), s);
		}
	}

	private boolean isValidSaveGame(File f) {
		ArrayList<RegionFile> rf;
		try {
			rf = NBTFileIO.getRegionFiles(f);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Unable to load save file", e);
			return false;
		}
		return ((rf != null) && (rf.size() > 0));

	}

	private ArrayList<String> readFile(File f) throws IOException {
		ArrayList<String> result = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = br.readLine();
		while (line != null) {
			/*
			 * if (!line.equals("")) { line = " " + line; } line = index + line;
			 */
			try {
				if (line.matches("[0-9]+(:?[0-9]+)? [0-9a-zA-Z.]+"))
					result.add(line);

			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this, "That's not how you format IDNames \"" + line + System.getProperty("line.separator")
						+ "example:" + System.getProperty("line.separator") + "1 stone", "Error", JOptionPane.ERROR_MESSAGE);
				logger.config("User tried to input incorrectly formatted IDNames, no big deal");
			}
			line = br.readLine();
		}
		br.close();
		return result;
	}
}
