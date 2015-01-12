/**
 * Copyright 2012-2014 - SFR (http://www.sfr.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.sfr.tv.messaging.client.impl;

import net.sfr.tv.messaging.impl.MessagingProvidersConfiguration;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.sfr.tv.exceptions.ResourceInitializerException;
import net.sfr.tv.hornetq.client.impl.HornetQClientImpl;
import net.sfr.tv.hornetq.client.impl.LoggerMessageHandler;
import net.sfr.tv.jms.client.JmsClientImpl;
import net.sfr.tv.jms.client.MultiListenersJmsClientImpl;
import net.sfr.tv.messaging.client.api.MessagingClient;
import net.sfr.tv.jms.client.impl.LifecycleControllerImpl;
import net.sfr.tv.jms.client.impl.listener.LoggerMessageListener;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Boostrap facility class :
 * <ul>
 * <li> Parse CLI arguments
 * <li> Load configuration
 * <li> Log runtime plafform environement
 * <li> Follow up initialization to main class
 * <li> Registering SIGTERM shutdown hook.
 * </ul>
 *
 * @author matthieu.chaplin@sfr.com
 * @author pierre.cheynier@sfr.com
 */
public class Bootstrap {

    private static final List<String> systemProperties = new ArrayList<>();
    
    private static final String VERSION = "1.2.6";

    static {
        systemProperties.add("java.vm.version");
        systemProperties.add("java.runtime.version");
        systemProperties.add("java.home");
        systemProperties.add("java.class.path");
        systemProperties.add("user.dir");
        systemProperties.add("java.io.tmpdir");
        systemProperties.add("file.encoding");
    }

    /**
     * Bootstrap method
     *
     * @param args
     */
    public static void main(String[] args) {
        try {

            String jndiCnxFactory = "PreAckConsumerConnectionFactory";

            /* Get a configuration path property, consider configuration is in the binary directory instead */
            String configurationPath = System.getProperty("config.path", "/");
            
            System.out.println("Hello, this is the Jalam Messaging Client, v" + VERSION);
            System.out.println("Loading configuration from " + configurationPath + " ...");

            /* Instantiate Logger */
            try {
                Properties logProps = new Properties();
                String log4jfile = configurationPath.concat("/").concat("log4j.properties");
                InputStream is = Bootstrap.class.getResourceAsStream(log4jfile);
                if (is != null) {
                    logProps.load(is);
                } else {
                    logProps.load(new FileInputStream(log4jfile));
                }
                PropertyConfigurator.configure(logProps);
            } catch (FileNotFoundException ex) {
                System.out.println("Log4J configuration not found, it's now the wrapper responsability to configure logging !");
            }
            
            Logger logger = Logger.getLogger(Bootstrap.class);
            logger.debug("Logging initialized");

            
            Boolean jmsMode = Boolean.TRUE;
            /* Retrieve and test consistency of arguments */
            String destination = null;
            String clientId = null;
            String subscriptionName = null;
            String preferredServer = null;
            String selector = "";
            Boolean isTopicSubscription = Boolean.TRUE;
            Boolean isDurableSubscription = Boolean.FALSE;
            Boolean unsubscribeAndExit = Boolean.FALSE;
            for (int i = 0; i < args.length; i++) {
                switch (CliArgs.fromString(args[i])) {
                    case MODE_HQCORE:
                        jmsMode = Boolean.FALSE;
                        break;
                    case DESTINATION:
                        destination = args[++i];
                        break;
                    case CLIENTID:
                        clientId = args[++i];
                        break;
                    case SUBSCRIPTIONNAME:
                        subscriptionName = args[++i];
                        break;
                    case QUEUE:
                        isTopicSubscription = Boolean.FALSE;
                        break;
                    case DURABLE:
                        isDurableSubscription = Boolean.TRUE;
                        break;
                    case UNSUBSCRIBE:
                        unsubscribeAndExit = Boolean.TRUE;
                        break;
                    case CONNECTION_FACTORY:
                        jndiCnxFactory = args[++i];
                        break;
                    case PREFERRED_SERVER:
                        preferredServer = args[++i];
                    case FILTER:
                        while (++i < args.length) {
                            selector += args[i].concat(" ");
                        }
                        break;
                    default:
                        break;
                }
            }
            if ((configurationPath == null && destination == null) || subscriptionName == null) {
                logger.info("Usage : ");
                logger.info("\tjava (-Dconfig.path=your.configPath) (-Dhandler.class=your.lifeCycleController) -jar jalam.jar -d [destination] (-c [clientId]) -s [subscriptionName] (-q) (-p)  (-cf[connectionFactoryName]) <-f [filter]>\n");
                logger.info("\t -d  : Destination JNDI name. Mandatory.");
                logger.info("\t -c  : JMS ClientID.");
                logger.info("\t -s  : JMS subscription name.");
                logger.info("\t -f  : JMS selector.");
                logger.info("\t -q  : Destination is a queue. Topic is default.");
                logger.info("\t -p  : Create a persistent ('durable') subscription. Default is false.");
                logger.info("\t -u  : Unsubscribe an active durable subscription, then exit.");
                logger.info("\t -t  : Target server alias. Otherwise randomly connects to one of the configured servers.");
                logger.info("\t -cf : JNDI Connection Factory name. Default 'ConsumerConnectionFactory'.");
                logger.info("\n");
                logger.info("Examples : ");
                logger.info("\t Persistent topic subscription, with clientID set : java -jar jalam.jar -d /topic/1 -p -s mySubscriptionIdentifier -c myClientId");
                logger.info("\t Unsubscribe then exit : java -jar jalam.jar -u -s mySubscriptionIdentifier -c myClientId");
                logger.info("\t Subscribe to multiple destinations : java -jar jalam.jar -d /topic/1,/topic/2,/topic/3 -s mySubscriptionIdentifier");
                System.exit(1);
            }
            if (clientId == null || clientId.trim().equals("")) {
                // If not specified, a default clientId will be jalam-X.Y@hostname (reverse lookup)
                clientId = "jalam-".concat(VERSION).concat("@").concat(InetAddress.getLocalHost().getHostName().replaceAll("\\.", "-"));
            }

            /* Log Runtime Data */
            Properties system = System.getProperties();
            logger.info("********** System Properties **********\t");
            if (system != null && !system.isEmpty()) {
                for (String prop : systemProperties) {
                    if (prop != null) {
                        logger.info("\t".concat(prop).concat(" : ").concat(system.get(prop) != null ? system.get(prop).toString() : ""));
                    }
                }
            }
            logger.info("***************************************\t");

            // TODO : File shall now be named 'messaging.properties'
            /* Get JMS Properties to get lists of JMS servers */
            Properties jmsProps = new Properties();
            try {
                InputStream is = Bootstrap.class.getResourceAsStream(configurationPath.concat("/").concat("messaging.properties"));
                if (is != null) {
                    jmsProps.load(is);
                } else {
                    jmsProps.load(new FileInputStream(configurationPath.concat("/").concat("messaging.properties")));
                }
            } catch (FileNotFoundException ex) {
                logger.fatal("Unable to find jms.properties configuration file, please read the doc !");
                System.exit(1);
            }

            final MessagingProvidersConfiguration messagingProvidersConfig = new MessagingProvidersConfiguration(jmsProps, null);
            
            Class lifecycleControllerClass = null;
            try {
                lifecycleControllerClass = System.getProperty("handler.class") != null ? ClassLoader.getSystemClassLoader().loadClass(System.getProperty("handler.class")) : LifecycleControllerImpl.class;
            } catch (ClassNotFoundException ex) {
                logger.fatal("Class not found ! : ".concat(System.getProperty("handler.class")));
                System.exit(1);
            }
            
            final MessagingClient client;
            
            if (!jmsMode) {
                // HORNETQ CORE CLIENT PROTOCOL
                client = new HornetQClientImpl(messagingProvidersConfig, preferredServer, subscriptionName, selector, lifecycleControllerClass, LoggerMessageHandler.class, destination.split("\\,"));
            } else {
                // JMS API CLIENT
                if (jmsProps.containsKey("config.listeners")) {
                    // COMPLEX LISTENERS/DESTINATIONS DEFINED IN jms.properties
                    Map<String[], String> destinationsByListeners = new HashMap<>();
                    String listenerClassNames = jmsProps.getProperty("config.listeners");
                    for (String listenerClass : Arrays.asList(listenerClassNames.split("\\,"))) {
                        String destinationsString = jmsProps.getProperty(listenerClass.concat(".destinations"), null);
                        destinationsByListeners.put(destinationsString.split("\\,"), listenerClass);
                    }

                    client = new MultiListenersJmsClientImpl(
                            messagingProvidersConfig,
                            preferredServer,
                            isTopicSubscription,
                            isDurableSubscription,
                            clientId,
                            subscriptionName,
                            selector,
                            lifecycleControllerClass,
                            destinationsByListeners,
                            jndiCnxFactory);

                } else {

                    Class listenerClass = null;
                    try {
                        listenerClass = System.getProperty("listener.class") != null ? ClassLoader.getSystemClassLoader().loadClass(System.getProperty("listener.class")) : LoggerMessageListener.class;
                    } catch (ClassNotFoundException ex) {
                        logger.fatal("Class not found ! : ".concat(System.getProperty("handler.class")));
                        System.exit(1);
                    }

                    client = new JmsClientImpl(
                            messagingProvidersConfig,
                            preferredServer,
                            isTopicSubscription,
                            isDurableSubscription,
                            clientId,
                            subscriptionName,
                            selector,
                            lifecycleControllerClass,
                            listenerClass,
                            destination.split("\\,"),
                            jndiCnxFactory);
                }
            }
            
            /* BOOTSTRAP */
            final RunnableMessagingClient runnable = new RunnableMessagingClient(client);
            
            if (unsubscribeAndExit) {
                runnable.shutdown();
                System.exit(0);
            }
            Thread clientThread = new Thread(runnable);
            clientThread.start();

            /* Register a Shutdown Hook (handle SIGTERM, Ctrl+C, ..) */
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    synchronized (runnable.monitor) {
                        runnable.monitor.notify();
                    }
                    runnable.shutdown();
                }
            });

        } catch (NumberFormatException | IOException | ResourceInitializerException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }
}