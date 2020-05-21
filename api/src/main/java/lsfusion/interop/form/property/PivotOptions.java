package lsfusion.interop.form.property;

import java.io.Serializable;

public class PivotOptions implements Serializable {
    String type;
    PropertyGroupType aggregation;
    Boolean showSettings;

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

    public Boolean getShowSettings() {
        return showSettings;
    }

    public void setShowSettings(Boolean showSettings) {
        this.showSettings = showSettings;
    }

    public void merge(PivotOptions pivotOptions) {
        if(pivotOptions.type != null)
            this.type = pivotOptions.type;
        if(pivotOptions.aggregation != null)
            this.aggregation = pivotOptions.aggregation;
        if(pivotOptions.showSettings != null)
            this.showSettings = pivotOptions.showSettings;
    }
}
