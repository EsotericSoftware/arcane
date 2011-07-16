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
 * Created: Jun 12, 2006
 */
package com.captiveimagination.jgn.event;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.logging.Logger;

import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.MessageException;
import com.captiveimagination.jgn.message.Message;

/**
 * The default implementation of a DynamicMessageListener.
 * <p/>
 * Each DML caches all of its message-methods for the 4 handlingclasses RECEIVED, SENT, ...
 * When the handle(MESSAGE_EVENT, Message, DynamicMessageListener) is called the first time,
 * this analysis takes place in collectSubclassMessageMethods(..). Thereafter we can do a
 * superfast lookup for the method that should be called for the given parameter (eg.
 * Message(Sub-) class.
 * During startup registration of messageclasses, JGN analyses and stores the class
 * hierarchy for each messagetype that gets registered. Note this includes all Interfaces
 * implemented on all levels of the superclass hierarchy.
 * <p/>
 * The target method will be found based on the MATCHing strategy:
 * NORMAL: will first try a direct match, if that fails, will call for baseclass:Message
 * EXACT:  will try a direct match, do nothing, if that fails
 * HIERARCHICAL: starting with messageclass (as given in the parameter) try that first, then
 *               look for implemented Interfaces for that class, then superclass, their interfaces,
 *               their superclass etc, until Message.class is reached.
 *               Note, there will always be an (empty) method for Message.class, since this is
 *               mandatory by MessageListener Interface.
 *
 * @author Alfons Seul
 * @author Nathan Sweet
 */
public class DynamicMessageAdapter implements DynamicMessageListener {
	public static enum MATCH {NORMAL, HIERARCHICAL, EXACT }

	private static Logger LOG = Logger.getLogger("com.captiveimagination.jgn.event.BaseDynamicMessageListener");

	private boolean initialized;
	private MATCH matchKind;
	private HashMap<Class<?>, Method> recvMethods;
	private HashMap<Class<?>, Method> sentMethods;
	private HashMap<Class<?>, Method> crtfMethods;
	private HashMap<Class<?>, Method> failMethods;

  protected DynamicMessageAdapter() {
    recvMethods = new HashMap<Class<?>, Method>();
    sentMethods = new HashMap<Class<?>, Method>();
    crtfMethods = new HashMap<Class<?>, Method>();
    failMethods = new HashMap<Class<?>, Method>();
    initialized = false;
    matchKind = MATCH.NORMAL;
  }

  private void collectSubclassMessageMethods(DynamicMessageListener dml) {
    Method[] listenersAllMethods = dml.getClass().getMethods(); // eg. from sub-class
    for (Method m : listenersAllMethods) {
      String mname = m.getName();

      if (m.getParameterTypes().length != 1) continue;

      HashMap<Class<?>, Method> mm;
      if (mname.equals("messageReceived"))       mm = recvMethods;
      else if (mname.equals("messageSent"))      mm = sentMethods;
      else if (mname.equals("messageCertified")) mm = crtfMethods;
      else if (mname.equals("messageFailed"))    mm = failMethods;
      else continue;

      Class messClass = m.getParameterTypes()[0];

      if (Message.class.isAssignableFrom(messClass) || messClass.isInterface()) {
        // this asserts the parameter is a message - (if it's not an Interface)
        // otherwise we'll just ignore that method silently
        if (! mm.containsKey(messClass)) {
          mm.put(messClass, m);
          m.setAccessible(true);
        }
      }
    }
    initialized = true;
  }

	/**
	 * set the strategy on how to handle non-exactly fitting calls
	 *
	 * @param m the new MatchKind, see descriütion of class
	 */
	public void setMatchKind(MATCH m) {
    matchKind = m;
  }

  /**
   * dynamically invoke the listener method for the given <type>, the given <MessageClass>, on the
   * <DynamicListener> given. If that method doesn't exist, the baseMethod 'message...(Message m)' will
   * be called.
	 * <p/>
   * Note this implementation is FINAL !!!
   *
   * @param type     - a MessageListener.MESSAGE_EVENT; eg RECEIVED, SENT, ...
   * @param mess     - the message must be a subtype from Message
   * @param listener - the DynamicMessageListener object, to be used on invocation
   */
  public void handle(MessageListener.MESSAGE_EVENT type, Message mess, DynamicMessageListener listener) {
    HashMap<Class<?>, Method> mm;
    Class<? extends Message> messClass = mess.getClass();

    // lazy init !!
    if (! initialized) collectSubclassMessageMethods(listener);

    switch (type) {
			case CERTIFIED: mm = crtfMethods; break;
			case FAILED:	mm = failMethods; break;
			case RECEIVED:	mm = recvMethods; break;
			case SENT:		mm = sentMethods; break;
			default:		return;
		}

		// see if there is a method that directly matches the parameter class
		Method m = mm.get(messClass);

		if (m == null) {	// nope, now ...

			switch (matchKind) {
				case EXACT:
					return;		// if no nethod found, do nothing

        case HIERARCHICAL:		// search through the hierarchy
          for (Class c : JGN.getMessageClassHierarchy(messClass)) {
            if ((m = mm.get(c)) != null)
              break; // found a super something method
          }
          if (m == null) m = mm.get(Message.class); // force Message to be used
          break;

        case NORMAL:
          m = mm.get(Message.class); // note, this is always defined
          break;
      }
    }
    if (m == null) return;
    try {
      m.invoke(listener, mess);
    } catch (Exception ex) {
   	 throw new MessageException("Error invoking dynamic message listener: " + listener.getClass().getName() + ", type="
				+ type + ", message=" + mess.getClass().getName(), ex);
    }
  }
  
	public void messageReceived(Message message) {
	}

	public void messageCertified (Message message) {
	}

	public void messageFailed (Message message) {
	}

	public void messageSent (Message message) {
	}
}
