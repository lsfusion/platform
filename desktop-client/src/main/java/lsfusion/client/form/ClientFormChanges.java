package lsfusion.client.form;

import lsfusion.base.BaseUtils;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.grid.ClientGridProperty;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.ClientPropertyReader;
import lsfusion.interop.form.property.PropertyReadType;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

public class ClientFormChanges {

    public final Map<ClientGroupObject, ClientGroupObjectValue> objects;

    // assertion что ObjectInstance из того же GroupObjectInstance
    public final Map<ClientGroupObject, List<ClientGroupObjectValue>> gridObjects;

    // assertion для ключа GroupObjectInstance что в значении ObjectInstance из верхних GroupObjectInstance TreeGroupInstance'а этого ключа,
    // так же может быть ObjectInstance из этого ключа если GroupObject - отображается рекурсивно (тогда надо цеплять к этому GroupObjectValue, иначе к верхнему)
    public Map<ClientGroupObject, List<ClientGroupObjectValue>> parentObjects;

    public final Map<ClientGroupObject, Map<ClientGroupObjectValue, Integer>> expandables;

    public final Map<ClientPropertyReader, Map<ClientGroupObjectValue, Object>> properties; // keys of the values in that map are "grid" objects (for panelProperties there are no "grid" objects) + "group-columns" objects 
    public final Set<ClientPropertyDraw> updateProperties; // needed for async changes, that check means that property was not changed, so async change should be dropped (old value need to be set)

    public final Set<ClientPropertyDraw> dropProperties;

    public final Map<ClientGroupObject, Boolean> updateStateObjects;

    public final List<ClientComponent> activateTabs;
    public final List<ClientPropertyDraw> activateProps;
    
    public final List<ClientContainer> collapseContainers;
    public final List<ClientContainer> expandContainers;

    public final boolean needConfirm;

    public final int size;

    public ClientFormChanges(byte[] formChanges, ClientForm clientForm) throws IOException {
        DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(formChanges));

        objects = new HashMap<>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientGroupObject clientGroupObject = clientForm.getGroupObject(inStream.readInt());
            objects.put(clientGroupObject, new ClientGroupObjectValue(inStream, clientForm));
        }

        gridObjects = readGridObjectsMap(inStream, clientForm);
        parentObjects = readGridObjectsMap(inStream, clientForm);

        expandables = new HashMap<>();
        count = inStream.readInt();
        for (int i = 0; i < count; ++i) {
            ClientGroupObject groupObject = clientForm.getGroupObject(inStream.readInt());
            Map<ClientGroupObjectValue, Integer> groupExpandables = new HashMap<>();
            int cnt = inStream.readInt();
            for (int j = 0; j < cnt; ++j) {
                ClientGroupObjectValue key = new ClientGroupObjectValue(inStream, clientForm);
                int expandable = inStream.readInt();
                groupExpandables.put(key, expandable);
            }

            expandables.put(groupObject, groupExpandables);
        }

        properties = new HashMap<>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {

            ClientPropertyReader clientPropertyRead = deserializePropertyReader(clientForm, inStream);

            Map<ClientGroupObjectValue, Object> propertyValues = new HashMap<>();
            int mapCount = inStream.readInt();
            for (int j = 0; j < mapCount; j++) {
                propertyValues.put(new ClientGroupObjectValue(inStream, clientForm), BaseUtils.deserializeObject(inStream));
            }

            properties.put(clientPropertyRead, propertyValues);
        }

        updateProperties = new HashSet<>();

        //DropProperties
        dropProperties = new HashSet<>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            dropProperties.add(clientForm.getProperty(inStream.readInt()));
        }

        //DropProperties
        updateStateObjects = new HashMap<>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            updateStateObjects.put(clientForm.getGroupObject(inStream.readInt()), inStream.readBoolean());
        }

        //ActivateTabs
        activateTabs = new ArrayList<>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            activateTabs.add(clientForm.findContainerByID(inStream.readInt()));
        }

        //ActivateProps
        activateProps = new ArrayList<>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            activateProps.add(clientForm.getProperty(inStream.readInt()));
        }

        //CollapseContainers
        collapseContainers = new ArrayList<>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            collapseContainers.add(clientForm.findContainerByID(inStream.readInt()));
        }

        //ExpandContainers
        expandContainers = new ArrayList<>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            expandContainers.add(clientForm.findContainerByID(inStream.readInt()));
        }

        needConfirm = inStream.readBoolean();
        size = formChanges.length;
    }

    private ClientPropertyReader deserializePropertyReader(ClientForm clientForm, DataInputStream inStream) throws IOException {
        switch (inStream.readByte()) {
            case PropertyReadType.DRAW:
                return clientForm.getProperty(inStream.readInt());
            case PropertyReadType.CAPTION:
                return clientForm.getProperty(inStream.readInt()).captionReader;
            case PropertyReadType.SHOWIF:
                return clientForm.getProperty(inStream.readInt()).showIfReader;
            case PropertyReadType.FOOTER:
                return clientForm.getProperty(inStream.readInt()).footerReader;
            case PropertyReadType.CELL_VALUEELEMENTCLASS:
                return clientForm.getProperty(inStream.readInt()).valueElementClassReader;
            case PropertyReadType.CAPTIONELEMENTCLASS:
                return clientForm.getProperty(inStream.readInt()).captionElementClassReader;
            case PropertyReadType.CELL_FONT:
                return clientForm.getProperty(inStream.readInt()).fontReader;
            case PropertyReadType.CELL_BACKGROUND:
                return clientForm.getProperty(inStream.readInt()).backgroundReader;
            case PropertyReadType.CELL_FOREGROUND:
                return clientForm.getProperty(inStream.readInt()).foregroundReader;
            case PropertyReadType.READONLY:
                return clientForm.getProperty(inStream.readInt()).readOnlyReader;
            case PropertyReadType.ROW_BACKGROUND:
                return clientForm.getGroupObject(inStream.readInt()).rowBackgroundReader;
            case PropertyReadType.ROW_FOREGROUND:
                return clientForm.getGroupObject(inStream.readInt()).rowForegroundReader;
            case PropertyReadType.LAST:
                return clientForm.getProperty(inStream.readInt()).lastReaders.get(inStream.readInt());
            case PropertyReadType.CONTAINER_CAPTION:
                return clientForm.findContainerByID(inStream.readInt()).captionReader;
            case PropertyReadType.CONTAINER_IMAGE:
                return clientForm.findContainerByID(inStream.readInt()).imageReader;
            case PropertyReadType.CONTAINER_CAPTIONCLASS:
                return clientForm.findContainerByID(inStream.readInt()).captionClassReader;
            case PropertyReadType.CONTAINER_VALUECLASS:
                return clientForm.findContainerByID(inStream.readInt()).valueClassReader;
            case PropertyReadType.IMAGE:
                return clientForm.getProperty(inStream.readInt()).imageReader;
            case PropertyReadType.CUSTOM:
                return clientForm.findContainerByID(inStream.readInt()).customPropertyDesignReader;
            case PropertyReadType.COMPONENT_SHOWIF:
                return clientForm.findComponentByID(inStream.readInt()).showIfReader;
            case PropertyReadType.COMPONENT_ELEMENTCLASS:
                return clientForm.findComponentByID(inStream.readInt()).elementClassReader;
            case PropertyReadType.GRID_VALUECLASS:
                return ((ClientGridProperty)clientForm.findComponentByID(inStream.readInt())).valueElementClassReader;
            case PropertyReadType.CUSTOM_OPTIONS:
                return clientForm.getGroupObject(inStream.readInt()).customOptionsReader;
            case PropertyReadType.COMMENT:
                return clientForm.getProperty(inStream.readInt()).commentReader;
            case PropertyReadType.COMMENTELEMENTCLASS:
                return clientForm.getProperty(inStream.readInt()).commentElementClassReader;
            case PropertyReadType.PLACEHOLDER:
                return clientForm.getProperty(inStream.readInt()).placeholderReader;
            case PropertyReadType.TOOLTIP:
                return clientForm.getProperty(inStream.readInt()).tooltipReader;
            case PropertyReadType.VALUETOOLTIP:
                return clientForm.getProperty(inStream.readInt()).valueTooltipReader;
            case PropertyReadType.PATTERN:
                return clientForm.getProperty(inStream.readInt()).patternReader;
            case PropertyReadType.REGEXP:
                return clientForm.getProperty(inStream.readInt()).regexpReader;
            case PropertyReadType.REGEXPMESSAGE:
                return clientForm.getProperty(inStream.readInt()).regexpMessageReader;
            default:
                throw new IOException();
        }
    }

    private Map<ClientGroupObject, List<ClientGroupObjectValue>> readGridObjectsMap(DataInputStream inStream, ClientForm clientForm) throws IOException {
        Map<ClientGroupObject, List<ClientGroupObjectValue>> gridObjects = new HashMap<>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientGroupObject clientGroupObject = clientForm.getGroupObject(inStream.readInt());

            List<ClientGroupObjectValue> clientGridObjects = new ArrayList<>();
            int listCount = inStream.readInt();
            for (int j = 0; j < listCount; j++) {
                clientGridObjects.add(new ClientGroupObjectValue(inStream, clientForm));
            }

            gridObjects.put(clientGroupObject, clientGridObjects);
        }

        return gridObjects;
    }
}
