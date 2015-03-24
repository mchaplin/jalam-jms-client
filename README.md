Jalam
=====

Production grade J2SE JMS client.

Features : 
==========

 - Dual support of JMS 1.1 and HornetQ native ('core') protocol.
 - Failover : Automatic reconnection.
 - Cascading failover between servers if available.
 - Parallel connections to multiple servers.
 - Parallel subscription to multiple topics/queues.
 - Includes default listeners : STDOUT, journaling.
 - Extensible : Implements custom listener, or more complex interface, to setup/release
    additionnal resources. (I/O, network, connection pools)

Building from sources :
=======================

    git clone https://github.com/sfr-network-service-platforms/jalam.git

    git clone https://github.com/sfr-network-service-platforms/jalam.git
    mvn dependency:copy-dependencies package install

Binary release is available under ${basedir}/target/jalam-<version>.jar
Required runtime libraries are available under ${basedir}/target/dependency

The JAR archive classpath requires the following deployment layout :

    ./jalam-<version>.jar
    ./lib/<runtime-libraries>.jar

Usage :
=======

Upon startup, a list of configured JNDI providers is loaded from a classpath file named 'messaging.properties'.
It allows to define :

 - Logical groups of servers
 - Servers that belongs to those groups.
 
Simple setup (messaging.properties) :
-------------------------------------
 
 The simplest configuration file contains the following :

    # A default group
    config.groups=default

    # A JNDI provider whose alias is 'mybroker'
    default.jms.server.mybroker.host=mybroker.mydomain.com
    default.jms.server.mybroker.port=7676
    default.hqtransport.server.mybroker.port=5465

 You can now run the client :
    java -Dconfig.path=. -jar /usr/local/bin/jalam/jalam-current.jar -d /jms/someTopic -c myClientId -s mySubscription


 Available arguments : 

    (-Dconfig.path=your.configPath) (-Dhandler.class=your.lifeCycleController) -jar jalam.jar -d [destination] (-c [clientId]) -s [subscriptionName] (-q) (-p) (-cf[connectionFactoryName]) <-f [filter]>
 - -Dconfig.path : /path/to/configuration. Configuration includes the file 'messaging.properties' and 'log4j.properties'
 - -q  : Destination is a queue. Default : Topic.
 - -p  : Create a persistent ('durable') subscription. Default is false.
 - -u  : Unsubscribe an active durable subscription, then exit.
 - -cf : JNDI Connection Factory name. Default 'ConsumerConnectionFactory'.
 - -Dhandler.class : Set the LifeCycleController to use. Default is net.sfr.tv.jms.client.DefaultLifeCycleController. If a MessageListener is passed, then the DefaultLifeCycleController use a dedicated one instead of net.sfr.tv.jms.client.listener.LoggerMessageListener

Logging :
---------

Logging preferences are specified in the log4j.properties file (here's an example. See https://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/Level.html for information on the different logging levels)
 
    # Root logger option
    log4j.rootLogger=INFO, stdout, fileout

    # Direct log messages to stdout
    log4j.appender.stdout=org.apache.log4j.ConsoleAppender
    log4j.appender.stdout.Target=System.out
    log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
    log4j.appender.stdout.layout.ConversionPattern=%d{HH:mm:ss} %5p : %m%n
    
    # redirect to file -- format and file location.
    log4j.appender.fileout=org.apache.log4j.FileAppender
    log4j.appender.fileout.File=/tmp/jms-client.log
    log4j.appender.fileout.layout=org.apache.log4j.PatternLayout
    log4j.appender.fileout.layout.ConversionPattern=%d{HH:mm:ss} %5p : %m%n

    # Logging levels for specific modules
    log4j.logger.org.apache=INFO
    log4j.logger.httpclient.wire=INFO
    log4j.logger.org.apache.commons=INFO
    log4j.logger.org.apache.jackrabbit=INFO


Active/active setup, with failover :
------------------------------------

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

Listener/Destination Setup :
----------------------------

To use a listener other than the default, the names of the listener classes and their destinations must be specified in the jms.properties file. Multiple listeners and multiple destinations are specified in comma separated strings:

    # Class names of listeners
    config.listeners=net.sfr.tv.listener.TopicListener1,net.sfr.tv.listener.TopicListener2,net.sfr.tv.listener.TopicListener3
    # Destinations
    net.sfr.tv.listener.TopicListener1.destinations=/topic/1
    net.sfr.tv.listener.TopicListener2.destinations=/topic/1,/topic/2
    net.sfr.tv.listener.TopicListener3.destinations=/topic/3
	
Usage :
=======

    java -cp jalam-<version>.jar net.sfr.tv.jms.client.Bootstrap <arguments>

Available arguments are : 

    (-Dconfig.path=your.configPath) (-Dhandler.class=your.lifeCycleController) -jar jalam.jar -d [destination] (-c [clientId]) -s [subscriptionName] (-q) (-p) (-cf[connectionFactoryName]) <-f [filter]>

 - -q  : Destination is a queue. Topic is default.
 - -p  : Create a persistent ('durable') subscription. Default is false.
 - -u  : Unsubscribe an active durable subscription, then exit.
 - -cf : JNDI Connection Factory name. Default 'ConsumerConnectionFactory'.
 - -Dconfig.path : Set the path of the configuration files. Instead, they should be in the binary directory. 
 - -Dhandler.class : Set the LifeCycleController to use. Default is net.sfr.tv.jms.client.DefaultLifeCycleController. If a MessageListener is passed, then the DefaultLifeCycleController use a dedicated one instead of net.sfr.tv.jms.client.listener.LoggerMessageListener

                
Examples :

Persistent topic subscription, with clientID set :

    java -jar jalam.jar -d /topic/1 -p -s mySubscriptionIdentifier -c myClientId
Unsubscribe then exit :

    java -jar jalam.jar -u -s mySubscriptionIdentifier -c myClientId
Subscribe to multiple destinations :

    java -jar jalam.jar -d /topic/1,/topic/2,/topic/3 -s mySubscriptionIdentifier

Default Listeners :
-----------

By default, Jalam uses a 'Logger Listener' which output messages content to Log4J.

 - A custom logger can be used, by setting the system property 'listener.class' (-Dlistener.class=your.listener)
 - Output can be restricted to only include message body, by setting the system property 'listener.output.type' to 'BODY'

An alternative default listener which outputs to a file is provided :

 - The FileOutMessageListener is provided in the jalam package
  - -Dlistener.class=net.sfr.tv.jms.client.listener.FileOutMessageListener
 - Destination file is set with the system property 'listener.file.output'. (must be an absolute path)
  - -Dlistener.file.output=/home/username/tmp/jalam.log

Refer to the next section to use customs MessageListeners.
	
Extensibility :
===============

Message listeners :
-------------------

To use a custom MessageListener with Jalam, you have to implement the interface net.sfr.tv.jms.client.api.MessageListenerWrapper.
It provides a release() method, which is called upon program termination, and allows to release any specific resource.

Lifecycle provider :
--------------------

In order to handle more complex usage, where you need to initialize and keep tracks of many resources outside of the JMS listeners,
you can implement the net.sfr.tv.jms.client.api.LifecycleControllerInterface, which provides the following methods :

 - run() : Called at startup, and allows to initialize resource such as thread pools, external network connections (JDBC,FTP, etc...)
 - release() : Release previously mentionned resources.
 - getListener()
