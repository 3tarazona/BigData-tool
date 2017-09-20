scalaVersion := "2.11.11"

libraryDependencies ++= Seq("org.apache.spark" % "spark-core_2.10" % "2.2.0", "org.apache.hadoop" % "hadoop-common" % "2.6.5", "org.apache.hadoop" % "hadoop-mapreduce-client-core" % "2.6.5", "net.ripe.hadoop.pcap" % "hadoop-pcap-lib" % "1.1" from "file:/home/etarazona/Telecom_Bretagne/2do_Semestre/Stage_Ete/Projet/hadoop-pcap-lib-1.1.jar", "org.example.DnsInputFormat" % "dns-input-format" % "1.0-SNAPSHOT" from "file:/home/etarazona/tmp1/em/dns-input-format/target/dns-input-format-1.0-SNAPSHOT.jar", "dnsjava" % "dnsjava" % "2.1.8", "org.tb.entropy" %% "entropy-utils" % "1.0.0", "org.apache.spark" % "spark-streaming_2.10" % "2.2.0" % "provided", "org.apache.spark" % "spark-sql_2.10" % "2.2.0", "org.apache.spark" % "spark-mllib_2.10" % "2.2.0" % "provided")


