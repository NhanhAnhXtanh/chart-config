package com.company.chartconfig.model;

import com.company.chartconfig.constants.ChartConstants;
import com.company.chartconfig.enums.ContributionMode;
import com.company.chartconfig.enums.TimeGrain;
import com.company.chartconfig.view.common.MetricConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChartCommonSettings {
    private int seriesLimit;
    private ContributionMode contributionMode;
    private boolean isDonut;
    private String xAxisField;
    private String dimensionField;
    private TimeGrain timeGrain;

    // --- 1. QUERY SORT ---
    private MetricConfig querySortMetric;
    private boolean querySortDesc = true;

    // --- 2. X-AXIS CONFIG & SORT ---
    private boolean forceCategorical = false; // MỚI: Ép buộc dạng danh mục
    private String xAxisSortBy;
    private boolean xAxisSortAsc = true;

    public ChartCommonSettings(JsonNode root) {
        this.seriesLimit = root.path(ChartConstants.JSON_FIELD_SERIES_LIMIT).asInt(ChartConstants.DEFAULT_LIMIT_VALUE);

        String modeStr = root.path("contributionMode").asText("none");
        this.contributionMode = ContributionMode.fromId(modeStr);

        this.isDonut = root.path("isDonut").asBoolean(false);

        // Logic đọc: Tách biệt key trong JSON
        if (root.has("xAxis")) {
            this.xAxisField = root.path("xAxis").asText();
        }
        this.timeGrain = TimeGrain.fromId(root.path("timeGrain").asText(null));

        // Query Sort
        this.querySortDesc = root.path("querySortDesc").asBoolean(true);
        if (root.has("querySortBy")) {
            try { this.querySortMetric = new ObjectMapper().treeToValue(root.path("querySortBy"), MetricConfig.class); } catch (Exception e) { this.querySortMetric = null; }
        }
        // QUAN TRỌNG: Đọc đúng key "dimension"
        if (root.has("dimension")) {
            this.dimensionField = root.path("dimension").asText();
        }

        // Fallback (Hỗ trợ tương thích ngược nếu cần, hoặc bỏ nếu muốn strict)
        if (this.dimensionField == null && this.xAxisField != null) {
            this.dimensionField = this.xAxisField;
        }
        // X-Axis Config
        this.forceCategorical = root.path("forceCategorical").asBoolean(false); // Default false
        this.xAxisSortBy = root.path("xAxisSortBy").asText(null);
        this.xAxisSortAsc = root.path("xAxisSortAsc").asBoolean(true);
    }

    // Getters
    public TimeGrain getTimeGrain() { return timeGrain; }

    public MetricConfig getQuerySortMetric() { return querySortMetric; }
    public boolean isQuerySortDesc() { return querySortDesc; }

    public boolean isForceCategorical() { return forceCategorical; } // Getter Mới
    public String getXAxisSortBy() { return xAxisSortBy; }
    public boolean isXAxisSortAsc() { return xAxisSortAsc; }


    public int getSeriesLimit() {
        return seriesLimit;
    }

    public ContributionMode getContributionMode() {
        return contributionMode;
    }

    public boolean isDonut() {
        return isDonut;
    }

    public String getXAxisField() {
        return xAxisField;
    }

    public String getDimensionField() {
        return dimensionField;
    }
}