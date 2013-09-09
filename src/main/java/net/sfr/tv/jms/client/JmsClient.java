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

import net.sfr.tv.jms.cnxmgt.ConnectionManager;
import net.sfr.tv.jms.client.context.JndiServerDescriptor;
import net.sfr.tv.jms.client.api.MessageListenerWrapper;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.jms.JMSException;
import net.sfr.tv.exceptions.ResourceInitializerException;
import net.sfr.tv.jms.client.api.LifecycleProviderInterface;
import org.apache.log4j.Logger;

/**
 * Main client class. Implements the monitor pattern, run/shutdown lifecycle methods.
 * 
 * @author matthieu.chaplin@sfr.com.chaplin@sfr.com
 */
public class JmsClient implements Runnable {

    private static Logger LOGGER = Logger.getLogger(JmsClient.class);
    
    /** Monitor pattern */
    public final Object monitor = new Object();    
    
    /** Stateful JMS connection managers : Handles connection/failover for a logical group of JMS servers. */
    private Map<String, ConnectionManager> cnxManagers;
    
    /** JMS listener class */
    private Class listenerClass;
    
    /** Listener wrapper class (alternate to using a listener class */
    private LifecycleProviderInterface listenerWrapper = null;

    /**
     * 
     * Constructor.
     * 
     * @param servers               JMS servers list, grouped by logical group.
     * @param destination           JMS destination JNDI name
     * @param isTopicSubscription   Topic subscription flag
     * @param isDurableSubscription Durable subscription flag
     * @param clientId              JMS client ID
     * @param subscriptionBaseName  JMS subscription name prefix
     * @param selector              JMS selector
     * @param listenerClassName     JMS listener class name
     * @param cnxFactoryJndiName    JMS connection factory JNDI name
     */
    public JmsClient(Map<String, Set<JndiServerDescriptor>> servers, String destination, Boolean isTopicSubscription, Boolean isDurableSubscription, String clientId, String subscriptionBaseName, String selector, String listenerClassName, String cnxFactoryJndiName) {

        try {
            listenerWrapper = instantiateListenerWrapper(listenerClassName);
            listenerClass = ClassLoader.getSystemClassLoader().loadClass(listenerClassName);
        } catch (ResourceInitializerException ex) {
            // EXIT PROGRAM
            throw new RuntimeException(ex);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
        
        if (LOGGER.isInfoEnabled()) {
            //LOGGER.info("Host : ".concat(host));
            LOGGER.info("Destination : ".concat(destination));
            LOGGER.info("ClientID : ".concat(clientId));
            LOGGER.info("Subscription base name : ".concat(subscriptionBaseName != null ? subscriptionBaseName : ""));
            LOGGER.info("Filter : ".concat(selector != null ? selector : ""));
            LOGGER.info("Servers groups : ".concat(String.valueOf(servers.size())));
        }

        cnxManagers = new TreeMap<String, ConnectionManager>();

        for (String group : servers.keySet()) {
            try {
                ConnectionManager cnxManager = new ConnectionManager(group, servers.get(group), clientId, cnxFactoryJndiName, listenerWrapper.getListener(listenerClass));
                cnxManager.connect(2);
                String[] sDestinations = destination.split("\\,");
                int consumerIdx = 0;
                String subscriptionName;
                for (String sdst : sDestinations) {
                    subscriptionName = subscriptionBaseName.concat(sDestinations.length > 1 ? "-" + ++consumerIdx : "");
                    cnxManager.subscribe(sdst, isTopicSubscription, isDurableSubscription, subscriptionName, selector);
                }
                cnxManagers.put(group, cnxManager);
            } catch (ResourceInitializerException ex) {
                LOGGER.error("Unable to start a listener/context binded to : ".concat(group), ex);
            }
        }
        
        LOGGER.info("Durable subscription ? ".concat(String.valueOf(isDurableSubscription)));
        
        listenerWrapper.run();
        
        // START MESSAGE DELIVERY
        try {
            for (ConnectionManager cnxManager : cnxManagers.values()) {
                cnxManager.start();
            }
        } catch (JMSException ex) {
            LOGGER.fatal("Unable to start JMS connection !", ex);
            System.exit(1);
        }
    }

    /**
     * Instantiate the JMS Listener Wrapper instance
     * @see net.sfr.tv.jms.client.wrapper.ListenerWrapper
     * 
     * @param className
     * @return
     * @throws ResourceInitializerException 
     */
    private LifecycleProviderInterface instantiateListenerWrapper(String className) throws ResourceInitializerException {

        LifecycleProviderInterface ret = null;

        LOGGER.info("Instantiating handler : ".concat(className));
        
        try {
            // Instantiate MessageListener
            Class handlerClass = ClassLoader.getSystemClassLoader().loadClass(className);
            
            if (DefaultLifecycleProvider.class.isAssignableFrom(handlerClass)) {
                
                Constructor ct = handlerClass.getConstructor();
                ret = (LifecycleProviderInterface) ct.newInstance();
                
            } else if (MessageListenerWrapper.class.isAssignableFrom(handlerClass)) {
                ret = new DefaultLifecycleProvider(handlerClass);
            } else {
                throw new InstantiationException("Specified MessageListener : ".concat(className).concat(" does not inherit from javax.jms.MessageListener !"));
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

        return ret;
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
     * Client shutdown : Sequential closure of JMS and user specific
     * resources.
     * 
     * User-specific resources must be released in 
     * @see net.sfr.tv.jms.client.wrapper.ListenerWrapper#release()
     */
    public void shutdown() {
        
        for (ConnectionManager cnxManager : cnxManagers.values()) {
            cnxManager.disconnect();
        }
        
        if (listenerWrapper != null) {
            listenerWrapper.release();
        }
        
        LOGGER.info("Bye !");
    }
}