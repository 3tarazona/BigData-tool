
import org.apache.spark._
import org.apache.hadoop.io._
import net.ripe.hadoop._
import org.example.DnsInputFormat
import org.apache.spark.mllib.tree.impurity._
import net.ripe.hadoop.pcap.io.PcapInputFormat
import net.ripe.hadoop.pcap.packet.Packet
import net.ripe.hadoop.pcap.packet.DnsPacket

/*Function to calculate impurity inside Conditional Entropy*/


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
/*import into spark context the data handled by hadoop-pcap-lib, using the class created (dns-input-format) 
to get dns payload, keys as LongWritable & values as ObjectWritable*/
val input = sc.hadoopFile(folder, classOf[DnsInputFormat], classOf[LongWritable], classOf[ObjectWritable])
/*Cast keys as Long and values as DnsPacket*/
val array_in = input.map{case(k,v) => (k.get(),v.get().asInstanceOf[DnsPacket])}

//array_in.take(10).map(println)


/*********Calculate entropy for FQDN*************/

/*Get dns_qname values*/
val result = array_in.values.map {p => p.get("dns_qname") }
/*Filter the values not null to avoid nullPointer-errors, that assures to filter out data without dns information*/
val values_dns = result.filter(_ != null)

values_dns.take(20)

/* AggregateByKey: First function Count the values by each worker in all partitions, the 2nd function 
takes the previous result & sum up for all workers, always keeping track of the key in the process */
val values_dns_count = values_dns.map((_, 1)).aggregateByKey(0)( (n,v) => n+v, (n1,p) => n1+p)
/*Take the second element (the counting), collect to array and convert to double*/
val array_4_entropy = values_dns_count.map(_._2).collect().map(_.toDouble)
/*Sum up all elements in array*/
val total_count_entropy = array_4_entropy.sum

/*calculate receives Array[Double] and Double, it uses Spark ML to perform stats*/
val entropy_FQDN = Entropy.calculate(array_4_entropy, total_count_entropy)

//****************************************************************************


/* CONDITIONAL ENTROPY */


val values_dns_string = values_dns.map(_.toString) // object containing FQDN converted to string 

val values_dns_split = values_dns_string.map(_.split('.')) // split by .

/*Function to calculate conditional entropy*/

def conditional_entropy(domain_names:org.apache.spark.rdd.RDD[Array[String]], k: Int): scala.collection.immutable.Map[String,Double] = {
    /*Filter to get just those domains with more labels than the level desired*/
    val dns_filter = domain_names.filter(_.length > k)

    val py = dns_filter.map(_.mkString("."))

    /*Count for P(y) -> FQDN*/
    val py_count = py.map((_, 1)).aggregateByKey(0)( (n,v) => n+v, (n1,p) => n1+p)

    val dns_filter_reverse = dns_filter.map(_.reverse)

    /*Get sub-domain*/
    val px = dns_filter_reverse.map(s => s.slice(0,k))

    val px_mk = px.map(_.reverse).map(_.mkString("."))

    /*Count or P(x)*/
    val px_count = px_mk.map((_, 1)).aggregateByKey(0)( (n,v) => n+v, (n1,p) => n1+p)

    
    /*Map sub-domain, tuple (domain, queries) and stores in an array of this way: Array((kayak.org,(xayrxhvk.kayak.org,191)))*/
    val py_count_k = py_count.map { t => (t._1.split('.').reverse.slice(0, k).reverse.mkString("."), (t._1, t._2)) }

    /*Join both arrays domains and sub-domains in a format Key,Value: Array((maravilla.com,((t.maravilla.com,541),26669)))
    all this to be able to keep correlation between domain, sub-domain and number of occurrences*/
    val py_px = py_count_k.join(px_count)

    /*Compute entropy keeping relation between domain, sub-domain and entropy*/
    val py_px_impurity = py_px.map { r => ( r._1, r._2._1._1, impurity(r._2._1._2.toDouble, r._2._2) ) }

    /* Aggregate: we initialize the accumulator as an empty map, the first function sum up entropies with domains as key, 
    this result is stored in accumulator map which is passed to 2nd function, the second function does the same operation 
    but working with maps, the aggregated results are stored on the initial Map[String, Double], all this to sum up entropies
    of a domain given a sub-domain. note: foldRight allows to work with a format like: [(domain, value), (domain, value)..] */
    
    val py_px_entropy = py_px_impurity.aggregate(Map[String, Double]())( (acc, r) => acc + ( r._1 -> ( acc.getOrElse(r._1, 0.0) + r._3 ) ), (acc, m) => m.foldRight(acc)( (kv, acc2) => acc2 + (kv._1 -> (kv._2 + acc2.getOrElse(kv._1, 0.0)))))
  
  py_px_entropy
} 

val entropy_test = conditional_entropy(values_dns_split, 2)

/*Save to file*/

val array_e = entropy_test.toArray
val rdd = sc.parallelize(array_e)
/*Takes the 10 higher values, we reverse because by default it returns descending & it orders by entropy value */
val toprint = rdd.takeOrdered(10)(Ordering[Double].reverse.on{x => x._2})
/*it saves to a file the conditional entropy values, coalesce to 1 partition, to avoid multiples files. 
  In the Script code, folder is given as args*/
toprint.repartition(1).saveAsTextFile("/home/etarazona/Downloads/conditional_entropy")

/* ******************************************************************
Calculate entropy for aggregated level-0 (FQDN) mapping with src ip 

val src_dnsqname = array_in.values.map {p => (p.get("src"), p.get("dns_qname")) }

val src_dnsqname_count = src_dnsqname.map((_, 1)).aggregateByKey(0)( (n,v) => n+v, (n1,p) => n1+p)

val src_array_entropy = src_dnsqname_count.map(_._2).collect().toArray

val total_src_entropy = src_array_entropy.sum

val entropy_src_dns = Entropy.calculate(src_array_entropy, total_src_entropy)

****************************************************************************/


  
