package net.sfr.tv.jms.client.listener;

import net.sfr.tv.jms.client.api.MessageListenerWrapper;
import java.io.File;
import java.nio.ByteBuffer;
import javax.jms.Message;
import javax.jms.TextMessage;
import org.apache.log4j.Logger;
import org.hornetq.core.journal.SequentialFile;
import org.hornetq.core.journal.impl.NIOSequentialFileFactory;

/**
 * A simple message listener, printing message contents to file passed by -Dlistener.file.output
 * 
 * @author matthieu.chaplin@sfr.com
 */
public class FileOutMessageListener implements MessageListenerWrapper {

    private final Logger LOGGER = Logger.getLogger(FileOutMessageListener.class);
    
    private String name;
    
    private String fileName;
    
    private SequentialFile out;
    
    public FileOutMessageListener() throws Exception {
        
        name = FileOutMessageListener.class.getName();
        
        fileName = System.getProperty("listener.file.output", "");
        File wrapper = new File(fileName);
        
        NIOSequentialFileFactory fileFactory = new NIOSequentialFileFactory(wrapper.getParent());
        out = fileFactory.createSequentialFile(wrapper.getName(), 1); // MAX IO VALUE FOR NIO IS 1
        
        out.open();
        
    }
    
    @Override
    public void release() {
        try {
            out.close();
        } catch (Exception ex) {
            LOGGER.warn("Unable to properly close " + fileName, ex);
        } 
    }
    
    @Override
    public void onMessage(Message msg) {
        
        ByteBuffer bb;
        String body;
        
        try {

            body = "hq-message-id=" + msg.getStringProperty("hq-message-id") + ";";
            
            if (TextMessage.class.isAssignableFrom(msg.getClass())) {
                body += ((TextMessage) msg).getText();
                //body += System.lineSeparator();
                body += "\n";
                bb = ByteBuffer.wrap(body.getBytes());
                
                out.writeDirect(bb, false);
            }

            // ACK the message to remove it from the queue.
            msg.acknowledge();

        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String value) {}
}