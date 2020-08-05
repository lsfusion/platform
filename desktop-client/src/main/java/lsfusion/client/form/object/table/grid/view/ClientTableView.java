package lsfusion.client.form.object.table.grid.view;

import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.form.object.table.grid.user.design.GroupObjectUserPreferences;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface ClientTableView {

    // SETTERS
    void setRowKeysAndCurrentObject(List<ClientGroupObjectValue> irowKeys, ClientGroupObjectValue newCurrentObject);
    void removeProperty(ClientPropertyDraw property);
    boolean changePropertyOrders(LinkedHashMap<ClientPropertyDraw, Boolean> value, boolean alreadySet); // assert alreadySet is true if there is no ordering in view
    void addProperty(ClientPropertyDraw newProperty);

    // EXTRA SETTERS
    // keys
    void updateRowBackgroundValues(Map<ClientGroupObjectValue, Object> values);
    void updateRowForegroundValues(Map<ClientGroupObjectValue, Object> values);

    // columns
    void updateCellBackgroundValues(ClientPropertyDraw propertyDraw, Map<ClientGroupObjectValue, Object> values);
    void updateCellForegroundValues(ClientPropertyDraw propertyDraw, Map<ClientGroupObjectValue, Object> values);
    void updatePropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values, boolean update);
    void updatePropertyCaptions(ClientPropertyDraw propertyDraw, Map<ClientGroupObjectValue, Object> values);
    void updateShowIfValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> showIfs);
    void updateReadOnlyValues(ClientPropertyDraw propertyDraw, Map<ClientGroupObjectValue, Object> values);
    void updateColumnKeys(ClientPropertyDraw property, List<ClientGroupObjectValue> columnKeys);

    // event - FINISH SETTER
    void update(Boolean updateState);

    // GETTERS
    int getCurrentRow();
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

    boolean hasUserPreferences();
    boolean containsProperty(ClientPropertyDraw property); // for user preferences
    OrderedMap<ClientPropertyDraw, Boolean> getUserOrders(List<ClientPropertyDraw> propertyDrawList);
    GroupObjectUserPreferences getCurrentUserGridPreferences();
    GroupObjectUserPreferences getGeneralGridPreferences();
}