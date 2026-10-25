package lsfusion.gwt.client.form.filter.user.controller;

import lsfusion.gwt.client.form.filter.user.GUserFiltersController;
import lsfusion.gwt.client.form.filter.user.view.GFiltersHandler;

// the form's controller of a TABLE's FILTERCONTROLS - a list's grid's, or a tree's, one for all its groups -, of the
// kind of whoever draws it, chosen once (GReactFormData.createFilterControlsController): the platform's buttons
// (GPlatformFilterControlsController), or a view's part of the table (GReactFormData.FilterControlsPart). Either gives
// the commands of the table's filter input controller - add, apply, reset (GFiltersHandler) - and shows the table's
// user filters' manual apply mode, a view of them (GUserFiltersController.View)
public interface GFilterControlsController {
    // the filter input controller whose commands they give, once it is made - none where the platform makes none -, and
    // the table's user filters, which they show
    void bind(GFiltersHandler handler, GUserFiltersController userFilters);
    // whether the table's toolbar shows and hides them: the platform's buttons - what a view draws is the view's to
    // show
    boolean isShownByToolbar();
    // ... and whether the apply button is shown, which the manual apply mode asks for
    // (GUserFiltersController.isManual): the platform's while the toolbar shows them, a view's always - it draws them
    // itself - once bound to the commands it gives, none where there are none
    boolean isApplyShown(boolean shownByToolbar);

    // shown or hidden, as the table's toolbar shows and hides the filter controls
    void setVisible(boolean visible);
    // the table shown or hidden, which its filter input controller tells whichever controls it has, through the toggle
    // (GFilterControlsToggle.setTableVisible): the platform's buttons hide with the table; a view's part has nothing of
    // the platform's to hide - what a view draws is the view's to show
    void setTableVisible(boolean visible);
    // what stands for the platform's where its toolbar turns its filter UI off (GToolbar.showFilters, a flag of the
    // platform's alone): nothing bound, nothing shown, no apply button - no manual apply mode
    GFilterControlsController NONE = new GFilterControlsController() {
        public void bind(GFiltersHandler handler, GUserFiltersController userFilters) {
        }
        public boolean isShownByToolbar() {
            return false;
        }
        public boolean isApplyShown(boolean shownByToolbar) {
            return false;
        }
        public void setVisible(boolean visible) {
        }
        public void setTableVisible(boolean visible) {
        }
    };
}
