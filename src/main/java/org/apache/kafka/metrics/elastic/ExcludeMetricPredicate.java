package org.apache.kafka.metrics.elastic;

import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Created by dzhou on 2/20/16.
 */
public class ExcludeMetricPredicate implements MetricPredicate {

    private final Logger logger = LoggerFactory.getLogger(ExcludeMetricPredicate.class);

    final String excludeRegex;
    final Pattern pattern;

    public ExcludeMetricPredicate(String excludeRegex) {
        this.excludeRegex = excludeRegex;
        this.pattern = Pattern.compile(excludeRegex);
    }

    public boolean matches(MetricName name, Metric metric) {
        String n = MetricNameFormatter.format(name);
        boolean excluded = pattern.matcher(n).matches();
        if (excluded) {
            if (logger.isTraceEnabled()) {
                logger.trace("Metric " + n + " is excluded");
            }
        }
        return !excluded;
    }
}
