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

/**
 * This test checks the blacklist functionality.
 * Must be run on 2 different machines on the network
 * Remember to set the THIS/OTHER_MACHINE IP-Addresses for your needs
 *
 * This runs on the machine that will be blocked access (OTHER_MACHINE)
 *
 * @author Alfons Seul
 * Date: 18.01.2007
 * Time: 05:44:21
 * Project: JGN
 */
public class DeniedNode {

  public static String THIS_MACHINE;
  public static String SRV_MACHINE = "192.168.1.13";
  public static int    SRV_PORT = 1000;

  public static void main(String[] args) throws Exception {
    THIS_MACHINE = InetAddress.getLocalHost().getHostAddress();
    InetSocketAddress serverAddress = new InetSocketAddress(THIS_MACHINE, 2000);

    MessageServer server1 = new TCPMessageServer(serverAddress);
    server1.setMessageServerId(1);
    DebugListener lst1 = new DebugListener("DeniedClient");
    server1.addConnectionListener(lst1);
    server1.addMessageListener(lst1);
    JGN.createThread(server1).start();

    InetSocketAddress other = new InetSocketAddress(SRV_MACHINE, SRV_PORT);
    MessageClient client = server1.connectAndWait(other, 5000);
    if (client != null) {
      System.out.println("\nTest failed, could connect from localhost, ... too bad!");
    }
    else {
      System.out.println("\nConnect from this machine IS denied...\n...congrats");
    }
    System.out.println("wait 3 seconds for servers to settle ...");
//    Thread.sleep(3000);
    System.exit(0);
  }
}
