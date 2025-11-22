package com.company.chartconfig.utils;


public record FilterRule(
        String column,
        String operator,
        String value
) {}