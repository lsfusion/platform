package lsfusion.gwt.client.form.object.table.controller;

import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;
import lsfusion.gwt.client.form.object.GGroupObject;

import java.util.ArrayList;

// what shows a group's USER FILTERS, settled once where the design says who draws its FILTERS box
// (GFormController.filtersControllers): React's part of them where a view draws the box (GReactFormData.FiltersPart),
// else the group's controller (GGroupController extends it) - the platform's grid or tree, whose filter panel shows
// them, or the node React draws the rows on, which shows none
public interface GUserFiltersController {
    // the group's user filters as the server reports them, which it has applied already: shown, not sent
    void updateFilters(GGroupObject group, ArrayList<GPropertyFilterDTO> filters);
    // ... and as the form has just sent them, before the server answers - which the platform's filter panel, the one
    // that sent them, shows already
    void changeFilters(GGroupObject group, ArrayList<GPropertyFilterDTO> filters);
}
