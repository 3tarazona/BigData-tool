# Spark PCAP Reader

Script to read PCAP files from a file system, an pull information out of the packets, which is used to get statistics with Spark.

# Pre-requirements

  - Java jdk 1.8.x
  - Jnetpcap 1.4.x 
  - Scala lastest version
  - Spark 2.0.x	
  - Hadoop 2.x.x

# Installation
### Java JDK

#### Step 0: Check if JDK has already been Installed
Open a Terminal and issue this command:
```sh
$ javac -version
```
If a JDK version number (e.g., "javac 1.x.x_xx") appears, JDK has already been installed. You can skip the installation and goto step 2.

To remove OpenJDK, issue command:
```sh
$ sudo apt-get purge openjdk-\*
```
#### Step 1: Download and Install JDK

We will install JDK under "/usr/local/java" (or Ubuntu's default JDK directory /usr/lib/jvm; or /opt/java). First, create a directory "java" under "/usr/local". Open a Terminal:
```sh
$ cd /usr/local
$ sudo mkdir java
```

Extract the downloaded package (Check your downloaded filename!)
```sh
$ cd /usr/local/java
$ sudo tar xzvf ~/Downloads/jdk-8u{xx}-linux-x64.tar.gz
```
JDK will be extracted in a folder "/usr/local/java/jdk1.8.0_xx"

Inform the Ubuntu to use this JDK/JRE:
```sh
// Setup the location of java, javac and javaws
$ sudo update-alternatives --install "/usr/bin/java" "java" "/usr/local/java/jdk1.8.0_{xx}/jre/bin/java" 1
      // --install symlink name path priority
$ sudo update-alternatives --install "/usr/bin/javac" "javac" "/usr/local/java/jdk1.8.0_{xx}/bin/javac" 1
$ sudo update-alternatives --install "/usr/bin/javaws" "javaws" "/usr/local/java/jdk1.8.0_{xx}/jre/bin/javaws" 1
 
// Use this Oracle JDK/JRE as the default
$ sudo update-alternatives --set java /usr/local/java/jdk1.8.0_{xx}/jre/bin/java
      // --set name path
$ sudo update-alternatives --set javac /usr/local/java/jdk1.8.0_{xx}/bin/javac
$ sudo update-alternatives --set javaws /usr/local/java/jdk1.8.0_{xx}/jre/bin/javaws
```
The above steps set up symlinks java, javac, javaws at /usr/bin (which is in the PATH), that link to /etc/alternatives and then to JDK bin directory.
The "alternatives" system aims to resolve the situation where several programs fulfilling the same function (e.g., different version of JDKs). It sets up symlinks thru /etc/alternatives to refer to the actual programs to be used.
```sh
$ ls -ld /usr/bin/java*
```
```sh
$ ls -ld /etc/alternatives/java*
```

Alternatively, you can include the JDK's bin and JRE's bin into the PATH directly.

To verify the JDK installation, issue these commands:
```sh
$ javac -version
javac 1.8.0_xx
```
```sh
$ java -version
java version "1.8.0_xx"
Java(TM) SE Runtime Environment (build 1.8.0_xx-xxx)
Java HotSpot(TM) 64-Bit Server VM (build 25.xx-xxx, mixed mode)
```
 
Show the location of javac and java
```sh
$ which javac
/usr/bin/javac
$ which java
/usr/bin/java
```
### Jnetpcap
Use jnetpcap-1.4.r1425 version to run this script, located in the root repository, this file is already compiled.

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

Download it from https://spark.apache.org/downloads.html
Untar the file:
```sh
$ tar -xf  spark-2.2.0-bin-hadoop2.7.tgz
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

    $.sudo ./spark-shell --jars /root/.m2/repository/net/ripe/hadoop/pcap/hadoop-pcap-lib/1.2-SNAPSHOT/hadoop-pcap-lib-1.2-SNAPSHOT.jar,/home/etarazona/tmp1/em/dns-input-format/target/dns-input-format-1.0-SNAPSHOT.jar,/home/etarazona/Downloads/dnsjava-2.1.8.jar

    scala>

Create .jar file of the script:

    $jar cf jar-file input-file(s)

Run the jar file in Spark-shell:

    scala> 


