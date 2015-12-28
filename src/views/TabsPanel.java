package views;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import views.components.JSlidePanel;

public class TabsPanel extends JPanel implements MouseListener {

	private static final long	serialVersionUID	= 8155818731609154350L;

	private JSlidePanel<JPanel>	slider;

	private JLabel				lblTrackers;
	private JLabel				lblPeers;

	private JPanel				container;
	private boolean				watchingTrackers;

	public TabsPanel() {
		setOpaque(true);

		this.slider = new JSlidePanel<JPanel>(this);

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 50, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		setLayout(gridBagLayout);

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
		gbc_lblTrackers.gridx = 0;
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
		gbc_lblPeers.gridx = 1;
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
		if (e.getSource() == lblTrackers) {
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