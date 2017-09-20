
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
import org.apache.spark.mllib.tree.impurity._

/*Function to take the nth value in a list/Array*/

def nth(idx: Int, list: Array[String]) : String = idx match {
        case x if x < 0 => throw new Exception("Negative index not allowed")
        case 0 => list.head
        case x if list.length < x + 1 => throw new Exception("Invalid index")
        case _ => nth(idx - 1, list.tail)
    }

def impurity(py: Double, px: Int) : Double = {
    if (px == 0) {
      return 0
    }
    var impur = 0.0
    def log2(x: Double) = scala.math.log(x) / scala.math.log(2)
    val freq = py / px
    impur += -(freq * log2(freq))
  impur
} 


/*Open and read the files*/

val folder = "/home/etarazona/Telecom_Bretagne/2do_Semestre/Stage_Ete/Projet/pcapsfiles"

val input = sc.hadoopFile(folder, classOf[DnsInputFormat], classOf[LongWritable], classOf[ObjectWritable])

val array_in = input.map{case(k,v) => (k.get(),v.get().asInstanceOf[DnsPacket])}

//array_in.take(10).map(println)


/*********Calculate entropy for FQDN (without mapping with src ip)*************/

val values_dns = array_in.values.map {p => p.get("dns_qname") }

values_dns.take(20)

val values_dns_count = values_dns.map((_, 1)).aggregateByKey(0)( (n,v) => n+v, (n1,p) => n1+p)

val array_4_entropy = values_dns_count.map(_._2).collect().map(_.toDouble)

val total_count_entropy = array_4_entropy.sum


val entropy_FQDN = Entropy.calculate(array_4_entropy, total_count_entropy)

//****************************************************************************


/* CONDITIONAL ENTROPY */


val values_dns_string = values_dns.map(_.toString) // object containing FQDN converted to string 

val values_dns_split = values_dns_string.map(_.split('.')) // split by .


def conditional_entropy(domain_names:org.apache.spark.rdd.RDD[Array[String]], k: Int): scala.collection.immutable.Map[String,Double] = {

    val dns_filter = domain_names.filter(_.length > k)

    val py = dns_filter.map(_.mkString("."))

    val py_count = py.map((_, 1)).aggregateByKey(0)( (n,v) => n+v, (n1,p) => n1+p)

    //val py_array = py_count.map(_._2).collect.toArray

    val dns_filter_reverse = dns_filter.map(_.reverse)

    val px = dns_filter_reverse.map(s => s.slice(0,k))

    val px_mk = px.map(_.reverse).map(_.mkString("."))

    val px_count = px_mk.map((_, 1)).aggregateByKey(0)( (n,v) => n+v, (n1,p) => n1+p)

    //val px_array = px_count.map(_._2).collect.toArray

    val py_count_k = py_count.map { t => (t._1.split('.').reverse.slice(0, k).reverse.mkString("."), (t._1, t._2)) }

    val py_px = py_count_k.join(px_count)

    val py_px_impurity = py_px.map { r => ( r._1, r._2._1._1, impurity(r._2._1._2.toDouble, r._2._2) ) }

    val py_px_entropy = py_px_impurity.aggregate(Map[String, Double]())( (acc, r) => acc + ( r._1 -> ( acc.getOrElse(r._1, 0.0) + r._3 ) ), (acc, m) => m.foldRight(acc)( (kv, acc2) => acc2 + (kv._1 -> (kv._2 + acc2.getOrElse(kv._1, 0.0)))))
    val result = py_px_entropy.take(10)
  result
} 

val entropy_test = conditional_entropy(values_dns_split, 2)

/*Save to a ext file*/

val array_e = entropy_test.toArray
val rdd = sc.parallelize(array_e)
rdd.repartition(1).saveAsTextFile("/home/etarazona/Downloads/conditional_entropy")

/* ******************************************************************
Calculate entropy for aggregated level-0 (FQDN) mapping with src ip 

val src_dnsqname = array_in.values.map {p => (p.get("src"), p.get("dns_qname")) }

val src_dnsqname_count = src_dnsqname.map((_, 1)).aggregateByKey(0)( (n,v) => n+v, (n1,p) => n1+p)

val src_array_entropy = src_dnsqname_count.map(_._2).collect().toArray

val total_src_entropy = src_array_entropy.sum

val entropy_src_dns = calculate_entropy(src_array_entropy, total_src_entropy)

****************************************************************************/


  
