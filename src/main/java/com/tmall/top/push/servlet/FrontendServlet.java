package com.tmall.top.push.servlet;

import java.util.Enumeration;
import java.util.Hashtable;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

import com.tmall.top.push.Client;
import com.tmall.top.push.PushManager;
import com.tmall.top.push.Receiver;
import com.tmall.top.push.WebSocketClientConnection;

//handle all client's request
public class FrontendServlet extends WebSocketServlet {

	@Override
	public WebSocket doWebSocketConnect(HttpServletRequest arg0, String arg1) {
		PushManager manager = PushManager.Current();
		WebSocketClientConnection clientConnection = manager.ClientConnectionPool
				.acquire();
		clientConnection.init(Utils.parseHeaders(arg0));

		return new FrontendWebSocket(manager.receiver,
				manager.getClient(clientConnection.getId()), clientConnection);
	}

	private class FrontendWebSocket extends WebSocketBase {

		public FrontendWebSocket(Receiver receiver, Client client,
				WebSocketClientConnection clientConnection) {
			super(receiver, client, clientConnection);
		}
	}

}
