package com.company.chartconfig.enums;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

public enum TimeGrain implements EnumClass<String> {

    SECOND("second", "Second"),
    FIVE_SECONDS("5_second", "5 Seconds"),
    THIRTY_SECONDS("30_second", "30 Seconds"),

    MINUTE("minute", "Minute"),
    FIVE_MINUTES("5_minute", "5 Minutes"),
    TEN_MINUTES("10_minute", "10 Minutes"),
    FIFTEEN_MINUTES("15_minute", "15 Minutes"),
    THIRTY_MINUTES("30_minute", "30 Minutes"),

    HOUR("hour", "Hour"),
    DAY("day", "Day"),
    WEEK("week", "Week"),
    MONTH("month", "Month"),
    QUARTER("quarter", "Quarter"),
    YEAR("year", "Year");

    private final String id;
    private final String label;

    TimeGrain(String id, String label) {
        this.id = id;
        this.label = label;
    }

    @Override public String getId() { return id; }
    public String getLabel() { return label; }

    @Nullable
    public static TimeGrain fromId(String id) {
        for (TimeGrain at : TimeGrain.values()) {
            if (at.getId().equals(id)) return at;
        }
        return null;
    }
}