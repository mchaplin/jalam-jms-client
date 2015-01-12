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
package net.sfr.tv.jms.client.impl.listener;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import java.util.concurrent.TimeUnit;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import net.sfr.tv.jms.client.impl.AbstractMessageConsumer;
import org.apache.log4j.Logger;

/**
 *
 * @author matthieu
 */
public class ThroughputMessageListener extends AbstractMessageConsumer implements MessageListener {
    
    private static final Logger logger = Logger.getLogger(ThroughputMessageListener.class.getName());
    
    private static final MetricRegistry metrics = new MetricRegistry();
    
    private final Meter messagesMeter = metrics.meter("Throughput");
    
    public ThroughputMessageListener(final String[] destinations) {
        super(destinations);
        
        final ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .build();
        reporter.start(5, TimeUnit.SECONDS);
    }

    @Override
    public void onMessage(Message msg) {
        messagesMeter.mark();
        try { msg.acknowledge(); } catch (JMSException e) {logger.error(msg, e);};
    }
}
