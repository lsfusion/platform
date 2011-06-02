package platform.gwt.form.client.ui;

import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import platform.gwt.form.client.FormFrame;
import platform.gwt.view.GForm;
import platform.gwt.view.GGroupObject;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.changes.GFormChanges;
import platform.gwt.view.changes.GGroupObjectValue;

import java.util.ArrayList;

public class GGroupObjectController {
    private final FormFrame frame;
    private final GForm form;
    public GGroupObject groupObject;

    public GGridTable grid;
    public GGroupPanel panel;

    public VLayout view;
    private SectionStackSection groupSection;

    public GGroupObjectController(FormFrame iframe, GForm iform, GGroupObject igroupObject) {
        this.frame = iframe;
        this.form = iform;
        this.groupObject = igroupObject;

        grid = new GGridTable(frame, form, this);
        grid.setCanSort(false);
        grid.setShowHeaderContextMenu(false);
        grid.setShowHeaderMenuButton(false);
        grid.setHeight(130);

        panel = new GGroupPanel(frame, form, this);

        view = new VLayout();
        view.setAutoHeight();
        view.addMember(grid);
        view.addMember(panel);

        groupSection = new SectionStackSection(groupObject.getCaption());
        groupSection.setItems(view);
        groupSection.setExpanded(true);
    }

    public void processFormChanges(GFormChanges fc) {
        for (GPropertyDraw property : fc.dropProperties) {
            if (property.groupObject == groupObject) {
                removeProperty(property);
            }
        }

        for (GPropertyDraw property : fc.properties.keySet()) {
            if (property.groupObject == groupObject) {
                if (fc.panelProperties.contains(property)) {
                    addPanelProperty(property);
                    panel.setValue(property, fc.properties.get(property));
                } else {
                    addGridProperty(property);
                    grid.setValues(property, fc.properties.get(property));
                }
            }
        }

        ArrayList<GGroupObjectValue> keys = fc.gridObjects.get(groupObject);
        if (keys != null) {
            grid.setKeys(keys);
        }

        GGroupObjectValue currentKey = fc.objects.get(groupObject);
        if (currentKey != null) {
            grid.setCurrentKey(currentKey);
        }

        update();
    }

    private void removeProperty(GPropertyDraw property) {
        panel.removeProperty(property);
        grid.removeProperty(property);
    }

    private void addGridProperty(GPropertyDraw property) {
        grid.addProperty(property);
        panel.removeProperty(property);
    }

    private void addPanelProperty(GPropertyDraw property) {
        grid.removeProperty(property);
        panel.addProperty(property);
    }

    private void update() {
        panel.update();
        grid.update();

        groupSection.setHidden(panel.isEmpty() && grid.isEmpty());
    }

    public SectionStackSection getSection() {
        return groupSection;
    }

}
