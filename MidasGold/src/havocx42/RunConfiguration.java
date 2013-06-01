package havocx42;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import pfaeff.IDChanger;

public class RunConfiguration {
	public PluginLoader pluginLoader;
	public File world;
	HashMap<BlockUID, BlockUID> translations;
	public IDChanger ui;
	private static Logger logger = Logger.getLogger(new Object() { }.getClass().getEnclosingClass().getName());
	

	
	public void setTranslations(DefaultListModel<TranslationRecord> model){
		HashMap<BlockUID, BlockUID> result =  new HashMap<BlockUID, BlockUID>();
		for (int i = 0; i < model.size(); i++) {
			TranslationRecord tr = (TranslationRecord) model.get(i);
			if (tr.source != null && tr.target != null) {
				result.put(tr.source, tr.target);
			}
		}
		translations = result;
	}
		
		public String verify(){
			String result = null;
			if (translations.size() == 0) {
				result="No IDs have been chosen!";
			}
			return result;
		}

}
