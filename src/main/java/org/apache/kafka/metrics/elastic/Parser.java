package org.apache.kafka.metrics.elastic;

import com.yammer.metrics.core.MetricName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by dzhou on 2/20/16.
 */
public abstract class Parser {

    static final Logger logger = LoggerFactory.getLogger(Parser.class);

    protected String name;
    protected String[] tags;

    public String getName() {
        return name;
    }

    public String[] getTags() {
        return tags;
    }

    public abstract void parse(MetricName metricName);

    public abstract Map<String, Object> getTagMap();
}
