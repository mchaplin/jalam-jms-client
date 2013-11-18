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

import java.util.concurrent.Callable;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.NamingException;
import net.sfr.tv.exceptions.ResourceInitializerException;
import net.sfr.tv.jms.client.context.InboundJmsContext;
import net.sfr.tv.jms.client.context.JmsContext;
import net.sfr.tv.jms.client.context.JmsSubscription;
import net.sfr.tv.jms.client.context.JmsSubscriptionDescriptor;
import org.apache.log4j.Logger;

/**
 *
 * @author matthieu.chaplin@sfr.com
 */
public class SubscribeTask implements Callable<InboundJmsContext> {

    private static final Logger LOGGER = Logger.getLogger(SubscribeTask.class);
    
    //private AbstractConnectionManager parent;
    
    private JmsSubscriptionDescriptor metadata;
    
    private InboundJmsContext context;
    
    private MessageListener listener;
    
    public SubscribeTask(JmsContext context, JmsSubscriptionDescriptor metadata, MessageListener listener) {
        this.context = new InboundJmsContext(context.getJndiContext(), context.getConnection(), context.getSession());
        this.metadata = metadata;
        this.listener = listener;
    }
    
    @Override
    public InboundJmsContext call() throws Exception {
        
        LOGGER.info("Trying to subscribe to ".concat(metadata.toString()));
        
        try {

            Destination dst;
            MessageConsumer consumer;

            dst = lookupDestination(context.getJndiContext(), metadata.getDestination(), metadata.isIsTopicSubscription());
            if (dst != null) {
                consumer = createSubscription(metadata.isIsTopicSubscription() ? (Topic) dst : (Queue) dst, context.getSession(), metadata.isIsTopicSubscription(), metadata.getSubscriptionName(), metadata.getSelector());
                //jmsSubscriptions.add(new JmsSubscription(metadata, subscription, dst, consumer));
                context.addSubscription(new JmsSubscription(metadata, metadata.getSubscriptionName(), dst, consumer));

                consumer.setMessageListener(listener);
            }

        } catch (ResourceInitializerException rie) {
            LOGGER.error("Error while attempting to create a JMS subscription : ".concat(rie.getMessage()));
            return context;
        }
        
        return context;
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
    private Destination lookupDestination(Context ctx, String destination, boolean isTopicSubscription) {

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
    private MessageConsumer createSubscription(Destination dst, Session session, boolean isTopicSubscription, String subscriptionName, String selector) throws ResourceInitializerException {

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