package com.company.chartconfig.model;

import com.company.chartconfig.enums.ContributionMode;
import com.fasterxml.jackson.databind.JsonNode;

public class ChartCommonSettings {
    private int seriesLimit;
    private ContributionMode contributionMode;
    private String xAxisField;

    public ChartCommonSettings(JsonNode root) {
        this.seriesLimit = root.path("seriesLimit").asInt(0);
        String modeStr = root.path("contributionMode").asText("none");
        this.contributionMode = ContributionMode.fromId(modeStr);
        this.xAxisField = root.path("xAxis").asText();
    }

    public int getSeriesLimit() { return seriesLimit; }
    public ContributionMode getContributionMode() { return contributionMode; }
    public String getXAxisField() { return xAxisField; }
}