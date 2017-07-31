package parsers

import java.io.EOFException

import org.jnetpcap.Pcap
import org.jnetpcap.packet.JPacket
import org.jnetpcap.packet.JPacketHandler
import org.jnetpcap.protocol.tcpip.Udp
import scala.collection.mutable
import org.pcap4j.packet.{UdpPacket, IpV4Packet, Packet}
import org.pcap4j.core.NotOpenException
import org.pcap4j.core.PcapHandle
import org.pcap4j.core.PcapNativeException
import org.pcap4j.core.Pcaps

/**
 * Adopted from http://jnetpcap.com/?q=tutorial/usage
 */
class Pcap2NetFlow(filter: (NetFlowItemIPV4) => Boolean) extends JPacketHandler[java.lang.StringBuilder] {


  val udp = new Udp()
  val sb = new mutable.StringBuilder()
  val flows = new mutable.MutableList[NetFlowItemIPV4]()
  final val NfPacketLen = 48

  def readFile(path: String): mutable.MutableList[NetFlowItemIPV4] = {
    val errbuf = new java.lang.StringBuilder()
    val pcap = Pcap.openOffline(path, errbuf)
    sb.clear()
    flows.clear()
    if (pcap == null) {
      sb.append(errbuf)
      return flows
    }
    pcap.loop(10, this, errbuf)
    pcap.close()
    flows
  }

  def nextPacket(packet: JPacket, errbuf: java.lang.StringBuilder) {
    if (packet.hasHeader(udp) && udp.destination() == 53) {
      val payload = udp.getPayload()
      Range(24, payload.length - NfPacketLen, NfPacketLen).foreach(i => {
        val nfPacket = new NetFlowItemIPV4(FlowPacket(payload, i))
        if (filter(nfPacket)) flows += nfPacket
      })
    }
  }
}
  