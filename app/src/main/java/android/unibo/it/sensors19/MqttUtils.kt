package android.unibo.it.sensors19

import android.content.Context
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.eclipse.paho.client.mqttv3.MqttClientPersistence



class MqttUtils  {
	//protected var clientid: String? = null
	protected var eventId: String? = "mqtt"
	protected var eventMsg: String? = ""
	protected lateinit var client: MqttAndroidClient


	protected val RETAIN = false;

	fun connect(context : Context, clientid: String, brokerAddr: String ): Boolean {
		try {
  			println("	%%% MqttUtils doing connect for $clientid to $brokerAddr "  );
			val mqttClientPersistence = MemoryPersistence()
			client = MqttAndroidClient (context, brokerAddr, clientid,mqttClientPersistence)
            //println("	%%% MqttUtils for $clientid connect $brokerAddr client = $client" )
			val options = MqttConnectOptions()
			options.setKeepAliveInterval(480)
			options.setWill("unibo/clienterrors", "crashed".toByteArray(), 2, true)
			client.connect(options)
			println("	%%% MqttUtils connect DONE $clientid to $brokerAddr " )//+ " " + client
 			return true
		} catch (e: Exception) {
			println("	%%% MqttUtils for $clientid connect ERROR for: $brokerAddr" )
			return false
		}
	}

	fun disconnect() {
		try {
			println("	%%% MqttUtils disconnect " + client)
			client.disconnect()
		} catch (e: Exception) {
			println("	%%% MqttUtils disconnect ERROR ${e}")
		}
	}



	fun sendMsg( sender: String, msgID: String,  dest: String,  msgType: String ){
	  	val msgNum = 0
	  	//msg( MSGID, MSGTYPE SENDER, RECEIVER, CONTENT, SEQNUM )	  				
	  	val msgout = "msg( $msgID, $msgType, $sender, $dest, $msgNum )"
	  	//println(" ************ SENDING VIA MQTT mout=$msgout" )
	  	publish(   "unibo/qasys", msgout, 1, RETAIN);		
	}
	fun sendMsg(  msg: String, topic: String ){
		//println(" ************ SENDING VIA MQTT mout=$msg on $topic" )
 	  	publish( topic, msg, 1, RETAIN);
	}

	@Throws(MqttException::class)
	fun publish( topic: String, msg: String?,
		qos: Int, retain: Boolean) {
		val message = MqttMessage()
		message.setRetained(retain)
		if (qos == 0 || qos == 1 || qos == 2) {
			//qos=0 fire and forget; qos=1 at least once(default);qos=2 exactly once
			message.setQos(0)
		}
		message.setPayload(msg!!.toByteArray())
		try {
			client.publish(topic, message)
//			println("			%%% MqttUtils published "+ message + " on topic=" + topic);
		} catch (e:Exception) {
			//println("	%%% MqttUtils publish ERROR $e topic=$topic msg=$msg"  )
 		}
	}



	fun clearTopic( topic : String )  {
  		println("	%%%  clearTopic " +  topic );
		publish( topic, "", 1, true);	//send a retained message !!
	}


	fun println(msg: String?) {
		System.out.println(msg)
	}

}