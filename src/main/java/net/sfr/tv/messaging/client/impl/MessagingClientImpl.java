/*
 * Copyright 2015 matthieu.chaplin@sfr.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sfr.tv.messaging.client.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import net.sfr.tv.exceptions.ResourceInitializerException;
import net.sfr.tv.messaging.impl.MessagingProvidersConfiguration;
import net.sfr.tv.messaging.api.connection.ConsumerConnectionManager;
import net.sfr.tv.messaging.client.api.LifecycleController;
import net.sfr.tv.messaging.client.api.MessagingClient;
import org.apache.log4j.Logger;

/**
 *
 * @author matthieu.chaplin@sfr.com
 */
public abstract class MessagingClientImpl implements MessagingClient {
    
    private static final Logger logger = Logger.getLogger(MessagingClientImpl.class);

    /**
     * Stateful JMS connection managers : Handles connection/failover for a logical group of JMS servers.
     */
    protected Map<String, ConsumerConnectionManager> cnxManagers;

    /**
     * Listener wrapper class (alternate to using a listener class
     */
    protected LifecycleController lifecycleController;

    /**
     * Constructor.
     *
     * @param jndiProviderConfig References available JNDI servers & associated credentials.
     * @param preferredServer Preferred server alias.
     * @param subscriptionBaseName JMS subscription name prefix
     * @param selector JMS selector
     * @param lifecycleControllerClass LifecycleController class
     * @param listenerClass name of the JMS Listener class
     * @param destinations JNDI destinations to bind to
     * 
     * @throws net.sfr.tv.exceptions.ResourceInitializerException
     */
    public MessagingClientImpl(
            MessagingProvidersConfiguration jndiProviderConfig,
            String preferredServer,
            String subscriptionBaseName,
            String selector,
            Class lifecycleControllerClass,
            Class listenerClass,
            String[] destinations) throws ResourceInitializerException {
        
        lifecycleController = instantiateLifecycleController(lifecycleControllerClass, destinations);
        lifecycleController.initListener(listenerClass);
        
    }
    
    @Override
    public void start() throws ResourceInitializerException {
        
        lifecycleController.run();
        
        // START MESSAGE DELIVERY
        try {
            for (ConsumerConnectionManager cnxManager : cnxManagers.values()) {
                cnxManager.start();
            }
        } catch (Exception ex) {
            throw new ResourceInitializerException(ex);
        }
    }
    
    /**
     * Instantiate the LifecycleControllerInterface with the specified classname and the given listeners
     *
     * @see net.sfr.tv.jms.client.wrapper.ListenerWrapper
     *
     * @param lifeCycleControllerClass The name of the LifecycleController class to load and
     * @param destinations Destinations to bind to
     * 
     * @return the new instance of the LifecycleControllerInterface
     * @throws ResourceInitializerException
     */
    protected final LifecycleController instantiateLifecycleController(final Class lifeCycleControllerClass, final String[] destinations) throws ResourceInitializerException {

        try {
            if (LifecycleController.class.isAssignableFrom(lifeCycleControllerClass)) {
                Constructor ct = lifeCycleControllerClass.getConstructor();
                logger.info("LifecycleController class : ".concat(lifeCycleControllerClass.getName()));
                return lifecycleController = (LifecycleController) ct.newInstance();
            } else {
                throw new ResourceInitializerException(lifeCycleControllerClass.getName().concat(" is not a subtype of ").concat(LifecycleController.class.getName()), null);
            }
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            throw new ResourceInitializerException(ex);
        }
    }
    
}
