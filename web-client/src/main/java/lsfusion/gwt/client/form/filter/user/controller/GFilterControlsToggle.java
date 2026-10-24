package lsfusion.gwt.client.form.filter.user.controller;

import lsfusion.gwt.client.form.filter.user.GUserFiltersController;
import lsfusion.gwt.client.form.object.table.controller.GToolbarController;

// what the toolbar's filter button shows and hides of a table's user filters, whoever draws the box, the toolbar and
// the controls - one class for React and the platform: what of them THE PLATFORM draws - its rows (Rows), their
// selectors, and its controls in FILTERCONTROLS -, the controls bound to the input
// (GFilterInputController), whose commands they give, whoever draws them, and the manual apply mode the apply button
// shown brings. One rule, here, from the kinds already chosen - of FILTERS, of each FILTER, and of FILTERCONTROLS
// (GFilterControlsController.isShownByToolbar) -: the button is there where there is something of the platform's for it
// to show and hide, and nowhere else:
// - rows of the platform's - the box laid out, or a FILTER's -, and the platform's controls: the rows' selectors and
//   the controls, as ever;
// - the same and controls a view draws: the rows' selectors;
// - no row of the platform's, and the platform's controls: the controls;
// - no row of the platform's, and controls a view draws: nothing - there is no button.
// The button is the table's toolbar's, of whichever kind (GToolbarController.addFilterToggle) - a member where a view
// draws the toolbar. What the platform draws of the filters is driven with the table through here too
public final class GFilterControlsToggle implements GUserFiltersController.ApplyControls, GUserFiltersController.View {
    // the ROWS of the table's user filters as the toggle drives them - the platform's (GPlatformFilterRows), or none -:
    // whether a row is drawn at all - the toolbar's button is there for it -, whether the box has one now - the
    // controls hide once the last one is gone -, their selectors shown with the controls; and the rows driven with the
    // table - what draws the rows has changed, the table shown or hidden, the form closing
    public interface Rows {
        boolean drawsRows();
        boolean hasBoxRows();
        void setControlsVisible(boolean visible);
        void update();
        void setVisible(boolean visible);
        void destroy();
    }

    // what lays the form out anew once the controls are shown or hidden (GFormLayout.update) - handed in, so a test
    // needs no form
    private final Runnable relayout;
    private final GFilterControlsController controls;
    private final GToolbarController<?> toolbar;
    // the rows of the table's user filters it drives, none of them too - given with the input (bind)
    private Rows rows;
    // what shows and hides the controls: the table's toolbar, where it has something of the platform's to show and
    // hide - none where it would show and hide nothing of the platform's: nothing would show them again then, so they
    // are always shown
    private GToolbarController<?> shownBy;
    private boolean visible = false;
    // the table's user filters, whose manual apply mode follows the apply button shown (isApplyShown)
    private GUserFiltersController userFilters;
    // whether there was a row, as the model was drawn last: the controls hide once the last one is gone (changed)
    private boolean hadRows;

    // made by the platform where it wires a table's user filters up (GFormController.initializeTable)
    public GFilterControlsToggle(Runnable relayout, GFilterControlsController controls, GToolbarController<?> toolbar) {
        this.relayout = relayout;
        this.controls = controls;
        this.toolbar = toolbar;
    }

    // ... and given the input, once it is made - the controls asking it what they offer (GFiltersHandler) -, with what
    // the platform draws of the filters as rows, all of it made then: what the button is there for is known. A view of
    // the model after the rows (changed) (GReactFormData.createFiltersControllers)
    public void bind(GFilterInputController filter, GUserFiltersController userFilters, Rows rows) {
        this.userFilters = userFilters;
        this.rows = rows;
        shownBy = rows.drawsRows() || controls.isShownByToolbar() ? toolbar : null;
        if (shownBy != null)
            shownBy.addFilterToggle(filter);
        else
            visible = true;
        rows.setControlsVisible(visible);

        controls.bind(filter, userFilters);
        controls.setVisible(visible);
        userFilters.addView(this);
    }
    // whether the table's apply button is shown: the manual apply mode asks it (GUserFiltersController.isManual)
    public boolean isApplyShown() {
        return controls.isApplyShown(visible);
    }

    public boolean isVisible() {
        return visible;
    }

    // the controls shown or hidden, the rows' selectors with them, the form laid out anew
    public void setVisible(boolean visible) {
        if (shownBy == null) // nothing shows or hides them: they are always shown
            return;

        this.visible = visible;
        rows.setControlsVisible(visible);

        controls.setVisible(visible);
        relayout.run();

        shownBy.showFilterControls(visible);
        if (userFilters != null)
            userFilters.modeChanged();
    }

    // ===== WITH THE TABLE, as its input is told (GFilterInputController): what draws the rows has changed, once per
    // delta - the rows read the columns anew, built the first time -; the table shown or hidden - the rows and the
    // controls with it -; the form closing - the rows let go of
    public void update() {
        rows.update();
        hadRows = hasRows();
    }
    public void setTableVisible(boolean visible) {
        controls.setTableVisible(visible && this.visible);
        rows.setVisible(visible);
    }
    public void destroy() {
        rows.destroy();
    }

    // ===== THE MODEL CHANGED, its rows drawn: the controls hidden once the last row is gone - a row of the platform's
    // box, a FILTER's, React drawing it or not, for good -, the new state taken first, as the hiding tells the model's
    // views again
    public void changed(GUserFiltersController.Change change) {
        boolean hasRows = hasRows();
        boolean gone = hadRows && !hasRows;
        hadRows = hasRows;
        if (gone && visible)
            setVisible(false);
    }
    private boolean hasRows() {
        return userFilters.hasFilters() || rows.hasBoxRows();
    }
}
