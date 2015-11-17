package views;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import controllers.Controller;
import views.components.JSlidePanel;

public class TabsPanel extends JPanel implements MouseListener {

	private static final long serialVersionUID = 8155818731609154350L;

	private JSlidePanel<JPanel> slider;

	private JLabel	lblDisconnect;
	private JLabel	lblTrackers;
	private JLabel	lblPeers;

	private JPanel	container;
	private boolean	watchingTrackers;

	private Controller configController;

	public TabsPanel() {
		setOpaque(true);

		this.configController = new Controller();
		this.slider = new JSlidePanel<JPanel>(this);

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 60, 0, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 50, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 1.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		setLayout(gridBagLayout);

		lblDisconnect = new JLabel(new ImageIcon("icons/disconnect-icon.png"));
		lblDisconnect.addMouseListener(this);
		lblDisconnect.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lblDisconnect.setText("");
		lblDisconnect.setBackground(new Color(240, 128, 128));
		lblDisconnect.setOpaque(true);
		GridBagConstraints gbc_lblDisconnect = new GridBagConstraints();
		gbc_lblDisconnect.fill = GridBagConstraints.BOTH;
		gbc_lblDisconnect.gridx = 0;
		gbc_lblDisconnect.gridy = 0;
		add(lblDisconnect, gbc_lblDisconnect);

		lblTrackers = new JLabel("Trackers");
		lblTrackers.addMouseListener(this);
		lblTrackers.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lblTrackers.setForeground(Color.WHITE);
		lblTrackers.setHorizontalAlignment(SwingConstants.CENTER);
		lblTrackers.setOpaque(true);
		lblTrackers.setBackground(new Color(102, 205, 170));
		lblTrackers.setFont(new Font("Tahoma", Font.PLAIN, 23));
		GridBagConstraints gbc_lblTrackers = new GridBagConstraints();
		gbc_lblTrackers.fill = GridBagConstraints.BOTH;
		gbc_lblTrackers.gridx = 1;
		gbc_lblTrackers.gridy = 0;
		add(lblTrackers, gbc_lblTrackers);

		lblPeers = new JLabel("Peers");
		lblPeers.addMouseListener(this);
		lblPeers.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lblPeers.setForeground(Color.WHITE);
		lblPeers.setHorizontalAlignment(SwingConstants.CENTER);
		lblPeers.setOpaque(true);
		lblPeers.setBackground(new Color(255, 218, 185));
		lblPeers.setFont(new Font("Tahoma", Font.PLAIN, 23));
		GridBagConstraints gbc_lblPeers = new GridBagConstraints();
		gbc_lblPeers.fill = GridBagConstraints.BOTH;
		gbc_lblPeers.gridx = 2;
		gbc_lblPeers.gridy = 0;
		add(lblPeers, gbc_lblPeers);

		container = slider.getBasePanel();
		GridBagConstraints gbc_panContent = new GridBagConstraints();
		gbc_panContent.gridwidth = 3;
		gbc_panContent.fill = GridBagConstraints.BOTH;
		gbc_panContent.gridx = 0;
		gbc_panContent.gridy = 1;
		add(container, gbc_panContent);

		this.watchingTrackers = true;
		this.slider.addComponent(new TrackersPanel());
		this.slider.addComponent(new PeersPanel());
	}

	public void mouseClicked(MouseEvent e) {
		if (e.getSource() == lblDisconnect) {
			this.configController.disconnect();
			Window.getInstance().setTitle("Tracker");
			Window.getInstance().getSlider().slideRight();
		} else if (e.getSource() == lblTrackers) {
			if (!this.watchingTrackers) {
				this.slider.slideTop();
				this.watchingTrackers = true;
			}
		} else if (e.getSource() == lblPeers) {
			if (this.watchingTrackers) {
				this.slider.slideRight();
				this.watchingTrackers = false;
			}
		}
	}

	public void mouseEntered(MouseEvent e) {

	}

	public void mouseExited(MouseEvent e) {

	}

	public void mousePressed(MouseEvent e) {

	}

	public void mouseReleased(MouseEvent e) {

	}
}