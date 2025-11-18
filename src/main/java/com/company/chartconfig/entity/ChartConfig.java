package com.company.chartconfig.entity;

import com.company.chartconfig.enums.ChartType;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@JmixEntity
@Table(name = "CHART_CONFIG")
@Entity(name = "ChartConfig")
public class ChartConfig {

    @JmixGeneratedValue
    @Id
    @Column(name = "ID", nullable = false)
    private UUID id;

    @InstanceName
    @NotNull
    @Column(name = "NAME", nullable = false)
    private String name;

    @NotNull
    @Column(name = "CHART_TYPE", nullable = false)
    private String chartType;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DATASET_ID", nullable = false)
    private Dataset dataset;

    @Lob
    @Column(name = "SETTINGS_JSON")
    private String settingsJson;


    public void setChartType(ChartType chartType) {
        this.chartType = chartType == null ? null : chartType.getId();
    }

    public ChartType getChartType() {
        return chartType == null ? null : ChartType.fromId(chartType);
    }

    // -------- getters & setters ----------

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public String getSettingsJson() {
        return settingsJson;
    }

    public void setSettingsJson(String settingsJson) {
        this.settingsJson = settingsJson;
    }
}
