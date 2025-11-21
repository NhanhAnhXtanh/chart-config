package com.company.chartconfig.enums;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum ChartType implements EnumClass<String> {

    BAR("BAR"),
    PIE("PIE"),
    LINE("LINE");

    private final String id;

    ChartType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static ChartType fromId(String id) {
        for (ChartType at : ChartType.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}