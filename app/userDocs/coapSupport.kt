package android.unibo.it.sensors19

import org.eclipse.californium.core.CoapClient
import org.eclipse.californium.core.coap.MediaTypeRegistry
import org.eclipse.californium.core.CoapResponse

object coapSupport{
lateinit var client : CoapClient
lateinit var host   : String
	
	fun init( address : String ){
		host = address
 	}
	
	private fun setClientForPath( path : String ){
		val url = host + "/" + path
		println("coapSupport | setClientForPath url=$url")
		client = CoapClient( url )
		client.setTimeout( 1000L )
	}
	
	fun updateResource(  path: String, msg : String ){
		setClientForPath( path )
		//println("coapSupport | updateResource $msg $client")
		val resp : CoapResponse = client.put(msg, MediaTypeRegistry.TEXT_PLAIN)
		//println("coapSupport | updateResource respCode=${resp.getCode()}")
	}
	
	fun readResource (   path : String ) : String {
		setClientForPath( path )
		val respGet : CoapResponse = client.get( )
		val v = respGet.getResponseText()
		//println("coapSupport | readResource v=$v")
		return v
	}
	

}
