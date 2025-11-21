package com.company.chartconfig.model;

import com.company.chartconfig.constants.ChartConstants;
import com.company.chartconfig.enums.ContributionMode;
import com.fasterxml.jackson.databind.JsonNode;

public class ChartCommonSettings {
    private int seriesLimit;
    private ContributionMode contributionMode;
    private boolean isDonut;
    private String xAxisField;
    private String dimensionField;

    public ChartCommonSettings(JsonNode root) {
        this.seriesLimit = root.path(ChartConstants.JSON_FIELD_SERIES_LIMIT).asInt(ChartConstants.DEFAULT_LIMIT_VALUE);

        String modeStr = root.path("contributionMode").asText("none");
        this.contributionMode = ContributionMode.fromId(modeStr);

        this.isDonut = root.path("isDonut").asBoolean(false);

        // Logic đọc: Tách biệt key trong JSON
        if (root.has("xAxis")) {
            this.xAxisField = root.path("xAxis").asText();
        }

        // QUAN TRỌNG: Đọc đúng key "dimension"
        if (root.has("dimension")) {
            this.dimensionField = root.path("dimension").asText();
        }

        // Fallback (Hỗ trợ tương thích ngược nếu cần, hoặc bỏ nếu muốn strict)
        if (this.dimensionField == null && this.xAxisField != null) {
            this.dimensionField = this.xAxisField;
        }
    }

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