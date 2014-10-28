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
import javax.jms.TextMessage;
import net.sfr.tv.jms.client.impl.AbstractMessageListener;
import org.apache.log4j.Logger;

/**
 * A simple message listener, printing message content to logging facility.
 * 
 */
public class LoggerMessageListener extends AbstractMessageListener {

    private Logger LOGGER;
    
    private final String outputType;
        
    private final String outputProperty;
    
    public LoggerMessageListener() {
        super();
        String loggerName = System.getProperty("listener.logger.name");
        if (loggerName != null && loggerName.trim().length() > 0) {
            LOGGER = Logger.getLogger(loggerName);
        } else {
            LOGGER = Logger.getLogger(LoggerMessageListener.class);
        }
        
        outputType = System.getProperty("listener.output.type", "FULL");
        outputProperty = System.getProperty("listener.output.property");
    }
    
    @Override
    public void onMessage(Message msg) {

        try {

            if (outputType.equals("FULL")) {
                LOGGER.info("Received message :: ID : "
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
                        LOGGER.info("\t".concat(prop).concat(" : ").concat(val != null ? val : "null"));
                    } catch (JMSException ex) {
                        try {
                            oVal = msg.getObjectProperty(prop);
                            LOGGER.info("\t".concat(prop).concat(" : ").concat(oVal != null ? oVal.toString() : "null"));
                        } catch (JMSException ex2) {
                            LOGGER.error("Unable to retrieve value for ".concat(prop));
                        }
                    }
                }
            }
            
            if (!outputType.equals("PROPERTY")) {
                if (TextMessage.class.isAssignableFrom(msg.getClass())) {
                    String text = ((TextMessage) msg).getText();
                    
                    LOGGER.info("[".concat(String.valueOf(text.length())).concat("] : ").concat(text));
                } else if (BytesMessage.class.isAssignableFrom(msg.getClass())) {
                    BytesMessage bm = (BytesMessage) msg;
                    byte[] body = new byte[(int)bm.getBodyLength()];
                    bm.readBytes(body);

                    LOGGER.info("[".concat(String.valueOf(body.length)).concat("] : ").concat(new String(body)));
                }   
            }
            
            if (outputType.equals("PROPERTY")) {
                LOGGER.info(msg.getStringProperty(outputProperty));
            }

            // ACK the message to remove it from the queue.
            msg.acknowledge();
            
        } catch (JMSException ex) {
            if (IllegalStateException.class.isAssignableFrom(ex.getClass())) {
                // Connection failure, try failover ?
                
            }
            LOGGER.error(ex.getMessage(), ex);
        }
    }
}
