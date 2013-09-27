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

git clone https://github.com/sfr-network-service-platforms/jalam.git

mvn dependency:copy-dependencies package install

Binary release is available under ${basedir}/target/jalam-<version>.jar
Required runtime libraries are available under ${basedir}/target/dependency

The JAR archive classpath requires the following deployment layout :

    ./jalam-<version>.jar
    ./lib/<runtime-libraries>.jar

Configuration :
===============================================================================

Upon startup, a list of configured JNDI providers is loaded from a classpath file named 'jms.properties'.
It allows to define :

 - Logical groups of servers
 - Servers that belongs to those groups.
 
Simple setup :
~~~~~~~~~~~~~~
 
 The simplest configuration file contains the following :

# A default group
config.groups=default

# A JNDI provider whose alias is 'broker00'
default.jms.server.broker00.host=broker00.mydomain.com
default.jms.server.broker00.port=7676

Active/active setup, with failover :
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

By defining multiple logical groups, it's possible to create an active/active message consumer :
 - At runtime, a subscription will be established to one server in each group.
 - This allows to balance load between server, for ex. in a cluster.

Defining multiple servers in a group allows for failover capabilities, when/if the active server
becomes unavailable.

config.groups=datacenter1,datacenter2

datacenter1.jms.server.server1.host=server1.datacenter1.organization.com
datacenter1.jms.server.server1.port=7676
datacenter1.jms.server.server2.host=server2.datacenter1.organization.com
datacenter1.jms.server.server2.port=7676

datacenter2.jms.server.server1.host=server1.datacenter2.organization.com
datacenter2.jms.server.server1.port=7676
datacenter2.jms.server.server2.host=server2.datacenter2.organization.com
datacenter2.jms.server.server2.port=7676

	
Usage :
===============================================================================

java -cp jalam-<version>.jar net.sfr.tv.jms.client.Bootstrap <arguments>

    -Dhandler.class=<your.listener.class> -jar jalam.jar -d [destination] (-c [clientId]) -s [subscriptionName] (-q) (-p) (-cf[connectionFactoryName]) <-f [filter]>
    -q  : Destination is a queue. Topic is default.
    -p  : Create a persistent ('durable') subscription. Default is false.
    -u  : Unsubscribe an active durable subscription, then exit.
    -cf : JNDI Connection Factory name. Default 'ConsumerConnectionFactory'.
    -Dhandler.class : Set the MessageListener to use. Default is net.sfr.tv.jms.client.listener.LoggerMessageListener
                
Examples :
    Persistent topic subscription, with clientID set : java -jar jalam.jar -d /topic/1 -p -s mySubscriptionIdentifier -c myClientId
    Unsubscribe then exit : java -jar jalam.jar -u -s mySubscriptionIdentifier -c myClientId
    Subscribe to multiple destinations : java -jar jalam.jar -d /topic/1,/topic/2,/topic/3 -s mySubscriptionIdentifier

Listeners :
~~~~~~~~~~~

By default, Jalam uses a 'Logger Listener' which output messages content to Log4J.
 - A custom logger can be used, by setting the system property 'listener.logger.name'
 - Output can be restricted to only include message body, by setting the system property 'listener.output.type' to 'BODY'

An alternative listener which output to a file is provided :
 - -Dhandler.class=net.sfr.tv.jms.client.listener.FileOutMessageListener
 - Destination file is set with the system property 'listener.file.output'. (Absolute path)

Refer to the next section to use customs MessageListeners.
	
Extensibility :
===============================================================================

Message listeners :
~~~~~~~~~~~~~~~~~~~

To use a custom MessageListener with Jalam, you have to implement the interface net.sfr.tv.jms.client.api.MessageListenerWrapper.
It provides a release() method, which is called upon program termination, and allows to release any specific resource.


Lifecyle provider :
~~~~~~~~~~~~~~~~~~~

In order to handle more complex usage, where you need to initialize and keep tracks of many resources outside of the JMS listeners,
you can implement the net.sfr.tv.jms.client.api.LifecycleControllerInterface, which provides the following methods :

 - run() : Called at startup, and allows to initialize resource such as thread pools, external network connections (JDBC,FTP, etc...)
 - release() : Release previously mentionned resources.
 - getListener()
