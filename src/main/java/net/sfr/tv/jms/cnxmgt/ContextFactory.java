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

import net.sfr.tv.jms.client.context.JndiServerDescriptor;
import java.util.Hashtable;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import net.sfr.tv.exceptions.ResourceInitializerException;
import org.apache.log4j.Logger;

/**
 * JMSContext resources factory : Given a pool of JmsServer, provides
 * utility methods to retrieve JMS resources (Connection, Session, ...)
 * 
 * @see net.sfr.tv.jms.client.JmsContext
 * @see net.sfr.tv.jms.client.JmsServer
 * 
 * @author matthieu.chaplin@sfr.com.chaplin@sfr.com
 */
public class ContextFactory {

    private static final Logger LOGGER = Logger.getLogger(ContextFactory.class);

    public static Context initializeJndiContext(JndiServerDescriptor target) {
        
        // Set up JNDI context & lookup destination. Interrupt in case of failure.
        Context ctx = null;
        
        Hashtable props = new Hashtable(3);
        props.put(Context.PROVIDER_URL, target.getProviderUrl());
        props.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
        try {
            ctx = new InitialContext(props);
        } catch (NamingException ex) {
            LOGGER.error("JNDI Context initialization failure ! ", ex);
        }
        return ctx;
    }

    /**
     * Lookup a destination in current JNDI context.
     * 
     * Refer to JMS spec for further information.
     * 
     * @param destination
     * @param isTopicSubscription
     * @return Destination or null.
     */
    public static Destination lookupDestination(Context ctx, String destination, boolean isTopicSubscription) {

        Destination dst = null;

        try {
            if (isTopicSubscription) {
                dst = (Topic) ctx.lookup(destination);
            } else {
                dst = (Queue) ctx.lookup(destination);
            }
        } catch (NamingException ex) {
            LOGGER.error("JNDI lookup failed for destination ".concat(destination).concat(" ! "));
        }

        return dst;
    }

    /**
     * Creates a JMS Connection.
     * 
     * Refer to JMS spec for further information.
     * 
     * @param ctx
     * @param clientId
     * @param connectionFactory 
     * @return
     * @throws ResourceInitializerException
     */
    public static Connection getConnection(Context ctx, String clientId, String connectionFactory) throws ResourceInitializerException {

        Connection cnx = null;

        try {
            // TODO : Variabiliser Consumer factory
            ConnectionFactory cf = (ConnectionFactory) ctx.lookup("/".concat(connectionFactory));
            cnx = cf.createConnection("guest", "guest");
            cnx.setClientID(clientId);
        } catch (NamingException ex) {
            throw new ResourceInitializerException(ex);
        } catch (JMSException ex) {
            if (cnx != null) {
                try { cnx.close(); } catch (JMSException e) { LOGGER.error("Unable to gracefully close connection upon error !", e); };
            }
            throw new ResourceInitializerException(ex);
        }

        return cnx;
    }

    /**
     * Create a Session in CLIENT_ACKNOWLEDGE mode.
     * 
     * Refer to JMS spec for further information.
     * 
     * @param cnx
     * @return 
     * @throws ResourceInitializerException
     */
    public static Session createSession(Connection cnx) throws ResourceInitializerException {
        try {
            return cnx.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        } catch (JMSException ex) {
            throw new ResourceInitializerException(ex);
        }
    }

    /**
     * Creates a subscription to specified Destination.
     * 
     * Refer to JMS spec for further information.
     * 
     * @param dst
     * @param session
     * @param isTopicSubscription
     * @param subscriptionName
     * @param selector
     * @return 
     * @throws ResourceInitializerException
     */
    public static MessageConsumer createSubscription(Destination dst, Session session, boolean isTopicSubscription, String subscriptionName, String selector) throws ResourceInitializerException {

        MessageConsumer consumer = null;

        try {

            if (isTopicSubscription) {
                Topic topic = (Topic) dst;
                try {
                    if (selector != null && !selector.trim().equals("")) {
                        LOGGER.info("Creating a durable Topic subscription to ".concat(topic.getTopicName()).concat(" with filter : ").concat(selector));
                        // Create a subscriber with noLocal 'flag' : Don't consume messages we would 'potentially' publish
                        consumer = session.createDurableSubscriber(topic, subscriptionName, selector, true);
                    } else {
                        LOGGER.info("Creating a durable Topic subscription to ".concat(topic.getTopicName()));
                        consumer = session.createDurableSubscriber(topic, subscriptionName);
                    }
                } catch (javax.jms.IllegalStateException ex) {
                    // Subscription already exists, consumer connecting back from a dirty disconnect
                    LOGGER.error(ex.getMessage(), ex);
                    session.unsubscribe(subscriptionName);
                    return createSubscription(topic, session, isTopicSubscription, subscriptionName, selector);
                }

            } else {
                LOGGER.info("Creating a Queue consumer to ".concat(((Queue) dst).getQueueName()));
                consumer = session.createConsumer(dst);
            }

        } catch (JMSException ex) {
            throw new ResourceInitializerException(ex);
        }
        return consumer;
    }
}
