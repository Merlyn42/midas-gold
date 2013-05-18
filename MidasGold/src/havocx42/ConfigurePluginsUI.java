package havocx42;

import javax.swing.JFrame;
import javax.swing.JList;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.AbstractListModel;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.JPanel;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Component;
import javax.swing.Box;
import java.awt.Dialog.ModalityType;
import javax.swing.JScrollPane;
import java.awt.FlowLayout;

public class ConfigurePluginsUI extends JDialog implements TableModelListener, ActionListener {
	Object[][]							values;
	private JTable						table;
	PluginLoader						pluginLoader;
	Entry<ConverterPlugin, Boolean>[]	plugins	= new Entry[0];

	public ConfigurePluginsUI(PluginLoader plIn) {
		super();

		pluginLoader = plIn;
		plugins = pluginLoader.getPlugins().entrySet().toArray(plugins);
		values = new Object[plugins.length][];
		for (int i = 0; i < plugins.length; i++) {
			values[i] = new Object[] { plugins[i].getKey().getPluginName(), plugins[i].getValue() };
		}

		setLocationRelativeTo(null);

		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		table = new JTable();
		table.setBackground(new Color(255, 255, 255));

		table.setModel(new DefaultTableModel(values, new String[] { "Plugin", "Enabled?" }) {
			Class[]	columnTypes	= new Class[] { String.class, Boolean.class };

			public Class getColumnClass(int columnIndex) {
				return columnTypes[columnIndex];
			}
		});
		table.getModel().addTableModelListener(this);
		table.getColumnModel().getColumn(1).setPreferredWidth(55);

		JScrollPane scrollPane = new JScrollPane(table);
		panel.add(scrollPane);

		JButton btnNewButton = new JButton("OK");
		btnNewButton.setActionCommand("save");
		btnNewButton.addActionListener(this);
		getContentPane().add(btnNewButton, BorderLayout.SOUTH);
		setModalityType(ModalityType.TOOLKIT_MODAL);
		pack();
		setVisible(true);

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		this.dispose();

	}

	@Override
	public void tableChanged(TableModelEvent e) {
		int row = e.getFirstRow();
		int column = e.getColumn();
		TableModel model = (TableModel) e.getSource();
		Object data = model.getValueAt(row, column);
		if ((Boolean) data) {
			pluginLoader.enablePlugin(plugins[row].getKey());
		} else {
			pluginLoader.disablePlugin(plugins[row].getKey());

		}

	}

}
