/*
 * Copyright 2014 matthieu.chaplin@sfr.com.
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
package net.sfr.tv.jms.client.impl;

import java.util.Map;
import net.sfr.tv.exceptions.ResourceInitializerException;
import net.sfr.tv.messaging.client.impl.MessagingClientImpl;
import net.sfr.tv.messaging.impl.MessagingProvidersConfiguration;

/**
 *
 * @author matthieu.chaplin@sfr.com
 */
public class MultiListenersJmsClientImpl extends MessagingClientImpl {

    /**
     * Constructor.
     *
     * @param jndiProviderConfig References available JNDI servers & associated credentials.
     * @param preferredServer Preferred server alias.
     * @param isTopicSubscription Topic subscription flag
     * @param isDurableSubscription Durable subscription flag
     * @param clientId JMS client ID
     * @param subscriptionBaseName JMS subscription name prefix
     * @param selector JMS selector
     * @param lifecycleControllerClass LifecycleController class
     * @param destinationsByListeners Map of listener class names with their associated destinations
     * @param cnxFactoryJndiName JMS connection factory JNDI name
     * @throws net.sfr.tv.exceptions.ResourceInitializerException
     */
    public MultiListenersJmsClientImpl(
            MessagingProvidersConfiguration msgingProviderConfig,
            String preferredServer,
            Boolean isTopicSubscription,
            Boolean isDurableSubscription,
            String clientId,
            String subscriptionBaseName,
            String selector,
            Class lifecycleControllerClass,
            Map<String[], String> destinationsByListeners,
            String cnxFactoryJndiName) throws ResourceInitializerException {
        
        //super(msgingProviderConfig, preferredServer, subscriptionBaseName, selector, lifecycleControllerClass, listenerClass, destinations);
        super(msgingProviderConfig, preferredServer, subscriptionBaseName, selector, lifecycleControllerClass, null, null);
        // TODO
        
    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    
}
