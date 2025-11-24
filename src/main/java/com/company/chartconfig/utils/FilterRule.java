package com.company.chartconfig.utils;

import java.io.Serializable;

public class FilterRule implements Serializable {
    private String column;
    private String operator;
    private String value;

    // 1. Constructor rỗng (Bắt buộc cho Jackson JSON)
    public FilterRule() {}

    // 2. Constructor đầy đủ
    public FilterRule(String column, String operator, String value) {
        this.column = column;
        this.operator = operator;
        this.value = value;
    }

    // 3. Getters & Setters
    public String getColumn() { return column; }
    public void setColumn(String column) { this.column = column; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}