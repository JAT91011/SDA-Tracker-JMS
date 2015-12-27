package views;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import entities.Peer;
import models.PeersManager;
import utilities.ErrorsLog;

public class PeersPanel extends JPanel implements Observer {

	private static final long		serialVersionUID	= 4986034677227823532L;
	private JTable					tablePeers;
	private final DefaultTableModel	modelTablePeers;
	private JTable					tableContent;
	private DefaultTableModel		modelTableContent;
	private String[]				headerPeers;
	private String[]				headerContent;

	public PeersPanel() {
		setBackground(new Color(255, 218, 185));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 450, 0 };
		gridBagLayout.rowHeights = new int[] { 300, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
		setLayout(gridBagLayout);

		headerPeers = new String[3];
		headerPeers[0] = "ID";
		headerPeers[1] = "IP";
		headerPeers[2] = "Port";
		modelTablePeers = new DefaultTableModel();
		modelTablePeers.setDataVector(new String[0][headerPeers.length], headerPeers);

		headerContent = new String[4];
		headerContent[0] = "ID";
		headerContent[1] = "INFO HASH";
		headerContent[2] = "Mode";
		headerContent[3] = "Completed";

		modelTableContent = new DefaultTableModel();
		modelTableContent.setDataVector(new String[0][headerContent.length], headerContent);

		JSplitPane splitPane = new JSplitPane();
		splitPane.setBackground(new Color(255, 218, 185));
		splitPane.setDividerSize(5);
		splitPane.setResizeWeight(.7d);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		GridBagConstraints gbc_splitPane = new GridBagConstraints();
		gbc_splitPane.insets = new Insets(10, 10, 10, 10);
		gbc_splitPane.fill = GridBagConstraints.BOTH;
		gbc_splitPane.gridx = 0;
		gbc_splitPane.gridy = 0;
		add(splitPane, gbc_splitPane);

		JScrollPane scrollPanePeers = new JScrollPane();
		scrollPanePeers.setSize(new Dimension(0, 300));
		scrollPanePeers.setPreferredSize(new Dimension(4, 300));
		splitPane.setLeftComponent(scrollPanePeers);
		tablePeers = new JTable(modelTablePeers);
		tablePeers.getTableHeader().setReorderingAllowed(false);
		tablePeers.setShowVerticalLines(true);
		tablePeers.setShowHorizontalLines(true);
		tablePeers.setDragEnabled(false);
		tablePeers.setSelectionForeground(Color.WHITE);
		tablePeers.setSelectionBackground(Color.BLUE);
		tablePeers.setForeground(Color.BLACK);
		tablePeers.setBackground(Color.WHITE);
		tablePeers.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 14));
		tablePeers.setRowHeight(30);
		tablePeers.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (tablePeers.getSelectedRow() > -1) {
					updateContentsTableData();
				}
			}
		});

		tablePeers.getTableHeader().setFont(new Font("Arial", Font.PLAIN, 15));

		scrollPanePeers.setViewportView(tablePeers);

		JScrollPane scrollPaneContent = new JScrollPane();
		scrollPaneContent.setSize(new Dimension(0, 100));
		scrollPaneContent.setPreferredSize(new Dimension(4, 100));
		splitPane.setRightComponent(scrollPaneContent);
		tableContent = new JTable(modelTableContent);
		tableContent.getTableHeader().setReorderingAllowed(false);
		tableContent.setShowVerticalLines(true);
		tableContent.setShowHorizontalLines(true);
		tableContent.setDragEnabled(false);
		tableContent.setSelectionForeground(Color.WHITE);
		tableContent.setSelectionBackground(Color.BLUE);
		tableContent.setForeground(Color.BLACK);
		tableContent.setBackground(Color.WHITE);
		tableContent.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 14));
		tableContent.setRowHeight(30);
		tableContent.getTableHeader().setFont(new Font("Arial", Font.PLAIN, 15));
		scrollPaneContent.setViewportView(tableContent);

		PeersManager.getInstance().addObserver(this);
	}

	private void updateContentsTableData() {
		try {
			if (this.tablePeers.getSelectedRow() > -1) {

				Peer peer = PeersManager.getInstance().getPeers()
						.get(Integer.parseInt((String) this.tablePeers.getModel().getValueAt(this.tablePeers.getSelectedRow(), 0)));
				String[][] data = new String[peer.getContents().size()][this.headerContent.length];

				for (int i = 0; i < peer.getContents().size(); i++) {
					data[i][0] = Integer.toString(peer.getContents().get(i).getId());
					data[i][1] = peer.getContents().get(i).getInfoHash();
					if (peer.getContents().get(i).getPercent() == 0) {
						data[i][2] = "Leecher";
					} else if (peer.getContents().get(i).getPercent() > 0 && peer.getContents().get(i).getPercent() < 100) {
						data[i][2] = "Leecher / Seeder";
					} else if (peer.getContents().get(i).getPercent() == 100) {
						data[i][2] = "Seeder";
					}
					data[i][3] = Integer.toString(peer.getContents().get(i).getPercent()) + " %";
				}

				this.modelTableContent = new DefaultTableModel();
				this.modelTableContent.setDataVector(data, headerContent);
				this.tableContent.setModel(this.modelTableContent);
			}
		} catch (Exception e) {
			System.out.println("Falla en update contents");
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void update(Observable o, Object arg) {
		if (o == PeersManager.getInstance()) {
			try {
				int selectedRow = this.tablePeers.getSelectedRow();
				ConcurrentHashMap<Integer, Peer> peers = PeersManager.getInstance().getPeers();
				if (peers.size() != tablePeers.getRowCount()) {
					modelTablePeers.setDataVector(new String[peers.size()][headerPeers.length], headerPeers);
				}

				int i = 0;
				for (Map.Entry<Integer, Peer> entry : peers.entrySet()) {
					tablePeers.getModel().setValueAt(Integer.toString(entry.getValue().getId()), i, 0);
					tablePeers.getModel().setValueAt(entry.getValue().getIp(), i, 1);
					tablePeers.getModel().setValueAt(Long.toString(entry.getValue().getPort()), i, 2);

					i++;
				}
				if (peers.size() > selectedRow && selectedRow > -1) {
					this.tablePeers.setRowSelectionInterval(selectedRow, selectedRow);
				}
				updateContentsTableData();

			} catch (Exception e) {
				ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
				}.getClass().getEnclosingMethod().getName(), e.toString());
				e.printStackTrace();
			}
		}
	}
}