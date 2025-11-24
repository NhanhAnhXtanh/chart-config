package com.company.chartconfig.view.config;

import com.company.chartconfig.enums.ChartType;
import com.company.chartconfig.view.chartfragment.*;
import com.company.chartconfig.view.config.common.ChartConfigFragment;
import io.jmix.flowui.fragment.Fragment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ChartFragmentRegistry {

    private final Map<ChartType, Class<? extends Fragment<?>>> registry = new HashMap<>();

    public ChartFragmentRegistry() {
        // Đăng ký các Fragment tại đây
        register(ChartType.BAR, BarConfigFragment.class);
        register(ChartType.PIE, PieConfigFragment.class);
        register(ChartType.LINE, LineConfigFragment.class);
        register(ChartType.AREA, AreaConfigFragment.class);
        register(ChartType.GAUGE, GaugeConfigFragment.class);
    }

    public void register(ChartType type, Class<? extends Fragment<?>> fragmentClass) {
        registry.put(type, fragmentClass);
    }

    public Class<? extends Fragment<?>> getFragmentClass(ChartType type) {
        return registry.get(type);
    }
}