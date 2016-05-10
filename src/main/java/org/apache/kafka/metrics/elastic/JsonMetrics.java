package org.apache.kafka.metrics.elastic;

import com.yammer.metrics.core.*;

import java.util.Date;
import java.util.Map;

/**
 * Created by dzhou on 2/21/16.
 */
public class JsonMetrics {

    /**
     * A abstract json metric class, from which all other classes inherit
     * The other classes are simply concrete json implementations of the existing metrics classes
     */
    public static abstract class JsonMetric<T> {
        private final String name;
        private long timestamp;
        private final T value;
        private Map<String, Object> tags;

        public JsonMetric(String name, long timestamp, T value, Map<String, Object> tags) {
            this.name = name;
            this.timestamp = timestamp;
            this.value = value;
            this.tags = tags;
        }

        public String name() {
            return name;
        }

        public long timestamp() {
            return timestamp;
        }

        public Date timestampAsDate() {
            return new Date(timestamp * 1000);
        }

        public T value() {
            return value;
        }

        @Override
        public String toString() {
            return String.format("%s %s %s", type(), name, timestamp);
        }

        public abstract String type();

        public Map<String, Object> getTags() {
            return tags;
        }
    }

    public static class JsonGauge extends JsonMetric<Gauge> {
        private static final String TYPE = "gauge";

        public JsonGauge(String name, long timestamp, Gauge value, Map<String, Object> tags) {
            super(name, timestamp, value, tags);
        }

        @Override
        public String type() {
            return TYPE;
        }
    }

    public static class JsonCounter extends JsonMetric<Counter> {
        private static final String TYPE = "counter";

        public JsonCounter(String name, long timestamp, Counter value, Map<String, Object> tags) {
            super(name, timestamp, value, tags);
        }

        @Override
        public String type() {
            return TYPE;
        }
    }

    public static class JsonHistogram extends JsonMetric<Histogram> {
        private static final String TYPE = "histogram";

        public JsonHistogram(String name, long timestamp, Histogram value, Map<String, Object> tags) {
            super(name, timestamp, value, tags);
        }

        @Override
        public String type() {
            return TYPE;
        }
    }

    public static class JsonMeter extends JsonMetric<Meter> {
        private static final String TYPE = "meter";
        public JsonMeter(String name, long timestamp, Meter value, Map<String, Object> tags) {
            super(name, timestamp, value, tags);
        }

        @Override
        public String type() {
            return TYPE;
        }
    }

    public static class JsonTimer extends JsonMetric<Timer> {
        private static final String TYPE = "timer";

        public JsonTimer(String name, long timestamp, Timer value, Map<String, Object> tags) {
            super(name, timestamp, value, tags);
        }

        @Override
        public String type() {
            return TYPE;
        }
    }
}
