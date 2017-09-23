# Spark PCAP Reader

Script to read PCAP files from a file system, an pull DNS information out of the packets, which is used to get statistics with Spark. This script is able to calculate entropy between FQDNs and Conditional Entropy between FQDNs and a sub-domain level passed as argument. It is coded in scala and spark.

## Licence

This library is distributed under the MIT licence.

## Pre-requirements

  - Open jdk 1.8.0
  - Scala 2.11.11
  - Spark 2.2.0 Pre-built with Hadoop 2.6.0
  - Hadoop-pcap-lib.1.1 RIPE-NCC library
  - dns-input-format-1.0-SNAPSHOT.jar
  - entropy-utils_2.11-1.0.0.jar
  - dnsjava-2.1.8.jar


## Installation


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
## Components

This project consists of two components:
1. A main class call script, that contains the code to compute FQDN Entropy and Conditional Entropy of domain names over pcap files, using RIPE-NCC's library hadoop-pcap-lib to parse pcap data.
2. ConditionalEntropy class that calculate Entropy between FQDNs and a sub-domain level passed as argument.

## Jars Location

All compiled jars are located in ../Code_Hadoop_Pcap/jars_submit, from there it can be taken those used for spark-shell mode too.


## Interactive Scala Shell

There is a script located in ../Code_Hadoop_Pcap/scala_test_3_conditional.scala to test the code by shell. It contains ConditionalEntropy class as a function and variables that refer to paths have to be modifiy to meet yours localy. This code has comments and explanations, and it can be modify easily:
```sh
    ./bin/spark-shell
```

### Usage
```sh
$.sudo ./spark-shell --jars $PATH_TO_FILE/hadoop-pcap-lib-1.2-SNAPSHOT.jar,$PATH_TO_FILE/dns-input-format-1.0-SNAPSHOT.jar,$PATH_TO_FILE/dnsjava-2.1.8.jar

    scala>
```
It's comma separated without spaces!

## Submit Spark

To be able to run the script without copy-paste, we use ./spark-submit, the syntax is as follows:


1. Specify the jars dependencies:

	- hadoop-pcap-lib-1.2-SNAPSHOT.jar, hadoop pcap library
	- dns-input-format-1.0-SNAPSHOT.jar, to get DNS payload
	- dnsjava-2.1.8.jar, required by hadoop-pcap-lib
	- entropy-utils_2.11-1.0.0.jar, class to compute Conditional Entropy

2. Specify main class that will run the code, in this case the name is Script located inside /Code_hadoop_pcap/script/src/main/scala/script.scala

3. With the option --name, we can put a name to the job we are running.

4. The option master is to be set as local since we are not working distributed mode.

5. then come the application jar, which correspond to the script's jar

6. At the end the arguments are passed to the script, in this order: 

	- PATH_TO_PCAP_FILES
	- The label we want to calculate the conditional entropy for. As an www.example.com, if we want to calculate the conditional entropy for that domain given the tld (.com), we put 1.If we want to calculate given the sld (example.com) we put 2, etc.
	- PATH_TO_FOLDER where the output for condition entropies will be store.

Script as well as ConditionalEntropy.scala (class to calculate Conditional Entropy /Code_hadoop_pcap/entropy-utils) were compiled with sbt compile, the parameters and dependencies are defined in built.sbt located in there main folder, ../entropy-utils and ../script respectively.

Here the code inside ../script/built.sbt

```sh
scalaVersion := "2.11.11"

libraryDependencies ++= Seq("org.apache.spark" % "spark-core_2.10" %% "2.2.0", "org.apache.hadoop" % "hadoop-common" % "2.6.5", "org.apache.hadoop" % "hadoop-mapreduce-client-core" % "2.6.5", "net.ripe.hadoop.pcap" % "hadoop-pcap-lib" % "1.1" from "file:$PATH_TO_FILE/hadoop-pcap-lib-1.1.jar", "org.example.DnsInputFormat" % "dns-input-format" % "1.0-SNAPSHOT" from "file:$PATH_TO_FILE/dns-input-format-1.0-SNAPSHOT.jar", "dnsjava" % "dnsjava" % "2.1.8", "org.tb.entropy" %% "entropy-utils" % "1.0.0", "org.apache.spark" % "spark-streaming_2.10" % "2.2.0" % "provided", "org.apache.spark" % "spark-sql_2.10" % "2.2.0", "org.apache.spark" % "spark-mllib_2.10" % "2.2.0" % "provided")
 
```
Those paths to jars have to be change, with respect to the path used locally in your machine if you want to compile the code again due to modification in script.scala, after saving the changes in build.sbt, we have to compile and publish localy:

```sh
sbt compile
sbt
>publish-local

```
The same procedure apply for ConditionalEntropy class, build.sbt for this project is located in ../entropy-utils, here an the configuration for built.sbt

```sh
import Dependencies._

libraryDependencies += "org.apache.spark" % "spark-core_2.10" % "2.2.0"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "org.tb.entropy",
      scalaVersion := "2.11.11",
      version      := "1.0.0"
    )),
    name := "entropy-utils",
    libraryDependencies += scalaTest % Test
  )

```

### Usage

```sh
bin/spark-submit --jars $PATH_TO_FILE/hadoop-pcap-lib-1.2-SNAPSHOT.jar,$PATH_TO_FILE/dns-input-format-1.0-SNAPSHOT.jar,$PATH_TO_FILE/dnsjava-2.1.8.jar,$PATH_TO_FILE/entropy-utils_2.11-1.0.0.jar --class Script --name test --master local $PATH_TO_FILE/script_2.11-0.1-SNAPSHOT.jar $PATH_TO_PCAP_FILE 2 $PATH_TO_OUTPUT_FILE

```

