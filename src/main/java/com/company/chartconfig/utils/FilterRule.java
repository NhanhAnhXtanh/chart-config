package com.company.chartconfig.utils; // Hoặc package common
public class FilterRule {
    private String column;
    private String operator;
    private String value;

    public FilterRule() {}
    public FilterRule(String column, String operator, String value) {
        this.column = column;
        this.operator = operator;
        this.value = value;
    }
    // Getters & Setters
    public String getColumn() { return column; }
    public void setColumn(String column) { this.column = column; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}