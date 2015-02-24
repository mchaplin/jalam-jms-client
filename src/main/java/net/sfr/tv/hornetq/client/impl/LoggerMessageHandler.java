/*
 * Copyright 2015 matthieu.chaplin@sfr.com.
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
package net.sfr.tv.hornetq.client.impl;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sfr.tv.messaging.client.impl.LoggerConsumerImpl;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.MessageHandler;

/**
 *
 * @author matthieu.chaplin@sfr.com
 */
public class LoggerMessageHandler extends LoggerConsumerImpl implements MessageHandler {

    @Override
    public void onMessage(ClientMessage msg) {

        logger.debug("onMessage(" + msg.toString() + ")");
        
        try {

            if (outputType.equals("FULL")) {
                logger.info("Received message :: ID : "
                        .concat(String.valueOf(msg.getMessageID()))
                        .concat(", type : ").concat(String.valueOf(msg.getType()))
                        .concat(", tstamp : ").concat(String.valueOf(msg.getTimestamp()))
                        .concat(", expiration : ").concat(String.valueOf(msg.getExpiration()))
                        .concat(", delivery count ? ").concat(String.valueOf(msg.getDeliveryCount())));

                Set<SimpleString> propNames = msg.getPropertyNames();
                String val;
                for (SimpleString prop : propNames) {

                    val = msg.getStringProperty(prop);
                    logger.info("\t".concat(prop.toString()).concat(" : ").concat(val != null ? val : "null"));

                }
            }

            if (!outputType.equals("PROPERTY")) {
                logger.debug("Message body size : " + msg.getBodySize() + ", readable bytes : " + msg.getBodyBuffer().readableBytes());
                ByteBuffer bb = ByteBuffer.allocate(msg.getBodyBuffer().readableBytes());
                msg.getBodyBuffer().readBytes(bb);
                
                try {
                    logger.info(new String(bb.array(), "UTF-8"));
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(LoggerMessageHandler.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

            if (outputType.equals("PROPERTY")) {
                logger.info(msg.getStringProperty(outputProperty));
            }

            // ACK the message to remove it from the queue.
            msg.acknowledge();

        } catch (HornetQException ex) {
            if (IllegalStateException.class.isAssignableFrom(ex.getClass())) {
                // Connection failure, try failover ?

            }
            logger.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void release() {}
}
