package lsfusion.gwt.client.form.object.table.controller;

import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;
import lsfusion.gwt.client.form.object.GGroupObject;

import java.util.ArrayList;

// what shows a group's USER FILTERS: the list, told it ready - as the server reports it, or as the form sends it -, not
// the input the platform makes a new list of (GFilterInputController). Settled once where the design says who draws its
// FILTERS box (GFormController.userFiltersControllers): React's part of them where a view draws what the box holds
// (GReactFormData.FiltersPart), whether the platform wires a filter input controller up or not, else the platform's
// (GPlatformFiltersController) where it builds one, else none (NONE) - none of a group in PANEL view either, which
// shows no user filters, as the platform shows none for it
public interface GUserFiltersController {
    // the group's user filters as the server reports them, which it has applied already: shown, not sent
    void updateFilters(GGroupObject group, ArrayList<GPropertyFilterDTO> filters);
    // ... and as the form has just sent them, before the server answers, told to whichever kind shows them
    // (GFormController.applyCurrentFilters) - which the platform's filter panel, the one that sent them, shows already
    void changeFilters(GGroupObject group, ArrayList<GPropertyFilterDTO> filters);

    // what shows them where nothing does - no view draws what the box holds, and the platform builds no panel, or the
    // group is in PANEL view: nothing, the form keeping them all the same (GFormController.currentFilters)
    GUserFiltersController NONE = new GUserFiltersController() {
        public void updateFilters(GGroupObject group, ArrayList<GPropertyFilterDTO> filters) {
        }
        public void changeFilters(GGroupObject group, ArrayList<GPropertyFilterDTO> filters) {
        }
    };
}
