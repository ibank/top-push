package com.taobao.top.push.mqtt.connect;

import com.taobao.top.push.mqtt.MqttHeader;
import com.taobao.top.push.mqtt.MqttMessage;
import com.taobao.top.push.mqtt.MqttMessageType;

public class MqttConnectMessage extends MqttMessage {
	public MqttConnectVariableHeader VariableHeader;

	// TODO: impl mqtt CONNECT payload
	// http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#connect
	
	public MqttConnectMessage() {
		this.Header = new MqttHeader();
		this.Header.MessageType = MqttMessageType.Connect;
		this.VariableHeader = new MqttConnectVariableHeader();
	}
}
