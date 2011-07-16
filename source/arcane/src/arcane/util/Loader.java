
package arcane.util;

import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JFrame;

import org.h2.jdbc.JdbcSQLException;

import arcane.Arcane;
import arcane.ui.util.MessageFrame;
import arcane.ui.util.ProgressDialog;

abstract public class Loader implements Runnable {
	protected ProgressDialog dialog = new ProgressDialog();

	private MessageFrame errorFrame;
	private boolean cancelled;
	private boolean success;

	public Loader (String title) {
		dialog.setMessage("Initializing...");
		dialog.setAlwaysOnTop(true);
		dialog.setValue(-1);
		dialog.setTitle(title);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosed (WindowEvent evt) {
				if (!success) cancel();
			}
		});
	}

	protected synchronized MessageFrame getErrorFrame () {
		if (errorFrame == null) {
			errorFrame = new MessageFrame("Error - Arcane v" + Arcane.version);
			errorFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			errorFrame.editorPane.setContentType("text/plain; charset=UTF-8");
			errorFrame.addButton("Close");
		}
		return errorFrame;
	}

	protected synchronized void setErrorFrame (MessageFrame errorFrame) {
		if (errorFrame != null) {
			this.errorFrame = errorFrame;
		}
	}

	protected String getExceptionText (Exception ex) {
		if (ex instanceof JdbcSQLException && ex.getMessage().contains("Database may be already open"))
			return "Error: Arcane is already running.\n\n" + //
				"Only a single instance of Arcane may be ran at once.\n" + //
				"Try using the Tools menu or File->New Window to open multiple Arcane applications at the same time.";
		StringWriter writer = new StringWriter();
		ex.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}

	protected void handleError (Exception ex) {
		handleError(getExceptionText(ex));
	}

	protected void handleError (String error) {
		System.out.println(error);
		final class Worker implements Runnable{
			private String error;
			Worker(String e){
				error = e;
			}

			public void run() {
				MessageFrame errorFrame = getErrorFrame();
				errorFrame.setTitle(dialog.getTitle());
				errorFrame.setVisible(true);
				errorFrame.appendText(error);
				errorFrame.scrollToEnd();
			}
		};
		EventQueue.invokeLater(new Worker(error));
	}

	public void start (String threadName) {
		new Thread(this, threadName).start();
		dialog.setVisible(true);
		dialog.dispose();
	}

	public final void run () {
		try {
			if (isCancelled()) return;
			load();
			if (isCancelled()) return;
			success = true;
		} catch (Exception ex) {
			cancel();
			handleError(ex);
		} finally {
			dialog.setVisible(false);
		}
	}

	protected void cancel () {
		cancelled = true;
	}

	protected boolean isCancelled () {
		return cancelled;
	}

	public boolean failed () {
		return !success;
	}

	abstract public void load () throws Exception;
}
