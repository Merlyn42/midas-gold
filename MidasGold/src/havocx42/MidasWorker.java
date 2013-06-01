package havocx42;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import pfaeff.IDChanger;

import com.mojang.nbt.CompoundTag;
import com.mojang.nbt.NbtIo;

public class MidasWorker extends SwingWorker<Long, Status> {
	private IDChanger ui;
	private World world;
	private HashMap<BlockUID, BlockUID> translations;
	private PluginLoader pluginLoader;
	private static Logger logger = Logger.getLogger(new Object() { }.getClass().getEnclosingClass().getName());

	public MidasWorker(RunConfiguration config) throws IOException {
		super();
		this.ui = config.ui;
		this.world = new World(config.world);
		this.translations = config.getTranslations();
		this.pluginLoader =config.pluginLoader;
	}

	@Override
	protected Long doInBackground() throws Exception {
		ArrayList<RegionFileExtended>	regionFiles =world.getRegionFiles();
		ArrayList<PlayerFile>			playerFiles =world.getPlayerFiles();
		Status localStatus = new Status();
		localStatus.changedChest = 0;
		localStatus.changedPlaced = 0;
		localStatus.changedPlayer = 0;
		int count_file = 0;
		long beginTime = System.currentTimeMillis();

		// player inventories
		localStatus.pb_file.setMaximum(playerFiles.size() - 1);

		ArrayList<ConverterPlugin> regionPlugins = pluginLoader.getPluginsOfType(PluginType.REGION);
		ArrayList<ConverterPlugin> playerPlugins = pluginLoader.getPluginsOfType(PluginType.PLAYER);

		for (PlayerFile playerFile : playerFiles) {
			localStatus.pb_file.setValue(count_file++);
			localStatus.lb_file.setText("Current File: " + playerFile.getName());
			DataInputStream dis = null;
			DataOutputStream dos =null;
			try {
				dis = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(playerFile))));

				CompoundTag root = NbtIo.read(dis);
				for (ConverterPlugin plugin : playerPlugins) {
					plugin.convert(localStatus, root, translations);
				}
				 dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(playerFile)));
				NbtIo.writeCompressed(root, dos);
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Unable to convert player inventories", e);
				return System.currentTimeMillis() - beginTime;
			} finally {
				if(dos != null){
					try {
						dos.close();
					} catch (IOException e) {
						logger.log(Level.WARNING, "Unable to close output stream", e);
					}
				}
				if (dis != null) {
					try {
						dis.close();
					} catch (IOException e) {
						logger.log(Level.WARNING, "Unable to close input stream", e);
					}
				}
			}

		}
		// PROGESSBAR FILE
		count_file = 0;
		if (regionFiles == null) {
			// No valid region files found
			return System.currentTimeMillis() - beginTime;
		}
		localStatus.pb_file.setValue(0);
		localStatus.pb_file.setMaximum(regionFiles.size() - 1);

		for (RegionFileExtended r : regionFiles) {
			localStatus.lb_file.setText("Current File: " + r.fileName.getName());
			localStatus.pb_file.setMaximum(regionFiles.size() - 1);
			localStatus.pb_file.setValue(count_file++);
			

			try {
				r.convert(localStatus, translations, regionPlugins);
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Unable to convert placed blocks", e);
				return System.currentTimeMillis() - beginTime;
			}finally{
				if(r!=null){
						try {
							r.close();
						} catch (IOException e) {
							logger.log(Level.WARNING, "Unable to close region file",e);
						}
				}
			}
		}
		long duration = System.currentTimeMillis() - beginTime;
		return duration;
	}

}
