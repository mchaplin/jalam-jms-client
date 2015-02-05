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
package net.sfr.tv.jms.client.impl.listener;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import net.sfr.tv.messaging.impl.MessageConsumerImpl;
import org.apache.log4j.Logger;

/**
 *
 * @author matthieu.chaplin@sfr.com
 */
public class LatencyListener extends MessageConsumerImpl implements MessageListener {
    
    private static final Logger logger = Logger.getLogger(ThroughputMessageListener.class.getName());
    
    private static final MetricRegistry metrics = new MetricRegistry();
    
    private final Histogram histogram = metrics.histogram("Latency");
    
    public LatencyListener(final String[] destinations) {
        super(destinations);
        
        final ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .build();
        reporter.start(5, TimeUnit.SECONDS);
    }

    @Override
    public void onMessage(Message msg) {
        
        TextMessage tm = (TextMessage) msg;
        long origStamp = 0;
        try { 
            
            String[] keyVals = tm.getText().split("\\;");
            for (String kv : keyVals) {
                if (kv.startsWith("ts=")) {
                    origStamp = Long.valueOf(kv.split("\\=")[1]);
                }
            }
            
            histogram.update(new Date().getTime() - origStamp);
            
            msg.acknowledge();
        } catch (JMSException e) {logger.error(msg, e);};
    }
    
}
