package lsfusion.interop.form.property;

import java.io.Serializable;

public class PivotOptions implements Serializable {
    String type;
    PropertyGroupType aggregation;
    boolean showSettings = true;

    public PivotOptions() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public PropertyGroupType getAggregation() {
        return aggregation;
    }

    public void setAggregation(PropertyGroupType aggregation) {
        this.aggregation = aggregation;
    }

    public boolean showSettings() {
        return showSettings;
    }

    public void setShowSettings(boolean showSettings) {
        this.showSettings = showSettings;
    }
}
