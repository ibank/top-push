package com.taobao.top.push.websocket;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.time.StopWatch;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.Connection;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;
import org.junit.Test;

import com.taobao.top.push.PushManager;
import com.taobao.top.push.messages.Message;
import com.taobao.top.push.messages.MessageIO;
import com.taobao.top.push.messages.MessageType;
import com.taobao.top.push.mqtt.MqttMessageIO;
import com.taobao.top.push.mqtt.MqttQos;
import com.taobao.top.push.mqtt.publish.MqttPublishMessage;
import com.taobao.top.push.websocket.BackendServlet;
import com.taobao.top.push.websocket.FrontendServlet;
import com.taobao.top.push.websocket.InitServlet;

public class WebSocketPushServerTest {

	@Test
	public void init_test() throws Exception {
		Server server = this.initServer(8001, 8002);
		server.start();
		WebSocketClientFactory factory = new WebSocketClientFactory();
		factory.start();
		this.connect(factory, "ws://localhost:8001/front", "front-test", "mqtt", null).close();
		this.connect(factory, "ws://localhost:8002/back", "back-test", "mqtt", null).close();
		server.stop();
		Thread.sleep(1000);
	}

	@Test
	public void publish_confirm_test() throws Exception {
		publish_confirm_test(null, 9011, 9012);
	}

	@Test
	public void publish_confirm_mqtt_test() throws Exception {
		publish_confirm_test("mqtt", 9013, 9014);
	}

	@Test
	public void publish_confirm_long_running_test() throws Exception {
		publish_confirm_long_running_test(null);
	}

	@Test
	public void publish_confirm_long_running_mqtt_test() throws Exception {
		publish_confirm_long_running_test("mqtt");
	}

	private void publish_confirm_test(final String protocol, int frontPort, int backPort) throws Exception {
		Server server = this.initServer(frontPort, backPort);
		server.start();

		WebSocketClientFactory factory = new WebSocketClientFactory();
		factory.start();

		// front-end client like a subscriber
		String frontId = "front";
		final Message publishMessage = new Message();
		final MqttPublishMessage mqttPublishMessage = new MqttPublishMessage();

		final Object waitFront = new Object();
		Connection front = this.connect(factory, "ws://localhost:" + frontPort + "/front",
				frontId, protocol, new WebSocket.OnBinaryMessage() {

					@Override
					public void onOpen(Connection arg0) {
					}

					@Override
					public void onClose(int arg0, String arg1) {
					}

					@Override
					public void onMessage(byte[] data, int offset, int length) {
						// receiving publish-message from server
						ByteBuffer buffer = ByteBuffer.allocate(length);
						buffer.put(data, offset, length);

						if ("mqtt".equals(protocol)) {
							MqttMessageIO.parseClientReceiving(
									mqttPublishMessage, buffer);
						} else {
							int messageType = MessageIO
									.parseMessageType(data[offset]);
							assertEquals(MessageType.PUBLISH, messageType);
							MessageIO.parseClientReceiving(publishMessage,
									buffer);
						}
						System.out
								.println("---- [frontend] receiving publish-message from server");
						synchronized (waitFront) {
							waitFront.notifyAll();
						}
					}
				});

		// back-end client like a publisher
		String backId = "back";
		final Message confirmMessage = new Message();
		final MqttPublishMessage mqttConfirmMessage = new MqttPublishMessage();
		final Object waitBack = new Object();
		Connection back = this.connect(factory, "ws://localhost:" + backPort + "/back",
				backId, protocol, new WebSocket.OnBinaryMessage() {

					@Override
					public void onOpen(Connection arg0) {
					}

					@Override
					public void onClose(int arg0, String arg1) {
					}

					@Override
					public void onMessage(byte[] data, int offset, int length) {
						// receiving confirm-message from server
						ByteBuffer buffer = ByteBuffer.allocate(length);
						buffer.put(data, offset, length);

						if ("mqtt".equals(protocol)) {
							MqttMessageIO.parseClientReceiving(
									mqttConfirmMessage, buffer);
						} else {
							int messageType = MessageIO
									.parseMessageType(data[offset]);
							assertEquals(MessageType.PUBCONFIRM, messageType);
							MessageIO.parseClientReceiving(confirmMessage,
									buffer);
						}

						System.out
								.println("---- [backend] receiving confirm-message from server");
						synchronized (waitBack) {
							waitBack.notifyAll();
						}
					}
				});

		Thread.sleep(1000);
		// send publish
		ByteBuffer publish = this.createPublishMessage(protocol, frontId);
		back.sendMessage(publish.array(), 0, publish.position());

		// receive publish
		synchronized (waitFront) {
			waitFront.wait();
		}

		Message msg = "mqtt".equals(protocol) ? mqttPublishMessage
				: publishMessage;
		assertEquals(backId, msg.from);
		assertEquals(7, msg.remainingLength);
		// assert body is expected
		assertEquals('a', (char) ((ByteBuffer) msg.body).get());
		assertEquals('b', (char) ((ByteBuffer) msg.body).get());
		assertEquals('c', (char) ((ByteBuffer) msg.body).get());
		assertEquals('d', (char) ((ByteBuffer) msg.body).get());
		assertEquals('e', (char) ((ByteBuffer) msg.body).get());
		assertEquals('f', (char) ((ByteBuffer) msg.body).get());
		assertEquals('g', (char) ((ByteBuffer) msg.body).get());
		// send confirm
		ByteBuffer confirm = this.createConfirmMessage(protocol, msg);
		front.sendMessage(confirm.array(), 0, confirm.limit());

		// receive confirm
		synchronized (waitBack) {
			waitBack.wait();
		}

		msg = "mqtt".equals(protocol) ? mqttConfirmMessage : confirmMessage;
		assertEquals(frontId, msg.from);

		front.close();
		back.close();
		Thread.sleep(1000);
		server.stop();
		Thread.sleep(1000);
	}

	private void publish_confirm_long_running_test(String protocol)
			throws Exception {
		Server server = this.initServer(9005, 9006);
		server.start();

		WebSocketClientFactory factory = new WebSocketClientFactory();
		factory.start();

		this.connect(factory, "ws://localhost:9005/front", "front", protocol,
				null);
		this.connect(factory, "ws://localhost:9005/front", "front", protocol,
				null);

		Connection back = this.connect(factory, "ws://localhost:9006/back",
				"back", protocol, null);

		int total = 10000;
		StopWatch watch = new StopWatch();
		watch.start();
		for (int i = 0; i < total; i++) {
			ByteBuffer publish = this.createPublishMessage(protocol, "front");
			back.sendMessage(publish.array(), 0, publish.limit());
		}
		watch.stop();
		// jetty websocket client slower than nodejs impl
		System.out.println(String.format("---- publish %s messages cost %sms",
				total, watch.getTime()));

		Thread.sleep(2000);
		while (!PushManager.current().isIdleClient("front")) {
			Thread.sleep(1000);
		}
		server.stop();
	}

	private ByteBuffer createPublishMessage(String protocol, String to) {
		byte[] bytes = new byte[1024];
		ByteBuffer buffer = ByteBuffer.wrap(bytes);

		if ("mqtt".equals(protocol)) {
			MqttPublishMessage msg = new MqttPublishMessage();
			msg.messageType = MessageType.PUBLISH;// 1
			msg.to = to;// 8
			msg.bodyFormat = 0;// 1
			msg.remainingLength = 7;// 4

			msg.Header.Qos = MqttQos.AtLeastOnce;
			msg.VariableHeader.TopicName = "abc";
			msg.VariableHeader.MessageIdentifier = 10;

			MqttMessageIO.parseClientSending(msg, buffer);
			// 1+1+5+1+4+7=19
			// 19+9
			// size = 28;
		} else {
			Message msg = new Message();
			msg.messageType = MessageType.PUBLISH;
			msg.to = to;
			msg.bodyFormat = 0;
			msg.remainingLength = 7;
			MessageIO.parseClientSending(msg, buffer);
			// size = 19;
		}
		buffer.put((byte) 'a');
		buffer.put((byte) 'b');
		buffer.put((byte) 'c');
		buffer.put((byte) 'd');
		buffer.put((byte) 'e');
		buffer.put((byte) 'f');
		buffer.put((byte) 'g');
		return buffer;
	}

	private ByteBuffer createConfirmMessage(String protocol,
			Message publishMessage) {
		byte[] bytes = new byte[1024];
		ByteBuffer buffer = ByteBuffer.wrap(bytes);

		if ("mqtt".equals(protocol)) {
			// also use MqttPublishMessage
			MqttPublishMessage msg = new MqttPublishMessage();
			msg.messageType = MessageType.PUBCONFIRM;
			msg.to = publishMessage.from;
			msg.bodyFormat = 0;
			msg.remainingLength = 100;
			MqttMessageIO.parseClientSending(msg, buffer);
		} else {
			Message msg = new Message();
			msg.messageType = MessageType.PUBCONFIRM;
			msg.to = publishMessage.from;
			msg.bodyFormat = 0;
			msg.remainingLength = 100;
			MessageIO.parseClientSending(msg, buffer);
		}
		return buffer;
	}

	private WebSocket.Connection connect(WebSocketClientFactory factory,
			String uri, String origin, String protocol, WebSocket handler)
			throws InterruptedException, ExecutionException, TimeoutException,
			IOException, URISyntaxException {
		WebSocketClient client = factory.newWebSocketClient();
		if (protocol != null)
			client.setProtocol(protocol);
		client.setOrigin(origin);
		return client.open(new URI(uri),
				handler == null ? new WebSocket.OnTextMessage() {
					public void onOpen(Connection connection) {
					}

					public void onClose(int closeCode, String message) {
					}

					public void onMessage(String data) {
					}
				} : handler).get(1, TimeUnit.SECONDS);
	}

	private Server initServer(int front, int back) throws IOException {
		Server server = new Server();
		// front
		SelectChannelConnector connector0 = new SelectChannelConnector();
		connector0.setPort(front);
		connector0.setMaxIdleTime(30000);
		connector0.setRequestHeaderSize(8192);
		connector0.accept(2);
		connector0.setThreadPool(new QueuedThreadPool(20));
		// back
		SelectChannelConnector connector1 = new SelectChannelConnector();
		connector1.setPort(back);
		connector1.setMaxIdleTime(30000);
		connector1.setRequestHeaderSize(8192);
		connector1.accept(1);
		connector1.setThreadPool(new QueuedThreadPool(20));

		// web-context
		ServletContextHandler context = new ServletContextHandler(
				ServletContextHandler.NO_SESSIONS);
		context.addServlet(new ServletHolder(new FrontendServlet()), "/front");
		context.addServlet(new ServletHolder(new BackendServlet()), "/back");
		// init push server
		ServletHolder initHolder = new ServletHolder(new InitServlet());
		initHolder.setInitParameter("maxConnectionCount", "10");
		initHolder.setInitParameter("maxMessageSize", "1024");
		initHolder.setInitParameter("maxMessageBufferCount", "10000");
		initHolder.setInitParameter("senderCount", "4");
		initHolder.setInitParameter("senderIdle", "10");
		initHolder.setInitParameter("stateBuilderIdle", "200");
		context.addServlet(initHolder, "/init");
		context.setContextPath("/");

		server.setConnectors(new Connector[] { connector0, connector1 });
		server.setHandler(context);
		return server;
	}
}
