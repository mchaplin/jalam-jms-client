package net.sfr.tv.jms.client.api;

import javax.jms.MessageListener;

/**
 * A named MessageListener, with complementary lifecycle methods.
 * 
 * Those can be released upon shutdown by implementing the release() method.
 * 
 * @author matthieu.chaplin@sfr.com.chaplin@sfr.com
 */

public interface MessageListenerWrapper extends MessageListener {

    /**
     * Returns instance name
     * 
     * @return 
     */
    public String getName();
    
    /**
     * Set instance name
     * 
     * @param value 
     */
    public void setName(String value);
    
    /**
     * Call upon client shutdown, override to free any resource.
     */
    public void release();
}
