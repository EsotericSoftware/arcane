/**
 * Copyright (c) 2005-2006 JavaGameNetworking
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'JavaGameNetworking' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Created: Jan 18, 2007
 */
package com.captiveimagination.jgn.test.net.blacklist;

import com.captiveimagination.jgn.MessageServer;
import com.captiveimagination.jgn.TCPMessageServer;
import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.event.DebugListener;

import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * This test checks the blacklist functionality.
 * Must be run on 2 different machines on the network
 * Remember to set the OTHER_MACHINE IP-Address for your needs
 *
 * @author Alfons Seul
 * Date: 18.01.2007
 * Time: 05:44:21
 * Project: JGN
 */
public class DenyingNode {

  public static String THIS_MACHINE;
  public static int    THIS_PORT;
  public static String OTHER_MACHINE = "192.168.1.13";

  public static void main(String[] args) throws Exception {

    THIS_MACHINE = InetAddress.getLocalHost().getHostAddress();
    THIS_PORT = 1000;
    InetSocketAddress serverAddress = new InetSocketAddress(THIS_MACHINE, THIS_PORT);

    MessageServer server1 = new TCPMessageServer(serverAddress);
    server1.setMessageServerId(1);
    DebugListener lst1 = new DebugListener("DenyingSRV");
    server1.addConnectionListener(lst1);
    server1.addMessageListener(lst1);

    ArrayList<String> deniedhosts = new ArrayList<String>();
    deniedhosts.add(OTHER_MACHINE);   // my other machine in the network should be blocked
    server1.setBlacklist(deniedhosts);

    JGN.createThread(server1).start();

    // check a connection from somewhere else IS possible
    MessageServer server2 = new TCPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), 2000));
    server2.setMessageServerId(2);
    DebugListener lst2 = new DebugListener("LocalClient");
    server2.addConnectionListener(lst2);
    server2.addMessageListener(lst2);
    JGN.createThread(server2).start();

    MessageClient client = server2.connectAndWait(serverAddress, 5000);
    if (client != null) {
      System.out.println("\nconnect from this machine works...\n...now try the other (DeniedNode");
    }
    else {
      System.out.println("\test failed, no connect from localhost, ... check!");
      System.exit(1);
    }
  }
}
