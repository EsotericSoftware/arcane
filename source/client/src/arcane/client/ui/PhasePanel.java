
package arcane.client.ui;


import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import arcane.client.GameClient;
import arcane.network.Network.SetPriorityStops;
import arcane.network.Phase;
import arcane.ui.util.DashedBorder;
import arcane.ui.util.MouseAdapter;

public class PhasePanel extends JLabel {
	private Phase currentPhase;
	private Map<Phase, JLabel> phaseToLabel = new HashMap<Phase, JLabel>();
	private final GameClient gameClient;

	public PhasePanel (GameClient gameClient) {
		this.gameClient = gameClient;

		addPhase(Phase.untap, "Untap");
		addPhase(Phase.upkeep, "Upkeep");
		addPhase(Phase.draw, "Draw");
		addPhase(Phase.main1, "Main 1");
		addPhase(Phase.beginCombat, "<html>Begin<br>Combat");
		addPhase(Phase.declareAttackers, "<html>Declare<br>Attackers");
		addPhase(Phase.declareBlockers, "<html>Declare<br>Blockers");
		addPhase(Phase.combatDamage, "<html>Combat<br>Damage");
		addPhase(Phase.endCombat, "<html>End<br>Combat");
		addPhase(Phase.main2, "Main 2");
		addPhase(Phase.endTurn, "<html>End<br>Turn");
		addPhase(Phase.cleanup, "Cleanup");
	}

	public void setPhase (Phase phase) {
		if (currentPhase != null) phaseToLabel.get(currentPhase).setBackground(null);
		phaseToLabel.get(phase).setBackground(Color.white);
		currentPhase = phase;
	}

	private void addPhase (final Phase phase, String text) {
		JLabel label = new JLabel();
		add(label);
		phaseToLabel.put(phase, label);
		label.setOpaque(true);
		label.setFont(Font.decode("Tahoma-10"));
		label.setText(text);
		setPhaseBorder(phase);

		label.addMouseListener(new MouseAdapter() {
			public void mouseActuallyClicked (MouseEvent evt) {
				Set<Phase> priorityStops = gameClient.getLocalPlayer().getPriorityStops();
				if (!priorityStops.remove(phase)) priorityStops.add(phase);
				setPhaseBorder(phase);
				SetPriorityStops message = new SetPriorityStops();
				message.priorityStops = priorityStops.toArray(new Phase[priorityStops.size()]);
				gameClient.sendToServer(message);
			}
		});
	}

	private void setPhaseBorder (Phase phase) {
		JLabel label = phaseToLabel.get(phase);
		if (gameClient.getLocalPlayer().getPriorityStops().contains(phase))
			label.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.black));
		else
			label.setBorder(new DashedBorder(Color.black, 3));
	}

	public void layout () {
		int width = getWidth();
		int y = 0;
		int labelHeight = 26;

		for (int i = 0, n = getComponentCount(); i < n; i++) {
			JLabel label = (JLabel)getComponent(i);
			label.setSize(width, labelHeight);
			label.setLocation(0, y);
			y += labelHeight + 3;
		}
	}
}
