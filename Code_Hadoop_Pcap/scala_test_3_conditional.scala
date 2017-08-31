
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
import java.lang.Math

/*Function to take the nth value in a list/Array*/

def nth(idx: Int, list: Array[String]) : String = idx match {
        case x if x < 0 => throw new Exception("Negative index not allowed")
        case 0 => list.head
        case x if list.length < x + 1 => throw new Exception("Invalid index")
        case _ => nth(idx - 1, list.tail)
    }


/* Entropy Function */
def calculate_entropy(counts: Array[Int], totalCount: Double): Double = {
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


/*Open and read the files*/

val folder = "/home/etarazona/Telecom_Bretagne/2do_Semestre/Stage_Ete/Projet/pcapsfiles"

val input = sc.hadoopFile(folder, classOf[DnsInputFormat], classOf[LongWritable], classOf[ObjectWritable])

val array_in = input.map{case(k,v) => (k.get(),v.get().asInstanceOf[DnsPacket])}

array_in.take(10).map(println)

/*********Calculate entropy for FQDN (without mapping with src ip)*************/

val values_dns = array_in.values.map {p => p.get("dns_qname") }

values_dns.take(20)

val values_dns_count = values_dns.map((_, 1)).aggregateByKey(0)( (n,v) => n+v, (n1,p) => n1+p)

val array_4_entropy = values_dns_count.map(_._2).collect().toArray

val total_count_entropy = array_4_entropy.sum


val entropy_FQDN = calculate_entropy(array_4_entropy, total_count_entropy)

//****************************************************************************


/* CONDITIONAL ENTROPY */

/*CONDITIONAL ENTROPY LEVEL-1*/

val values_dns_string = values_dns.map(_.toString) // object containing FQDN converted to string 

val values_dns_split = values_dns_string.map(_.split('.')) // split by .

val dns_tld = values_dns_split.map(s => s.last)

val dns_sld = values_dns_split.filter(_.length < 3).map(_.mkString("."))

val dns_sld_count = dns_sld.map((_, 1)).aggregateByKey(0)( (n,v) => n+v, (n1,p) => n1+p)

val dns_sld_array = dns_sld_count.map(_._2).collect().toArray

val dns_sld_total = dns_sld_array.sum


/*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
// Ratio de todos los valores .com

val tld = dns_tld.filter(_.startsWith("com"))
val tld_count = tld.count
val px =  tld_count.toDouble/values_dns.count

// Array de valores example.com, arbol.com, tienda.com...etc 

val sld = dns_sld.filter(_.endsWith(".com"))
val sld_count = sld.map((_, 1)).aggregateByKey(0)( (n,v) => n+v, (n1,p) => n1+p)

val sld_count_array = sld_count.map(_._2).collect().toArray


/*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/



def entropy_conditional(counts_y: Array[Int], p_x: Double, totalCount: Double): Double = {
    if (totalCount == 0) {
      return 0
    }
    def log2(x: Double) = scala.math.log(x) / scala.math.log(2)

    val numClasses = counts_y.length
    var impurity = 0.0
    var classIndex_y = 0

    while (classIndex_y < numClasses) {
      val classCount_y = counts_y(classIndex_y)
      if (classCount_y != 0 ) {
        val freq_y_x = classCount_y / (totalCount * p_x)
        impurity += -(freq_y_x * log2(freq_y_x))
      }
      classIndex_y += 1
    }
  impurity
}

val conditional_entropy_com = entropy_conditional(sld_count_array, px, dns_sld_total)






/* Calculate entropy for aggregated level-0 (FQDN) mapping with src ip */

val src_dnsqname = array_in.values.map {p => (p.get("src"), p.get("dns_qname")) }

val src_dnsqname_count = src_dnsqname.map((_, 1)).aggregateByKey(0)( (n,v) => n+v, (n1,p) => n1+p)

val src_array_entropy = src_dnsqname_count.map(_._2).collect().toArray

val total_src_entropy = src_array_entropy.sum

val entropy_src_dns = calculate_entropy(src_array_entropy, total_src_entropy)


/*Calculate entropy for Aggregated Level-2*/


/* Calculate entropy for Non-Aggregated Level-0: H(D0) = H(D1) + H(D0/H(D1)) */

val src_dnsqname = array_in.values.map {p => (p.get("src"), p.get("dns_qname")) }

val dnsqname_string = src_dnsqname.map{case(k,v) => (k, v.toString)}

val dnsqname_split = dnsqname_string.map{case(k,v) => (k, v.split('.'))}

val dnsqname_tld = dnsqname_split.map{case(k,v) => (k, v(1))}

val dnsqname_count = dnsqname_tld.map((_, 1)).aggregateByKey(0)( (n,v) => n+v, (n1,p) => n1+p)

val dnsqname_array_entropy = dnsqname_count.map(_._2).collect().toArray

val total_dnsqname_entropy = dnsqname_array_entropy.sum




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

  
