package lsfusion.interop.form.property;

import java.io.Serializable;

public class PivotOptions implements Serializable {
    private String type;
    private PropertyGroupType aggregation;
    private Boolean showSettings;
    private String configFunction;

    public PivotOptions() {
    }

    public PivotOptions(String type, PropertyGroupType aggregation, Boolean showSettings, String configFunction) {
        this.type = type;
        this.aggregation = aggregation;
        this.showSettings = showSettings;
        this.configFunction = configFunction;
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

    public String getConfigFunction() {
        return configFunction;
    }

    public void setConfigFunction(String configFunction) {
        this.configFunction = configFunction;
    }

    public void merge(PivotOptions pivotOptions) {
        if(pivotOptions.type != null)
            this.type = pivotOptions.type;
        if(pivotOptions.aggregation != null)
            this.aggregation = pivotOptions.aggregation;
        if(pivotOptions.showSettings != null)
            this.showSettings = pivotOptions.showSettings;
        if(pivotOptions.configFunction != null)
            this.configFunction = pivotOptions.configFunction;
    }
}
