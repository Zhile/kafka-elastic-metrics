package org.apache.kafka.metrics.elastic;

import com.yammer.metrics.core.MetricName;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetricNameFormatter {
    static final Pattern whitespaceRegex = Pattern.compile("\\s+");


    public static String formatWithScope(MetricName metricName) {
        StringBuilder sb = new StringBuilder(128)
                .append(metricName.getGroup())
                .append('.')
                .append(metricName.getType())
                .append('.');
        if (metricName.hasScope() && !metricName.getScope().isEmpty()) {
            sb.append(metricName.getScope())
                    .append(".");
        }
        sb.append(sanitizeName(metricName.getName()));
        return sb.toString();
    }

    public static String format(MetricName metricName) {
        return format(metricName, "");
    }

    public static String format(MetricName metricName, String suffix) {
        return new StringBuilder(128)
                .append(metricName.getGroup())
                .append('.')
                .append(metricName.getType())
                .append('.')
                .append(sanitizeName(metricName.getName()))
                .append(suffix)
                .toString();
    }

    public static String sanitizeName(String name) {
        Matcher m = whitespaceRegex.matcher(name);
        if (m.find())
            return m.replaceAll("_");
        else
            return name;
    }
}
