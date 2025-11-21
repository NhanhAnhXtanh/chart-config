package com.company.chartconfig.model;

// Đặt trong ChartConfigView hoặc package model
public class FieldItem {
    private String name;
    private String type;

    public FieldItem(String name, String type) {
        this.name = name;
        this.type = type;
    }
    // Getters
    public String getName() { return name; }
    public String getType() { return type; }
}