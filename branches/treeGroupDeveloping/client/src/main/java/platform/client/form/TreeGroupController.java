package platform.client.form;

import platform.client.form.tree.TreeView;
import platform.client.logics.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TreeGroupController {

    public final ClientTreeGroup treeGroup;
    private final ClientFormController form;
    private final TreeView view;

    public TreeGroupController(ClientTreeGroup iTreeGroup, ClientFormController iform, ClientFormLayout formLayout) throws IOException {
        treeGroup = iTreeGroup;
        this.form = iform;

        view = new TreeView(this, form);

        addView(formLayout);
    }

    private void addView(ClientFormLayout formLayout) {
        formLayout.add(treeGroup, view);
    }

    public void processFormChanges(ClientFormChanges fc,
                                   Map<ClientGroupObject, List<ClientGroupObjectValue>> cachedGridObjects
    ) {
        for (ClientGroupObject groupObject : treeGroup.groups) {
            if (fc.treeObjects.containsKey(groupObject)) {
                view.updateKeys(groupObject, fc.gridObjects.get(groupObject), fc.treeObjects.get(groupObject), fc.treeRefresh.contains(groupObject));
            }

            // добавляем новые свойства
            for (ClientPropertyRead read : fc.properties.keySet()) {
                if (read instanceof ClientPropertyDraw) {
                    ClientPropertyDraw property = (ClientPropertyDraw) read;
                    if (property.groupObject == groupObject && property.shouldBeDrawn(form)) {
                        view.addDrawProperty(property);
                    }
                }
            }

            // удаляем ненужные
            for (ClientPropertyDraw property : fc.dropProperties) {
                if (property.groupObject == groupObject) {
                    view.removeProperty(property);
                }
            }

            // обновляем значения свойств
            for (Map.Entry<ClientPropertyRead, Map<ClientGroupObjectValue, Object>> readProperty : fc.properties.entrySet()) {
                ClientPropertyRead propertyRead = readProperty.getKey();
                if (propertyRead instanceof ClientPropertyDraw) {
                    ClientPropertyDraw propertyDraw = (ClientPropertyDraw) propertyRead;
                    if (propertyDraw.groupObject == groupObject) {
                        view.updateDrawPropertyValues(propertyDraw, readProperty.getValue());
                    }
                }
            }
        }
    }
}