package com.company.chartconfig.enums;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum ContributionMode implements EnumClass<String> {

    NONE("none"),
    ROW("row"),       // 100% Stacked (theo dòng)
    SERIES("series");

    private final String id;

    ContributionMode(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static ContributionMode fromId(String id) {
        for (ContributionMode at : ContributionMode.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}