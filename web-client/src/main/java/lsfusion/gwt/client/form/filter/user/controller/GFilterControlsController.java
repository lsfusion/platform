package lsfusion.gwt.client.form.filter.user.controller;

import lsfusion.gwt.client.form.filter.user.view.GFilterEditors;
import lsfusion.gwt.client.form.filter.user.view.GFiltersHandler;

// the form's controller of a TABLE's FILTERCONTROLS - a list's grid's, or a tree's, one for all its groups -, of the
// kind of whoever draws it, chosen once (GReactFormData.createFilterControlsController): the platform's buttons
// (GPlatformFilterControlsController), or a view's part of the table (GReactFormData.FilterControlsPart). Either gives
// the commands of the table's filter input controller - add, reset (GFiltersHandler) - and the apply of the platform's
// editors of its conditions (GFilterEditors), and shows what the controller tells it
public interface GFilterControlsController {
    // the filter input controller whose commands they give, once it is made - none where the platform makes none -, and
    // the platform's editors of its conditions, whose apply they give - none where a view draws the conditions
    void bind(GFiltersHandler handler, GFilterEditors editors);
    // whether the table's toolbar shows and hides them: the platform's buttons - what a view draws is the view's to
    // show
    boolean isShownByToolbar();

    // shown or hidden, as the table's toolbar shows and hides the filter controls - with them the manual apply mode,
    // which asks for the apply button (GFilterEditors.isManualApplyMode)
    void setVisible(boolean visible);
    // the conditions typed into the editors wait to be applied: what lights the apply button
    void setApplyEnabled(boolean enabled);
    // the table shown or hidden, which its filter input controller tells whichever controls it has, through the toggle
    // (GFilterControlsToggle.setTableVisible): the platform's buttons hide with the table; a view's part has nothing of
    // the platform's to hide - what a view draws is the view's to show
    void setTableVisible(boolean visible);
    // what stands for the platform's where its toolbar turns its filter UI off (GToolbar.showFilters, a flag of the
    // platform's alone): nothing bound, nothing shown, nothing for the toolbar to show
    GFilterControlsController NONE = new GFilterControlsController() {
        public void bind(GFiltersHandler handler, GFilterEditors editors) {
        }
        public boolean isShownByToolbar() {
            return false;
        }
        public void setVisible(boolean visible) {
        }
        public void setApplyEnabled(boolean enabled) {
        }
        public void setTableVisible(boolean visible) {
        }
    };
}
