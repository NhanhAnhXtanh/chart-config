package com.company.chartconfig.service;

import com.company.chartconfig.entity.Dataset;
import com.company.chartconfig.enums.ChartType;
import com.company.chartconfig.service.builder.ChartBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.MapDataItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class ChartConfigService {

    private final ObjectMapper objectMapper;
    private final List<ChartBuilder> builders; // Spring tự inject tất cả các class implement ChartBuilder vào đây

    public ChartConfigService(ObjectMapper objectMapper, List<ChartBuilder> builders) {
        this.objectMapper = objectMapper;
        this.builders = builders;
    }

    public Chart buildPreviewChart(Dataset dataset, ChartType type, String settingsJson) {
        try {
            // 1. Parse Common Data (Raw JSON -> MapDataItem)
            List<MapDataItem> rawItems = parseRawData(dataset.getRawJson());
            JsonNode config = objectMapper.readTree(settingsJson);

            // 2. Tìm Builder phù hợp (Strategy Pattern)
            ChartBuilder builder = builders.stream()
                    .filter(b -> b.supports(type))
                    .findFirst()
                    .orElseThrow(() -> new UnsupportedOperationException("Chưa hỗ trợ loại chart: " + type));

            // 3. Delegate việc vẽ chart cho Builder
            return builder.build(config, rawItems);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo chart: " + e.getMessage(), e);
        }
    }

    // Helper: Parse Raw JSON
    private List<MapDataItem> parseRawData(String rawJson) throws Exception {
        JsonNode array = objectMapper.readTree(rawJson);
        List<MapDataItem> result = new ArrayList<>();
        if (array.isArray()) {
            for (JsonNode item : array) {
                MapDataItem mapItem = new MapDataItem();
                Iterator<String> names = item.fieldNames();
                while (names.hasNext()) {
                    String key = names.next();
                    JsonNode val = item.get(key);
                    if (val.isNumber()) mapItem.add(key, val.numberValue());
                    else if (val.isTextual()) mapItem.add(key, val.textValue());
                    else if (val.isBoolean()) mapItem.add(key, val.booleanValue());
                    else mapItem.add(key, val.toString());
                }
                result.add(mapItem);
            }
        }
        return result;
    }
}