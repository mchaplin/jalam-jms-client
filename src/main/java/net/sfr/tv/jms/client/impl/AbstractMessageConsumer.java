/*
 * Copyright 2014 matthieu.
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
package net.sfr.tv.jms.client.impl;

import net.sfr.tv.messaging.api.MessageConsumer;

/**
 *
 * @author matthieu
 */
public abstract class AbstractMessageConsumer implements MessageConsumer {
    
    protected String name;
    
    protected final String[] destinations;
    
    public AbstractMessageConsumer(final String[] destinations) {
        this.name = getClass().getName();
        this.destinations = destinations;
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String[] getDestinations() {
        return destinations;
    }

    @Override
    public void release() {}
}
