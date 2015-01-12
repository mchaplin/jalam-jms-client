/**
 * Copyright 2012-2014 - SFR (http://www.sfr.com/)
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
package net.sfr.tv.jms.client.impl.listener;

import java.util.Enumeration;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import net.sfr.tv.messaging.client.impl.AbstractLoggerConsumer;

/**
 * A simple message listener, printing message content to logging facility.
 * 
 */
public class LoggerMessageListener extends AbstractLoggerConsumer implements MessageListener {
    
    public LoggerMessageListener(final String[] destinations) {
        super(destinations);
    }
    
    @Override
    public void onMessage(Message msg) {

        try {

            if (outputType.equals("FULL")) {
                logger.info("Received message :: ID : "
                        .concat(msg.getJMSMessageID() != null ? msg.getJMSMessageID() : "(?)")
                        .concat(", type : ").concat(msg.getJMSType() != null ? msg.getJMSType() : "(?)")
                        .concat(", tstamp : ").concat(String.valueOf(msg.getJMSTimestamp()))
                        .concat(", expiration : ").concat(String.valueOf(msg.getJMSExpiration()))
                        .concat(", delivery mode ? ").concat(String.valueOf(msg.getJMSDeliveryMode()))
                        .concat(", redelivery ? ").concat(String.valueOf(msg.getJMSRedelivered())));
                
                Enumeration enm = msg.getPropertyNames();
                String prop;
                String val;
                Object oVal;
                while (enm.hasMoreElements()) {
                    prop = (String) enm.nextElement();
                    try {
                        val = msg.getStringProperty(prop);
                        logger.info("\t".concat(prop).concat(" : ").concat(val != null ? val : "null"));
                    } catch (JMSException ex) {
                        try {
                            oVal = msg.getObjectProperty(prop);
                            logger.info("\t".concat(prop).concat(" : ").concat(oVal != null ? oVal.toString() : "null"));
                        } catch (JMSException ex2) {
                            logger.error("Unable to retrieve value for ".concat(prop));
                        }
                    }
                }
            }
            
            if (!outputType.equals("PROPERTY")) {
                if (TextMessage.class.isAssignableFrom(msg.getClass())) {
                    String text = ((TextMessage) msg).getText();
                    //logger.info("[".concat(String.valueOf(text.length())).concat("] : ").concat(text));
                    logger.info(text);
                } else if (BytesMessage.class.isAssignableFrom(msg.getClass())) {
                    BytesMessage bm = (BytesMessage) msg;
                    byte[] body = new byte[(int)bm.getBodyLength()];
                    bm.readBytes(body);

                    logger.info("[".concat(String.valueOf(body.length)).concat("] : ").concat(new String(body)));
                }   
            }
            
            if (outputType.equals("PROPERTY")) {
                logger.info(msg.getStringProperty(outputProperty));
            }

            // ACK the message to remove it from the queue.
            msg.acknowledge();
            
        } catch (JMSException ex) {
            if (IllegalStateException.class.isAssignableFrom(ex.getClass())) {
                // Connection failure, try failover ?
                
            }
            logger.error(ex.getMessage(), ex);
        }
    }
}
