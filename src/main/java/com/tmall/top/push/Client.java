package com.tmall.top.push;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.tmall.top.push.messages.Message;

public class Client {
	private final static int MAX_FLUSH_COUNT = 100000;
	private String id;
	private ConcurrentLinkedQueue<ClientConnection> connections;
	private ConcurrentLinkedQueue<Message> pendingMessages;

	public Client(String id) {
		this.id = id;
		this.connections = new ConcurrentLinkedQueue<ClientConnection>();
		this.pendingMessages = new ConcurrentLinkedQueue<Message>();
	}

	public String getId() {
		return this.id;
	}

	public int getPendingMessagesCount() {
		return this.pendingMessages.size();
	}

	public void AddConnection(ClientConnection conn) {
		this.connections.add(conn);
		System.out.println(String.format(
				"client#%s add new connection from %s", this.getId(),
				conn.getOrigin()));
	}

	public void RemoveConnection(ClientConnection conn) {
		this.connections.remove(conn);
		System.out.println(String.format(
				"client#%s remove a connection from %s", this.getId(),
				conn.getOrigin()));
	}

	public void pendingMessage(Message msg) {
		if (msg != null)
			this.pendingMessages.add(msg);
	}

	public void flush(CancellationToken token, int count) {
		int temp = 0;
		for (int i = 0; i < count; i++) {
			if (token.isCancelling())
				break;
			// prevent client and bandwidth usage
			if (i == MAX_FLUSH_COUNT - 1)
				break;
			Message msg = this.pendingMessages.poll();
			if (msg == null)
				break;
			this.SendMessage(msg);
			temp++;
		}
		if (temp > 0)
			System.out.println(String.format("flush %s messages to client#%s ",
					temp, this.getId()));
	}

	protected void SendMessage(Message msg) {
		// FIFO queue for easy load-balance
		while (true) {
			ClientConnection connection = this.connections.poll();
			if (connection == null)
				break;
			if (!connection.isOpen()) {
				System.out.println(String.format(
						"connection#%s[%s] is closed, remove it",
						connection.getId(), connection.getOrigin()));
				continue;
			}
			try {
				connection.sendMessage(msg);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				// FIXME:maybe course dead-loop
				this.AddConnection(connection);
			}
		}

	}
}