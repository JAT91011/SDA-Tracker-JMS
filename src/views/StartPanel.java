package views;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.InetAddress;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import controllers.Controller;
import utilities.ErrorsLog;
import utilities.Properties;
import views.components.ITextField;

public class StartPanel extends JPanel implements ActionListener {

	private static final long	serialVersionUID	= -1527615628965557447L;

	private ITextField			txtIp;
	private ITextField			txtPort;
	private JButton				btnNext;
	private JLabel				lblMessage;
	private JPanel				panLoading;

	public static String		IPADDRESS_PATTERN	= "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
			+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

	public StartPanel() {

		setBackground(new Color(100, 149, 237));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 360, 0, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 50, 50, 30, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
		setLayout(gridBagLayout);

		JLabel lblIpAddress = new JLabel("Direcci\u00F3n IP:");
		lblIpAddress.setForeground(Color.WHITE);
		lblIpAddress.setFont(new Font("Microsoft Sans Serif", Font.PLAIN, 15));
		GridBagConstraints gbc_lblIpAddress = new GridBagConstraints();
		gbc_lblIpAddress.anchor = GridBagConstraints.WEST;
		gbc_lblIpAddress.insets = new Insets(0, 0, 5, 10);
		gbc_lblIpAddress.gridx = 1;
		gbc_lblIpAddress.gridy = 1;
		add(lblIpAddress, gbc_lblIpAddress);

		txtIp = new ITextField("228.5.6.7");
		txtIp.setErrorIcon(new ImageIcon("icons/error-icon.png"));
		GridBagConstraints gbc_txtIp = new GridBagConstraints();
		gbc_txtIp.insets = new Insets(0, 5, 5, 5);
		gbc_txtIp.fill = GridBagConstraints.BOTH;
		gbc_txtIp.gridx = 2;
		gbc_txtIp.gridy = 1;
		add(txtIp, gbc_txtIp);

		btnNext = new JButton(new ImageIcon("icons/next-icon.png"));
		btnNext.setContentAreaFilled(false);
		btnNext.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnNext.setFocusPainted(false);
		btnNext.setBorderPainted(false);
		btnNext.addActionListener(this);
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.fill = GridBagConstraints.BOTH;
		gbc_btnNewButton.gridheight = 2;
		gbc_btnNewButton.insets = new Insets(0, 0, 5, 5);
		gbc_btnNewButton.gridx = 3;
		gbc_btnNewButton.gridy = 1;
		add(btnNext, gbc_btnNewButton);

		JLabel lblPort = new JLabel("Port:");
		lblPort.setForeground(Color.WHITE);
		lblPort.setFont(new Font("Microsoft Sans Serif", Font.PLAIN, 15));
		GridBagConstraints lblPortPeers = new GridBagConstraints();
		lblPortPeers.insets = new Insets(0, 0, 5, 10);
		lblPortPeers.anchor = GridBagConstraints.WEST;
		lblPortPeers.gridx = 1;
		lblPortPeers.gridy = 2;
		add(lblPort, lblPortPeers);

		txtPort = new ITextField("1 - 65535");
		txtPort.setErrorIcon(new ImageIcon("icons/error-icon.png"));
		txtPort.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				char c = e.getKeyChar();
				if (!(Character.isDigit(c) || (c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE))) {
					e.consume();
				}
			}
		});
		GridBagConstraints gbc_txtPortPeers = new GridBagConstraints();
		gbc_txtPortPeers.insets = new Insets(0, 5, 5, 5);
		gbc_txtPortPeers.fill = GridBagConstraints.BOTH;
		gbc_txtPortPeers.gridx = 2;
		gbc_txtPortPeers.gridy = 2;
		add(txtPort, gbc_txtPortPeers);

		panLoading = new JPanel();
		panLoading.setVisible(false);
		panLoading.setOpaque(false);
		GridBagConstraints gbc_panLoading = new GridBagConstraints();
		gbc_panLoading.insets = new Insets(0, 0, 5, 5);
		gbc_panLoading.fill = GridBagConstraints.BOTH;
		gbc_panLoading.gridx = 2;
		gbc_panLoading.gridy = 3;
		add(panLoading, gbc_panLoading);
		GridBagLayout gbl_panLoading = new GridBagLayout();
		gbl_panLoading.columnWidths = new int[] { 0, 0, 0 };
		gbl_panLoading.rowHeights = new int[] { 0, 0 };
		gbl_panLoading.columnWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		gbl_panLoading.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		panLoading.setLayout(gbl_panLoading);

		JLabel lblSpinner = new JLabel(new ImageIcon("icons/loading.gif"));
		GridBagConstraints gbc_lblSpinner = new GridBagConstraints();
		gbc_lblSpinner.anchor = GridBagConstraints.WEST;
		gbc_lblSpinner.insets = new Insets(0, 0, 0, 5);
		gbc_lblSpinner.gridx = 0;
		gbc_lblSpinner.gridy = 0;
		panLoading.add(lblSpinner, gbc_lblSpinner);
		lblSpinner.setText("");

		lblMessage = new JLabel("Receiving data...");
		GridBagConstraints gbc_lblMessage = new GridBagConstraints();
		gbc_lblMessage.anchor = GridBagConstraints.EAST;
		gbc_lblMessage.gridx = 1;
		gbc_lblMessage.gridy = 0;
		panLoading.add(lblMessage, gbc_lblMessage);
		lblMessage.setForeground(Color.WHITE);

		JLabel lblSign = new JLabel("Created by Jordan Aranda & Endika Salgueiro");
		lblSign.setForeground(Color.WHITE);
		GridBagConstraints gbc_lblSign = new GridBagConstraints();
		gbc_lblSign.anchor = GridBagConstraints.EAST;
		gbc_lblSign.gridwidth = 5;
		gbc_lblSign.insets = new Insets(0, 0, 10, 10);
		gbc_lblSign.gridx = 0;
		gbc_lblSign.gridy = 5;
		add(lblSign, gbc_lblSign);

		loadProperties();
	}

	public JButton getNextButton() {
		return this.btnNext;
	}

	private void loadProperties() {
		if (!Properties.getIp().isEmpty()) {
			txtIp.setText(Properties.getIp());
			txtIp.showAsHint(false);
		}
		if (Properties.getPort() != 0) {
			txtPort.setText(Integer.toString(Properties.getPort()));
			txtPort.showAsHint(false);
		}
	}

	public boolean save() {
		try {
			boolean errors = false;
			String ip = txtIp.getText().trim();
			int port = 0;

			if (!ip.matches(IPADDRESS_PATTERN)) {
				errors = true;
				txtIp.showError();
			} else {
				if (!InetAddress.getByName(ip).isMulticastAddress()) {
					errors = true;
					txtIp.showError();
				} else {
					txtIp.hideError();
				}
			}

			if (!txtPort.getText().trim().isEmpty()) {
				port = Integer.parseInt(txtPort.getText().trim());
				txtPort.hideError();
			} else {
				errors = true;
				txtPort.showError();
			}

			if (port > 65535 || port < 1) {
				errors = true;
				txtPort.showError();
			} else {
				txtPort.hideError();
			}

			if (!errors) {
				Properties.setIp(ip);
				Properties.setPort(port);
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
			return false;
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnNext) {
			if (save()) {
				new ConnectThread(this, txtIp.getText().trim(), Integer.parseInt(txtPort.getText().trim())).start();
			}
		}
	}

	public JPanel getLoadingPanel() {
		return this.panLoading;
	}

}

class ConnectThread extends Thread {

	private String		ip;
	private int			port;
	private StartPanel	startPanel;

	public ConnectThread(final StartPanel startPanel, final String ip, final int port) {
		this.startPanel = startPanel;
		this.ip = ip;
		this.port = port;
	}

	public void run() {
		this.startPanel.getLoadingPanel().setVisible(true);
		if (Controller.getInstance().connect(this.ip, this.port)) {
			this.startPanel.getLoadingPanel().setVisible(false);
			Window.getInstance().getSlider().slideLeft();
		} else {
			System.out.println("No se ha podido conectar");
		}
	}
}