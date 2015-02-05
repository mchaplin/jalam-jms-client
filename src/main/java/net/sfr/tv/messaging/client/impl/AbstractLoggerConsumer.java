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

import net.sfr.tv.messaging.impl.MessageConsumerImpl;
import org.apache.log4j.Logger;

/**
 *
 * @author matthieu.chaplin@sfr.com
 */
public abstract class AbstractLoggerConsumer extends MessageConsumerImpl {
 
    protected Logger logger;
    
    protected final String outputType;
        
    protected final String outputProperty;
    
    public AbstractLoggerConsumer(final String[] destinations) {
        super(destinations);
        String loggerName = System.getProperty("listener.logger.name");
        if (loggerName != null && loggerName.trim().length() > 0) {
            logger = Logger.getLogger(loggerName);
        } else {
            logger = Logger.getLogger(AbstractLoggerConsumer.class);
        }
        
        outputType = System.getProperty("listener.output.type", "FULL");
        outputProperty = System.getProperty("listener.output.property");
    }
    
}
