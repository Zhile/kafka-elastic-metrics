package org.apache.kafka.metrics.elastic;

import com.yammer.metrics.core.MetricName;

import java.util.Map;

import static org.apache.kafka.metrics.elastic.MetricNameFormatter.formatWithScope;

/**
 * Created by dzhou on 2/20/16.
 */
public class ParserForNoTag extends Parser {

    public static final String[] EMPTY_TAG = new String[]{};

    @Override
    public void parse(MetricName metricName) {
        name = formatWithScope(metricName);
        tags = EMPTY_TAG;
    }

    @Override
    public Map<String, Object> getTagMap() {
        return null;
    }
}
