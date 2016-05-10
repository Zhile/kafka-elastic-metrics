package org.apache.kafka.metrics.elastic;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.stats.Snapshot;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by dzhou on 2/21/16.
 */
public class MetricsElasticModule extends Module {

    public static final Version VERSION = new Version(3, 0, 0, "", "yammer-metrics-elastic", "yammer-metrics-elastic");

    private static void writeAdditionalFields(final Map<String, Object> additionalFields, final JsonGenerator json) throws IOException {
        if (additionalFields != null) {
            for (final Map.Entry<String, Object> field : additionalFields.entrySet()) {
                json.writeObjectField(field.getKey(), field.getValue());
            }
        }
    }

    private static class GaugeSerializer extends StdSerializer<JsonMetrics.JsonGauge> {
        private final String timestampFieldname;
        private final Map<String, Object> additionalFields;

        private GaugeSerializer(String timestampFieldname, Map<String, Object> additionalFields) {
            super(JsonMetrics.JsonGauge.class);
            this.timestampFieldname = timestampFieldname;
            this.additionalFields = additionalFields;
        }

        @Override
        public void serialize(JsonMetrics.JsonGauge gauge,
                              JsonGenerator json,
                              SerializerProvider provider) throws IOException {
            json.writeStartObject();
            json.writeStringField("name", gauge.name());
            json.writeObjectField(timestampFieldname, gauge.timestampAsDate());
            final Object value;
            try {
                value = gauge.value().value();
                json.writeObjectField("value", value);
            } catch (RuntimeException e) {
                json.writeObjectField("error", e.toString());
            }
            writeAdditionalFields(additionalFields, json);
            writeAdditionalFields(gauge.getTags(), json);
            json.writeEndObject();
        }
    }

    private static class CounterSerializer extends StdSerializer<JsonMetrics.JsonCounter> {
        private final String timestampFieldname;
        private final Map<String, Object> additionalFields;

        private CounterSerializer(String timestampFieldname, Map<String, Object> additionalFields) {
            super(JsonMetrics.JsonCounter.class);
            this.timestampFieldname = timestampFieldname;
            this.additionalFields = additionalFields;
        }

        @Override
        public void serialize(JsonMetrics.JsonCounter counter,
                              JsonGenerator json,
                              SerializerProvider provider) throws IOException {
            json.writeStartObject();
            json.writeStringField("name", counter.name());
            json.writeObjectField(timestampFieldname, counter.timestampAsDate());
            json.writeNumberField("count", counter.value().count());
            writeAdditionalFields(additionalFields, json);
            writeAdditionalFields(counter.getTags(), json);
            json.writeEndObject();
        }
    }

    private static class HistogramSerializer extends StdSerializer<JsonMetrics.JsonHistogram> {

        private final String timestampFieldname;
        private final Map<String, Object> additionalFields;

        private HistogramSerializer(String timestampFieldname, Map<String, Object> additionalFields) {
            super(JsonMetrics.JsonHistogram.class);
            this.timestampFieldname = timestampFieldname;
            this.additionalFields = additionalFields;
        }

        @Override
        public void serialize(JsonMetrics.JsonHistogram jsonHistogram,
                              JsonGenerator json,
                              SerializerProvider provider) throws IOException {
            json.writeStartObject();
            json.writeStringField("name", jsonHistogram.name());
            json.writeObjectField(timestampFieldname, jsonHistogram.timestampAsDate());
            Histogram histogram = jsonHistogram.value();

            final Snapshot snapshot = histogram.getSnapshot();
            json.writeNumberField("count", histogram.sum());
            json.writeNumberField("max", histogram.max());
            json.writeNumberField("mean", histogram.mean());
            json.writeNumberField("min", histogram.min());
            json.writeNumberField("p50", snapshot.getMedian());
            json.writeNumberField("p75", snapshot.get75thPercentile());
            json.writeNumberField("p95", snapshot.get95thPercentile());
            json.writeNumberField("p98", snapshot.get98thPercentile());
            json.writeNumberField("p99", snapshot.get99thPercentile());
            json.writeNumberField("p999", snapshot.get999thPercentile());

            json.writeNumberField("stddev", histogram.stdDev());
            writeAdditionalFields(additionalFields, json);
            writeAdditionalFields(jsonHistogram.getTags(), json);
            json.writeEndObject();
        }
    }

    private static class MeterSerializer extends StdSerializer<JsonMetrics.JsonMeter> {
        private final String rateUnit;
        private final double rateFactor;
        private final String timestampFieldname;
        private final Map<String, Object> additionalFields;

        public MeterSerializer(TimeUnit rateUnit, String timestampFieldname, Map<String, Object> additionalFields) {
            super(JsonMetrics.JsonMeter.class);
            this.timestampFieldname = timestampFieldname;
            this.rateFactor = rateUnit.toSeconds(1);
            this.rateUnit = calculateRateUnit(rateUnit, "events");
            this.additionalFields = additionalFields;
        }

        @Override
        public void serialize(JsonMetrics.JsonMeter jsonMeter,
                              JsonGenerator json,
                              SerializerProvider provider) throws IOException {
            json.writeStartObject();
            json.writeStringField("name", jsonMeter.name());
            json.writeObjectField(timestampFieldname, jsonMeter.timestampAsDate());
            Meter meter = jsonMeter.value();
            json.writeNumberField("count", meter.count());
            json.writeNumberField("m1_rate", meter.oneMinuteRate() * rateFactor);
            json.writeNumberField("m5_rate", meter.fiveMinuteRate() * rateFactor);
            json.writeNumberField("m15_rate", meter.fifteenMinuteRate() * rateFactor);
            json.writeNumberField("mean_rate", meter.meanRate() * rateFactor);
            json.writeStringField("units", rateUnit);
            writeAdditionalFields(additionalFields, json);
            writeAdditionalFields(jsonMeter.getTags(), json);
            json.writeEndObject();
        }
    }

    private static class TimerSerializer extends StdSerializer<JsonMetrics.JsonTimer> {
        private final String rateUnit;
        private final double rateFactor;
        private final String durationUnit;
        private final double durationFactor;
        private final String timestampFieldname;
        private final Map<String, Object> additionalFields;

        private TimerSerializer(TimeUnit rateUnit, TimeUnit durationUnit, String timestampFieldname, Map<String, Object> additionalFields) {
            super(JsonMetrics.JsonTimer.class);
            this.timestampFieldname = timestampFieldname;
            this.rateUnit = calculateRateUnit(rateUnit, "calls");
            this.rateFactor = rateUnit.toSeconds(1);
            this.durationUnit = durationUnit.toString().toLowerCase(Locale.US);
            this.durationFactor = 1.0 / durationUnit.toNanos(1);
            this.additionalFields = additionalFields;
        }

        @Override
        public void serialize(JsonMetrics.JsonTimer jsonTimer,
                              JsonGenerator json,
                              SerializerProvider provider) throws IOException {
            json.writeStartObject();
            json.writeStringField("name", jsonTimer.name());
            json.writeObjectField(timestampFieldname, jsonTimer.timestampAsDate());
            Timer timer = jsonTimer.value();
            final Snapshot snapshot = timer.getSnapshot();
            json.writeNumberField("count", timer.count());
            json.writeNumberField("max", timer.max() * durationFactor);
            json.writeNumberField("mean", snapshot.getMedian() * durationFactor);
            json.writeNumberField("min", timer.min() * durationFactor);

            json.writeNumberField("p50", snapshot.getMedian() * durationFactor);
            json.writeNumberField("p75", snapshot.get75thPercentile() * durationFactor);
            json.writeNumberField("p95", snapshot.get95thPercentile() * durationFactor);
            json.writeNumberField("p98", snapshot.get98thPercentile() * durationFactor);
            json.writeNumberField("p99", snapshot.get99thPercentile() * durationFactor);
            json.writeNumberField("p999", snapshot.get999thPercentile() * durationFactor);

            json.writeNumberField("stddev", timer.stdDev() * durationFactor);
            json.writeNumberField("m1_rate", timer.oneMinuteRate() * rateFactor);
            json.writeNumberField("m5_rate", timer.fiveMinuteRate() * rateFactor);
            json.writeNumberField("m15_rate", timer.fifteenMinuteRate() * rateFactor);
            json.writeNumberField("mean_rate", timer.meanRate() * rateFactor);
            json.writeStringField("duration_units", durationUnit);
            json.writeStringField("rate_units", rateUnit);
            writeAdditionalFields(additionalFields, json);
            writeAdditionalFields(jsonTimer.getTags(), json);
            json.writeEndObject();
        }
    }


    /**
     * Serializer for the first line of the bulk index operation before the json metric is written
     */
    private static class BulkIndexOperationHeaderSerializer extends StdSerializer<BulkIndexOperationHeader> {

        public BulkIndexOperationHeaderSerializer() {
            super(BulkIndexOperationHeader.class);
        }

        @Override
        public void serialize(BulkIndexOperationHeader bulkIndexOperationHeader, JsonGenerator json, SerializerProvider provider) throws IOException {
            json.writeStartObject();
            json.writeObjectFieldStart("index");
            if (bulkIndexOperationHeader.index != null) {
                json.writeStringField("_index", bulkIndexOperationHeader.index);
            }
            if (bulkIndexOperationHeader.type != null) {
                json.writeStringField("_type", bulkIndexOperationHeader.type);
            }
            json.writeEndObject();
            json.writeEndObject();
        }
    }

    public static class BulkIndexOperationHeader {
        public String index;
        public String type;

        public BulkIndexOperationHeader(String index, String type) {
            this.index = index;
            this.type = type;
        }
    }

    private final TimeUnit rateUnit;
    private final TimeUnit durationUnit;
    private final String timestampFieldname;
    private final Map<String, Object> additionalFields;

    public MetricsElasticModule(TimeUnit rateUnit, TimeUnit durationUnit, String timestampFieldname, Map<String, Object> additionalFields) {
        this.rateUnit = rateUnit;
        this.durationUnit = durationUnit;
        this.timestampFieldname = timestampFieldname;
        this.additionalFields = additionalFields;
    }

    @Override
    public String getModuleName() {
        return "yammer-metrics-elastic-serialization";
    }

    @Override
    public Version version() {
        return VERSION;
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addSerializers(new SimpleSerializers(Arrays.<JsonSerializer<?>>asList(
                new GaugeSerializer(timestampFieldname, additionalFields),
                new CounterSerializer(timestampFieldname, additionalFields),
                new HistogramSerializer(timestampFieldname, additionalFields),
                new MeterSerializer(rateUnit, timestampFieldname, additionalFields),
                new TimerSerializer(rateUnit, durationUnit, timestampFieldname, additionalFields),
                new BulkIndexOperationHeaderSerializer()
        )));
    }

    private static String calculateRateUnit(TimeUnit unit, String name) {
        final String s = unit.toString().toLowerCase(Locale.US);
        return name + '/' + s.substring(0, s.length() - 1);
    }
}
