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

/**
 * JNDI server URL.
 * 
 * @author matthieu.chaplin@sfr.com.chaplin@sfr.com
 */
public class JndiServerDescriptor {
 
    private String host;
    
    private Integer port;
    
    public JndiServerDescriptor(String host, Integer port) {
        this.host = host;
        this.port = port;
    }
    
    /**
     * 
     * @return 
     */
    public String getProviderUrl() {
        return "jnp://".concat(host).concat(":").concat(port.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (JndiServerDescriptor.class.isAssignableFrom(obj.getClass())) {
            return ((JndiServerDescriptor) obj).getProviderUrl().compareTo(getProviderUrl()) == 0 ? true : false;
        } else {
            return super.equals(obj);
        }
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (host != null ? host.hashCode() : 0) + (port != null ? port.hashCode() : 0);
        return hash;
    }
}