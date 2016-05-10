package org.apache.kafka.metrics.elastic;

import com.yammer.metrics.core.MetricName;

import java.util.Map;

import static org.apache.kafka.metrics.elastic.MetricNameFormatter.formatWithScope;

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
