package lsfusion.client.form.object.table.controller;

import lsfusion.base.Pair;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.ClientObject;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.view.Column;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface TableController {

    void updateCellBackgroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values);
    void updateCellForegroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values);
    void updateDrawPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values, boolean update);
    void updatePropertyCaptions(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values);
    void updateShowIfValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values);
    void updateReadOnlyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values);
    void updateRowBackgroundValues(Map<ClientGroupObjectValue, Object> values);
    void updateRowForegroundValues(Map<ClientGroupObjectValue, Object> values);
    void updateImageValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values);

    ClientFormController getFormController();
    ClientGroupObject getSelectedGroupObject();
    ClientGroupObject getGroupObject();
    List<ClientPropertyDraw> getGroupObjectProperties();
    List<ClientObject> getObjects();
    List<ClientPropertyDraw> getPropertyDraws();
    ClientPropertyDraw getSelectedFilterProperty();
    ClientGroupObjectValue getSelectedColumn();
    Object getSelectedValue(ClientPropertyDraw property, ClientGroupObjectValue columnKey);
    List<Pair<Column, String>> getSelectedColumns();
    
    ClientContainer getFiltersContainer();

    boolean changeOrders(ClientGroupObject groupObject, LinkedHashMap<ClientPropertyDraw, Boolean> value, boolean alreadySet);
}
