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
package net.sfr.tv.jms.client;

import net.sfr.tv.jms.model.JndiProviderConfiguration;
import net.sfr.tv.jms.cnxmgt.AbstractConnectionManager;
import net.sfr.tv.jms.client.api.MessageListenerWrapper;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.TreeMap;
import javax.jms.JMSException;
import net.sfr.tv.exceptions.ResourceInitializerException;
import net.sfr.tv.jms.client.api.JmsClient;
import net.sfr.tv.jms.client.api.LifecycleController;
import net.sfr.tv.jms.cnxmgt.InboundConnectionManager;
import org.apache.log4j.Logger;

/**
 * Main client class. Implements the monitor pattern, run/shutdown lifecycle methods.
 *
 * @author matthieu.chaplin@sfr.com
 * @author scott.messner.prestataire@sfr.com
 * @author pierre.cheynier@sfr.com
 */
public class JmsClientImpl implements JmsClient {

    private static final Logger LOGGER = Logger.getLogger(JmsClientImpl.class);

    /**
     * Stateful JMS connection managers : Handles connection/failover for a logical group of JMS servers.
     */
    private Map<String, AbstractConnectionManager> cnxManagers;

    /**
     * Listener wrapper class (alternate to using a listener class
     */
    private LifecycleController lifecycleController;

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
     * @param listenerClass name of the JMS Listener class
     * @param destinations JNDI destinations to bind to
     * @param cnxFactoryJndiName JMS connection factory JNDI name
     * @throws net.sfr.tv.exceptions.ResourceInitializerException
     */
    public JmsClientImpl(
            JndiProviderConfiguration jndiProviderConfig,
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
        
        lifecycleController = instantiateLifecycleController(lifecycleControllerClass, destinations);
        lifecycleController.initListener(listenerClass);
        
        /*if (destinationsByListeners != null) {
            registerListeners(destinationsByListeners);
        }*/

        // Connect and Subscribe listeners to destinations
        cnxManagers = new TreeMap<>();
        int idxListener = 0;
        for (String group : jndiProviderConfig.getGroups()) {
            try {
                for (MessageListenerWrapper listener : lifecycleController.getListeners()) {
                    if (lifecycleController.getListeners().size() > 1) {
                        clientId = clientId.concat("/" + idxListener++);
                    }
                    InboundConnectionManager cnxManager = new InboundConnectionManager(group, jndiProviderConfig.getServersGroup(group), preferredServer, clientId, cnxFactoryJndiName, jndiProviderConfig.getCredentials(), listener);
                    cnxManager.connect(2);
                    LOGGER.info("Connection created for ".concat(listener.getName()));

                    String subscriptionName;
                    int subscriptionIdx = 0;
                    for (String dest : listener.getDestinations()) {
                        subscriptionName = subscriptionBaseName.concat("@").concat(dest).concat(listener.getDestinations().length > 1 ? "-" + subscriptionIdx++ : "");
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
                    cnxManagers.put(clientId, cnxManager);
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
            throw new ResourceInitializerException(ex);
            //LOGGER.fatal("Unable to start JMS connection !", ex);
        }
    }

    /**
     * Instantiate the LifecycleControllerInterface with the specified classname and the given listeners
     *
     * @see net.sfr.tv.jms.client.wrapper.ListenerWrapper
     *
     * @param lifeCycleControllerClassName The name of the LifecycleController class to load and
     * @param destinations Destinations to bind to
     * 
     * @return the new instance of the LifecycleControllerInterface
     * @throws ResourceInitializerException
     */
    private LifecycleController instantiateLifecycleController(final Class lifeCycleControllerClass, final String[] destinations) throws ResourceInitializerException {

        try {
            if (LifecycleController.class.isAssignableFrom(lifeCycleControllerClass)) {
                Constructor ct = lifeCycleControllerClass.getConstructor(String[].class);
                LOGGER.info("Using custom LifecycleController : ".concat(lifeCycleControllerClass.getName()));
                return lifecycleController = (LifecycleController) ct.newInstance(new Object[] {destinations});
            } else {
                throw new ResourceInitializerException(lifeCycleControllerClass.getName().concat(" is not a subtype of ").concat(LifecycleController.class.getName()), null);
            }
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            throw new ResourceInitializerException(ex);
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

        for (AbstractConnectionManager cnxManager : cnxManagers.values()) {
            cnxManager.disconnect();
        }
        if (lifecycleController != null) {
            lifecycleController.release();
        }
        LOGGER.info("Bye !");
    }
}
