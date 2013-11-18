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

import java.util.HashSet;
import java.util.Set;
import javax.jms.Connection;
import javax.jms.Session;
import javax.naming.Context;

/**
 *
 * @author matthieu
 */
public class InboundJmsContext extends JmsContext {
    
    private Set<JmsSubscription> subscriptions = null;
    
    public InboundJmsContext(Context jndiContext, Connection cnx, Session session) {
        super(jndiContext, cnx, session);
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
