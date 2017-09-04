
import java.io.File
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.sql.types._
import org.apache.spark.sql.Row
import org.apache.spark.sql.SQLImplicits
import org.apache.spark.rdd.RDD

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
def calculate_entropy(counts: Array[Double], totalCount: Double): Double = {
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

val array_4_entropy = values_dns_count.map(_._2).collect().toArray.map(_.toDouble)

val total_count_entropy = array_4_entropy.sum


val entropy_FQDN = calculate_entropy(array_4_entropy, total_count_entropy)

//****************************************************************************


/* CONDITIONAL ENTROPY */


val values_dns_string = values_dns.map(_.toString) // object containing FQDN converted to string 

val values_dns_split = values_dns_string.map(_.split('.')) // split by .


def conditional_entropy(domain_names:org.apache.spark.rdd.RDD[Array[String]], label: String): Double = {

  val label_split = label.split('.')
    
  val label_length = label_split.length //domain level for X 

  val array_fil_y = domain_names.filter(_.length > label_length) // filter to get just the entries with a "SLD" for previous"TLD"

  val array_y = array_fil_y.filter(_.containsSlice(label_split))

  val array_y_reverse = array_y.map(_.reverse)
  val kplus_values = array_y_reverse.map(s => s(label_length))
  val py_count = kplus_values.map((_, 1)).aggregateByKey(0)( (n,v) => n+v, (n1,p) => n1+p)
  val py = py_count.map(_._2).collect.toArray
  val count_y_total = array_y.count
  val py_array = py.map(s => s.toDouble/count_y_total)

  val px = count_y_total.toDouble/domain_names.count

  calculate_entropy(py_array, px)

}

conditional_entropy(values_dns_split, "kayak.org")


/*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/



/* ******************************************************************
Calculate entropy for aggregated level-0 (FQDN) mapping with src ip 

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

****************************************************************************/


  
