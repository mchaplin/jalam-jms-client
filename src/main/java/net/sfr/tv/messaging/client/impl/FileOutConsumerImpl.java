/*
 * Copyright 2015 matthieu.chaplin@sfr.com.
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
package net.sfr.tv.messaging.client.impl;

import java.io.File;
import net.sfr.tv.messaging.api.MessageConsumer;
import org.apache.log4j.Logger;
import org.hornetq.core.journal.SequentialFile;
import org.hornetq.core.journal.impl.NIOSequentialFileFactory;

/**
 *
 * @author matthieu.chaplin@sfr.com
 */
public abstract class FileOutConsumerImpl implements MessageConsumer {
 
    protected final Logger logger = Logger.getLogger(FileOutConsumerImpl.class);
    
    protected final String fileName;
    
    protected final SequentialFile out;
    
    public FileOutConsumerImpl() throws Exception {
        
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
}