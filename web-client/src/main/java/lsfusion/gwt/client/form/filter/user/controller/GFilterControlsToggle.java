package lsfusion.gwt.client.form.filter.user.controller;

import lsfusion.gwt.client.form.filter.user.GUserFiltersController;
import lsfusion.gwt.client.form.filter.user.view.GFilterEditors;
import lsfusion.gwt.client.form.object.table.controller.GToolbarController;

// what the toolbar's filter button shows and hides of a filter input controller, of whichever kind
// (GFilterInputController): what of the user filters THE PLATFORM draws - its controls in FILTERCONTROLS, and the
// conditions' editors where its panel draws the conditions. One rule, here, from the kinds already chosen - of FILTERS
// (the controller's) and of FILTERCONTROLS (GFilterControlsController.isShownByToolbar) -: the button is there where
// there is something of the platform's for it to show and hide, and nowhere else:
// - the panel and the platform's controls: the editors and the controls, as ever;
// - the panel and controls a view draws: the editors;
// - a view's conditions and the platform's controls: the controls;
// - a view's conditions and controls: nothing - there is no button.
// The button is the table's toolbar's, of whichever kind (GToolbarController.addFilterToggle) - a member where a view
// draws the toolbar. The controls give the controller's commands whoever draws them
public final class GFilterControlsToggle implements GUserFiltersController.ApplyControls {
    // what lays the form out anew once the controls are shown or hidden (GFormLayout.update) - handed in, so a test
    // needs no form
    private final Runnable relayout;
    private final GFilterControlsController controls;
    private final GToolbarController<?> toolbar;
    // what shows with the controls, and whose apply they give: the platform's editors of the conditions - the panel's,
    // their selectors and delete buttons -, none where a view draws the conditions
    private GFilterEditors editors;
    // what shows and hides them: the table's toolbar, where it has something of the platform's to show and hide -
    // none where it would show and hide nothing of the platform's: nothing would show them again then, so they are
    // always shown
    private GToolbarController<?> shownBy;
    private boolean visible = false;
    // the table's user filters, whose manual apply mode follows the apply button shown (isApplyShown)
    private GUserFiltersController userFilters;

    // made by the platform where it wires a filter input controller up
    // (GFormController.initializeTable), and handed to the kind of it, which binds it
    public GFilterControlsToggle(Runnable relayout, GFilterControlsController controls, GToolbarController<?> toolbar) {
        this.relayout = relayout;
        this.controls = controls;
        this.toolbar = toolbar;
    }

    // ... once the kind is made, as the controls ask it what they offer (GFiltersHandler): the panel with its editors,
    // the commands over a view's conditions with none
    public void bind(GFilterInputController filter, GFilterEditors editors, GUserFiltersController userFilters) {
        this.editors = editors;
        this.userFilters = userFilters;
        shownBy = editors != null || controls.isShownByToolbar() ? toolbar : null;
        if (shownBy != null)
            shownBy.addFilterToggle(filter);
        else
            visible = true;

        controls.bind(filter, userFilters);
        controls.setVisible(visible);
    }
    // whether the table's apply button is shown: the manual apply mode asks it (GUserFiltersController.isManual)
    public boolean isApplyShown() {
        return controls.isApplyShown(visible);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        if (shownBy == null) // nothing shows or hides them: they are always shown
            return;

        this.visible = visible;
        if (editors != null)
            editors.setConditionsControlsVisible(visible);

        controls.setVisible(visible);
        relayout.run();

        shownBy.showFilterControls(visible);
        if (userFilters != null)
            userFilters.modeChanged();
    }

    // the table shown or hidden: the controls with it
    public void setTableVisible(boolean visible) {
        controls.setTableVisible(visible && this.visible);
    }
}
