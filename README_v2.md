# Spark PCAP Reader

Script to read PCAP files from a file system, an pull information out of the packets, which is used to get statistics with Spark.

# Pre-requirements

  - Open jdk 1.8.0
  - Scala 2.11.8
  - Spark 2.2.0 Pre-built with Hadoop 2.6.0
  - Hadoop-pcap-lib.1.1 RIPE-NCC library

# Installation

### Jnetpcap
For the project we did not use at the end Jnetpcap, nontheless, here some notes in case you need to use it later. 

Use jnetpcap-1.4.r1425 version to run this script, located in the repository, this file is already compiled.

We have to set the environment variable to indicate the linker where to find the share folder that host Jnetpcap :
```sh
$ LD_LIBRARY_PATH= ~/path-directory/jnetpcap-1.4.r1425 
```
### Scala
```sh
$ sudo aptitude install scala 
```
Or download it from https://www.scala-lang.org/download/

### Spark

Download it from https://spark.apache.org/downloads.html, choose the option built-in that come with Hadoop v.2.6.0

Untar the file:
```sh
$ tar -xf  spark-2.2.0-bin-hadoop2.6.tgz
```
#### Building Spark

Spark is built using [Apache Maven](http://maven.apache.org/).
To build Spark and its example programs, run:

    build/mvn -DskipTests clean package

(You do not need to do this if you downloaded a pre-built package.)

You can build Spark using more than one thread by using the -T option with Maven, see ["Parallel builds in Maven 3"](https://cwiki.apache.org/confluence/display/MAVEN/Parallel+builds+in+Maven+3).
More detailed documentation is available from the project site, at
["Building Spark"](http://spark.apache.org/docs/latest/building-spark.html).

For general development tips, including info on developing Spark using an IDE, see 
[http://spark.apache.org/developer-tools.html](the Useful Developer Tools page).

#### Interactive Scala Shell

The easiest way to start using Spark is through the Scala shell:

    ./bin/spark-shell

## Usage

    $.sudo ./spark-shell --jars path-to-file/hadoop-pcap-lib-1.2-SNAPSHOT.jar,path-to-file/dns-input-format-1.0-SNAPSHOT.jar,path-to-file/dnsjava-2.1.8.jar

    scala>

It's comma separated without spaces!

#### Submit Scala 

To be able to run the script without copy-page, we use ./spark-submit, the syntax is tas follows:


First comes the jars:

- hadoop-pcap-lib-1.2-SNAPSHOT.jar, hadoop pcap library
- dns-input-format-1.0-SNAPSHOT.jar, to get DNS payload
- dnsjava-2.1.8.jar
- entropy-utils_2.11-1.0.0.jar, class to calculate Conditional Entropy

Second, main class is Script located inside /Code_hadoop_pcap/script/src/main/scala/script.scala

With the option --name, we can put a name to the job we are running

The option master is to be set as local since we are not working distributed mode.

then come the application jar, which correspond to the script's jar

At the end the arguments passed to the script, namely path-to-pcap-file and the label we want to calculate the conditional entropy for. As an www.example.com, if we want to calculate the conditional entropy for that domain given the tld (.com), we put 1. If we want to calculate given the sld (example.com) we put 2, etc.

Script as well as ConditionalEntropy.scala (class to calculate Conditional Entropy /Code_hadoop_pcap/entropy-utils) were compiled with sbt compile, the parameters and dependencies are defined in built.sbt located in there main folder, ../entropy-utils and ../script respectively.

Here the code inside ../script/built.sbt

```sh
scalaVersion := "2.11.11"

libraryDependencies ++= Seq("org.apache.spark" % "spark-core_2.10" % "2.2.0", "org.apache.hadoop" % "hadoop-common" % "2.6.5", "org.apache.hadoop" % "hadoop-mapreduce-client-core" % "2.6.5", "net.ripe.hadoop.pcap" % "hadoop-pcap-lib" % "1.1" from "file:/home/etarazona/Telecom_Bretagne/2do_Semestre/Stage_Ete/Projet/hadoop-pcap-lib-1.1.jar", "org.example.DnsInputFormat" % "dns-input-format" % "1.0-SNAPSHOT" from "file:/home/etarazona/tmp1/em/dns-input-format/target/dns-input-format-1.0-SNAPSHOT.jar", "dnsjava" % "dnsjava" % "2.1.8", "org.tb.entropy" %% "entropy-utils" % "1.0.0", "org.apache.spark" % "spark-streaming_2.10" % "2.2.0" % "provided", "org.apache.spark" % "spark-sql_2.10" % "2.2.0", "org.apache.spark" % "spark-mllib_2.10" % "2.2.0" % "provided")
 
```
Those paths to jars have to be change, with respect to the path used locally in your machine, then save:

```sh
sbt compile
publish-local

```
```sh
bin/spark-submit --jars /home/etarazona/Downloads/hadoop-pcap-lib-1.2-SNAPSHOT.jar,/home/etarazona/tmp1/em/dns-input-format/target/dns-input-format-1.0-SNAPSHOT.jar,/home/etarazona/Downloads/dnsjava-2.1.8.jar,/home/etarazona/Telecom_Bretagne/2do_Semestre/Stage_Ete/Projet/scala_spark_4_sec/Code_Hadoop_Pcap/entropy-utils/target/scala-2.11/entropy-utils_2.11-1.0.0.jar --class Script --name test --master local /home/etarazona/Telecom_Bretagne/2do_Semestre/Stage_Ete/Projet/scala_spark_4_sec/Code_Hadoop_Pcap/script/target/scala-2.11/script_2.11-0.1-SNAPSHOT.jar /home/etarazona/Telecom_Bretagne/2do_Semestre/Stage_Ete/Projet/pcapsfiles/ 2

```

