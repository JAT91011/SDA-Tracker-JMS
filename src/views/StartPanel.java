package views;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import controllers.Controller;

public class StartPanel extends JPanel implements ActionListener {

	private static final long	serialVersionUID	= -1527615628965557447L;

	private Controller			configController;
	private JButton				btnNext;
	private JLabel				lblMessage;
	private JPanel				panLoading;

	public StartPanel() {

		configController = new Controller();

		setBackground(new Color(100, 149, 237));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 360, 0, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 40, 40, 40, 30, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
		setLayout(gridBagLayout);

		btnNext = new JButton(new ImageIcon("icons/next-icon.png"));
		btnNext.setContentAreaFilled(false);
		btnNext.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnNext.setFocusPainted(false);
		btnNext.setBorderPainted(false);
		btnNext.addActionListener(this);
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.fill = GridBagConstraints.BOTH;
		gbc_btnNewButton.gridheight = 3;
		gbc_btnNewButton.insets = new Insets(0, 0, 5, 5);
		gbc_btnNewButton.gridx = 3;
		gbc_btnNewButton.gridy = 1;
		add(btnNext, gbc_btnNewButton);

		panLoading = new JPanel();
		panLoading.setVisible(false);
		panLoading.setOpaque(false);
		GridBagConstraints gbc_panLoading = new GridBagConstraints();
		gbc_panLoading.insets = new Insets(0, 0, 5, 5);
		gbc_panLoading.fill = GridBagConstraints.BOTH;
		gbc_panLoading.gridx = 2;
		gbc_panLoading.gridy = 4;
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

		lblMessage = new JLabel("Espera un momento...");
		GridBagConstraints gbc_lblMessage = new GridBagConstraints();
		gbc_lblMessage.anchor = GridBagConstraints.EAST;
		gbc_lblMessage.gridx = 1;
		gbc_lblMessage.gridy = 0;
		panLoading.add(lblMessage, gbc_lblMessage);
		lblMessage.setForeground(Color.WHITE);

		JLabel lblSign = new JLabel("Creado por Jordan Aranda y Endika Salgueiro");
		lblSign.setForeground(Color.WHITE);
		GridBagConstraints gbc_lblSign = new GridBagConstraints();
		gbc_lblSign.anchor = GridBagConstraints.EAST;
		gbc_lblSign.gridwidth = 5;
		gbc_lblSign.insets = new Insets(0, 0, 10, 10);
		gbc_lblSign.gridx = 0;
		gbc_lblSign.gridy = 6;
		add(lblSign, gbc_lblSign);
	}

	public JButton getNextButton() {
		return this.btnNext;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnNext) {
			new ConnectThread(this, this.configController).start();
		}
	}

	public JPanel getLoadingPanel() {
		return this.panLoading;
	}

}

class ConnectThread extends Thread {

	private Controller	controller;
	private StartPanel	startPanel;

	public ConnectThread(final StartPanel startPanel, final Controller controller) {
		this.startPanel = startPanel;
		this.controller = controller;
	}

	public void run() {
		this.startPanel.getLoadingPanel().setVisible(true);
		if (this.controller.connect()) {
			this.startPanel.getLoadingPanel().setVisible(false);
			Window.getInstance().getSlider().slideLeft();
		} else {
			System.out.println("No se ha podido conectar");
		}
	}
}