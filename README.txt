Mulgara Semantic Store (Mulgara) Installation Guide
-------------------------------------------------

Table of Contents

I. Introduction
    i. Directory Layout
    ii. Release Notes
II. Installing Java
III. Building Mulgara
IV. Running a Mulgara Server
V. Mulgara Server Options
VI. License


I. Introduction
===============

Mulgara Semantic Store is a directed graph daatabase designed to store metadata in
a highly scalable, transaction safe environment.

i. Directory Layout
-------------------

bin      Executables
conf     Build configuration templates (read only)
data     Test data (read only)
dist     Distributable product
doc      Documentation, both sources and generated
lib      Components external to this project (read only)
obj      Generated files
scripts  Scripts used by the build process
src      Source code
test     Test results (auto generated)

ii. Release Notes
-----------------

New Features:
* Create best practises example. Demonstrates an RDF based music player using
Mulgara. SF feature 1088763.
* XML Literal support. SF feature 1077157.
* Improved subquery speed.  SF feature 1081837.
* Increased small query speed. SF feature 1081838.
* Improved XSD Support.  SF feature 1081840.
* Resolvers.  SF feature 945093.
* Query statements based on type.  SF feature 1081836.
* A list of currently distributed resolvers and content handlers.  SF
feature 1081845.

Known Bugs:
* Database.delete does not remove write lock. SF bug 1057988.
* File URI generation is incorrect. SF bug 1088190.
* No Protocol Registry. SF bug 1086274.
* Client side JRDF does not support blank nodes. SF bug 1081738.
* Literals lose language and datatype attribute. SF bug 1081768.
* Query has no type capable of globalized UNBOUND. SF bug 1081811.
* Fix RemoveDuplicates with Unbound. SF bug 1081806.
* Backup and restore don't handle resolvers. SF bug 1081776.
* Does not compile under Java 1.5. SF bug 1081719.
* Lucene exceptions are not serializable over RMI. SF bug 1081718.
* IN clause subsitution is not working correctly. SF bug 1088254.
* MBox resolver leaves behind index files. SF bug 1082540.
* SessionFactoryFactory fails in Tomcat. SF bug 1081817.
* Cannot insert into Lucene a statement with a bNode subject. SF bug
1081773.
* Result of a count should be xsd:nonNegativeInteger. SF bug 1081724.
* Timezone support for dates and times. SF bug 1070718.
* AbstractTuples shouldn't impl getRowCount(). SF bug 1081797.
* Can't compile using JRockit JVM. SF bug 1081808.
* Illegal characters in hostname. SF bug 1086138.
* Exiting in OS X causes system instability. SF bug 1081814.
* jxUnitTests don't exercise FileTuples.beforeFirst(prefix). SF bug
1081813.
* Zipped RDF limited to 2GB. SF bug 1081805.
* HybridTuples memory use requires capping. SF bug 1081803.
* Append and Join unification isolated. SF bug 1081801.
* Answer.getObject(String columnName) is not implemented in
UnconstrainedAnswer, SubqueryAnswer and StreamAnswer. SF bug 1081784.
* UnorderedProjection is occurring on a ordered projection. SF bug
1081777.
* WebUI does not honor RMI server name. SF bug 1081771.
* Resolver Exception not serializable in client. SF bug 1081019.
* HybridTuples/BlockCacheLine prefixing requires unit testing. SF bug
1059178.
* Exception management. SF bug 1048970.
* Move TuplesException to CursorException. SF bug 1081798.
* OrderByRowComparator failing without projection. SF bug 1081786.
* Make HybridTuples memory usage adaptive. SF bug 1081802.
* TuplesOperations should consume input parameters. SF bug 1081800.

For a list of bugs fixed:
http://sourceforge.net/tracker/index.php?atid=591704&group_id=89874&_group=369112&set=custom&_status=2&_assigned_to=0

II. Installing Java
===================

1. Download a J2SE 1.4.X for your platform from http://java.sun.com/j2se/,
and install it. Installation instructions for Windows and Linux are
available. You should then check that the installation added the java
commands to your path by typing:

$ java -version

You should get something like the following:

java version "1.4.2_06"
Java(TM) 2 Runtime Environment, Standard Edition (build 1.4.2_06-b03)
Java HotSpot(TM) Client VM (build 1.4.2_06-b03, mixed mode)

If your shell reports that it cannot find the command, add <JAVA_HOME>/bin
(where JAVA_HOME is the location where you installed J2SE to) to your path
in the appropriate way for your shell.

Note. You must use J2SE 1.4.2 or above for compiling and running Mulgara.


III. Building Mulgara
====================

If you have downloaded the binary distribution of Mulgara please skip this
section and go on to "Running a Mulgara Server".

To build Mulgara, you must either use build.sh (for Unix opeating systems) or
build.bat (for Windows).  You must have you JAVA_HOME enviroment variable
set in order for the script to work or modify the script to point to your
current installation of Java.

To build the distribution in Unix:
$ ./build.sh dist

To build the distribution in Windows:
C:\Mulgara\> build dist


IV. Running a Mulgara Server
============================

The Mulgara server is currently run from a shell script under Linux or a batch
file under Windows. To start the server using this script, you'll need to do
the following:

Note. This assumes PATH has been set to the
C:\Program Files\Java\j2re1.4.2\bin directory.

1. Change to the Mulgara directory:
$ cd <mulgarahome>

Note. If the directory does not exist create one and copy the mulgara-1.1.0.jar
and itql-1.1.0.jar into it.

2. Start the executable JAR :

$ cd <mulgarahome>
$ java -jar mulgara-1.1.0.jar

Once you see the following line appear in the console the server is ready to be used.

11:01:47.763 EVENT Started SocketListener on 0.0.0.0:8080

However, if the following message appears then the HTTP port is already occupied by
another process. Please refer to Mulgara Server options to change this
configuration.

2004-04-23 11:20:23,823 ERROR EmbeddedMulgaraServer - java.net.BindException: Address already in use

To verify your installation is working correctly open your browser and enter the following URL

http://localhost:8080

Your HTTP port may be different if you have supplied a -p option.

Follow the links to the user documentation to learn more about using Mulgara.


V. Mulgara Server Options
=========================

You can change the basic Mulgara server options by suppling them as arguments
to the startup command. To view the basic options supply the --help option.

$ java -jar mulgara-1.1.0.jar --help

This will return the following options :

-h, --help          display this help screen
-n, --normi         disable automatic starting of the RMI registry
-x, --shutdown      shutdown the local running server
-l, --logconfig     use an external logging configuration file
-c, --serverconfig  use an external server configuration file
-k, --serverhost    the hostname to bind the server to
-o, --httphost      the hostname for HTTP requests
-p, --port          the port for HTTP requests
-r, --rmiport       the RMI registry port
-s, --servername    the (RMI) name of the server
-a, --path          the path server data will persist to, specifying
                    '.' or 'temp' will use the current working directory
                    or the system temporary directory respectively
-m, --smtp          the SMTP server for email notifications

Since Mulgara has an embedded HTTP server you may have a conflict with an
existing HTTP running on port 8080. For example, to change the HTTP port of
the Mulgara server to 8081

$java -jar mulgara-1.1.0.jar -p 8081

By default the database files are stored in the current directory under a
server1 directory. To change the location of the database files supply the
-a followed by a path. For example :

$java -jar mulgara-1.1.0.jar -a file:///usr/local/mulgara
Under Windows :

$java -jar mulgara-1.1.0.jar -a c:\mulgara-data

VI. License
==========

The Mulgara Semantic Store is licensed under the Open Software License
version 1.1 which is included with the distribution in a file called
LICENSE.txt.

Copyright (c) 2001-2004 Tucana Technologies, Inc. All rights reserved.
Copyright (c) 2005 Kowari Project. All rights reserved.
Copyright (c) 2006 Mulgara Project. Some rights reserved.

Last updated on 1 April 2005

