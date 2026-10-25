package lsfusion.gwt.client.form.filter.user.view;

// the platform's EDITORS of a table's conditions - its panel's rows, where it draws them (GPlatformFiltersController);
// none where a view draws them -: their selectors and delete buttons, shown and hidden with the filter controls by the
// toolbar's button (GFilterControlsToggle). What waits for the apply, and the apply, are the model's
// (GUserFiltersController)
public interface GFilterEditors {
    void setConditionsControlsVisible(boolean visible);
}
