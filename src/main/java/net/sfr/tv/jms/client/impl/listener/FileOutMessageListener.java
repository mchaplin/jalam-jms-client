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
package net.sfr.tv.jms.client.impl.listener;

import java.io.File;
import java.nio.ByteBuffer;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import net.sfr.tv.jms.client.impl.AbstractMessageConsumer;
import org.apache.log4j.Logger;
import org.hornetq.core.journal.SequentialFile;
import org.hornetq.core.journal.impl.NIOSequentialFileFactory;

/**
 * A simple message listener, printing message contents to file passed by -Dlistener.file.output
 * 
 * @author matthieu.chaplin@sfr.com
 */
public class FileOutMessageListener extends AbstractMessageConsumer implements MessageListener {

    private final Logger logger = Logger.getLogger(FileOutMessageListener.class);
    
    private final String fileName;
    
    private final SequentialFile out;
    
    public FileOutMessageListener(final String[] destinations) throws Exception {
        
        super(destinations);
        
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
            logger.warn("Unable to properly close " + fileName, ex);
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
            logger.error(ex.getMessage(), ex);
        }
    }
}