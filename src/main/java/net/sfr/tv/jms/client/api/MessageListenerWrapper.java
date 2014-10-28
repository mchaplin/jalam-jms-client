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

import javax.jms.MessageListener;

/**
 * A named MessageListener, with additional lifecycle methods.
 * 
 * Those can be released upon shutdown by implementing the release() method.
 * 
 * @author matthieu.chaplin@sfr.com
 */
public interface MessageListenerWrapper extends MessageListener {

    /**
     * Returns instance name
     * 
     * @return Name
     */
    public String getName();
    
    /**
     * Set instance name
     * 
     * @param name  Name 
     */
    public void setName(String name);
    
    /**
     * Call upon client shutdown, override to free any resource.
     */
    public void release();
}
