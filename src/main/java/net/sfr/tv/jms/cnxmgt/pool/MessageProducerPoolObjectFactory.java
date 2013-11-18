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
package net.sfr.tv.jms.cnxmgt.pool;

import java.util.Set;
import javax.jms.JMSException;
import net.sfr.tv.jms.client.context.JndiServerDescriptor;
import net.sfr.tv.jms.client.context.OutboundJmsContext;
import net.sfr.tv.jms.cnxmgt.OutboundConnectionManager;
import net.sfr.tv.model.Credentials;
import org.apache.log4j.Logger;
import org.pacesys.kbop.IPoolObjectFactory;
import org.pacesys.kbop.PoolKey;

/**
 *
 * @author matthieu
 */
public class MessageProducerPoolObjectFactory implements IPoolObjectFactory<String,OutboundJmsContext> {

    private static final Logger LOGGER = Logger.getLogger(MessageProducerPoolObjectFactory.class.getName());
    
    private OutboundConnectionManager connectionManager;
    
    public MessageProducerPoolObjectFactory(String name, Set<JndiServerDescriptor> servers, String preferredServer, String clientId, String cnxFactoryJndiName, Credentials credentials) {
        connectionManager = new OutboundConnectionManager(name, servers, preferredServer, clientId, cnxFactoryJndiName, credentials);
    }
    
    @Override
    public OutboundJmsContext create(PoolKey<String> pk) {
        return connectionManager.createProducer(pk.get());
    }

    @Override
    public void activate(OutboundJmsContext v) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void passivate(OutboundJmsContext v) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void destroy(OutboundJmsContext v) {
        try {
            v.getProducer().close();
        } catch (JMSException ex) {
            LOGGER.error("Unable to properly close MessageProducer !", ex);
        }
    }

    
}
