/*import java.io.EOFException

import org.jnetpcap.Pcap
import org.jnetpcap.packet.JPacket
import org.jnetpcap.packet.JPacketHandler
import org.jnetpcap.protocol.tcpip.Udp
import scala.collection.mutable
import org.pcap4j.packet.{UdpPacket, IpV4Packet, Packet}
import org.pcap4j.core.NotOpenException
import org.pcap4j.core.PcapHandle
import org.pcap4j.core.PcapNativeException
import org.pcap4j.core.Pcaps*/

//import org.jnetpcap.examples.packet;  
  	
import java.util.HashMap;  
import java.util.List;  
import java.util.Map;  
  
import org.jnetpcap.Pcap;  
import org.jnetpcap.nio.JMemory;  
import org.jnetpcap.packet.JFlow;  
import org.jnetpcap.packet.JFlowKey;  
import org.jnetpcap.packet.JFlowMap;  
import org.jnetpcap.packet.JPacket;  
import org.jnetpcap.packet.JPacketHandler;  
import org.jnetpcap.packet.JScanner;  
import org.jnetpcap.packet.PcapPacket;  
import org.jnetpcap.protocol.tcpip.Http;  
import org.jnetpcap.protocol.tcpip.Tcp;


class DNSreader {

	val ipadd = new Ip4()
	val sb = new mutable.StringBuilder()
	val flows = new mutable.StringBuilder()

	def readPcap(path: String){

		val errbuf = new java.lang.StringBuilder()
		val path = "dump-dns-random-12K.pcap"
    	val pcap = Pcap.openOffline(path, errbuf)
    	sb.clear()
    	flows.clear()
    	if (pcap == null) {
	     	sb.append(errbuf)
	      	return flows
	    }
	    pcap.loop(10, new JPacketHandler<StringBuilder>, errbuf)
	    pcap.close()
	    flows
	}

    
    def nextPacket(packet: JPacket, errbuf: java.lang.StringBuilder) {

    	if (packet.hasHeader(ipadd)){
    		val ipaddressD = packet.getHeader(ipadd).destination()
    		val ipaddressS = packet.getHeader(ipadd).source()
    		String sourceIP = org.jnetpcap.packet.format.FormatUtils.ip(ipaddressS)
			String destinationIP = org.jnetpcap.packet.format.FormatUtils.ip(ipaddressD)
			println(sourceIP)
			println(destinationIP)
    	}

	    
	}
	
}

object DNSreader {
    def main(args: Array[String]) {
      println("Hello, world!")
    }
  }