package lsfusion.gwt.client.form.object.table.controller;

import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.view.Column;

import java.util.LinkedHashMap;

// what shows a group's USER ORDERS, settled once where the design says who draws its rows
// (GFormController.ordersControllers): React's part of them where a view draws the rows (GReactFormData.OrdersPart),
// else the group's controller (GGroupController extends it) - the platform's grid or tree, whose header shows them. A
// sorting is the order the rows are drawn in, so it is shown wherever they are
public interface GUserOrdersController {
    // the group's user orders as the server reports them - the form's own ORDERS in the first changes, then every list
    // it sets -: shown, not sent; true - ascending
    void updateOrders(GGroupObject group, LinkedHashMap<Column, Boolean> orders);
    // ... and as the form changes them, before the server answers - which a header only asks for, so it shows them as
    // it shows the reported ones
    void changeOrders(GGroupObject group, LinkedHashMap<Column, Boolean> orders);
}
