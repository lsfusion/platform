package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.form.object.table.grid.view.PivotRendererType;

import java.io.Serializable;

public class GPivotOptions implements Serializable {
    String type;
    GPropertyGroupType aggregation;
    Boolean showSettings;

    public GPivotOptions() {
    }

    public GPivotOptions(String type, GPropertyGroupType aggregation, Boolean showSettings) {
        this.type = type;
        this.aggregation = aggregation;
        this.showSettings = showSettings;
    }

    public String getLocalizedType() {
        return type != null ? PivotRendererType.getType(type).localize() : type;
    }
    
    public GPropertyGroupType getAggregation() {
        return aggregation;
    }

    public boolean isShowSettings() {
        return showSettings == null || showSettings;
    }
}
