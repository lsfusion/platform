package lsfusion.server.language.form;

public class FormOptions {
    private String image = null;
    private Integer autoRefresh = null; 
    private Boolean localAsync = null;
    
    private FormPropertyOptions propertyOptions = null;
    
    public FormOptions() {}

    public FormOptions(String image, Integer autoRefresh, Boolean localAsync, FormPropertyOptions propertyOptions) {
        this.image = image;
        this.autoRefresh = autoRefresh;
        this.localAsync = localAsync;
        this.propertyOptions = propertyOptions;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Integer getAutoRefresh() {
        return autoRefresh;
    }

    public void setAutoRefresh(int autoRefresh) {
        this.autoRefresh = autoRefresh;
    }

    public Boolean getLocalAsync() {
        return localAsync;
    }

    public void setLocalAsync(boolean localAsync) {
        this.localAsync = localAsync;
    }

    public FormPropertyOptions getPropertyOptions() {
        return propertyOptions;
    }

    public void setPropertyOptions(FormPropertyOptions propertyOptions) {
        this.propertyOptions = propertyOptions;
    }
}
