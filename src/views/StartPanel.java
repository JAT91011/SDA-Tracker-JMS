package views;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import controllers.Controller;

public class StartPanel extends JPanel implements ActionListener {

	private static final long	serialVersionUID	= -1527615628965557447L;

	private JButton				btnNext;
	private JLabel				lblMessage;
	private JPanel				panLoading;
	private JPanel				panFooter;

	public StartPanel() {

		setBackground(new Color(100, 149, 237));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 360, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 40, 0, 40, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 0.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 1.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
		setLayout(gridBagLayout);

		btnNext = new JButton(new ImageIcon("icons/start-icon-off.png"));
		btnNext.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				btnNext.setIcon(new ImageIcon("icons/start-icon-on.png"));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				btnNext.setIcon(new ImageIcon("icons/start-icon-off.png"));
			}
		});
		btnNext.setContentAreaFilled(false);
		btnNext.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnNext.setFocusPainted(false);
		btnNext.setBorderPainted(false);
		btnNext.addActionListener(this);
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.insets = new Insets(0, 0, 5, 5);
		gbc_btnNewButton.gridx = 1;
		gbc_btnNewButton.gridy = 1;
		add(btnNext, gbc_btnNewButton);

		panFooter = new JPanel();
		panFooter.setOpaque(false);
		GridBagConstraints gbc_panFooter = new GridBagConstraints();
		gbc_panFooter.gridwidth = 3;
		gbc_panFooter.insets = new Insets(0, 10, 10, 10);
		gbc_panFooter.fill = GridBagConstraints.BOTH;
		gbc_panFooter.gridx = 0;
		gbc_panFooter.gridy = 3;
		add(panFooter, gbc_panFooter);
		GridBagLayout gbl_panFooter = new GridBagLayout();
		gbl_panFooter.columnWidths = new int[] { 0, 0, 0 };
		gbl_panFooter.rowHeights = new int[] { 0, 0 };
		gbl_panFooter.columnWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		gbl_panFooter.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
		panFooter.setLayout(gbl_panFooter);

		panLoading = new JPanel();
		GridBagConstraints gbc_panLoading = new GridBagConstraints();
		gbc_panLoading.anchor = GridBagConstraints.SOUTHWEST;
		gbc_panLoading.insets = new Insets(0, 0, 0, 5);
		gbc_panLoading.gridx = 0;
		gbc_panLoading.gridy = 0;
		panFooter.add(panLoading, gbc_panLoading);
		panLoading.setVisible(false);
		panLoading.setOpaque(false);
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
		GridBagConstraints gbc_lblSign = new GridBagConstraints();
		gbc_lblSign.anchor = GridBagConstraints.SOUTH;
		gbc_lblSign.gridx = 1;
		gbc_lblSign.gridy = 0;
		panFooter.add(lblSign, gbc_lblSign);
		lblSign.setForeground(Color.WHITE);
	}

	public JButton getNextButton() {
		return this.btnNext;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnNext) {
			new ConnectThread(this).start();
		}
	}

	public JPanel getLoadingPanel() {
		return this.panLoading;
	}

}

class ConnectThread extends Thread {

	private StartPanel startPanel;

	public ConnectThread(final StartPanel startPanel) {
		this.startPanel = startPanel;
	}

	public void run() {
		this.startPanel.getLoadingPanel().setVisible(true);
		if (Controller.getInstance().connect()) {
			this.startPanel.getLoadingPanel().setVisible(false);
			Window.getInstance().getSlider().slideLeft();
		} else {
			System.out.println("No se ha podido conectar");
		}
	}
}