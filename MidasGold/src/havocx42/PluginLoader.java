package havocx42;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PluginLoader {
	private File					pluginDirectory;
	Map<ConverterPlugin, Boolean>	classes;
	private Logger					logger	= Logger.getLogger(this.getClass().getName());

	public PluginLoader() throws FileNotFoundException {
		pluginDirectory = new File("plugins/");
		if (!pluginDirectory.exists()) {
			throw new FileNotFoundException("Unable to find the plugins directory");
		}
		if (!pluginDirectory.isDirectory()) {
			throw new FileNotFoundException("Unable to find the plugins directory");
		}
	}

	public void loadPlugins() {
		URLClassLoader loader = null;
		Enumeration<URL> props = null;
		InputStream stream = null;
		classes = new HashMap<ConverterPlugin, Boolean>();

		loader = (URLClassLoader) ClassLoader.getSystemClassLoader();

		try (MyClassLoader myLoader = new MyClassLoader(loader.getURLs())) {
			ExtensionFilter filter = new ExtensionFilter("jar");
			File[] files = pluginDirectory.listFiles(filter);
			for (File file : files) {
				logger.info("Loading plugin: " + file.getName());
				myLoader.addURL(file.toURI().toURL());
			}

			props = myLoader.getResources("midasPlugin.properties");
			while (props.hasMoreElements()) {
				Properties prop = new Properties();
				stream = props.nextElement().openStream();
				prop.load(stream);
				logger.info("loading class: " + prop.getProperty("class"));
				String classLocation;
				if ((classLocation = prop.getProperty("class")) != null) {
					try {
						ConverterPlugin plugin = (ConverterPlugin) myLoader.loadClass(classLocation).newInstance();
						logger.info("Loaded Plugin: " + plugin.getPluginName());
						classes.put(plugin, true);
					} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
						logger.log(Level.SEVERE, "Unable to load plugin: " + e.getMessage(), e);
					}
				}
			}
		} catch (IOException e1) {
			logger.log(Level.SEVERE, "Unable to load plugins: " + e1.getMessage(), e1);
		}
	}

	public ArrayList<ConverterPlugin> getPluginsOfType(PluginType type) {
		ArrayList<ConverterPlugin> plugins = new ArrayList<ConverterPlugin>();
		for (Entry<ConverterPlugin, Boolean> plugin : classes.entrySet()) {
			if (plugin.getValue() && plugin.getKey().getPluginType() == type) {
				plugins.add(plugin.getKey());
			}
		}

		return plugins;
	}

	public Map<ConverterPlugin, Boolean> getPlugins() {
		return classes;
	}

	public void enablePlugin(ConverterPlugin plugin) {
		classes.put(plugin, true);
	}

	public void disablePlugin(ConverterPlugin plugin) {
		classes.put(plugin, false);
	}

}
