package views;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import entities.Tracker;
import models.TrackersManager;
import utilities.ErrorsLog;

public class TrackersPanel extends JPanel implements Observer {

	private static final long		serialVersionUID	= 1276595089834953384L;
	private JTable					trackersTable;
	private final DefaultTableModel	modelTable;
	private String[]				header;

	public TrackersPanel() {
		setBackground(new Color(102, 205, 170));

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
		setLayout(gridBagLayout);

		header = new String[4];
		header[0] = "ID";
		header[1] = "Master";
		header[2] = "Ultimo keep alive";
		header[3] = "Primera conexión";
		final String[][] content = new String[1][header.length];

		modelTable = new DefaultTableModel();
		modelTable.setDataVector(content, header);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBackground(new Color(102, 205, 170));
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.insets = new Insets(10, 10, 10, 10);
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 0;
		add(scrollPane, gbc_scrollPane);

		trackersTable = new JTable(modelTable);
		scrollPane.setViewportView(trackersTable);

		trackersTable.getTableHeader().setReorderingAllowed(false);
		trackersTable.setShowVerticalLines(true);
		trackersTable.setShowHorizontalLines(true);
		trackersTable.setDragEnabled(false);
		trackersTable.setSelectionForeground(Color.WHITE);
		trackersTable.setSelectionBackground(Color.BLUE);
		trackersTable.setForeground(Color.BLACK);
		trackersTable.setBackground(Color.WHITE);
		trackersTable.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 14));
		trackersTable.setRowHeight(30);

		trackersTable.getTableHeader().setFont(new Font("Arial", Font.PLAIN, 15));

		TrackersManager.getInstance().addObserver(this);
	}

	@Override
	public synchronized void update(Observable o, Object arg) {
		if (o == TrackersManager.getInstance()) {
			try {
				ConcurrentHashMap<Integer, Tracker> trackers = TrackersManager.getInstance().getTrackers();

				if (trackers.size() != trackersTable.getRowCount()) {
					modelTable.setDataVector(new String[trackers.size()][header.length], header);
				}

				int i = 0;
				for (Map.Entry<Integer, Tracker> entry : trackers.entrySet()) {
					if (entry.getValue() != null && entry.getValue().getDifferenceBetweenKeepAlive() < 2) {
						trackersTable.getModel().setValueAt(Integer.toString(entry.getValue().getId()), i, 0);
						trackersTable.getModel().setValueAt(entry.getValue().isMaster() ? "Maestro" : "Esclavo", i, 1);
						trackersTable.getModel().setValueAt(
								Long.toString(entry.getValue().getDifferenceBetweenKeepAlive()) + " segundos", i, 2);
						trackersTable.getModel().setValueAt(new SimpleDateFormat("HH:mm:ss - dd/MM/yyyy")
								.format(entry.getValue().getFirstConnection()), i, 3);
						i++;
					}
				}

			} catch (Exception e) {
				ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
				}.getClass().getEnclosingMethod().getName(), e.toString());
				e.printStackTrace();
			}
		}
	}
}