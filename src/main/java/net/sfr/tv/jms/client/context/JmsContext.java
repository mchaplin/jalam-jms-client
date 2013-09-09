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

import java.util.HashSet;
import java.util.Set;
import javax.jms.Connection;
import javax.jms.Session;
import javax.naming.Context;

/**
 * A JMS context resources : References/wraps :
 * <ul>
 *  <li> JNDI context
 *  <li> JMS connection and session
 *  <li> A set of active subscriptions
 * </ul>.
 * 
 * @see net.sfr.tv.jms.client.JmsContextFactory
 * @see net.sfr.tv.jms.client.JmsClient#start(java.lang.String, java.lang.String, boolean, java.lang.String, java.lang.String, net.sfr.tv.jms.client.JmsContextFactory, java.lang.String)
 * 
 * @author matthieu.chaplin@sfr.com
 */
public class JmsContext {

    private Context jndiContext;
    private Connection cnx = null;
    private Session session;
    private Set<JmsSubscription> subscriptions = null;
    
    public JmsContext(Context jndiContext, Connection cnx, Session session) {
        this.jndiContext = jndiContext;
        this.cnx = cnx;
        this.session = session;
        //this.subscriptions = subscriptions;
    }

    public Context getJndiContext() {
        return jndiContext;
    }

    public void setJndiContext(Context jndiContext) {
        this.jndiContext = jndiContext;
    }
    
    public Connection getConnection() {
        return cnx;
    }
    
    public Session getSession() {
        return session;
    }

    public void addSubscription(JmsSubscription subscription) {
        if (subscriptions == null) {
            subscriptions = new HashSet<JmsSubscription>();
        }
        subscriptions.add(subscription);
    }
    
    public Set<JmsSubscription> getSubscriptions() {
        return subscriptions;
    }
}
