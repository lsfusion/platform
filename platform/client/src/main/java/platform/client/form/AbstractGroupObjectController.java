package platform.client.form;

import platform.client.form.cell.PropertyController;
import platform.client.form.panel.PanelController;
import platform.client.form.panel.PanelShortcut;
import platform.client.form.panel.PanelToolbar;
import platform.client.logics.ClientObject;
import platform.client.logics.ClientPropertyDraw;

import java.awt.*;
import java.util.*;
import java.util.List;

public abstract class AbstractGroupObjectController implements GroupObjectLogicsSupplier {
    protected final ClientFormController form;
    protected final ClientFormLayout formLayout;

    protected final LogicsSupplier logicsSupplier;

    protected PanelController panel;
    protected PanelToolbar panelToolbar;
    protected Set<ClientPropertyDraw> panelProperties = new HashSet<ClientPropertyDraw>();

    public PanelShortcut panelShortcut;

    public AbstractGroupObjectController(ClientFormController form, LogicsSupplier logicsSupplier, ClientFormLayout formLayout) {
        this.form = form;
        this.logicsSupplier = logicsSupplier;
        this.formLayout = formLayout;

        panelToolbar = new PanelToolbar(form, formLayout);
    }

    public ClientFormController getForm() {
        return form;
    }

    // реализация LogicsSupplier
    @Override
    public List<ClientObject> getObjects() {
        return logicsSupplier.getObjects();
    }

    public void addPropertyToToolbar(PropertyController property) {
        panelToolbar.addProperty(property);
    }

    public void addToToolbar(Component component) {
        panelToolbar.addComponent(component);
    }

    public void removePropertyFromToolbar(PropertyController property) {
        panelToolbar.removeProperty(property);
    }

    public void addPropertyToShortcut(PropertyController property) {
        panelShortcut.addProperty(property);
    }

    public void removePropertyFromShortcut(PropertyController property) {
        panelShortcut.removeProperty(property);
    }

    public void showShortcut(Component invoker, Point point, ClientPropertyDraw currentProperty) {
        panelShortcut.setCurrentProperty(currentProperty);
        panelShortcut.show(invoker, point);
    }

    public boolean hasDefaultAction(ClientPropertyDraw currentProperty) {
        return panelShortcut.hasDefaultAction(currentProperty);
    }

    public boolean invokeDefaultAction(ClientPropertyDraw currentProperty) {
        return panelShortcut.invokeDefaultAction(currentProperty);
    }
}
