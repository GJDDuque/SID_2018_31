package iscte.sid.sensorsuite.broker;

import java.io.IOException;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import iscte.sid.sensorsuite.model.LightMeasure;
import iscte.sid.sensorsuite.model.MqttConfig;
import iscte.sid.sensorsuite.model.TemperatureMeasure;
import iscte.sid.sensorsuite.model.exceptions.ArgumentNullExeption;
import iscte.sid.sensorsuite.mongo.MongoDb;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SensorInformationBroker {

	private static final int MQTT_CONNECTION_TIMEOUT = 60000;
	public static final String SID_31_2019 = "SID-31-2019";
	private MqttAsyncClient client;
	@Autowired
	private ObjectMapper mapper;
	@Autowired
	private MongoDb db;
	private int configMqttQos;
	private MqttConfig backUpConfig;
	private boolean backup=false;
	private MqttConfig mqttMainConfig;
	private int counter=0;

	public SensorInformationBroker(int config_mqtt_qos, MqttConfig mqttMainConfig, MqttConfig backUpConfig) {
		super();
		this.configMqttQos = config_mqtt_qos;
		this.mqttMainConfig = mqttMainConfig;
		this.backUpConfig = backUpConfig;
		log.debug("created MongoBroker with UUID " + UUID.randomUUID());
		createClient(mqttMainConfig);
	}

	private void createClient(MqttConfig config) {
		try {
			setClient(new MqttAsyncClient(config.getAddress(), SID_31_2019));
		} catch (MqttException e) {
			setClient(null);
			log.error("initializing the mqttclient failed and mongo broker is useless now...", e);
			db.addErrorEntry(e.getMessage(), "mqttClient", "init_error");
			return;
		}

		setCallBack(config.getTopic());

		connectToMqtt();
	}

	private void connectToMqtt() {
		try {
			MqttConnectOptions options = new MqttConnectOptions();
			options.setConnectionTimeout(MQTT_CONNECTION_TIMEOUT);
			
			getClient().connect(options, null, new IMqttActionListener() {

				public void onSuccess(IMqttToken asyncActionToken) {
					System.out.println("connected ");

				}

				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					tryAnotherMqtt();
					System.out.println("not connected " + exception);

				}
			});
		} catch (MqttSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void setCallBack(String topic) {
		getClient().setCallback(new MqttCallbackExtended() {

			public void messageArrived(String topic, MqttMessage message) {

				boolean temperror = false;
				try {
					TemperatureMeasure.convertFromString(message.toString(), mapper);

				} catch (JsonParseException e) {
					temperror = true;
					db.addErrorEntry(message.toString(), "temperature", e.getMessage());
				} catch (JsonMappingException e) {
					temperror = true;
					db.addErrorEntry(message.toString(), "temperature", e.getMessage());
				} catch (IOException e) {
					temperror = true;
					db.addErrorEntry(message.toString(), "temperature", e.getMessage());

				} catch (ArgumentNullExeption e1) {
					db.addErrorEntry(message.toString(), "temperature", "null_value");
				}

				boolean lighterror = false;
				try {
				LightMeasure.convertFromString(message.toString(), mapper);

						
				} catch (JsonParseException e) {
					lighterror = true;
					db.addErrorEntry(message.toString(), "light", e.getMessage());
				} catch (JsonMappingException e) {
					lighterror = true;
					db.addErrorEntry(message.toString(), "light", e.getMessage());
				} catch (IOException e) {
					lighterror = true;
					db.addErrorEntry(message.toString(), "light", e.getMessage());
				} catch (ArgumentNullExeption e) {
					db.addErrorEntry(message.toString(), "light", "null_value");
				}

				if (!lighterror || !temperror)
					db.addEntry(message.toString());
			}

			public void deliveryComplete(IMqttDeliveryToken token) {
				// TODO Auto-generated method stub

			}

			public void connectionLost(Throwable cause) {
				// TODO Auto-generated method stub

			}

			public void connectComplete(boolean reconnect, String serverURI) {
				System.out.println("connected to " + serverURI);
				try {
					System.out.println("subscribing to " + topic);
					client.subscribe(topic, configMqttQos);
				} catch (MqttException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});
	}

	/**
	 * Switch the client from the main to the backup one
	 */
	protected void tryAnotherMqtt() {
		System.out.println("Trying another client...");
		backup = !backup;
		if(counter < 10)
		if(backup) {
		createClient(backUpConfig);
		}else {
			createClient(mqttMainConfig);	
		}
		
		counter++;
	}

	public MqttAsyncClient getClient() {
		return client;
	}

	public void setClient(MqttAsyncClient client) {
		this.client = client;
	}

	public void terminate() {

		try {
			client.close(true);
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		db.terminate();
	}
}
