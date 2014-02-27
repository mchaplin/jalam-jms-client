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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import net.sfr.tv.exceptions.ResourceInitializerException;
import net.sfr.tv.jms.client.api.LifecycleControllerInterface;
import net.sfr.tv.jms.client.api.MessageListenerWrapper;
import net.sfr.tv.jms.client.listener.LoggerMessageListener;
import org.apache.log4j.Logger;

/**
 *
 * @author matthieu.chaplin@sfr.com
 */
public class DefaultLifecycleController implements LifecycleControllerInterface {

   private final static Class DEFAULT_LISTENER_CLASS = LoggerMessageListener.class;

   private final Logger LOGGER = Logger.getLogger(DefaultLifecycleController.class);

   private Map<Integer, MessageListenerWrapper> listeners;

   private int insertIdx = 0;

   private int retrievalIdx = 0;

   /**
    * Jalam 1.0 Requires the listenerClass to be provided at initialization.
    *
    * @param listenerClass listenerWrapper to use in this controller
    * @throws ResourceInitializerException
    */
   public DefaultLifecycleController(Class listenerClass) throws ResourceInitializerException {
      listeners = new HashMap<Integer, MessageListenerWrapper>();
      listeners.put(insertIdx++, createListener(listenerClass));
   }

   /**
    * No-arg constructor that requires listenerClasses to be specified in the jms.properties
    *
    * @throws ResourceInitializerException
    */
   public DefaultLifecycleController() throws ResourceInitializerException {
      listeners = new HashMap<Integer, MessageListenerWrapper>();
      Properties props = loadJmsProperties();
      configureListeners(props);
   }

   @Override
   public MessageListenerWrapper getListener(Class listenerClass) throws ResourceInitializerException {
      if (insertIdx <= retrievalIdx) {
         listeners.put(insertIdx++, createListener(listenerClass));
      }
      // RETRIEVE A LISTENER AND INCREMENT POSITION IDX.
      MessageListenerWrapper ret = listeners.get(retrievalIdx++);
      LOGGER.info("Will be using listener instance : " + ret.toString());
      return ret;
   }

   /**
    * Register a new listener of the class specified and add it to the list of
    * listeners
    *
    * @param listenerClass class of new listener to be instantiated and added.
    * @throws ResourceInitializerException
    */
   public void registerListener(Class listenerClass) throws ResourceInitializerException {
      listeners.put(insertIdx++, createListener(listenerClass));
   }

   private MessageListenerWrapper createListener(Class listenerClass) throws ResourceInitializerException {
      MessageListenerWrapper ret = null;
      try {
         ret = (MessageListenerWrapper) listenerClass.newInstance();
      } catch (InstantiationException ex) {
         throw new ResourceInitializerException(ex);
      } catch (IllegalAccessException ex) {
         throw new ResourceInitializerException(ex);
      }
      return ret;
   }

   @Override
   public Collection<MessageListenerWrapper> getListeners() {
      return listeners.values();
   }

   @Override
   public void run() {
   }

   @Override
   public void release() {
      for (MessageListenerWrapper listener : listeners.values()) {
         listener.release();
      }
   }

   /**
    * Find and load listener classes specified in the given properties object.
    * Finally, choose the first class found as the listener class (defaulting if
    * needed) TODO: SMM Add support for multiple listeners (this requires
    * linking a listener to it's jms destinations).
    *
    * @param props
    * @throws ResourceInitializerException if any of the listenerClasses
    * supplied could not be found.
    */
   private void configureListeners(Properties props) throws ResourceInitializerException {
      String listenersString = props.getProperty("config.listeners", DEFAULT_LISTENER_CLASS.getName());
      String[] listenerClassNames = listenersString.split("\\,");
      Class[] listenerClasses = new Class[listenerClassNames.length];
      try {
         for (int i = 0; i < listenerClassNames.length; i++) {
            listenerClasses[i] = ClassLoader.getSystemClassLoader().loadClass(listenerClassNames[i]);
         }
      } catch (ClassNotFoundException e) {
         LOGGER.error("Failure in loading the listeners", e);
      }

      Class listenerClass;
      if (listenerClasses[0] != null) {
         listenerClass = listenerClasses[0];
      } else {
         listenerClass = DEFAULT_LISTENER_CLASS;
      }
      LOGGER.debug("Using Listener class: ".concat(listenerClass.getName()));

      listeners.put(insertIdx++, createListener(listenerClass));
   }

   /**
    * Recover the jms.properties file which should be stored in the conf
    * directory TODO: Add support for supplying the location of the
    * jms.properties.
    *
    * @return
    */
   private Properties loadJmsProperties() {
      Properties jmsProps = new Properties();
      try {
         InputStream in = new FileInputStream(new File("jms.properties"));
         jmsProps.load(in);
         in.close();

         LOGGER.info("******* JMS Properties **********\t");
         for (Object key : jmsProps.keySet()) {
            LOGGER.info("\t".concat(key.toString()).concat(" : ").concat(jmsProps.get(key) != null ? jmsProps.get(key).toString() : ""));
         }
         LOGGER.info("************************************\t");
      } catch (IOException e) {
         //LOGGER.error("Unable to find jms.properties server configuration file, read the doc !", e);
      }
      return jmsProps;
   }
}
