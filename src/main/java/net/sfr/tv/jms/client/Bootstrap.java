/**
 * Copyright 2012,2013 - SFR (http://www.sfr.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.sfr.tv.jms.client;

import net.sfr.tv.jms.client.context.JndiServerDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import net.sfr.tv.exceptions.ResourceInitializerException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Boostrap facility class :
 * <ul>
 *  <li> Parse CLI arguments
 *  <li> Load configuration
 *  <li> Log runtime plafform environement
 *  <li> Follow up initialization to main class
 *  <li> Registering SIGTERM shutdown hook.
 * </ul>
 * 
 * @author matthieu.chaplin@sfr.com.chaplin@sfr.com
 */
public class Bootstrap {

    private static final String VERSION = "1.0.0";
    private static final List<String> systemProperties = new ArrayList<String>();

    static {
        //systemProperties.add("sun.java.command");
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

            Properties props = new Properties();
            // TRY TO LOAD LOG4J PROPERTIES. (OTHERWISE, IT WILL BE THE RESPONSABILITY OF THE TARGET WRAPPER)
            try {
                //InputStream is = Bootstrap.class.getResourceAsStream("/log4j.properties");
                InputStream is = Bootstrap.class.getResourceAsStream("log4j.properties");
                if (is != null) {
                    props.load(is);
                } else {
                    props.load(new FileInputStream("log4j.properties"));
                }
                PropertyConfigurator.configure(props);
                
            } catch (FileNotFoundException ex) {
                System.out.println("Log4J configuration not found, it's now the wrapper responsability to configure logging !");
            }
            
            Logger LOGGER = Logger.getLogger(Bootstrap.class);
            LOGGER.info("Hello, this is the Jalam JMS Client, v".concat(VERSION));

            String destination = null;
            String clientId = null;
            String subscriptionName = null;
            String selector = "";
            String jndiConnectionFactory = "ConsumerConnectionFactory";
            Boolean isTopicSubscription = Boolean.TRUE;
            Boolean isDurableSubscription = Boolean.FALSE;
            Boolean unsubscribeAndExit = Boolean.FALSE;
            

            for (int i = 0; i < args.length; i++) {

                switch (CliArgs.fromString(args[i])) {
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
                        jndiConnectionFactory = args[++i];
                        break;
                    case FILTER:
                        while (++i < args.length) {
                            selector += args[i].concat(" ");
                        }
                        break;
                    default:
                        break;
                }
            }

            String handlerClassName = System.getProperty("handler.class", "net.sfr.tv.jms.client.listener.LoggerMessageListener");

            // CHECK FOR ARGUMENTS CONSISTENCY
            if (destination == null || subscriptionName == null) {
                LOGGER.info("Usage : ");
                LOGGER.info("\tjava (-Dhandler.class=your.listener) -jar jalam.jar -d [destination] (-c [clientId]) -s [subscriptionName] (-q) (-p)  (-cf[connectionFactoryName]) <-f [filter]>\n");
                LOGGER.info("\t -q  : Destination is a queue. Topic is default.");
                LOGGER.info("\t -p  : Create a persistent ('durable') subscription. Default is false.");
                LOGGER.info("\t -u  : Unsubscribe an active durable subscription, then exit.");
                LOGGER.info("\t -cf : JNDI Connection Factory name. Default 'ConsumerConnectionFactory'.");
                LOGGER.info("\n");
                LOGGER.info("Examples : ");
                LOGGER.info("\t Persistent topic subscription, with clientID set : java -jar jalam.jar -d /topic/1 -p -s mySubscriptionIdentifier -c myClientId");
                LOGGER.info("\t Unsubscribe then exit : java -jar jalam.jar -u -s mySubscriptionIdentifier -c myClientId");
                LOGGER.info("\t Subscribe to multiple destinations : java -jar jalam.jar -d /topic/1,/topic/2,/topic/3 -s mySubscriptionIdentifier");
                System.exit(1);
            }
            
            if (clientId == null || clientId.trim().equals("")) {
                clientId = "jms-client@".concat(InetAddress.getLocalHost().getHostName().replaceAll("\\.", "-"));//.concat("-").concat(String.valueOf(new Date().getTime())).replaceAll("\\.", "_"));
            }

            // PARSE jms.properties FILE TO GET A LIST OF KNOWN SERVERS                        
            props = new Properties();
            try {
                InputStream is = Bootstrap.class.getResourceAsStream("/jms.properties");
                if (is != null) {
                    props.load(is);
                } else {
                    props.load(new FileInputStream("jms.properties"));
                }
            } catch (FileNotFoundException ex) {
                LOGGER.fatal("Unable to find jms.properties server configuration file, read the doc !");
                System.exit(1);
            }
            
            String[] groups = props.getProperty("config.groups", "").split("\\,");

            String serverAlias;
            Set<String> keys = props.stringPropertyNames();
            //int pos;
            String prefix;
            JndiServerDescriptor server;

            Map<String, Set<JndiServerDescriptor>> servers = new HashMap<String, Set<JndiServerDescriptor>>();
            for (String group : groups) {
                LOGGER.debug("Group : " + group);
                prefix = group.concat(!group.trim().equals("") ? "." : "");
                group = !group.equals("") ? group : "default";
                LOGGER.debug("Prefix : " + prefix + " , Group : " + group);
                for (String key : keys) {
                    if (key.startsWith(group)) {
                        serverAlias = key.startsWith("jms.server") ? key.split("\\.")[2] : key.split("\\.")[3];
                        LOGGER.debug("Server alias : " + serverAlias);

                        if (servers.get(group) == null) {
                            servers.put(group, new HashSet<JndiServerDescriptor>());
                        }
                        server = new JndiServerDescriptor(
                            props.getProperty(prefix.concat("jms.server.").concat(serverAlias).concat(".host")),
                            Integer.valueOf(props.getProperty(prefix.concat("jms.server.").concat(serverAlias).concat(".port"))));
                                
                        boolean duplicate = servers.get(group).add(server);
                        LOGGER.info("Group ".concat(group).concat(duplicate ? ". Added server : " : ". Skipped duplicate : ").concat(server.getProviderUrl()).concat(" "));
                    }
                }
            }

            String login = props.getProperty("jms.login", "guest");
            String password = props.getProperty("jms.password", "guest");
            
            // LOG RUNTIME DATA
            Properties system = System.getProperties();
            if (system != null && !system.isEmpty()) {
                for (String prop : systemProperties) {
                    if (prop != null) { // CA FAIT UN PEU BCP, MAIS BON..
                        LOGGER.info("\t".concat(prop).concat(" : ").concat(system.get(prop) != null ? system.get(prop).toString() : ""));
                    }
                }
            }

            if (unsubscribeAndExit) {
                final JmsClient client = new JmsClient(servers, destination, isTopicSubscription, Boolean.FALSE, clientId, subscriptionName, selector, handlerClassName, jndiConnectionFactory, login, password);
                client.shutdown();
                System.exit(0);
            }
            
            // BOOTSTRAP
            final JmsClient client = new JmsClient(servers, destination, isTopicSubscription, isDurableSubscription, clientId, subscriptionName, selector, handlerClassName, jndiConnectionFactory, login, password);
            Thread clientThread = new Thread(client);
            clientThread.start();
            
            // REGISTER SHUTDOWN HOOK
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() { 
                    synchronized(client.monitor) {
                        client.monitor.notify();
                    };
                    client.shutdown(); 
                }
            });

        } catch (NumberFormatException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);            
        } catch (ResourceInitializerException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);            
        }
    }
}