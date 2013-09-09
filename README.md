===============================================================================
    jalam
===============================================================================

A J2SE JMS client.

Features : 
===============================================================================

 - Failover : Automatic reconnection.
 - Cascading failover between servers if available.
 - Parallel connections to multiple servers
 - Parallel subscription to multiple topics/queues
 - Includes default listeners : STDOUT, journaling
 - Extensible : Implements custom listener, or more complex interface, to setup/release
    additionnal resources. (I/O, network, connection pools)

Building from sources :
===============================================================================

git clone git://github.com/TODO

mvn package

Binary release is available under target/TODO

Usage :
===============================================================================

TODO
