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

import java.util.Hashtable;
import java.util.concurrent.Callable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import net.sfr.tv.jms.client.context.JndiServerDescriptor;
import org.apache.log4j.Logger;

/**
 *
 * @author matthieu.chaplin@sfr.com
 */
public class JndiLookupTask implements Callable<Context> {

    private static final Logger LOGGER = Logger.getLogger(JndiLookupTask.class);
    
    private JndiServerDescriptor jndiServer;
    
    public JndiLookupTask(JndiServerDescriptor jndiServer) {
        this.jndiServer = jndiServer;
    }
    
    @Override
    public Context call() throws Exception {
        
        // Set up JNDI context & lookup destination. Interrupt in case of failure.
        Context ctx = null;
        
        Hashtable props = new Hashtable(3);
        props.put(Context.PROVIDER_URL, jndiServer.getProviderUrl());
        props.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
        try {
            ctx = new InitialContext(props);
        } catch (NamingException ex) {
            LOGGER.error("JNDI Context initialization failure ! ", ex);
        }
        return ctx;
    }
    
}
