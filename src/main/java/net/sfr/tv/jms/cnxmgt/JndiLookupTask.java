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
import javax.naming.Context;
import net.sfr.tv.jms.client.context.JndiServerDescriptor;

/**
 *
 * @author matthieu.chaplin@sfr.com
 */
public class JndiLookupTask implements Callable<Context> {

    private JndiServerDescriptor jndiServer;
    
    public JndiLookupTask(JndiServerDescriptor jndiServer) {
        this.jndiServer = jndiServer;
    }
    
    @Override
    public Context call() throws Exception {
        return ContextFactory.initializeJndiContext(jndiServer);
    }
}
