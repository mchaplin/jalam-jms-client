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
package net.sfr.tv.jms.client.context;

import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.Context;

/**
 *
 * @author matthieu
 */
public class OutboundJmsContext extends JmsContext {
    
    private MessageProducer producer;
    
    public OutboundJmsContext(Context jndiContext, Connection cnx, Session session, MessageProducer producer) {
        super(jndiContext, cnx, session);
        this.producer = producer;
    }

    public MessageProducer getProducer() {
        return producer;
    }
}
