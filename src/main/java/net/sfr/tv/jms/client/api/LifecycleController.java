/**
 * Copyright 2012-2014 - SFR (http://www.sfr.com/)
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
package net.sfr.tv.jms.client.api;

import java.util.Collection;
import net.sfr.tv.exceptions.ResourceInitializerException;

/**
 * Wrap a @see javax.jms.MessageListener.
 * 
 * Allows : 
 * <ul>
 *  <li> Extra service initialization
 *  <li> Multiple parallel listeners
 * </ul>
 * 
 * @author matthieu.chaplin@sfr.com
 */
public interface LifecycleController {

    /**
     * Starts the message listener
     * Called by the main client upon startup.
     */
    public void run();
    
    /**
     * Release listener resources
     * Called by the main client upon shutdown.
     */
    public void release();
    
    /**
     * Get all the registered listeners
     * 
     * @return 
     */
    public Collection<MessageListenerWrapper> getListeners();
    
    /**
     * Returns the wrapped listener
     * 
     * @param listenerClass
     * @return 
     * 
     * @throws ResourceInitializerException
     */
    public MessageListenerWrapper getListener(Class listenerClass) throws ResourceInitializerException;
 
    /**
     * Register a new Listener
     * 
     * @param   listenerClass   Class of the listener, implementing MessageListenerWrapper
     * @param   destinations    Destinations to bind to
     * @see     net.sfr.tv.jms.client.api.MessageListenerWrapper
     * 
     * @throws ResourceInitializerException
     */
    //public void registerListener(Class listenerClass, final String[] destinations) throws ResourceInitializerException;
    
}
