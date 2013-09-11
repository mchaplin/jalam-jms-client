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
package net.sfr.tv.jms.cnxmgt;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.naming.Context;
import net.sfr.tv.jms.client.context.JmsContext;
import net.sfr.tv.jms.client.context.JndiServerDescriptor;
import net.sfr.tv.jms.client.context.JmsSubscription;
import net.sfr.tv.jms.client.context.JmsSubscriptionDescriptor;
import org.apache.log4j.Logger;

/**
 * Stateful management of JMS connection, handling failover & reconnections
 * 
 * @author matthieu.chaplin@sfr.com
 */
public class ConnectionManager implements ExceptionListener {
    
    private static final Logger LOGGER = Logger.getLogger(ConnectionManager.class);
    
    private String name;
    
    /** JMS Connection Factory JNDI name */
    private String cnxFactoryJndiName;
    
    private String clientId;
        
    /** Configuration reference */
    private Set<JndiServerDescriptor> availableServers;
    /** Active configuration reference */
    private JndiServerDescriptor activeServer;
    
    /** Currently used JMS resources */
    private JmsContext context;
    
    /** Current JNDI context */
    private Context jndiContext;
    
    /** ExecutorService used for periodic JMS connect/subscribe tasks */
    private ScheduledExecutorService scheduler;
    
    /** TODO : Add support for a dedicated listener per subscription. JMS2 supports multiple listener instance.*/
    private MessageListener listener;
    
    public ConnectionManager(String name, Set<JndiServerDescriptor> servers, String clientId, String cnxFactoryJndiName, MessageListener listener) {
        
        this.name = name;
        this.clientId = clientId;
        this.cnxFactoryJndiName = cnxFactoryJndiName;
        this.listener = listener;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Try with 1st server. TODO : Optimize and round-robin
        this.availableServers = servers;
        this.activeServer = servers.iterator().next();

        lookup(activeServer, 2);
        
        LOGGER.info("Group : ".concat(name).concat(" , Service provider URL : ").concat(activeServer.getProviderUrl()));
    }
    
    public final void lookup(JndiServerDescriptor jndiServer, long delay) {
        ScheduledFuture<Context> futureContext = null;
        JndiLookupTask jlt;
        boolean initConnect = true;
        try {
            while (futureContext == null || (jndiContext = futureContext.get()) == null) {
                // reschedule a task
                jlt = new JndiLookupTask(jndiServer);
                futureContext = scheduler.schedule(jlt, initConnect ? 0 : delay, TimeUnit.SECONDS);
                initConnect = false;
            }
            
        } catch (InterruptedException ex) {
            LOGGER.error(ex.getMessage(), ex);
        } catch (ExecutionException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }
    
    /**
     * Establish a JMS connection and session.
     * 
     * @param delay     Periodic attempts delay.
     */
    public final void connect(long delay) {
        ScheduledFuture<JmsContext> futureContext = null;
        ConnectTask ct;
        boolean initConnect = true;
        try {
            while (futureContext == null || (context = futureContext.get()) == null) {
                // reschedule a task
                ct = new ConnectTask(jndiContext, clientId, cnxFactoryJndiName, this);
                futureContext = scheduler.schedule(ct, initConnect ? 0 : delay, TimeUnit.SECONDS);
                initConnect = false;
            }
            
        } catch (InterruptedException ex) {
            LOGGER.error(ex.getMessage(), ex);
        } catch (ExecutionException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }
    
    
    /**
     * Subscribe to a JMS destination.
     * 
     * @param metadata  Subscription metadata.
     * @param delay     Periodic attempts delay.
     */
    public final void subscribe(JmsSubscriptionDescriptor metadata, long delay) {
        ScheduledFuture<JmsContext> futureContext = null;
        SubscribeTask ct;
        boolean initConnect = true;
        try {
            while (futureContext == null || (context = futureContext.get()) == null) {
                // reschedule a task
                ct = new SubscribeTask(context, metadata, listener);
                futureContext = scheduler.schedule(ct, initConnect ? 0 : delay, TimeUnit.SECONDS);
                initConnect = false;
            }
            
        } catch (InterruptedException ex) {
            LOGGER.error(ex.getMessage(), ex);
        } catch (ExecutionException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }
    
    public final void subscribe(String destination, boolean isTopicSubscription, boolean isDurableSubscription, String subscriptionBaseName, String selector) {
        subscribe(new JmsSubscriptionDescriptor(destination, isTopicSubscription, isDurableSubscription, subscriptionBaseName, selector), 2);
    }
    
    /**
     * Starts message delivery for subscriptions associated to a connection.
     * 
     * @throws JMSException 
     */
    public final void start() throws JMSException {
        context.getConnection().start();
    }
    
    /**
     * Release a JMS subscription
     * 
     * @param consumer
     * @param session
     * @param subscriptionName 
     */
    public final void unsubscribe(JmsSubscription subscription, Session session) {

        LOGGER.info("About to unsubscribe : ".concat(subscription.getSubscriptionName()));
        
        // CLOSE CONSUMTER
        if (subscription.getConsumer() != null) {
            try {
                subscription.getConsumer().close();
            } catch (JMSException ex) {
                LOGGER.warn(ex.getMessage());
            }

        }
        
        // UNSUBSCRIBE
        if (session != null && subscription.getMetadata().isIsTopicSubscription() && !subscription.getMetadata().isIsDurableSubscription()) {
            // Unsubscribe, to prevent leaving a potential 'shadow' queue & permit reusing the same clientId later on.
            try {
                ((Session) session).unsubscribe(subscription.getSubscriptionName());
                LOGGER.info("Unsubscribed : ".concat(subscription.getSubscriptionName()));
            } catch (JMSException ex) {
                LOGGER.warn(ex.getMessage());
            }
        }
    }
    
    /**
     * Release a connection, terminating associated resources :
     * <ul>
     *  <li> Subscriptions
     *  <li> Session
     * </ul>
     */
    public final void disconnect() {
        
        // UNSUBSCRIBE
        if (context.getSubscriptions() != null) {
            for (JmsSubscription subscription : context.getSubscriptions()) {
                unsubscribe(subscription, context.getSession());
            }   
        }

        // TERMINATE SESSION
        if (context.getSession() != null) {
            try {
                ((Session) context.getSession()).close();
            } catch (JMSException ex) {
                LOGGER.warn(ex.getMessage());
            }
        }
        
        // CLOSE CONNECTION
        if (context.getConnection() != null) {
            try {
                context.getConnection().stop();
                context.getConnection().close();
            } catch (JMSException ex) {
                LOGGER.warn(ex.getMessage());
            }
        }
    }

    @Override
    public void onException(JMSException jmse) {
        
        LOGGER.warn("onException : ".concat(jmse.getMessage()));
        
        if (jmse.getMessage().toUpperCase().indexOf("DISCONNECTED") != -1) {
            // KEEP TRACK OF PREVIOUS SUBSCRIPTIONS METADATA
            Set<JmsSubscriptionDescriptor> subscriptionsMeta = new HashSet<JmsSubscriptionDescriptor>();
            for (JmsSubscription subscription : context.getSubscriptions()) {
                subscriptionsMeta.add(subscription.getMetadata());
            }

            // BLACKLIST ACTIVE SERVER
            LOGGER.error("Active Server not available anymore ! ".concat(activeServer.getProviderUrl()));
            if (availableServers.size() > 1) {
                for (JndiServerDescriptor srv : availableServers) {
                    if (!srv.equals(activeServer)) {
                        activeServer = srv;
                        break;
                    }
                }            
            }

            // LOOKUP NEW JNDI CONTEXT
            lookup(activeServer, 2);
            LOGGER.info("Group : ".concat(name).concat(" , JNDI service provider URL : ").concat(activeServer.getProviderUrl()));

            // CONNECT TO NEW ACTIVE SERVER WITH A 2 SECONDS PERIOD.
            connect(2);

            // RESUME SUBSCRIPTION OVER NEW ACTIVE SERVER
            for (JmsSubscriptionDescriptor meta : subscriptionsMeta) {
                subscribe(meta, 5);
            }

            try {
                start();
            } catch (JMSException ex) {
                LOGGER.error("Unable to start connection !", ex);
                for (JmsSubscription subscription : context.getSubscriptions()) {
                    unsubscribe(subscription, context.getSession());
                }
            }   
        }
    }

    public String getJndiConnectionFactory() {
        return cnxFactoryJndiName;
    }

    public Context getJndiContext() {
        return jndiContext;
    }

    public JmsContext getJmsContext() {
        return context;
    }

    public String getClientId() {
        return clientId;
    }

    public String getName() {
        return name;
    }
}
