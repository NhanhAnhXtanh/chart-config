package com.company.chartconfig.model;

import com.company.chartconfig.constants.ChartConstants;
import com.company.chartconfig.enums.ContributionMode;
import com.company.chartconfig.enums.TimeGrain;
import com.company.chartconfig.view.common.MetricConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChartCommonSettings {
    // 1. Cấu hình chung
    private String xAxisField;
    private String dimensionField; // Cho Pie

    // --- TÁCH BIỆT 2 LIMIT ---
    private int seriesLimit; // Giới hạn số lượng Series (Top N groups)
    private int rowLimit;    // Giới hạn số dòng dữ liệu (Last N rows)
    // -------------------------

    private ContributionMode contributionMode;
    private TimeGrain timeGrain;
    private boolean isDonut;

    // 2. Query Sort
    private MetricConfig querySortMetric;
    private boolean querySortDesc = true;

    // 3. Visual Sort
    private boolean forceCategorical = false;
    private String xAxisSortBy;
    private boolean xAxisSortAsc = true;

    public ChartCommonSettings(JsonNode root) {
        // X-Axis / Dimension
        if (root.has("xAxis") && !root.path("xAxis").isNull()) {
            this.xAxisField = root.path("xAxis").asText();
        } else if (root.has("dimension")) {
            this.xAxisField = root.path("dimension").asText();
        }
        this.dimensionField = this.xAxisField; // Alias

        // --- ĐỌC RIÊNG BIỆT ---
        this.seriesLimit = root.path("seriesLimit").asInt(0);
        this.rowLimit = root.path("rowLimit").asInt(0);
        // ----------------------

        String modeStr = root.path("contributionMode").asText("none");
        this.contributionMode = ContributionMode.fromId(modeStr);
        this.timeGrain = TimeGrain.fromId(root.path("timeGrain").asText(null));
        this.isDonut = root.path("isDonut").asBoolean(false);

        // Sort
        this.querySortDesc = root.path("querySortDesc").asBoolean(true);
        if (root.has("querySortBy")) {
            try { this.querySortMetric = new ObjectMapper().treeToValue(root.path("querySortBy"), MetricConfig.class); } catch (Exception e) { this.querySortMetric = null; }
        }
        this.forceCategorical = root.path("forceCategorical").asBoolean(false);
        this.xAxisSortBy = root.path("xAxisSortBy").asText(null);
        this.xAxisSortAsc = root.path("xAxisSortAsc").asBoolean(true);
    }

    // Getters
    public String getXAxisField() { return xAxisField; }
    public String getDimensionField() { return dimensionField; }

    public int getSeriesLimit() { return seriesLimit; }
    public int getRowLimit() { return rowLimit; }

    public ContributionMode getContributionMode() { return contributionMode; }
    public TimeGrain getTimeGrain() { return timeGrain; }
    public boolean isDonut() { return isDonut; }
    public MetricConfig getQuerySortMetric() { return querySortMetric; }
    public boolean isQuerySortDesc() { return querySortDesc; }
    public boolean isForceCategorical() { return forceCategorical; }
    public String getXAxisSortBy() { return xAxisSortBy; }
    public boolean isXAxisSortAsc() { return xAxisSortAsc; }
}