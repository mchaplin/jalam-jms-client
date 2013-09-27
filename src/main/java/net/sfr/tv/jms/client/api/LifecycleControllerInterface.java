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
package net.sfr.tv.jms.client.api;

import java.util.Collection;
import net.sfr.tv.exceptions.ResourceInitializerException;

/**
 * Wrap a @see javax.jms.MessageListener.
 * 
 * Allows : 
 * <ul>
 *  <li> Extra service initialization
 *  <li> TODO : Multiple parallel listeners
 * </ul>
 * 
 * @author matthieu.chaplin@sfr.com
 */
public interface LifecycleControllerInterface {

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
     * Creates and return a wrapped listener.
     * 
     * @param listenerClass
     * @return
     * @throws Exception 
     */
    //public ResourceHolderMessageListener createListener(Class listenerClass) throws ResourceInitializerException;
    
    
    public Collection<MessageListenerWrapper> getListeners();
    
    /**
     * Returns the wrapped listener
     * 
     * @return 
     */
    public MessageListenerWrapper getListener(Class listenerClass) throws ResourceInitializerException;
    
}
