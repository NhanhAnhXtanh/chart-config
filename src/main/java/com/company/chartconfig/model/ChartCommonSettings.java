package com.company.chartconfig.model;

import com.company.chartconfig.constants.ChartConstants;
import com.company.chartconfig.enums.ContributionMode;
import com.company.chartconfig.enums.TimeGrain;
import com.company.chartconfig.view.common.MetricConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChartCommonSettings {
    private String xAxisField;
    private ContributionMode contributionMode;
    private TimeGrain timeGrain;
    private int seriesLimit;

    // --- 1. QUERY SORT ---
    private MetricConfig querySortMetric;
    private boolean querySortDesc = true;

    // --- 2. X-AXIS CONFIG & SORT ---
    private boolean forceCategorical = false; // MỚI: Ép buộc dạng danh mục
    private String xAxisSortBy;
    private boolean xAxisSortAsc = true;

    public ChartCommonSettings(JsonNode root) {
        this.xAxisField = root.path("xAxis").asText(null);
        this.contributionMode = ContributionMode.fromId(root.path("contributionMode").asText("none"));
        this.timeGrain = TimeGrain.fromId(root.path("timeGrain").asText(null));
        this.seriesLimit = root.path(ChartConstants.JSON_FIELD_SERIES_LIMIT).asInt(0);

        // Query Sort
        this.querySortDesc = root.path("querySortDesc").asBoolean(true);
        if (root.has("querySortBy")) {
            try { this.querySortMetric = new ObjectMapper().treeToValue(root.path("querySortBy"), MetricConfig.class); } catch (Exception e) { this.querySortMetric = null; }
        }

        // X-Axis Config
        this.forceCategorical = root.path("forceCategorical").asBoolean(false); // Default false
        this.xAxisSortBy = root.path("xAxisSortBy").asText(null);
        this.xAxisSortAsc = root.path("xAxisSortAsc").asBoolean(true);
    }

    // Getters
    public String getXAxisField() { return xAxisField; }
    public ContributionMode getContributionMode() { return contributionMode; }
    public TimeGrain getTimeGrain() { return timeGrain; }
    public int getSeriesLimit() { return seriesLimit; }

    public MetricConfig getQuerySortMetric() { return querySortMetric; }
    public boolean isQuerySortDesc() { return querySortDesc; }

    public boolean isForceCategorical() { return forceCategorical; } // Getter Mới
    public String getXAxisSortBy() { return xAxisSortBy; }
    public boolean isXAxisSortAsc() { return xAxisSortAsc; }
}