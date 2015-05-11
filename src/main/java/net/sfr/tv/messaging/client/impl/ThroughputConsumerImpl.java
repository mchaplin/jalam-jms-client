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

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import java.util.concurrent.TimeUnit;
import net.sfr.tv.messaging.api.MessageConsumer;

/**
 *
 * @author matthieu.chaplin@sfr.com
 */
public abstract class ThroughputConsumerImpl implements MessageConsumer {

    private static final MetricRegistry metrics = new MetricRegistry();
    
    private final ConsoleReporter reporter;
    
    protected final Meter messagesMeter = metrics.meter("Throughput");
    
    public ThroughputConsumerImpl() {
        
        reporter = ConsoleReporter.forRegistry(metrics)
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .build();
        reporter.start(5, TimeUnit.SECONDS);
    }
    
    @Override
    public void release() {
        reporter.stop();
        reporter.close();
    }
}
