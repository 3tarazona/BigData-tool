
import java.io.File
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.sql.types._
import org.apache.spark.sql.Row
import org.apache.spark.sql.SQLImplicits

import net.ripe.hadoop.pcap.io.PcapInputFormat
import net.ripe.hadoop.pcap.packet.Packet
import net.ripe.hadoop.pcap.packet.DnsPacket
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark._

import org.apache.hadoop.io._
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import net.ripe.hadoop._
import org.apache.hadoop.mapred.FileInputFormat
import org.apache.hadoop.mapred.JobConf
import org.example.DnsInputFormat



//val folder = new File("/home/etarazona/Telecom_Bretagne/2do_Semestre/Stage_Ete/Projet/pcapsfiles")
val folder = "/home/etarazona/Telecom_Bretagne/2do_Semestre/Stage_Ete/Projet/pcapsfiles"

val input = sc.hadoopFile(folder, classOf[DnsInputFormat], classOf[LongWritable], classOf[ObjectWritable])

val array_in = input.map{case(k,v) => (k.get(),v.get().asInstanceOf[DnsPacket])}

array_in.take(10).map(println)

val values_dns = array_in.values.map {p => p.get("dns_qname") }

values_dns.take(20)

val values_dns_count = values_dns.map((_, 1)).aggregateByKey(0)( (n,v) => n+v, (n1,p) => n1+p).take(10)

val array_4_entropy = values_dns_count.map(_._2)

val total_count_entropy = array_4_entropy.sum



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

val entropy_dns = calculate(array_4_entropy, total_count_entropy)





 // val sqlContext = new SQLContext(sc)
 // import sqlContext.implicits._

// array_in.toDF()





//val fields = StructType(Array("dns_additional", "dst", "ip_flags_df", "ip_header_length", "protocol", "ip_version", "len", "dns_opcode", "dns_qname", "dns_authority", "id", "dns_qr", "dns_qtype", "fragment_offset", "dns_flags", "src", "udp_length", "ttl", "dns_answer", "src_port", "fragment", "dns_question", "dns_queryid", "dns_rcode", "udp_sum", "dst_port", "ts_usec", "ip_flagsmf", "ts", "ts_micros").map( StructField(_, StringType, nullable = false )))

//val array_in_Row = array_in.map( (f,v) => Row(f._1, f._2, f._3, f._4, f._5, f._6, f._7, f._8, f._9, f._10, f._11, f._12, f._13, f._14, f._15, f._16, f._17, f._18, f._19, f._20, f._21, f._22, f._23, f._24, f._25, f._26, f._28, f._28, f._29, f._30) )

// val fields = StructType(Array("dns_qname").map( StructField(_, StringType, nullable = false )))

// val arra_in_Row = array_in.map( (f) => Row(f._1.getAs[String]("dns_qname"), f._2) )

// val array_in_DF = spark.createDataFrame(array_in_Row, fields)


// key_values.filter($"dns_qname").show()

// array_in.map { case (k, v) => (k, v._2, v._1.split('.')(1), v._1.split(',')(3)) }

  
