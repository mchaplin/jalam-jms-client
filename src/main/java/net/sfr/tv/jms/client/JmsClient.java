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
import net.sfr.tv.jms.cnxmgt.AbstractConnectionManager;
import net.sfr.tv.jms.client.api.MessageListenerWrapper;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import javax.jms.JMSException;
import net.sfr.tv.exceptions.ResourceInitializerException;
import net.sfr.tv.jms.client.api.LifecycleControllerInterface;
import net.sfr.tv.jms.cnxmgt.InboundConnectionManager;
import org.apache.log4j.Logger;

/**
 * Main client class. 
 * Implements the monitor pattern, run/shutdown lifecycle methods.
 *
 * @author matthieu.chaplin@sfr.com
 * @author scott.messner.prestataire@sfr.com
 * @author pierre.cheynier@sfr.com
 */
public class JmsClient implements Runnable {

   private static Logger LOGGER = Logger.getLogger(JmsClient.class);

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
    * @param destinations JMS destinations JNDI name
    * @param isTopicSubscription Topic subscription flag
    * @param isDurableSubscription Durable subscription flag
    * @param clientId JMS client ID
    * @param subscriptionBaseName JMS subscription name prefix
    * @param selector JMS selector
    * @param lifecycleControllerClassName name of the LifecycleController class
    * @param listenerClassNames Collection of names for the messageListener class
    * @param cnxFactoryJndiName JMS connection factory JNDI name
    */
   public JmsClient(
           JndiProviderConfiguration jndiProviderConfig,
           String preferredServer,
           Collection<String> destinations,
           Boolean isTopicSubscription,
           Boolean isDurableSubscription,
           String clientId,
           String subscriptionBaseName,
           String selector,
           String lifecycleControllerClassName,
           Collection<String> listenerClassNames, 
           String cnxFactoryJndiName) throws ResourceInitializerException {
      /* Instantiate the 2 main components */
      instantiate(lifecycleControllerClassName, DefaultLifecycleController.class, lifecycleController);
      for (String listenerClassName : listenerClassNames) {
         MessageListenerWrapper mlInstance = null;
         instantiate(listenerClassName, MessageListenerWrapper.class, mlInstance); 
         listenerClasses.add(mlInstance);
      }
      
      if (subscriptionBaseName == null) {
         subscriptionBaseName = "";
      }
      if (LOGGER.isInfoEnabled()) {
         LOGGER.info("Destinations : ".concat(Arrays.toString(destinations.toArray())));
         LOGGER.info("ClientID : ".concat(clientId));
         LOGGER.info("Subscription base name : ".concat(subscriptionBaseName));
         LOGGER.info("Filter : ".concat(selector != null ? selector : ""));
         LOGGER.info("Servers groups : ".concat(String.valueOf(jndiProviderConfig.getGroups().size())));
      }

      cnxManagers = new TreeMap<String, AbstractConnectionManager>();
      for (String group : jndiProviderConfig.getGroups()) {
         InboundConnectionManager cnxManager = new InboundConnectionManager(
                group, 
                jndiProviderConfig.getServersGroup(group), 
                preferredServer, 
                clientId, 
                cnxFactoryJndiName, 
                jndiProviderConfig.getCredentials(), 
                 // TODO : to suppor multiple listeners, instantiate N InboundConnectionManagers ? 
                listenerClasses.iterator().next()
         );
         cnxManager.connect(2);
         int consumerIdx = 0;
         String subscriptionName;
         for (String sdst : destinations) {
            subscriptionName = subscriptionBaseName.concat(destinations.size() > 1 ? "-" + ++consumerIdx : "");
            cnxManager.subscribe(sdst, isTopicSubscription, isDurableSubscription, subscriptionName, selector);
         }
         cnxManagers.put(group, cnxManager);
      }

      LOGGER.info("Durable subscription ? ".concat(String.valueOf(isDurableSubscription)));
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
    *
    * @see net.sfr.tv.jms.client.wrapper.ListenerWrapper
    *
    * @param className The name of the class to load and use. 
    * @param assignableRefClass A class assignable from the supertype of className
    * @param superclass A superclass to map to
    * @param obj the object in which an instance should be pushed 
    * 
    * @throws ResourceInitializerException
    */
   private void instantiate(String className, Class assignableRefClass, Object obj) throws ResourceInitializerException {

      LOGGER.info("Instantiating : ".concat(className));
      try {
         ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
         Class myClass = systemClassLoader.loadClass(className);
         if (assignableRefClass.isAssignableFrom(myClass)) {
            Constructor ct = myClass.getConstructor();
            obj = ct.newInstance();
         }
         else {
             throw new ClassNotFoundException(className);
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
