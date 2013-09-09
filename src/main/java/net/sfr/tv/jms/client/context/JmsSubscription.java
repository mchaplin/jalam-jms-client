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
package net.sfr.tv.jms.client.context;

import javax.jms.Destination;
import javax.jms.MessageConsumer;

/**
 * A JMS subscription is composed of :
 * <ul>
 *  <li> metadata : Subscription name prefix, durability, selector, destination type
 *  <li> a destination
 *  <li> a listener, consuming messages
 * </ul>
 * 
 * @author matthieu.chaplin@sfr.com
 */
public class JmsSubscription {
    
    private JmsSubscriptionDescriptor metadata;
    
    private Destination dst;
    
    private MessageConsumer consumer;
    
    private String subscriptionName;
    
    public JmsSubscription(JmsSubscriptionDescriptor metadata, String subscriptionName, Destination dst, MessageConsumer consumer) {
        this.metadata = metadata;
        this.dst = dst;
        this.consumer = consumer;
        this.subscriptionName = subscriptionName;   
    }

    public Destination getDst() {
        return dst;
    }

    public MessageConsumer getConsumer() {
        return consumer;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public JmsSubscriptionDescriptor getMetadata() {
        return metadata;
    }
}
