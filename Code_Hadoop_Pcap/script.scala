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
import org.tb.entropy.utils._



object Script {

  def main(args: Array[String]): Unit = {
    
  
        if (args.length > 1){
          /*Open and read the files*/

            val conf = new SparkConf().setAppName("script").setMaster("local")

            val sc = new SparkContext(conf)

            val folder = args(0)

            val input = sc.hadoopFile(folder, classOf[DnsInputFormat], classOf[LongWritable], classOf[ObjectWritable])

            val array_in: RDD[(Long, DnsPacket)] = input.map { case (k,v) => (k.get, v.get.asInstanceOf[DnsPacket]) }

            //array_in.take(10).map(println)


            /*********Calculate entropy for FQDN (without mapping with src ip)*************/

            val result = array_in.values.map {p => p.get("dns_qname") }
            val values_dns = result.filter(_ != null)

            values_dns.take(20)

            val values_dns_count = values_dns.map((_, 1)).aggregateByKey(0)( (n,v) => n+v, (n1,p) => n1+p)

            val array_4_entropy = values_dns_count.map(_._2).collect().toArray.map(_.toDouble)

            val total_count_entropy = array_4_entropy.sum


            val entropy_FQDN = Entropy.calculate(array_4_entropy, total_count_entropy)

            val file1 = "/home/etarazona/Downloads/EntropyFQDN.csv"

          //****************************************************************************


          /* CONDITIONAL ENTROPY */

        
          val values_dns_string = values_dns.map(_.toString) // object containing FQDN converted to string 

          val values_dns_split = values_dns_string.map(_.split('.')) // split by .

         
        
          val entropy_test = ConditionalEntropy.calculate(values_dns_split, args(1).toInt)
          
          entropy_test.take(10).map(println)

          val array_e = entropy_test.toArray
          val rdd = sc.parallelize(array_e)
          rdd.repartition(1).saveAsTextFile("/home/etarazona/Downloads/conditional_entropy")
          
        
      }  
      else {println("Bad Arguments")}
  }
}