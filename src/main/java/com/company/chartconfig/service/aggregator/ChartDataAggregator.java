package com.company.chartconfig.service.aggregator;

import com.company.chartconfig.enums.TimeGrain;
import com.company.chartconfig.model.ChartCommonSettings;
import com.company.chartconfig.view.common.MetricConfig;
import io.jmix.chartsflowui.data.item.MapDataItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ChartDataAggregator {

    private static final Logger log = LoggerFactory.getLogger(ChartDataAggregator.class);

    private static final DateTimeFormatter FLEXIBLE_DATE_PARSER = new DateTimeFormatterBuilder()
            .appendOptional(DateTimeFormatter.ofPattern("M/d/yyyy[ H:m:s]"))
            .appendOptional(DateTimeFormatter.ofPattern("d/M/yyyy[ H:m:s]"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd[ H:m:s]"))
            .appendOptional(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .appendOptional(DateTimeFormatter.ISO_LOCAL_DATE)
            .toFormatter(Locale.ENGLISH);

    public List<MapDataItem> aggregate(List<MapDataItem> rawData, List<MetricConfig> metrics, ChartCommonSettings settings) {
        if (rawData == null || rawData.isEmpty()) return new ArrayList<>();

        String dimension = settings.getXAxisField();
        TimeGrain timeGrain = settings.getTimeGrain();

        // 1. GROUP BY
        Map<Object, List<MapDataItem>> grouped = rawData.stream()
                .collect(Collectors.groupingBy(item -> {
                    Object val = item.getValue(dimension);
                    if (val == null) return "Unknown";
                    if (timeGrain != null) return truncateDate(val, timeGrain);
                    return val;
                }));

        List<MapDataItem> result = new ArrayList<>();

        // 2. CALCULATE METRICS
        for (Map.Entry<Object, List<MapDataItem>> entry : grouped.entrySet()) {
            Object key = entry.getKey();
            List<MapDataItem> items = entry.getValue();

            MapDataItem row = new MapDataItem();
            row.add(dimension, key.toString());

            for (MetricConfig m : metrics) {
                try {
                    BigDecimal val = calculateMetric(items, m.getColumn(), m.getAggregate(), key);
                    row.add(m.getLabel(), val.doubleValue());
                } catch (Exception e) {
                    row.add(m.getLabel(), 0.0);
                }
            }
            result.add(row);
        }

        // 3. SORTING
        if (settings.getQuerySortMetric() != null) {
            String sortKey = settings.getQuerySortMetric().getLabel();
            boolean hasMetric = metrics.stream().anyMatch(m -> m.getLabel().equals(sortKey));

            if (hasMetric) {
                result.sort(Comparator.comparingDouble(item -> {
                    Object v = item.getValue(sortKey);
                    return v instanceof Number ? ((Number) v).doubleValue() : 0.0;
                }));
            } else {
                result.sort(Comparator.comparing(item -> item.getValue(dimension).toString()));
            }
        } else {
            result.sort(Comparator.comparing(item -> item.getValue(dimension).toString()));
        }

        // 4. DESCENDING
        if (settings.isQuerySortDesc()) {
            Collections.reverse(result);
        }

        return result;
    }

    private BigDecimal calculateMetric(List<MapDataItem> items, String col, String aggType, Object groupKey) {
        if (aggType == null) aggType = "COUNT";
        String type = aggType.toUpperCase();
        List<BigDecimal> values = new ArrayList<>();

        if (!type.equals("COUNT")) {
            for (MapDataItem item : items) {
                BigDecimal v = getBigDecimal(item, col);
                if (v != null) values.add(v);
            }
        }

        BigDecimal result = BigDecimal.ZERO;
        switch (type) {
            case "SUM": for (BigDecimal v : values) result = result.add(v); break;
            case "AVG":
                if (!values.isEmpty()) {
                    BigDecimal sum = BigDecimal.ZERO;
                    for (BigDecimal v : values) sum = sum.add(v);
                    result = sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
                }
                break;
            case "MAX": result = values.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO); break;
            case "MIN": result = values.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO); break;
            case "COUNT": result = BigDecimal.valueOf(items.size()); break;
            case "COUNT_DISTINCT":
                long distinct = items.stream().map(item -> item.getValue(col)).filter(Objects::nonNull).distinct().count();
                result = BigDecimal.valueOf(distinct);
                break;
        }
        return result;
    }

    private BigDecimal getBigDecimal(MapDataItem item, String col) {
        if (col == null) return BigDecimal.ZERO;
        Object v = item.getValue(col);
        if (v == null) return null;
        if (v instanceof Number) return new BigDecimal(v.toString());
        if (v instanceof String) {
            String s = ((String) v).trim();
            if (s.isEmpty()) return null;
            String clean = s.replace(",", "").replace("$", "").replace(" ", "");
            try { return new BigDecimal(clean); } catch (Exception e) { return null; }
        }
        return null;
    }

    private String truncateDate(Object val, TimeGrain grain) {
        if (val == null) return "Unknown";
        try {
            String str = val.toString();
            if (str.length() > 19 && str.contains("T")) str = str.substring(0, 19);
            str = str.replace("T", " ");

            LocalDateTime dt;
            try {
                dt = LocalDateTime.parse(str, FLEXIBLE_DATE_PARSER);
            } catch (Exception e) {
                LocalDate date = LocalDate.parse(str, FLEXIBLE_DATE_PARSER);
                dt = date.atStartOfDay();
            }

            // Sử dụng switch expression với default để đảm bảo bao phủ mọi trường hợp
            return switch (grain) {
                case SECOND, FIVE_SECONDS, THIRTY_SECONDS -> dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                case MINUTE, FIVE_MINUTES, TEN_MINUTES, FIFTEEN_MINUTES, THIRTY_MINUTES -> dt.withSecond(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

                case HOUR -> dt.withMinute(0).withSecond(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00"));

                case DAY -> dt.toLocalDate().toString();

                case WEEK -> {
                    int week = dt.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                    int year = dt.get(IsoFields.WEEK_BASED_YEAR);
                    yield year + "-W" + String.format("%02d", week);
                }

                case MONTH -> dt.getYear() + "-" + String.format("%02d", dt.getMonthValue());

                case QUARTER -> dt.getYear() + "-Q" + ((dt.getMonthValue() - 1) / 3 + 1);

                case YEAR -> String.valueOf(dt.getYear());

                // Quan trọng: Phải có default để trình biên dịch không báo lỗi thiếu case
                default -> dt.toString();
            };
        } catch (Exception e) {
            return val.toString();
        }
    }

    public BigDecimal calculateMetric(List<MapDataItem> items, String col, String aggType) {
        return calculateMetric(items, col, aggType, null);
    }
}