package lsfusion.gwt.client.form.object.table.controller;

import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.view.Column;

import java.util.LinkedHashMap;
import java.util.List;

public interface GTableController {
    GGroupObjectValue getCurrentKey();
    GGroupObject getSelectedGroupObject();
    List<GPropertyDraw> getGroupObjectProperties();
    List<GObject> getObjects();
    List<GPropertyDraw> getPropertyDraws();
    GPropertyDraw getSelectedFilterProperty();
    GGroupObjectValue getSelectedColumnKey();
    Object getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey);
    List<Pair<Column, String>> getSelectedColumns();
    GContainer getFiltersContainer();

    GFormController getForm();

    boolean changeOrders(GGroupObject groupObject, LinkedHashMap<GPropertyDraw, Boolean> value, boolean alreadySet);
}
