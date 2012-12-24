package com.tmall.top.push.websocket;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

import com.alibaba.fastjson.JSON;
import com.tmall.top.push.Client;
import com.tmall.top.push.PushManager;

public class BackendServlet extends WebSocketServlet {

	private static final long serialVersionUID = 3431855312865710986L;

	@Override
	public WebSocket doWebSocketConnect(HttpServletRequest arg0, String arg1) {
		PushManager manager = PushManager.current();
		WebSocketClientConnection clientConnection = Utils
				.getClientConnectionPool().acquire();
		clientConnection.init(Utils.parseHeaders(arg0), manager);

		return new BackendWebSocket(manager, manager.getClient(clientConnection
				.getId()), clientConnection);
	}

	public class BackendWebSocket extends WebSocketBase {

		public BackendWebSocket(PushManager manager, Client client,
				WebSocketClientConnection clientConnection) {
			super(manager, client, clientConnection);
		}

		@Override
		public void onMessage(String arg0) {
			try {
				this.connection.sendMessage(JSON.toJSONString(Utils
						.processRequest(arg0, this.manager)));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}
