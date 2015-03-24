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
package net.sfr.tv.hornetq.client.impl;

import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import net.sfr.tv.exceptions.ResourceInitializerException;
import net.sfr.tv.hornetq.HqCoreConnectionManager;
import net.sfr.tv.messaging.api.connection.ConsumerConnectionManager;
import net.sfr.tv.messaging.api.MessageConsumer;
import net.sfr.tv.messaging.api.SubscriptionDescriptor;
import net.sfr.tv.messaging.client.impl.MessagingClientImpl;
import net.sfr.tv.messaging.impl.MessagingProvidersConfiguration;
import org.apache.log4j.Logger;
import org.hornetq.api.core.client.MessageHandler;

/**
 *
 * @author matthieu.chaplin@sfr.com
 */
public class HornetQClientImpl extends MessagingClientImpl {

    private static final Logger logger = Logger.getLogger(HornetQClientImpl.class);
    
    public HornetQClientImpl(
            MessagingProvidersConfiguration msgingProviderConfig, 
            String preferredServer, 
            String subscriptionBaseName, 
            String selector, 
            Class lifecycleControllerClass, 
            Class listenerClass, 
            String[] destinations) throws ResourceInitializerException {
        
        super(msgingProviderConfig, preferredServer, subscriptionBaseName, selector, lifecycleControllerClass, listenerClass, destinations);
        
        // Connect and Subscribe listeners to destinations
        cnxManagers = new TreeMap<>();
        for (String group : msgingProviderConfig.getGroups()) {
            try {
                for (MessageConsumer listener : lifecycleController.getListeners()) {
                    ConsumerConnectionManager cnxManager = new HqCoreConnectionManager(group, msgingProviderConfig.getCredentials(), msgingProviderConfig.getServersGroup(group), preferredServer, (MessageHandler) listener);
                    cnxManager.connect(2, TimeUnit.SECONDS);
                    logger.info("Connection created for ".concat(listener.getClass().getName()));

                    String subscriptionName;
                    int subscriptionIdx = 0;
                    for (String dest : destinations) {
                        subscriptionName = subscriptionBaseName.concat("@").concat(dest).concat(destinations.length > 1 ? "-" + subscriptionIdx++ : "");
                        // FIXME : Handle topic/durable subscription booleans
                        cnxManager.subscribe(new SubscriptionDescriptor(dest, true, true, subscriptionName, selector), 2, TimeUnit.SECONDS);
                        if (logger.isInfoEnabled() || logger.isDebugEnabled()) {
                            logger.info("\tDestination : ".concat(dest));
                            logger.info("\tSubscription base name : ".concat(subscriptionBaseName));
                            logger.info("\tFilter : ".concat(selector != null ? selector : ""));
                            logger.info("\tServers groups : ".concat(String.valueOf(msgingProviderConfig.getGroups().size())));
                        }
                    }
                    cnxManagers.put("clientId-unset", cnxManager);
                }
            } catch (Exception ex) {
                logger.error("Unable to start a listener/context binded to : ".concat(group), ex);
            }
        }
        
    }
    
    @Override
    public void shutdown() {
        logger.warn("shutdown() : Not implemented yet !");
    }
}
