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
package net.sfr.tv.jms.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import net.sfr.tv.jms.model.JndiServerDescriptor;
import net.sfr.tv.model.Credentials;
import org.apache.log4j.Logger;

/**
 *
 * @author matthieu
 */
public class JndiProviderConfiguration {
 
    private static final Logger LOGGER = Logger.getLogger(JndiProviderConfiguration.class.getName());
    
    private Credentials credentials;
    
    private Set<String> groups = new HashSet<>();
    
    private Map<String, Set<JndiServerDescriptor>> serversGroups;
    
    public JndiProviderConfiguration(Properties props, String prefix) {
        
        String[] sGroups = props.getProperty("config.groups", "").split("\\,");

        String serverAlias;
        Set<String> keys = props.stringPropertyNames();
        
        JndiServerDescriptor server;

        serversGroups = new HashMap<String, Set<JndiServerDescriptor>>();
        
        for (String group : sGroups) {
            LOGGER.debug("Group : " + group);
            groups.add(group);
            
            group = !group.equals("") ? group : "default";
            group = prefix != null && prefix.trim().length() != 0 ? prefix.concat(".").concat(group) : group;
            
            for (String key : keys) {
                if (key.startsWith(group)) {
                    serverAlias = key.startsWith("jms.server") ? key.split("\\.")[2] : key.split("\\.")[3];
                    LOGGER.debug("Server alias : " + serverAlias);

                    if (serversGroups.get(group) == null) {
                        serversGroups.put(group, new HashSet<JndiServerDescriptor>());
                    }
                    server = new JndiServerDescriptor(
                        serverAlias,
                        props.getProperty(group.concat(".jms.server.").concat(serverAlias).concat(".host")),
                        Integer.valueOf(props.getProperty(group.concat(".jms.server.").concat(serverAlias).concat(".port"))));

                    serversGroups.get(group).add(server);
                    LOGGER.info(server.toString());
                }
            }
        }

        credentials = new Credentials(props.getProperty("jms.login", "guest"), props.getProperty("jms.password", "guest"));   
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public Map<String, Set<JndiServerDescriptor>> getServersGroup() {
        return serversGroups;
    }
    
    public Set<JndiServerDescriptor> getServersGroup(String name) {
        return serversGroups.get(name);
    }

    public Set<String> getGroups() {
        return groups;
    }
}
