
package arcane.ui.util;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

/**
 * Dialog displaying a message and a progress bar.
 */
public class ProgressDialog extends JDialog {
	private JProgressBar progressBar;
	private JLabel messageLabel;

	public ProgressDialog () {
		super();
		construct();
	}

	public ProgressDialog (String title) {
		super((Frame)null, title);
		construct();
	}

	public ProgressDialog (JFrame frame, String title) {
		super(frame, title);
		construct();
	}

	public ProgressDialog (JDialog dialog, String title) {
		super(dialog, title);
		construct();
	}

	private void construct () {
		initialize();
		pack();
		progressBar.setPreferredSize(new Dimension(395, progressBar.getHeight()));
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(screenSize.width / 2 - getWidth() / 2, screenSize.height / 2 - getHeight() / 2);
	}

	public void setMessage (String message) {
		messageLabel.setText(message);
	}

	public void setValue (final float percentage) {
		if (percentage == -1) {
			progressBar.setIndeterminate(true);
			return;
		}
		if (progressBar.isIndeterminate()) progressBar.setIndeterminate(false);
		int value = Math.round(percentage >= 1 ? 10000 : percentage * 10000);
		progressBar.setValue(value);
	}

	private void initialize () {
		setResizable(false);
		setModal(true);

		GridBagLayout thisLayout = new GridBagLayout();
		thisLayout.rowWeights = new double[] {0.0};
		thisLayout.rowHeights = new int[] {0};
		thisLayout.columnWeights = new double[] {0.0};
		thisLayout.columnWidths = new int[] {0};
		getContentPane().setLayout(thisLayout);
		{
			progressBar = new JProgressBar(0, 10000);
			getContentPane().add(
				progressBar,
				new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(6,
					6, 6, 6), 0, 0));
		}
		{
			messageLabel = new JLabel();
			getContentPane().add(
				messageLabel,
				new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 6), 0, 0));
			messageLabel.setText("Please wait...");
		}
	}
}
