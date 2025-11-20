package com.company.chartconfig.service.builder;

import com.company.chartconfig.enums.ChartType;
import com.fasterxml.jackson.databind.JsonNode;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.MapDataItem;

import java.util.List;

public interface ChartBuilder {
    // Builder này hỗ trợ loại chart nào?
    boolean supports(ChartType type);

    // Hàm build chính
    Chart build(JsonNode config, List<MapDataItem> rawData);
}