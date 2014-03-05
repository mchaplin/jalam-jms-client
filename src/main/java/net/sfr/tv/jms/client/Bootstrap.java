/**
 * Copyright 2012-2014 - SFR (http://www.sfr.com/)
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

import net.sfr.tv.jms.model.JndiProviderConfiguration;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import net.sfr.tv.exceptions.ResourceInitializerException;
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
         /* Get the main properties file */
         Properties mainProps = new Properties();
         mainProps.load(Bootstrap.class.getResourceAsStream("project.properties"));
         String version = mainProps.getProperty("version");
         String log4jProperties = mainProps.getProperty("properties.log4j");
         String jmsProperties = mainProps.getProperty("properties.jms");
         String cnxFactoryJndi = mainProps.getProperty("connectionfactory.default");
         String defaultHandler = mainProps.getProperty("handler.default");
         String defaultListener = mainProps.getProperty("listener.default");
         
         /* Get a configuration path property, consider configuration is in the binary directory instead */
         String configurationPath = System.getProperty("config.path", "");
         
         /* Instantiate Logger */
         try {
            Properties logProps = new Properties();
            InputStream is = Bootstrap.class.getResourceAsStream(configurationPath.concat("/").concat(log4jProperties));
            if (is != null) {
               logProps.load(is);
            } else {
               logProps.load(new FileInputStream(configurationPath.concat("/").concat(log4jProperties)));
            }
            PropertyConfigurator.configure(logProps);
         } catch (FileNotFoundException ex) {
            System.out.println("Log4J configuration not found, it's now the wrapper responsability to configure logging !");
         }
         Logger LOGGER = Logger.getLogger(Bootstrap.class);
         LOGGER.info("Hello, this is the Jalam JMS Client, v".concat(version));

         /* Retrieve and test consistency of arguments */
         String destination = null;
         String clientId = null;
         String subscriptionName = null;
         String preferredServer = null;
         String selector = "";
         String jndiConnectionFactory = cnxFactoryJndi;
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
         if (destination == null || subscriptionName == null) {
            LOGGER.info("Usage : ");
            LOGGER.info("\tjava (-Dconfig.path=your.configPath) (-Dhandler.class=your.lifeCycleController) -jar jalam.jar -d [destination] (-c [clientId]) -s [subscriptionName] (-q) (-p)  (-cf[connectionFactoryName]) <-f [filter]>\n");
            LOGGER.info("\t -d  : Destination JNDI name. Mandatory.");
            LOGGER.info("\t -c  : JMS ClientID.");
            LOGGER.info("\t -s  : JMS subscription name.");
            LOGGER.info("\t -f  : JMS selector.");
            LOGGER.info("\t -q  : Destination is a queue. Topic is default.");
            LOGGER.info("\t -p  : Create a persistent ('durable') subscription. Default is false.");
            LOGGER.info("\t -u  : Unsubscribe an active durable subscription, then exit.");
            LOGGER.info("\t -t  : Target server alias. Otherwise randomly connects to one of the configured servers.");
            LOGGER.info("\t -cf : JNDI Connection Factory name. Default 'ConsumerConnectionFactory'.");
            LOGGER.info("\n");
            LOGGER.info("Examples : ");
            LOGGER.info("\t Persistent topic subscription, with clientID set : java -jar jalam.jar -d /topic/1 -p -s mySubscriptionIdentifier -c myClientId");
            LOGGER.info("\t Unsubscribe then exit : java -jar jalam.jar -u -s mySubscriptionIdentifier -c myClientId");
            LOGGER.info("\t Subscribe to multiple destinations : java -jar jalam.jar -d /topic/1,/topic/2,/topic/3 -s mySubscriptionIdentifier");
            System.exit(1);
         }
         if (clientId == null || clientId.trim().equals("")) {
            // If not specified, a default clientId will be jalam-X.Y@hostname (reverse lookup)
            clientId = "jalam-".concat(version).concat("@").concat(InetAddress.getLocalHost().getHostName().replaceAll("\\.", "-"));
         }
         
         /* Log Runtime Data */
         Properties system = System.getProperties();
         LOGGER.info("********** System Properties **********\t");
         if (system != null && !system.isEmpty()) {
            for (String prop : systemProperties) {
               if (prop != null) { 
                  LOGGER.info("\t".concat(prop).concat(" : ").concat(system.get(prop) != null ? system.get(prop).toString() : ""));
               }
            }
         }
         LOGGER.info("***************************************\t");

         /* Get JMS Properties to get lists of JMS servers */                       
         Properties jmsProps = new Properties();
         try {
            InputStream is = Bootstrap.class.getResourceAsStream(configurationPath.concat("/").concat(jmsProperties));
            if (is != null) {
               jmsProps.load(is);
            } else {
               jmsProps.load(new FileInputStream(configurationPath.concat("/").concat(jmsProperties)));
            }
         } catch (FileNotFoundException ex) {
            LOGGER.fatal("Unable to find " + jmsProperties + " server configuration file, please read the doc !");
            System.exit(1);
         }
         JndiProviderConfiguration jndiProviderConfig = new JndiProviderConfiguration(jmsProps, null);

         String lifecycleControllerClassName = System.getProperty("handler.class", defaultHandler);
         
         String listenerClassNames = System.getProperty("listener.class", defaultListener);
         if (jmsProps.containsKey("config.listeners")) {
             listenerClassNames = jmsProps.getProperty("config.listeners");
         }
         
         /* BOOTSTRAP */
         final JmsClient client = new JmsClient(
                 jndiProviderConfig, 
                 preferredServer, 
                 Arrays.asList(destination.split("\\,")), 
                 isTopicSubscription, 
                 (unsubscribeAndExit ? Boolean.FALSE : isDurableSubscription), 
                 clientId, 
                 subscriptionName, 
                 selector, 
                 lifecycleControllerClassName, 
                 Arrays.asList(listenerClassNames.split("\\,")), 
                 jndiConnectionFactory);
         if (unsubscribeAndExit) {
            client.shutdown();
            System.exit(0);
         }
         Thread clientThread = new Thread(client);
         clientThread.start();

         /* Register a Shutdown Hook (handle SIGTERM, Ctrl+C, ..) */
         Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
               synchronized (client.monitor) {
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
