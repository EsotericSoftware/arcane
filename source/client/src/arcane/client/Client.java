
package arcane.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import arcane.Arcane;
import arcane.client.ui.LobbyFrame;
import arcane.network.Network;

import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.clientserver.JGNClient;

public class Client extends JGNClient {
	static private int nextThreadID = 0;

	public Client () throws UnknownHostException, IOException {
		super(new InetSocketAddress((InetAddress)null, 0), null);
	}

	public void connectAndWait (String host) throws IOException, InterruptedException {
		Thread thread = new Thread(JGN.createRunnable(100, this), "Client" + nextThreadID++);
		thread.setDaemon(true);
		thread.start();
		InetAddress serverAddress;
		serverAddress = InetAddress.getByName(host);
		connectAndWait(new InetSocketAddress(serverAddress, Network.getPort()), null, 5000);
	}

	public void close () {
		try {
			super.close();
		} catch (IOException ignored) {
		}
	}

	public static void main (String[] args) {
		Arcane.setup("data/arcane.properties", "arcane.log", true);
		Network.register();
		new LobbyFrame().setVisible(true);
	}
}
