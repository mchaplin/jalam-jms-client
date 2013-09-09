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
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Topic;
import net.sfr.tv.exceptions.ResourceInitializerException;
import net.sfr.tv.jms.client.context.JmsContext;
import net.sfr.tv.jms.client.context.JmsSubscription;
import net.sfr.tv.jms.client.context.JmsSubscriptionDescriptor;
import org.apache.log4j.Logger;

/**
 *
 * @author matthieu.chaplin@sfr.com
 */
public class SubscribeTask implements Callable<JmsContext> {

    private static final Logger LOGGER = Logger.getLogger(SubscribeTask.class);
    
    private ConnectionManager parent;
    
    private JmsSubscriptionDescriptor metadata;
    
    private JmsContext context;
    
    private MessageListener listener;
    
    public SubscribeTask(JmsContext context, JmsSubscriptionDescriptor metadata, MessageListener listener) {
        this.context = context;
        this.metadata = metadata;
        this.listener = listener;
    }
    
    @Override
    public JmsContext call() throws Exception {
        
        LOGGER.info("Trying to connect to ".concat(metadata.toString()));
        
        try {

            Destination dst;
            MessageConsumer consumer;

            dst = ContextFactory.lookupDestination(context.getJndiContext(), metadata.getDestination(), metadata.isIsTopicSubscription());
            if (dst != null) {
                consumer = ContextFactory.createSubscription(metadata.isIsTopicSubscription() ? (Topic) dst : (Queue) dst, context.getSession(), metadata.isIsTopicSubscription(), metadata.getSubscriptionName(), metadata.getSelector());
                //jmsSubscriptions.add(new JmsSubscription(metadata, subscription, dst, consumer));
                context.addSubscription(new JmsSubscription(metadata, metadata.getSubscriptionName(), dst, consumer));

                consumer.setMessageListener(listener);
            }

        } catch (ResourceInitializerException rie) {
            LOGGER.error("Error while attempting to create a JMS connection/subscription : ".concat(rie.getMessage()), rie);
            return null;
        }
        
        return context;
    }
}