package lsfusion.gwt.client.form.property;

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

    public String getType() {
        return type;
    }

    public GPropertyGroupType getAggregation() {
        return aggregation;
    }

    public boolean isShowSettings() {
        return showSettings == null || showSettings;
    }
}
