package platform.gwt.form.client.ui;

import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VStack;
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
    public GFilterPanel filterPanel;

    public VStack view;
    private SectionStackSection groupSection;

    public GGroupObjectController(FormFrame iframe, GForm iform, GGroupObject igroupObject) {
        this.frame = iframe;
        this.form = iform;
        this.groupObject = igroupObject;

        grid = new GGridTable(frame, form, this);
        grid.setCanSort(false);
        grid.setShowHeaderContextMenu(false);
        grid.setShowHeaderMenuButton(false);
        grid.setAutoFitData(Autofit.VERTICAL);
        grid.setAutoFitMaxRecords(10);

        filterPanel = new GFilterPanel();

        panel = new GGroupPanel(frame, form, this);

        view = new VStack();
        view.setHeight(1);
        view.setOverflow(Overflow.VISIBLE);
        view.addMember(grid);
        view.addMember(filterPanel);
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

    public void addFilterComponent(FormItem item) {
        filterPanel.addFilterComponent(item);
    }

    private void update() {
        grid.update();
        panel.update();

        groupSection.setHidden(panel.isEmpty() && grid.isEmpty() && filterPanel.isEmpty());
    }

    public SectionStackSection getSection() {
        return groupSection;
    }
}
