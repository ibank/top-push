package com.tmall.top.push.mqtt;

public class MqttPublishMessage {
	// http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#publish
	// A PUBLISH message is sent by a client to a server for distribution to
	// interested subscribers. Each PUBLISH message is associated with a topic
	// name (also known as the Subject or Channel). This is a hierarchical name
	// space that defines a taxonomy of information sources for which
	// subscribers can register an interest. A message that is published to a
	// specific topic name is delivered to connected subscribers for that topic.

	// If a client subscribes to one or more topics, any message published to
	// those topics are sent by the server to the client as a PUBLISH message.
}