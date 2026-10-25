package lsfusion.gwt.client.form.filter.user.controller;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.filter.user.view.GFiltersHandler;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.property.GPropertyDraw;

// the INPUT of a group's user filters - or of a tree's, one for all its groups -, where the platform wires one up: what
// the platform's filter UI around the rows gives - the keys of the table, the controls' commands (GFiltersHandler), the
// toolbar's filter button -, written into the table's user filters (GUserFiltersController) with what the rows tell it
// (GTableController): the cell a condition starts on, the columns. Of the kind of whoever draws what the FILTERS box
// holds, chosen once (GReactFormData.createFiltersControllers): the platform's (GPlatformFiltersController), which
// draws the conditions too - F3 prepares a value in a row, its editor started -, or the input over the conditions a
// view draws, which writes a condition at once - the view's part shows it (GReactFormData.ReactFilterInputController).
// What both show and hide of the platform's - the controls, and the toolbar's button that shows them - is one rule for
// both (GFilterControlsToggle), held here with what draws the rows
public abstract class GFilterInputController implements GFiltersHandler {
    // what draws the rows: the cell a condition starts on, the columns
    protected final GTableController rows;
    // the controls and the toolbar's button, the platform's, which each kind binds once it is made - its commands, and
    // its editors where it has them (GFilterControlsToggle.bind)
    protected final GFilterControlsToggle toggle;

    protected GFilterInputController(GTableController rows, GFilterControlsToggle toggle) {
        this.rows = rows;
        this.toggle = toggle;
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
    // the table shown or hidden: what of the filters the platform draws with it - the controls
    public void setVisible(boolean visible) {
        toggle.setTableVisible(visible);
    }
    // what draws the rows has changed, once per delta - told by a table of the platform's before it shows or hides
    // what of the filters the platform draws, by the projection where React draws the rows: the platform's panel reads
    // anew the columns a condition can be on, and adds the design's conditions the first time
    public abstract void update();

    // ===== the commands, as the keys of the table give them to whichever kind it has - F3 adds a condition, Alt+F3
    // (replace) makes it the only one, typing in a column starts one on the panel, whose editor takes the key, and
    // nothing where a view draws the conditions -, and as Escape asks: whether there is anything to reset. The
    // controls' add, apply and reset are GFiltersHandler's; a pivot's drill-down states its list in the model
    // (GUserFiltersController.replaceConditions), whoever draws it
    public abstract void addCondition(Event keyEvent, boolean replace, boolean checkExistingFilter);
    public abstract void quickEditFilter(Event keyEvent, GPropertyDraw property, GGroupObjectValue columnKey);
    public abstract boolean hasConditionsToReset();

    // the form is closing: what it registered for the life of the page let go of - the platform's panel, whose
    // conditions' value cells are property values; nothing of the commands over the conditions a view draws. Told by
    // what draws the rows it is given to (GTableController.bindFilter): a table of the platform's as it goes, the
    // projection for the rows React draws
    public void destroy() {
    }
}
