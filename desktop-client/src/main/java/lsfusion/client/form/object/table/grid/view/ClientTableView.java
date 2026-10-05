package lsfusion.client.form.object.table.grid.view;

import lsfusion.base.Pair;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.view.Column;
import lsfusion.interop.form.object.table.grid.user.design.GroupObjectUserPreferences;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface ClientTableView {

    // SETTERS
    void setRowKeysAndCurrentObject(List<ClientGroupObjectValue> irowKeys, ClientGroupObjectValue newCurrentObject);
    void removeProperty(ClientPropertyDraw property);
    void updateOrders(LinkedHashMap<Column, Boolean> orders); // the orders the form has, shown
    void addProperty(ClientPropertyDraw newProperty);

    // EXTRA SETTERS
    // keys
    void updateRowBackgroundValues(Map<ClientGroupObjectValue, Object> values);
    void updateRowForegroundValues(Map<ClientGroupObjectValue, Object> values);

    // columns
    void updateCellBackgroundValues(ClientPropertyDraw propertyDraw, Map<ClientGroupObjectValue, Object> values);
    void updateCellForegroundValues(ClientPropertyDraw propertyDraw, Map<ClientGroupObjectValue, Object> values);
    void updateImageValues(ClientPropertyDraw propertyDraw, Map<ClientGroupObjectValue, Object> values);
    void updatePropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values, boolean update);
    void updatePropertyCaptions(ClientPropertyDraw propertyDraw, Map<ClientGroupObjectValue, Object> values);
    void updateShowIfValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> showIfs);
    void updateReadOnlyValues(ClientPropertyDraw propertyDraw, Map<ClientGroupObjectValue, Object> values);
    void updateColumnKeys(ClientPropertyDraw property, List<ClientGroupObjectValue> columnKeys);

    // event - FINISH SETTER
    void update(Boolean updateState);

    // GETTERS
    int getCurrentRow();
    ClientGroupObjectValue getCurrentKey();
    ClientGroupObjectValue getCurrentObject();
    ClientPropertyDraw getCurrentProperty(); // calculate sum / filtering default value
    ClientGroupObjectValue getCurrentColumn(); // calculate sum / filtering default value

    // focus
    void focusProperty(ClientPropertyDraw propertyDraw);
    boolean requestFocusInWindow();

    // add / delete
    void modifyGroupObject(ClientGroupObjectValue key, boolean add, int position);

    // toolbar features
    Object getSelectedValue(ClientPropertyDraw property, ClientGroupObjectValue columnKey); // for filter to set default value

    List<Pair<Column, String>> getFilterColumns();

    boolean hasUserPreferences();
    boolean containsProperty(ClientPropertyDraw property); // for user preferences
    GroupObjectUserPreferences getCurrentUserGridPreferences();
    GroupObjectUserPreferences getGeneralGridPreferences();
}