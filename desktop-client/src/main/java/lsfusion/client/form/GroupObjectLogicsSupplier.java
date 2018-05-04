package lsfusion.client.form;

import lsfusion.client.logics.ClientGroupObject;
import lsfusion.client.logics.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Order;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface GroupObjectLogicsSupplier extends LogicsSupplier {

    ClientGroupObject getGroupObject();
    List<ClientPropertyDraw> getGroupObjectProperties();

    // пока зафигачим в этот интерфейс, хотя может быть в дальнейшем выделим в отдельный
    // данный интерфейс отвечает за получение текущих выбранных значений
    ClientPropertyDraw getSelectedProperty();
    Object getSelectedValue(ClientPropertyDraw property, ClientGroupObjectValue columnKey);
    
    ClientFormController getForm();

    void changeOrder(ClientPropertyDraw property, Order modiType) throws IOException;

    void clearOrders() throws IOException;

    ClientGroupObject getSelectedGroupObject();

    void updateDrawPropertyCaptions(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> captions);

    void updateShowIfs(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> showIfs);

    void updateReadOnlyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values);

    void updateRowBackgroundValues(Map<ClientGroupObjectValue, Object> rowBackground);

    void updateRowForegroundValues(Map<ClientGroupObjectValue, Object> rowForeground);

    void updateDrawPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values, boolean update);

    void updateCellBackgroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellBackgroundValues);

    void updateCellForegroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellForegroundValues);
}
