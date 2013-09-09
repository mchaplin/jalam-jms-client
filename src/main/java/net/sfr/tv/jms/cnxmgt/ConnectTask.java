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
import javax.jms.Connection;
import javax.jms.Session;
import javax.naming.Context;
import net.sfr.tv.exceptions.ResourceInitializerException;
import net.sfr.tv.jms.client.context.JmsContext;
import org.apache.log4j.Logger;
import org.hornetq.api.core.client.SessionFailureListener;
import org.hornetq.jms.client.HornetQConnection;
import org.hornetq.jms.client.HornetQSession;

/**
 *
 * @author matthieu.chaplin@sfr.com
 */
public class ConnectTask implements Callable<JmsContext> {
    
    private static final Logger LOGGER = Logger.getLogger(ConnectTask.class);
    
    private Context jndiContext;
    
    private String clientId;
    
    private String connectionFactory;
    
    private SessionFailureListener sl;
    
    public ConnectTask(Context jndiContext, String clientId, String connectionFactory, SessionFailureListener sl) {
        this.jndiContext = jndiContext;
        this.clientId = clientId;
        this.connectionFactory = connectionFactory;
        this.sl = sl;
    }
    
    @Override
    public JmsContext call() throws Exception {
        
        LOGGER.info("Trying to connect to ".concat(jndiContext.toString()));
        
        Connection cnx;
        Session session;
        
        try {
                    
            cnx = ContextFactory.getConnection(jndiContext, clientId, connectionFactory);
            LOGGER.info("HornetQ connection UID : " + ((HornetQConnection) cnx).getUID());

            session = ContextFactory.createSession(cnx);
            ((HornetQSession) session).getCoreSession().addFailureListener(sl);
          
            return new JmsContext(jndiContext, cnx, session);
            
        } catch (ResourceInitializerException rie) {
            LOGGER.error("Error while attempting to create a JMS connection/subscription : ".concat(rie.getMessage()), rie);
            return null;
        } 
    }
}