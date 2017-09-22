package org.tb.entropy.utils


object ConditionalEntropy {
    
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

      def calculate(domain_names:org.apache.spark.rdd.RDD[Array[String]], k: Int): scala.collection.immutable.Map[String,Double] = {

          val dns_filter = domain_names.filter(_.length > k)

          val py = dns_filter.map(_.mkString("."))

          val py_count = py.map((_, 1)).aggregateByKey(0)( (n,v) => n+v, (n1,p) => n1+p)

          val dns_filter_reverse = dns_filter.map(_.reverse)

          val px = dns_filter_reverse.map(s => s.slice(0,k))

          val px_mk = px.map(_.reverse).map(_.mkString("."))

          val px_count = px_mk.map((_, 1)).aggregateByKey(0)( (n,v) => n+v, (n1,p) => n1+p)

          val py_count_k = py_count.map { t => (t._1.split('.').reverse.slice(0, k).reverse.mkString("."), (t._1, t._2)) }

          val py_px = py_count_k.join(px_count)

          val py_px_impurity = py_px.map { r => ( r._1, r._2._1._1, impurity(r._2._1._2.toDouble, r._2._2) ) }

          val py_px_entropy = py_px_impurity.aggregate(Map[String, Double]())( (acc, r) => acc + ( r._1 -> ( acc.getOrElse(r._1, 0.0) + r._3 ) ), (acc, m) => m.foldRight(acc)( (kv, acc2) => acc2 + (kv._1 -> (kv._2 + acc2.getOrElse(kv._1, 0.0)))))
          

        py_px_entropy
      } 

}
