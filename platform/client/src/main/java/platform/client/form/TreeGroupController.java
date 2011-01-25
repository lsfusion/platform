package platform.client.form;

import platform.client.form.tree.TreeView;
import platform.client.logics.*;

import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TreeGroupController {

    public final ClientTreeGroup treeGroup;
    private final ClientFormController form;
    private final TreeView view;

    public TreeGroupController(ClientTreeGroup treeGroup, ClientFormController form, ClientFormLayout formLayout) throws IOException {
        this.treeGroup = treeGroup;
        this.form = form;

        view = new TreeView(form);
        formLayout.add(treeGroup, view);
    }

    public void processFormChanges(ClientFormChanges fc,
                                   Map<ClientGroupObject, List<ClientGroupObjectValue>> cachedGridObjects
    ) {
        for (ClientGroupObject group : treeGroup.groups) {
            if (fc.gridObjects.containsKey(group)) {
                view.updateKeys(group, fc.gridObjects.get(group), fc.parentObjects.get(group));
            }

            // добавляем новые свойства
            for (ClientPropertyRead read : fc.properties.keySet()) {
                if (read instanceof ClientPropertyDraw) {
                    ClientPropertyDraw property = (ClientPropertyDraw) read;
                    if (property.groupObject == group && property.shouldBeDrawn(form)) {
                        view.addDrawProperty(property);
                    }
                }
            }

            // удаляем ненужные
            for (ClientPropertyDraw property : fc.dropProperties) {
                if (property.groupObject == group) {
                    view.removeProperty(property);
                }
            }

            // обновляем значения свойств
            for (Map.Entry<ClientPropertyRead, Map<ClientGroupObjectValue, Object>> readProperty : fc.properties.entrySet()) {
                ClientPropertyRead propertyRead = readProperty.getKey();
                if (propertyRead instanceof ClientPropertyDraw) {
                    ClientPropertyDraw propertyDraw = (ClientPropertyDraw) propertyRead;
                    if (propertyDraw.groupObject == group) {
                        view.updateDrawPropertyValues(propertyDraw, readProperty.getValue());
                    }
                }
            }

            view.setCurrentObjects(fc.objects.get(group));
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                view.getTree().updateUI();
           }
        });
    }
}