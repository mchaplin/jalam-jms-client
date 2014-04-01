/**
 * Copyright 2012-2014 - SFR (http://www.sfr.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.sfr.tv.jms.client;

import net.sfr.tv.jms.model.JndiProviderConfiguration;
import net.sfr.tv.jms.cnxmgt.AbstractConnectionManager;
import net.sfr.tv.jms.client.api.MessageListenerWrapper;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.jms.JMSException;
import net.sfr.tv.exceptions.ResourceInitializerException;
import net.sfr.tv.jms.client.api.LifecycleControllerInterface;
import net.sfr.tv.jms.cnxmgt.InboundConnectionManager;
import org.apache.log4j.Logger;

/**
 * Main client class. Implements the monitor pattern, run/shutdown lifecycle
 * methods.
 *
 * @author matthieu.chaplin@sfr.com
 * @author scott.messner.prestataire@sfr.com
 * @author pierre.cheynier@sfr.com
 */
public class JmsClient implements Runnable {

   private static final Logger LOGGER = Logger.getLogger(JmsClient.class);

   /**
    * Monitor pattern
    */
   public final Object monitor = new Object();

   /**
    * Stateful JMS connection managers : Handles connection/failover for a
    * logical group of JMS servers.
    */
   private Map<String, AbstractConnectionManager> cnxManagers;

   /**
    * JMS listener class
    */
   private Collection<MessageListenerWrapper> listenerClasses = new ArrayList<>();

   /**
    * Listener wrapper class (alternate to using a listener class
    */
   private LifecycleControllerInterface lifecycleController;

   /**
    *
    * Constructor.
    *
    * @param jndiProviderConfig References available JNDI servers & associated
    * credentials.
    * @param preferredServer Preferred server alias.
    * @param isTopicSubscription Topic subscription flag
    * @param isDurableSubscription Durable subscription flag
    * @param clientId JMS client ID
    * @param subscriptionBaseName JMS subscription name prefix
    * @param selector JMS selector
    * @param lifecycleControllerClassName name of the LifecycleController class
    * @param listenerDestinationsMap Map of listener class names with their
    * associated destinations
    * @param cnxFactoryJndiName JMS connection factory JNDI name
    * @throws net.sfr.tv.exceptions.ResourceInitializerException
    */
   public JmsClient(
           JndiProviderConfiguration jndiProviderConfig,
           String preferredServer,
           Boolean isTopicSubscription,
           Boolean isDurableSubscription,
           String clientId,
           String subscriptionBaseName,
           String selector,
           String lifecycleControllerClassName,
           Map<String, String[]> listenerDestinationsMap,
           String cnxFactoryJndiName) throws ResourceInitializerException {
      // LifecycleController and listeners
      instantiateLifecycleController(lifecycleControllerClassName, listenerDestinationsMap.keySet());

      // Connect and Subscribe listeners to destinations
      cnxManagers = new TreeMap<>();
      int consumerIdx = 0;
      for (String group : jndiProviderConfig.getGroups()) {
         try {
            for (MessageListenerWrapper listener : lifecycleController.getListeners()) {
               String clientUid = clientId.concat("/" + consumerIdx++);
               InboundConnectionManager cnxManager = new InboundConnectionManager(group, jndiProviderConfig.getServersGroup(group), preferredServer, clientUid, cnxFactoryJndiName, jndiProviderConfig.getCredentials(), listener);
               cnxManager.connect(2);
               LOGGER.info("Connection created for ".concat(listener.getName()));

               String subscriptionName;
               int subscriptionIdx = 0;
               for (String dest : listenerDestinationsMap.get(listener.getClass().getCanonicalName())) {
                  subscriptionName = subscriptionBaseName.concat("-".concat(dest + "-" + subscriptionIdx++));
                  cnxManager.subscribe(dest, isTopicSubscription, isDurableSubscription, subscriptionName, selector);
                  if (LOGGER.isInfoEnabled() || LOGGER.isDebugEnabled()) {
                     LOGGER.info("Destination : ".concat(dest));
                     LOGGER.info("ClientID : ".concat(clientId));
                     LOGGER.info("Subscription base name : ".concat(subscriptionBaseName));
                     LOGGER.info("Durable subscription ? ".concat(String.valueOf(isDurableSubscription)));
                     LOGGER.info("Filter : ".concat(selector != null ? selector : ""));
                     LOGGER.info("Servers groups : ".concat(String.valueOf(jndiProviderConfig.getGroups().size())));
                  }
               }
               cnxManagers.put(clientUid, cnxManager);
            }
         } catch (Exception ex) {
            LOGGER.error("Unable to start a listener/context binded to : ".concat(group), ex);
         }
      }

      lifecycleController.run();

      // START MESSAGE DELIVERY
      try {
         for (AbstractConnectionManager cnxManager : cnxManagers.values()) {
            cnxManager.start();
         }
      } catch (JMSException ex) {
         LOGGER.fatal("Unable to start JMS connection !", ex);
         System.exit(1);
      }
   }

   /**
    * Instantiate the LifecycleControllerInterface with the specified classname
    * and the given listeners
    *
    * @see net.sfr.tv.jms.client.wrapper.ListenerWrapper
    *
    * @param lifeCycleControllerClassName The name of the LifecycleController
    * class to load and
    * @param listenerClassNames Comma-separated list of listener class names.
    * use.
    * @return the new instance of the LifecycleControllerInterface
    * @throws ResourceInitializerException
    */
   private void instantiateLifecycleController(String lifecycleControllerClassName, Set<String> listenerClassNames) throws ResourceInitializerException {
      ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

      try {
         Class lcClass = systemClassLoader.loadClass(lifecycleControllerClassName);
         if (LifecycleControllerInterface.class.isAssignableFrom(lcClass)) {
            Constructor ct = lcClass.getConstructor();
            lifecycleController = (LifecycleControllerInterface) ct.newInstance();
            LOGGER.debug("Using the specified LifecycleControllerInterface: ".concat(lcClass.getName()));
            registerListeners(listenerClassNames);
         } else {
            throw new ClassNotFoundException(lifecycleControllerClassName);
         }
      } catch (ClassNotFoundException ex) {
         throw new ResourceInitializerException(ex);
      } catch (InstantiationException ex) {
         throw new ResourceInitializerException(ex);
      } catch (IllegalAccessException ex) {
         throw new ResourceInitializerException(ex);
      } catch (NoSuchMethodException ex) {
         throw new ResourceInitializerException(ex);
      } catch (InvocationTargetException ex) {
         throw new ResourceInitializerException(ex);
      }
   }

   /**
    * Register the listener classes with the lifecycle controller
    * @param listenerClassNames
    * @throws ResourceInitializerException 
    */
   private void registerListeners(Set<String> listenerClassNames) throws ResourceInitializerException {
      ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
      try {
         // Message Listeners
         for (String listenerClassName : listenerClassNames) {
            Class listenerClass = systemClassLoader.loadClass(listenerClassName);
            lifecycleController.registerListener(listenerClass);
         }
      } catch (ClassNotFoundException e) {
         throw new ResourceInitializerException(e);
      }
   }

   /**
    * Monitor pattern
    */
   @Override
   public void run() {

      // ... forever. The thread is waiting for someone to call notify() on the lock object.
      synchronized (monitor) {
         try {
            monitor.wait();
         } catch (InterruptedException ex) {
            LOGGER.warn("Got interrupted !");
         }
      }
   }

   /**
    * Client shutdown : Sequential closure of JMS and user specific resources.
    *
    * User-specific resources must be released in
    *
    * @see net.sfr.tv.jms.client.wrapper.ListenerWrapper#release()
    */
   public void shutdown() {

      for (AbstractConnectionManager cnxManager : cnxManagers.values()) {
         cnxManager.disconnect();
      }
      if (lifecycleController != null) {
         lifecycleController.release();
      }
      LOGGER.info("Bye !");
   }
}
