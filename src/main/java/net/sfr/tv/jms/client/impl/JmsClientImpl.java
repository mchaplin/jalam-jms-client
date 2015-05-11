/**
 * Copyright 2012-2014 - SFR (http://www.sfr.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.sfr.tv.jms.client.impl;

import net.sfr.tv.messaging.impl.MessagingProvidersConfiguration;
import net.sfr.tv.messaging.api.MessageConsumer;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import javax.jms.MessageListener;
import net.sfr.tv.exceptions.ResourceInitializerException;
import net.sfr.tv.jms.cnxmgt.JmsConsumerConnectionManager;
import net.sfr.tv.messaging.api.connection.ConnectionManager;
import net.sfr.tv.messaging.api.connection.ConsumerConnectionManager;
import net.sfr.tv.messaging.api.SubscriptionDescriptor;
import net.sfr.tv.messaging.client.impl.MessagingClientImpl;
import org.apache.log4j.Logger;

/**
 * Main client class. Implements the monitor pattern, run/shutdown lifecycle methods.
 *
 * @author matthieu.chaplin@sfr.com
 * @author scott.messner.prestataire@sfr.com
 * @author pierre.cheynier@sfr.com
 */
public class JmsClientImpl extends MessagingClientImpl {

    private static final Logger logger = Logger.getLogger(JmsClientImpl.class);

    /**
     * Constructor.
     *
     * @param msgingProviderConfig References available JNDI servers & associated credentials.
     * @param preferredServer Preferred server alias.
     * @param isTopicSubscription Topic subscription flag
     * @param isDurableSubscription Durable subscription flag
     * @param clientId JMS client ID
     * @param subscriptionBaseName JMS subscription name prefix
     * @param selector JMS selector
     * @param lifecycleControllerClass LifecycleController class
     * @param listenerClass name of the JMS Listener class
     * @param destinations JNDI destinations to bind to
     * @param cnxFactoryJndiName JMS connection factory JNDI name
     * @throws net.sfr.tv.exceptions.ResourceInitializerException
     */
    public JmsClientImpl(
            MessagingProvidersConfiguration msgingProviderConfig,
            String preferredServer,
            Boolean isTopicSubscription,
            Boolean isDurableSubscription,
            String clientId,
            String subscriptionBaseName,
            String selector,
            Class lifecycleControllerClass,
            Class listenerClass,
            String[] destinations,
            String cnxFactoryJndiName) throws ResourceInitializerException {
        
        super(msgingProviderConfig, preferredServer, subscriptionBaseName, selector, lifecycleControllerClass, listenerClass, destinations);

        // Connect and Subscribe listeners to destinations
        cnxManagers = new TreeMap<>();
        int idxListener = 0;
        for (String group : msgingProviderConfig.getGroups()) {
            try {
                for (MessageConsumer listener : lifecycleController.getListeners()) {
                    if (lifecycleController.getListeners().size() > 1) {
                        clientId = clientId.concat("/" + idxListener++);
                    }
                    ConsumerConnectionManager cnxManager = new JmsConsumerConnectionManager(group, msgingProviderConfig.getServersGroup(group), preferredServer, clientId, cnxFactoryJndiName, msgingProviderConfig.getCredentials(), (MessageListener) listener);
                    // FIXME : After a max number of tryout, round-robin to another server.
                    cnxManager.connect(2, TimeUnit.SECONDS);
                    logger.info("Connection created for ".concat(listener.getClass().getName()));

                    String subscriptionName;
                    int subscriptionIdx = 0;
                    for (String dest : destinations) {
                        subscriptionName = subscriptionBaseName.concat("@").concat(dest).concat(destinations.length > 1 ? "-" + subscriptionIdx++ : "");
                        cnxManager.subscribe(new SubscriptionDescriptor(dest, isTopicSubscription, isDurableSubscription, subscriptionName, selector), 2, TimeUnit.SECONDS);
                        if (logger.isInfoEnabled() || logger.isDebugEnabled()) {
                            logger.info("Destination : ".concat(dest));
                            logger.info("ClientID : ".concat(clientId));
                            logger.info("Subscription base name : ".concat(subscriptionBaseName));
                            logger.info("Durable subscription ? ".concat(String.valueOf(isDurableSubscription)));
                            logger.info("Filter : ".concat(selector != null ? selector : ""));
                            logger.info("Servers groups : ".concat(String.valueOf(msgingProviderConfig.getGroups().size())));
                        }
                    }
                    cnxManagers.put(clientId, cnxManager);
                }
            } catch (Exception ex) {
                logger.error("Unable to start a listener/context binded to : ".concat(group), ex);
            }
        }
    }

    /**
     * Register the listener classes with the lifecycle controller
     *
     * @param listenerClassNames
     * @throws ResourceInitializerException
     */
    /*private void registerListeners(final Map<String[], String> destinationsByListeners) throws ResourceInitializerException {
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        try {
            // Message Listeners
            for (String[] destinations : destinationsByListeners.keySet()) {
                Class listenerClass = systemClassLoader.loadClass(destinationsByListeners.get(destinations));
                lifecycleController.registerListener(listenerClass, destinations);
            }
        } catch (ClassNotFoundException e) {
            throw new ResourceInitializerException(e);
        }
    }*/

    /**
     * Client shutdown : Sequential closure of JMS and user specific resources.
     *
     * User-specific resources must be released in
     *
     * @see net.sfr.tv.jms.client.wrapper.ListenerWrapper#release()
     */
    @Override
    public void shutdown() {

        for (ConnectionManager cnxManager : cnxManagers.values()) {
            cnxManager.disconnect();
        }
        if (lifecycleController != null) {
            lifecycleController.release();
        }
        logger.info("Bye !");
    }
}
