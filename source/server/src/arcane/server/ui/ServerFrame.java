
package arcane.server.ui;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.StyledEditorKit;

import arcane.Arcane;
import arcane.ArcaneException;
import arcane.network.Network;
import arcane.server.Server;
import arcane.ui.ArcaneFrame;
import arcane.ui.util.TextComponentOutputStream;
import arcane.ui.util.UI;
import arcane.util.MultiplexOutputStream;

import com.captiveimagination.jgn.JGN;

public class ServerFrame extends ArcaneFrame {
	private Server server;
	private JEditorPane editorPane;

	public ServerFrame () {
		setTitle("Server - Arcane v" + Arcane.version);

		initializeComponents();
		loadPreferences();

		System.setOut(new PrintStream(new MultiplexOutputStream(System.out, new TextComponentOutputStream(editorPane)), true));
		// System.setErr(new PrintStream(new MultiplexOutputStream(System.err, new TextComponentOutputStream(editorPane)), true));
	}

	private void loadPreferences () {
		prefs.loadFrameState(this, "server", 640, 480);
	}

	protected void savePreferences () {
		prefs.saveFrameState(this, "server");
	}

	public void start () {
		try {
			server = new Server(new InetSocketAddress((InetAddress)null, Network.getPort()), null);
		} catch (IOException ex) {
			setVisible(false);
			dispose();
			throw new ArcaneException("Error starting server.", ex);
		}

		System.out.println("Server started.");
		Thread thread = new Thread(JGN.createRunnable(server), "Server");
		thread.setDaemon(true);
		thread.start();
	}

	public boolean exit () {
		if (server.areGamesInProgress()) {
			int result = JOptionPane.showConfirmDialog(ServerFrame.this, "Games in progress will be terminated.",
				"Confirm Shutdown", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			if (result != JOptionPane.OK_OPTION) return false;
		}
		System.out.println("Shutting down server...");
		prefs.save();
		server.close();
		setVisible(false);
		dispose();
		System.out.println("Server shutdown.");
		return true;
	}

	private void initializeComponents () {
		getContentPane().setLayout(new GridBagLayout());
		setVisible(false);
		{
			JScrollPane scrollPane = new JScrollPane();
			getContentPane().add(
				scrollPane,
				new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 6, 4,
					6), 0, 0));
			{
				editorPane = new JEditorPane();
				editorPane.setEditorKit(new StyledEditorKit());
				editorPane.setEditable(false);
				editorPane.setBackground(Color.white);
				editorPane.setContentType("text/plain");
				scrollPane.setViewportView(editorPane);
			}
		}
		{
			JPanel buttonSection = new JPanel();
			FlowLayout buttonSectionLayout = new FlowLayout();
			buttonSectionLayout.setVgap(0);
			buttonSectionLayout.setHgap(4);
			buttonSection.setLayout(buttonSectionLayout);
			getContentPane().add(
				buttonSection,
				new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 0, 6, 2), 0, 0));
			{
				JButton button = UI.getButton();
				buttonSection.add(button);
				button.setText("Clear");
				button.addActionListener(new ActionListener() {
					public void actionPerformed (ActionEvent evt) {
						editorPane.setText("");
					}
				});
			}
			{
				JButton button = UI.getButton();
				buttonSection.add(button);
				button.setText("Shutdown");
				button.addActionListener(new ActionListener() {
					public void actionPerformed (ActionEvent evt) {
						exit();
					}
				});
			}
		}
	}
}
