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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.sfr.tv.exceptions.ResourceInitializerException;
import net.sfr.tv.jms.client.api.LifecycleControllerInterface;
import net.sfr.tv.jms.client.api.MessageListenerWrapper;
import org.apache.log4j.Logger;

/**
 * An "embedded" Lifecycle Controller. 
 * Feel free to create your own to do more sophisticated things ! 
 * 
 * @see net.sfr.tv.jms.client.api.LifecycleControllerInterface
 * 
 * @author matthieu.chaplin@sfr.com
 * @author pierre.cheynier@sfr.com 
 */
public class DefaultLifecycleController implements LifecycleControllerInterface {

   private final Logger LOGGER = Logger.getLogger(DefaultLifecycleController.class);

   private Map<Integer, MessageListenerWrapper> listeners = new HashMap<Integer, MessageListenerWrapper>();;

   private int insertIdx = 0;
   private int retrievalIdx = 0;
   
   public DefaultLifecycleController() {};

   /**
    * Jalam 1.0 Requires the listenerClass to be provided at initialization.
    *
    * @param listenerClass listenerWrapper to use in this controller
    * 
    * @throws ResourceInitializerException
    */
   public DefaultLifecycleController(Class listenerClass) throws ResourceInitializerException {
      registerListener(listenerClass);
   }

   /**
    * Jalam 2.0 Requires that listenerClasses are specified in the jms.properties
    * 
    * @param listenerClasses listenerWrapper(s) to use in this controller
    * 
    * @throws ResourceInitializerException
    */
   public DefaultLifecycleController(Collection<Class> listenerClasses) throws ResourceInitializerException {
       for (Class tclass : listenerClasses) {
           registerListener(tclass);
       }
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
   @Override
   public final void registerListener(Class listenerClass) throws ResourceInitializerException {
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

}
