package org.apache.kafka.metrics.elastic;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.MetricPredicate;
import com.yammer.metrics.reporting.AbstractPollingReporter;
import kafka.metrics.KafkaMetricsReporter;
import kafka.utils.VerifiableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class KafkaElasticMetricsReporter implements KafkaElasticMetricsReporterMBean, KafkaMetricsReporter {

    private static final Logger logger = LoggerFactory.getLogger(KafkaElasticMetricsReporter.class);

    //(kafka\.server\.FetcherStats.*ConsumerFetcherThread.*)|(kafka\.consumer\.FetchRequestAndResponseMetrics.*)|(kafka\.server\.FetcherLagMetrics\..*)|
    public static final String DEFAULT_EXCLUDE_REGEX = "(.*ReplicaFetcherThread.*)|(kafka\\.logger\\.Log\\..*)|(kafka\\.cluster\\.Partition\\..*)";

    private boolean enabled;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private String host;
    private int port;
    private String prefix;

    private long pollingPeriodInSeconds;
    private MetricPredicate metricPredicate;
    private boolean isTagEnabled;

    private ElasticSearchReporter elasticSearchReporter;

    private AbstractPollingReporter underlying = null;

    private String index;

    public String getMBeanName() {
        return "kafka:type=" + getClass().getName();
    }

    public boolean isRunning() {
        return running.get();
    }

    public synchronized void init(VerifiableProperties props) {
        loadConfig(props);
        if (enabled) {
            logger.info("Reporter is enabled and starting...");
            startReporter(pollingPeriodInSeconds);
        } else {
            logger.warn("Reporter is disabled");
        }
    }

    /**
     * load config first
     * @param props
     */
    private void loadConfig(VerifiableProperties props) {
        enabled = props.getBoolean("external.kafka.elastic.reporter.enabled", false);

        host = props.getString("external.kafka.elastic.host", "192.68.99.100");
        port = props.getInt("external.kafka.elastic.port", 9200);
        prefix = props.getString("external.kafka.elastic.metrics.prefix", "");
        pollingPeriodInSeconds = props.getInt("kafka.metrics.polling.interval.secs", 10);
        index = props.getString("external.kafka.elastic.metrics.index", "metrics");

        String excludeRegex = props.getString("external.kafka.elastic.metrics.exclude_regex", DEFAULT_EXCLUDE_REGEX);
        if (excludeRegex != null && excludeRegex.length() != 0) {
            metricPredicate = new ExcludeMetricPredicate(excludeRegex);
        } else {
            metricPredicate = MetricPredicate.ALL;
        }

        this.isTagEnabled = props.getBoolean("external.kafka.elastic.tag.enabled", true);
    }

    public void startReporter(long pollingPeriodInSeconds) {
        if (pollingPeriodInSeconds <= 0) {
            throw new IllegalArgumentException("Polling period must be greater than zero");
        }

        synchronized (running) {
            if (running.get()) {
                logger.warn("Reporter is already running");
            } else {
                ElasticSearchReporter.Builder builder = createElasticsearchReporterBuilder();
                try {
                    elasticSearchReporter = builder.build();
                } catch (IOException e) {
                    logger.warn("Reporter is failed");
                    return;
                }
                underlying = new ElasticSearchMetricsReporter(Metrics.defaultRegistry(), elasticSearchReporter, metricPredicate, isTagEnabled);
                underlying.start(pollingPeriodInSeconds, TimeUnit.SECONDS);
                logger.info("Started Reporter with host={}, polling_period_secs={}, prefix={}", host, pollingPeriodInSeconds, prefix);
                running.set(true);
            }
        }
    }

    public void stopReporter() {
        if (!enabled) {
            logger.warn("Reporter is disabled");
        } else {
            synchronized (running) {
                if (running.get()) {
                    //underlying.shutdown();
                    running.set(false);
                    logger.info("Stopped Reporter with host={}", host);
                } else {
                    logger.warn("Reporter is not running");
                }
            }
        }
    }


    private ElasticSearchReporter.Builder createElasticsearchReporterBuilder() {
        Map<String, Object> additionalFields = new HashMap<>();
        String hostName;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
            additionalFields.put("host", hostName);
        } catch (UnknownHostException e) {
            logger.info("Unknown hosts");
        }
        return ElasticSearchReporter.forRegistry(Metrics.defaultRegistry())
                .hosts(host + ":" + port)
                .prefixedWith(prefix)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .index(index)
                .additionalFields(additionalFields)
                .timeout(100);
    }
}
