package platform.client.interop;

import platform.base.BaseUtils;
import platform.interop.form.client.AbstractFormChanges;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

public class ClientFormChanges extends AbstractFormChanges<ClientGroupObjectImplementView, ClientGroupObjectValue, ClientPropertyView> {

    public ClientFormChanges(DataInputStream inStream,ClientFormView clientFormView) throws IOException {
        
        classViews = new HashMap<ClientGroupObjectImplementView, Boolean>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++)
            classViews.put(clientFormView.getGroupObject(inStream.readInt()),inStream.readBoolean());

        objects = new HashMap<ClientGroupObjectImplementView, ClientGroupObjectValue>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientGroupObjectImplementView clientGroupObject = clientFormView.getGroupObject(inStream.readInt());
            objects.put(clientGroupObject,new ClientGroupObjectValue(inStream, clientGroupObject));
        }

        gridObjects = new HashMap<ClientGroupObjectImplementView, List<ClientGroupObjectValue>>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientGroupObjectImplementView clientGroupObject = clientFormView.getGroupObject(inStream.readInt());
            
            List<ClientGroupObjectValue> clientGridObjects = new ArrayList<ClientGroupObjectValue>();
            int listCount = inStream.readInt();
            for (int j = 0; j < listCount; j++)
                clientGridObjects.add(new ClientGroupObjectValue(inStream, clientGroupObject));

            gridObjects.put(clientGroupObject, clientGridObjects);
        }

        gridProperties = new HashMap<ClientPropertyView, Map<ClientGroupObjectValue, Object>>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientPropertyView clientPropertyView = clientFormView.getPropertyView(inStream.readInt());

            Map<ClientGroupObjectValue, Object> gridPropertyValues = new HashMap<ClientGroupObjectValue, Object>();
            int mapCount = inStream.readInt();
            for (int j = 0; j < mapCount; j++)
                gridPropertyValues.put(new ClientGroupObjectValue(inStream, clientPropertyView.groupObject),
                                   BaseUtils.deserializeObject(inStream));

            gridProperties.put(clientPropertyView, gridPropertyValues);
        }

        panelProperties = new HashMap<ClientPropertyView, Object>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            panelProperties.put(clientFormView.getPropertyView(inStream.readInt()),BaseUtils.deserializeObject(inStream));
        }

        //DropProperties
        dropProperties = new HashSet<ClientPropertyView>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++)
            dropProperties.add(clientFormView.getPropertyView(inStream.readInt()));
    }
}
