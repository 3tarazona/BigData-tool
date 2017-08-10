import org.jnetpcap.protocol.network.Ip4
import org.jnetpcap.Pcap
import org.jnetpcap.packet.PcapPacketHandler
import org.jnetpcap.packet.PcapPacket
import org.jnetpcap.packet.format.FormatUtils

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.sql.types._
import org.apache.spark.sql.Row


//val conf = new SparkConf().setAppName(appName).setMaster(master)
//val sc = new SparkContext(conf)

/*PCAP Parser*/

val filename = "/home/etarazona/Telecom_Bretagne/2do_Semestre/Stage_Ete/Projet/dump-dns-random-12K.pcap";

val pcap = Pcap.openOffline(filename, new java.lang.StringBuilder());
var sources = List[String]()
var ip_conversations = List[Tuple2[String,String]]()

val handler = new PcapPacketHandler[String]() {
  def nextPacket(packet: PcapPacket, s : String ) = {
    val ip4 = new Ip4();
    if (packet.hasHeader(ip4)) {
      val ip_source = packet.getHeader(ip4).source
      val ip_dst = packet.getHeader(ip4).destination
      //val payload = FormatUtils.asString(packet.getHeader(ip4).getPayload())
      sources = FormatUtils.ip(ip_source) :: sources
      ip_conversations = (FormatUtils.ip(ip_source), FormatUtils.ip(ip_dst)) :: ip_conversations
    }
  }
}

pcap.loop(Pcap.LOOP_INFINITE, handler, "");

/*Spark code*/

val rdd_sources = sc.parallelize(sources);
val rdd_conversations = sc.parallelize(ip_conversations)

val fields = StructType(Array("source", "destination").map( StructField(_, StringType, nullable = false ))) // table schema building
val conversationsRow = rdd_conversations.map( (f) => Row(f._1, f._2) )// transformation to Row to be converted to DF next 
val conversationsDF = spark.createDataFrame(conversationsRow, fields) // (rowRDD, schema)

// partitioning workers and adding number of times an ipadd is seen by that worker, 2nd function calculates amount of times an ipaddr is seen inside that partition
val ip_sources_count = rdd_sources.map((_, 1)).aggregateByKey(0)( (n, v) => n + v, (n1, p) => n1 + p ) // tuple to map ipadd with value=1=v n, n1=0
val conversations_count = rdd_conversations.map((_, 1)).aggregateByKey(0)( (n, v) => n + v, (n1, p) => n1 + p )

ip_sources_count.collect()
conversations_count.collect()

conversationsDF.groupBy("source", "destination").count().show()

conversationsDF.filter($"source".equalTo("2.5.102.210")).show()

/*Entropy*/  

val array = conversations_count.values.collect().toArray

def calculate(counts: Array[Int], totalCount: Double): Double = {
    if (totalCount == 0) {
      return 0
    }
    def log2(x: Double) = scala.math.log(x) / scala.math.log(2)
    val numClasses = counts.length
    var impurity = 0.0
    var classIndex = 0
    while (classIndex < numClasses) {
      val classCount = counts(classIndex)
      if (classCount != 0) {
        val freq = classCount / totalCount
        impurity += -(freq * log2(freq))
      }
      classIndex += 1
    }
    impurity
  }

  val entropy_count = calculate(array, conversationsDF.count().toDouble)


