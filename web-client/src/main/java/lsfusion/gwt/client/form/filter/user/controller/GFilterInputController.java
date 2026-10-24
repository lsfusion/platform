package lsfusion.gwt.client.form.filter.user.controller;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.filter.user.GUserFiltersController;
import lsfusion.gwt.client.form.filter.user.view.GFiltersHandler;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.property.GPropertyDraw;

// the INPUT of a group's user filters - or of a tree's, one for all its groups -, where the platform wires one up: what
// the platform's filter UI around the rows gives - the keys of the table, the controls' commands (GFiltersHandler), the
// toolbar's filter button -, written into the table's user filters (GUserFiltersController) with what the rows tell it
// (GTableController): the cell a condition starts on, the columns. Of the kind of whoever draws what the FILTERS box
// holds, chosen once (GReactFormData.createFiltersControllers): the platform's over its panel
// (GPlatformFilterInputController) - F3 prepares a value in a row, its editor started -, or the input over the
// conditions a view draws, which writes a condition at once - the view's part shows it
// (GReactFormData.ReactFilterInputController). A kind has its keys, and whether the design has the box; the rest is
// both kinds', here: the apply and the reset, and what the table tells it, passed on through the toggle of what of the
// filters the platform draws around the table - its rows, its controls - (GFilterControlsToggle), one for both kinds
public abstract class GFilterInputController implements GFiltersHandler {
    // what draws the rows: the cell a condition starts on, the columns
    protected final GTableController rows;
    // the toggle of what of the filters the platform draws around the table, which binds the input once it is made -
    // the controls give its commands (GFilterControlsToggle.bind)
    protected final GFilterControlsToggle toggle;
    // the table's user filters, which it writes into
    protected final GUserFiltersController userFilters;

    protected GFilterInputController(GTableController rows, GFilterControlsToggle toggle, GUserFiltersController userFilters) {
        this.rows = rows;
        this.toggle = toggle;
        this.userFilters = userFilters;
    }

    // the controls shown or hidden - by the toolbar's button, or by a key that starts a condition - and whether they
    // are, which the toolbar shows
    public boolean isControlsVisible() {
        return toggle.isVisible();
    }
    public void toggleControlsVisible() {
        setControlsVisible(!toggle.isVisible());
    }
    public void setControlsVisible(boolean visible) {
        toggle.setVisible(visible);
    }
    // what draws the rows has changed, once per delta - told by a table of the platform's before it shows or hides
    // what of the filters the platform draws, by the projection where React draws the rows: the platform's rows are
    // built the first time, the table's columns known then
    public void update() {
        toggle.update();
    }
    // the table shown or hidden: what of the filters the platform draws with it - its rows, the controls
    public void setVisible(boolean visible) {
        toggle.setTableVisible(visible);
    }

    // ===== the commands, as the keys of the table give them to whichever kind it has - F3 adds a condition, Alt+F3
    // (replace) makes it the only one, typing in a column starts one on the panel, whose editor takes the key, and
    // nothing where a view draws the conditions -, and as Escape asks: whether there is anything to reset. The
    // controls' add, apply and reset are GFiltersHandler's; a pivot's drill-down states its list in the model
    // (GUserFiltersController.replaceConditions), whoever draws it
    public abstract void addCondition(Event keyEvent, boolean replace, boolean checkExistingFilter);
    public abstract void quickEditFilter(Event keyEvent, GPropertyDraw property, GGroupObjectValue columnKey);
    // the controls' apply - what waits sent: what is written in the manual apply mode, what the platform's rows prepare
    // in either -, and their reset, Shift+F3 and Escape: every condition off, sent at once
    public void applyConditions() {
        userFilters.apply();
    }
    public void resetConditions() {
        userFilters.reset();
    }
    // ... whether there is anything to reset: an entry of the box's - one with no condition too, a row just added -, a
    // condition, or what the server still filters by, a change of it waiting
    public boolean hasConditionsToReset() {
        return !userFilters.getFreeEntries().isEmpty() || userFilters.hasActive() || userFilters.isPending();
    }

    // the form is closing: what it registered for the life of the page let go of - the platform's rows of the
    // conditions, whose value cells are property values. Told by what draws the rows it is given to
    // (GTableController.bindFilter): a table of the platform's as it goes, the projection for the rows React draws
    public void destroy() {
        toggle.destroy();
    }
}
