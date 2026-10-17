package lsfusion.gwt.client.form.filter.user.controller;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.view.GFiltersHandler;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.ArrayList;

// the INPUT of a group's user filters - or of a tree's, one for all its groups -, where the platform wires one up: what
// the platform's filter UI around the rows gives - the keys of the table, the controls' commands (GFiltersHandler), a
// pivot's drill-down, the toolbar's filter button -, made a new list of conditions with what the rows tell it
// (GTableController): the cell a condition starts on, the columns. Not what shows the list, which is told it ready, as
// the server reports it or the form sends it (GUserFiltersController). Of the kind of whoever draws what the FILTERS
// box holds, chosen once (GReactFormData.createFiltersControllers): the platform's (GPlatformFiltersController),
// which draws the conditions and shows the list too - the input goes through its editors -, or the input over the
// conditions a view draws, which states the list anew at once - the view's part shows it
// (GReactFormData.ReactFilterInputController). What both show and hide of the platform's - the controls, and the
// toolbar's button that shows them - is one rule for both (GFilterControlsToggle)
public interface GFilterInputController extends GFiltersHandler {
    // the controls shown or hidden - by the toolbar's button, or by a key that starts a condition - and whether they
    // are, which the toolbar shows
    boolean isControlsVisible();
    void toggleControlsVisible();
    void setControlsVisible(boolean visible);
    // the table shown or hidden: what of the filters the platform draws with it
    void setVisible(boolean visible);
    // what draws the rows has changed, once per delta - told by a table of the platform's before it shows or hides
    // what of the filters the platform draws, by the projection where React draws the rows: the platform's panel reads
    // anew the columns a condition can be on, and adds the design's conditions the first time
    void update();

    // ===== the commands, as the keys of the table give them to whichever kind it has - F3 adds a condition, Alt+F3
    // (replace) makes it the only one, typing in a column starts one on the panel, whose editor takes the key, and
    // nothing where a view draws the conditions -, as a pivot's drill-down does - the conditions it states, in place of
    // the ones there are -, and as Escape asks: whether there is anything to reset. The controls' add and reset are
    // GFiltersHandler's; their apply is the panel's editors' (GFilterEditors)
    void addCondition(Event keyEvent, boolean replace, boolean checkExistingFilter);
    void quickEditFilter(Event keyEvent, GPropertyDraw property, GGroupObjectValue columnKey);
    void replaceConditions(ArrayList<GPropertyFilter> conditions);
    boolean hasConditionsToReset();

    // the form is closing: what it registered for the life of the page let go of - the platform's panel, whose
    // conditions' value cells are property values; nothing of the commands over the conditions a view draws. Told by
    // what draws the rows it is given to (GTableController.bindFilter): a table of the platform's as it goes, the
    // projection for the rows React draws
    default void destroy() {
    }
}
