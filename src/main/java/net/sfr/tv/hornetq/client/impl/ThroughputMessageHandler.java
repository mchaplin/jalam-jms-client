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

import net.sfr.tv.messaging.client.impl.ThroughputConsumerImpl;
import org.apache.log4j.Logger;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.MessageHandler;

/**
 *
 * @author matthieu.chaplin@sfr.com
 */
public class ThroughputMessageHandler extends ThroughputConsumerImpl implements MessageHandler {
    
    private static final Logger logger = Logger.getLogger(ThroughputMessageHandler.class.getName());

    @Override
    public void onMessage(ClientMessage cm) {
        messagesMeter.mark();
        try { cm.acknowledge(); } catch (HornetQException e) {logger.error(cm, e);};
    }
    
    
}
