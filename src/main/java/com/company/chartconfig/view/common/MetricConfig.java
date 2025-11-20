package com.company.chartconfig.view.common;
import java.io.Serializable;

public class MetricConfig implements Serializable {
    public enum Type { SIMPLE, CUSTOM_SQL }
    private Type type = Type.SIMPLE;
    private String column;
    private String aggregate = "COUNT";
    private String sqlExpression;
    private String label;

    public MetricConfig() {}
    public MetricConfig(String column) {
        this.column = column;
        updateLabel();
    }

    public void updateLabel() {
        if (type == Type.SIMPLE) this.label = aggregate + "(" + column + ")";
        else this.label = "Custom SQL";
    }

    // Getters Setters
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getColumn() { return column; }
    public void setColumn(String column) { this.column = column; }
    public String getAggregate() { return aggregate; }
    public void setAggregate(String aggregate) { this.aggregate = aggregate; }
    public String getSqlExpression() { return sqlExpression; }
    public void setSqlExpression(String sqlExpression) { this.sqlExpression = sqlExpression; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}