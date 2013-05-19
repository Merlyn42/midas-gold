package havocx42;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;

public class RunConfiguration {
	public PluginLoader pluginLoader;
	public ArrayList<File> worlds = new ArrayList<File>();
	private DefaultListModel<TranslationRecord> translations = new DefaultListModel<TranslationRecord>();

	public void addTranslation(TranslationRecord tr)
			throws TranslationExistsException {
		for (int i = 0; i < translations.size(); i++) {
			if (translations.get(i).source.equals(tr.source)) {
				throw new TranslationExistsException("Source ID " + tr.source
						+ " is already being translated!");
			}
		}
		int index = 0;
		if (translations.getSize() > 0) {
			index = translations.getSize();
		}
		translations.add(index, tr);
		return;
	}
	
	public void removeTranslation(int index){
		translations.remove(index);
	}
	
	public HashMap<BlockUID, BlockUID> getTranslations(){
		HashMap<BlockUID, BlockUID> result =  new HashMap<BlockUID, BlockUID>();
		for (int i = 0; i < translations.size(); i++) {
			TranslationRecord tr = (TranslationRecord) translations.get(i);
			if (tr.source != null && tr.target != null) {
				result.put(tr.source, tr.target);
			}

		}
		return result;
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
			logger.log(Level.WARNING, "Unable to open patch file",
					filewriting);
	}
		
		public String verify(){
			String result = null;
			if ((worlds == null) || (worlds.size() == 0)) {
				result="No save game has been chosen!";

			}
			if (translations.size() == 0) {
				result="No IDs have been chosen!";
			}
			return result;
		}

}
