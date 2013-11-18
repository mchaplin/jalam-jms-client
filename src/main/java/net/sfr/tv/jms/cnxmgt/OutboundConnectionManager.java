/*
 * Copyright 2012,2013 - SFR (http://www.sfr.com/)
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
package net.sfr.tv.jms.cnxmgt;

import java.util.Set;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.naming.NamingException;
import net.sfr.tv.jms.client.context.JndiServerDescriptor;
import net.sfr.tv.jms.client.context.OutboundJmsContext;
import net.sfr.tv.model.Credentials;
import org.apache.log4j.Logger;

/**
 *
 * @author matthieu
 */
public class OutboundConnectionManager extends AbstractConnectionManager {
 
    private static final Logger LOGGER = Logger.getLogger(OutboundConnectionManager.class);
    
    public OutboundConnectionManager(String name, Set<JndiServerDescriptor> servers, String preferredServer, String clientId, String cnxFactoryJndiName, Credentials credentials) {
        super(name, servers, preferredServer, clientId, cnxFactoryJndiName, credentials);
        
        connect(2);
    }
    
    public OutboundJmsContext createProducer(String destination) {
        
        MessageProducer producer = null;
        
        try {
            
            Destination dest = (Destination) jndiContext.lookup(destination);

            producer = context.getSession().createProducer(dest);
            // Set Delivery Mode (Durable, Non-Durable)
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            // Disable messageId & timestamp, saves uniqueId & timestamp generation on the JMS server side
            producer.setDisableMessageID(true);
            //producer.setDisableMessageTimestamp(true);
            // Set TTL, afterwards message will be moved to an expiry queue
            producer.setTimeToLive(60 * 60 * 1000);
            
            LOGGER.info("JMS Producer configuration : ");
            LOGGER.info("\t Delivery Mode : " + producer.getDeliveryMode());
            LOGGER.info("\t TTL : " + producer.getTimeToLive());
            LOGGER.info("\t Message ID ? " + !producer.getDisableMessageID());
            LOGGER.info("\t Message Timestamp ? " + !producer.getDisableMessageTimestamp());
            
        } catch (NamingException | JMSException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        
        return new OutboundJmsContext(context.getJndiContext(), context.getConnection(), context.getSession(), producer);
    }
}
