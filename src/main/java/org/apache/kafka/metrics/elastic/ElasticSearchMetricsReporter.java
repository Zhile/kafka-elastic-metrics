package org.apache.kafka.metrics.elastic;

import com.yammer.metrics.core.*;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.reporting.AbstractPollingReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by jiyongwang on 2/25/16.
 */
public class ElasticSearchMetricsReporter extends AbstractPollingReporter implements MetricProcessor<Long> {
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchReporter.class);
    private static final String REPORTER_NAME = "kafka-elastic-metrics";
    private final ElasticSearchReporter elasticSearchReporter;

    private final Clock clock;
    private MetricPredicate metricPredicate;
    private boolean isTagEnabled;
    private Parser parser;

    private SortedMap<MetricName, Gauge> gauges;
    private SortedMap<MetricName, Counter> counters;
    private SortedMap<MetricName, Histogram> histograms;
    private SortedMap<MetricName, Meter> meters;
    private SortedMap<MetricName, Timer> timers;

    private Map<String, Map<String, Object>> tags;

    public ElasticSearchMetricsReporter(MetricsRegistry metricsRegistry,
                                        ElasticSearchReporter elasticSearchReporter,
                                        String reportName,
                                        MetricPredicate metricPredicate,
                                        boolean isTagEnabled) {
        super(metricsRegistry, reportName);
        this.elasticSearchReporter = elasticSearchReporter;
        this.clock = Clock.defaultClock();
        this.metricPredicate = metricPredicate;
        this.isTagEnabled = isTagEnabled;
        this.parser = null;

        gauges = new TreeMap<>();
        counters = new TreeMap<>();
        histograms = new TreeMap<>();
        meters = new TreeMap<>();
        timers = new TreeMap<>();

        tags = new HashMap<>();

    }

    public ElasticSearchMetricsReporter(MetricsRegistry metricsRegistry,
                                        ElasticSearchReporter elasticSearchReporter) {
        this(metricsRegistry, elasticSearchReporter, REPORTER_NAME, MetricPredicate.ALL, true);
    }

    public ElasticSearchMetricsReporter(MetricsRegistry metricsRegistry,
                                        ElasticSearchReporter elasticSearchReporter,
                                        MetricPredicate metricPredicate) {
        this(metricsRegistry, elasticSearchReporter, REPORTER_NAME, metricPredicate, true);
    }

    public ElasticSearchMetricsReporter(MetricsRegistry metricsRegistry,
                                        ElasticSearchReporter elasticSearchReporter,
                                        MetricPredicate metricPredicate,
                                        boolean isTagEnabled) {
        this(metricsRegistry, elasticSearchReporter, REPORTER_NAME, metricPredicate, isTagEnabled);
    }
    @Override
    public void run() {
        try {
            final long epoch = clock.time() / 1000;
            if (parser == null) {
                createParser(getMetricsRegistry());
            }
            sendAllKafkaMetrics(epoch);
        } catch (RuntimeException ex) {
            logger.error("Failed to print metrics to elastic search", ex);
        }
    }

    private void createParser(MetricsRegistry metricsRegistry) {
        if (isTagEnabled) {
            final boolean isMetricsTagged = isTagged(metricsRegistry.allMetrics());
            if (isMetricsTagged) {
                logger.info("Kafka metrics are tagged");
                parser = new ParserForTagInMBeanName();
            } else {
                parser = new ParserForNoTag();
            }
        } else {
            parser = new ParserForNoTag();
        }
    }

    public boolean isTagged(Map<MetricName, Metric> metrics) {
        for (MetricName metricName : metrics.keySet()) {
            if ("kafka.common:type=AppInfo,name=Version".equals(metricName.getMBeanName())
                    || metricName.hasScope()) {
                return true;
            }
        }
        return false;
    }

    private void sendAllKafkaMetrics(long epoch) {
        final Map<MetricName, Metric> allMetrics = new TreeMap<MetricName, Metric>(getMetricsRegistry().allMetrics());
        clearAllMaps();
        for (Map.Entry<MetricName, Metric> entry : allMetrics.entrySet()) {
            processMetric(entry.getKey(), entry.getValue(), epoch);
        }
        elasticSearchReporter.report(gauges, counters, histograms, meters, timers, parser);
    }

    private void processMetric(MetricName metricName, Metric metric, long epoch) {
        if (logger.isDebugEnabled() && metricName != null) {
            logger.debug("  MBeanName[{}], Group[{}], Name[{}], Scope[{}], Type[{}]",
                    metricName.getMBeanName(), metricName.getGroup(), metricName.getName(),
                    metricName.getScope(), metricName.getType());
        }

        //parser.parse(metricName);
        if (metricPredicate.matches(metricName, metric) && metric != null) {
            try {
                metric.processWith(this, metricName, epoch);
            } catch (Exception ignored) {
                logger.error("Error printing regular metrics:", ignored);
            }
        }
    }

    public void processCounter(MetricName metricName, Counter counter, Long epoch) throws Exception {
        counters.put(metricName, counter);
    }


    public void processMeter(MetricName metricName, Metered meter, Long epoch) {
        if (meter instanceof Meter) {
            meters.put(metricName, (Meter) meter);
        }
    }


    public void processHistogram(MetricName metricName, Histogram histogram, Long epoch) throws Exception {
        histograms.put(metricName, histogram);
    }


    public void processTimer(MetricName metricName, Timer timer, Long epoch) throws Exception {
        timers.put(metricName, timer);
    }


    public void processGauge(MetricName metricName, Gauge<?> gauge, Long context) throws Exception {
        gauges.put(metricName, gauge);
    }

    public void clearAllMaps() {
        gauges.clear();
        counters.clear();
        histograms.clear();
        meters.clear();
        timers.clear();
        tags.clear();
    }

}
