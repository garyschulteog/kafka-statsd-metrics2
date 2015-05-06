/**
 * Copyright (C) 2014-2015 Alexis Midon alexis.midon@airbnb.com
 * Copyright (C) 2012-2013 Sean Laurent
 * Copyright (C) 2013 metrics-statsd contributors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.airbnb.kafka;


import com.airbnb.metrics.MetricDimensionOptions;
import com.timgroup.statsd.StatsDClient;
import com.yammer.metrics.core.*;
import com.yammer.metrics.reporting.AbstractPollingReporter;
import com.yammer.metrics.stats.Snapshot;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class DatadogReporterTest {

    private static final String METRIC_BASE_NAME = "java.lang.Object.metric";
    @Mock
    private Clock clock;
    @Mock
    private StatsDClient statsD;
    private AbstractPollingReporter reporter;
    private TestMetricsRegistry registry;

    protected static class TestMetricsRegistry extends MetricsRegistry {
        public <T extends Metric> T add(MetricName name, T metric) {
            return getOrAdd(name, metric);
        }
    }

    @Before
    public void init() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(clock.tick()).thenReturn(1234L);
        when(clock.time()).thenReturn(5678L);
        registry = new TestMetricsRegistry();
        reporter = new DatadogReporter(registry,
                statsD,
                MetricDimensionOptions.ALL_ENABLED
        );
    }

    @Test
    public void isTaggedTest() {
        registry.add(new MetricName("kafka.common", "AppInfo", "Version", null, "kafka.common:type=AppInfo,name=Version"),
                new Gauge<String>() {
                    public String value() {
                        return "0.8.2";
                    }
                });
        assertTrue(((DatadogReporter) reporter).isTagged(registry.allMetrics()));
    }

    protected <T extends Metric> void addMetricAndRunReporter(Callable<T> action) throws Exception {
        // Invoke the callable to trigger (ie, mark()/inc()/etc) and return the metric
        final T metric = action.call();
        try {
            // Add the metric to the registry, run the reporter and flush the result
            registry.add(new MetricName(Object.class, "metric"), metric);
            reporter.run();
        } finally {
            reporter.shutdown();
        }
    }

    private void verifySend(String metricNameSuffix, double metricValue) {
        verify(statsD).gauge(METRIC_BASE_NAME + "." + metricNameSuffix, metricValue, null);
    }

    private void verifySend(double metricValue) {
        verify(statsD).gauge(METRIC_BASE_NAME, metricValue, null);
    }

    private void verifySend(long metricValue) {
        verify(statsD).gauge(METRIC_BASE_NAME, metricValue, null);
    }

    private void verifySend(String metricNameSuffix, String metricValue) {
        verify(statsD).gauge(METRIC_BASE_NAME + "." + metricNameSuffix, Double.valueOf(metricValue), null);
    }

    public void verifyTimer() {
        verifySend("samples", "1");
        verifySend("meanRate", "2.00");
        verifySend("1MinuteRate", "1.00");
        verifySend("5MinuteRate", "5.00");
        verifySend("15MinuteRate", "15.00");
        verifySend("min", "1.00");
        verifySend("max", "3.00");
        verifySend("mean", "2.00");
        verifySend("stddev", "1.50");
        verifySend("median", "0.50");
        verifySend("75percentile", "0.7505");
        verifySend("95percentile", "0.9509");
        verifySend("98percentile", "0.98096");
        verifySend("99percentile", "0.99098");
        verifySend("999percentile", "0.999998");
    }

    public void verifyMeter() {
        verifySend("samples", 1);
        verifySend("meanRate", 2.00);
        verifySend("1MinuteRate", 1.00);
        verifySend("5MinuteRate", 5.00);
        verifySend("15MinuteRate", 15.00);
    }

    public void verifyHistogram() {
        verifySend("min", 1.00);
        verifySend("max", 3.00);
        verifySend("mean", 2.00);
        verifySend("stddev", 1.50);
        verifySend("median", 0.50);
        verifySend("75percentile", "0.7505");
        verifySend("95percentile", "0.9509");
        verifySend("98percentile", "0.98096");
        verifySend("99percentile", "0.99098");
        verifySend("999percentile", "0.999998");
    }

    public void verifyCounter(long count) {
        verifySend(count);
    }

    @Test
    public final void counter() throws Exception {
        final long count = new Random().nextInt(Integer.MAX_VALUE);
        addMetricAndRunReporter(
                new Callable<Counter>() {
                    @Override
                    public Counter call() throws Exception {
                        return createCounter(count);
                    }
                });
        verifyCounter(count);
    }

    @Test
    public final void histogram() throws Exception {
        addMetricAndRunReporter(
                new Callable<Histogram>() {
                    @Override
                    public Histogram call() throws Exception {
                        return createHistogram();
                    }
                });
        verifyHistogram();
    }

    @Test
    public final void meter() throws Exception {
        addMetricAndRunReporter(
                new Callable<Meter>() {
                    @Override
                    public Meter call() throws Exception {
                        return createMeter();
                    }
                });
        verifyMeter();
    }

    @Test
    public final void timer() throws Exception {
        addMetricAndRunReporter(
                new Callable<Timer>() {
                    @Override
                    public Timer call() throws Exception {
                        return createTimer();
                    }
                });
        verifyTimer();
    }

    @Test
    public final void longGauge() throws Exception {
        final long value = 0xdeadbeef;
        addMetricAndRunReporter(
                new Callable<Gauge<Object>>() {
                    @Override
                    public Gauge<Object> call() throws Exception {
                        return createGauge(value);
                    }
                });
        verifySend(value);
    }

    @Test
    public void stringGauge() throws Exception {
        final String value = "The Metric";
        addMetricAndRunReporter(
                new Callable<Gauge<Object>>() {
                    @Override
                    public Gauge<Object> call() throws Exception {
                        return createGauge(value);
                    }
                });
        verify(statsD, never()).gauge(Matchers.anyString(), Matchers.anyDouble());
    }

    static Counter createCounter(long count) throws Exception {
        final Counter mock = mock(Counter.class);
        when(mock.count()).thenReturn(count);
        return configureMatcher(mock, doAnswer(new MetricsProcessorAction() {
            @Override
            void delegateToProcessor(MetricProcessor<Object> processor, MetricName name, Object context) throws Exception {
                processor.processCounter(name, mock, context);
            }
        }));
    }

    static Histogram createHistogram() throws Exception {
        final Histogram mock = mock(Histogram.class);
        setupSummarizableMock(mock);
        setupSamplingMock(mock);
        return configureMatcher(mock, doAnswer(new MetricsProcessorAction() {
            @Override
            void delegateToProcessor(MetricProcessor<Object> processor, MetricName name, Object context) throws Exception {
                processor.processHistogram(name, mock, context);
            }
        }));
    }


    static Gauge<Object> createGauge(Object value) throws Exception {
        @SuppressWarnings("unchecked")
        final Gauge<Object> mock = mock(Gauge.class);
        when(mock.value()).thenReturn(value);
        return configureMatcher(mock, doAnswer(new MetricsProcessorAction() {
            @Override
            void delegateToProcessor(MetricProcessor<Object> processor, MetricName name, Object context) throws Exception {
                processor.processGauge(name, mock, context);
            }
        }));
    }


    static Timer createTimer() throws Exception {
        final Timer mock = mock(Timer.class);
        when(mock.durationUnit()).thenReturn(TimeUnit.MILLISECONDS);
        setupSummarizableMock(mock);
        setupMeteredMock(mock);
        setupSamplingMock(mock);
        return configureMatcher(mock, doAnswer(new MetricsProcessorAction() {
            @Override
            void delegateToProcessor(MetricProcessor<Object> processor, MetricName name, Object context) throws Exception {
                processor.processTimer(name, mock, context);
            }
        }));
    }

    static Meter createMeter() throws Exception {
        final Meter mock = mock(Meter.class);
        setupMeteredMock(mock);
        return configureMatcher(mock, doAnswer(new MetricsProcessorAction() {
            @Override
            void delegateToProcessor(MetricProcessor<Object> processor, MetricName name, Object context) throws Exception {
                processor.processMeter(name, mock, context);
            }
        }));
    }

    @SuppressWarnings("unchecked")
    static <T extends Metric> T configureMatcher(T mock, Stubber stub) throws Exception {
        stub.when(mock).processWith(any(MetricProcessor.class), any(MetricName.class), any());
        return mock;
    }

    static abstract class MetricsProcessorAction implements Answer<Object> {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            @SuppressWarnings("unchecked")
            final MetricProcessor<Object> processor = (MetricProcessor<Object>) invocation.getArguments()[0];
            final MetricName name = (MetricName) invocation.getArguments()[1];
            final Object context = invocation.getArguments()[2];
            delegateToProcessor(processor, name, context);
            return null;
        }

        abstract void delegateToProcessor(MetricProcessor<Object> processor, MetricName name, Object context) throws Exception;
    }

    static void setupSummarizableMock(Summarizable summarizable) {
        when(summarizable.min()).thenReturn(1d);
        when(summarizable.max()).thenReturn(3d);
        when(summarizable.mean()).thenReturn(2d);
        when(summarizable.stdDev()).thenReturn(1.5d);
    }

    static void setupMeteredMock(Metered metered) {
        when(metered.count()).thenReturn(1L);
        when(metered.oneMinuteRate()).thenReturn(1d);
        when(metered.fiveMinuteRate()).thenReturn(5d);
        when(metered.fifteenMinuteRate()).thenReturn(15d);
        when(metered.meanRate()).thenReturn(2d);
        when(metered.eventType()).thenReturn("eventType");
        when(metered.rateUnit()).thenReturn(TimeUnit.SECONDS);
    }

    static void setupSamplingMock(Sampling sampling) {  //be careful how snapshot defines statistics
        final double[] values = new double[1001];
        for (int i = 0; i < values.length; i++) {
            values[i] = i / 1000.0;
        }
        when(sampling.getSnapshot()).thenReturn(new Snapshot(values));
    }
}
