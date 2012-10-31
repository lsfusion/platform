package platform.gwt.form.shared.view.panel;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form.client.form.dispatch.GEditPropertyDispatcher;
import platform.gwt.form.client.form.dispatch.GEditPropertyHandler;
import platform.gwt.form.client.form.ui.GFormController;
import platform.gwt.form.shared.actions.form.ServerResponseResult;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.classes.GType;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.EditManagerAdapter;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.editor.PopupBasedGridEditor;

public class ActionPanelRenderer implements PanelRenderer, GEditPropertyHandler {

    private final GFormController form;
    private final GEditPropertyDispatcher editDispatcher;
    private final EditManager editManager = new ActionEditManager();
    private final GPropertyDraw property;
    private final GGroupObjectValue columnKey;

    private final ImageButton button;

    public ActionPanelRenderer(final GFormController iform, final GPropertyDraw iproperty, GGroupObjectValue icolumnKey) {
        this.form = iform;
        this.property = iproperty;
        this.columnKey = icolumnKey;
        this.editDispatcher = new GEditPropertyDispatcher(form);

        button = new ImageButton(property.caption, property.iconPath);
        button.addStyleName("panelActionProperty");
        if (property.preferredHeight > -1) {
            button.setHeight(property.preferredHeight + "px");
        }
        button.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                editDispatcher.executePropertyEditAction(ActionPanelRenderer.this, property, null, columnKey);
            }
        });
    }

    @Override
    public void requestValue(GType valueType, Object oldValue) {
        GridCellEditor editor = valueType.createGridCellEditor(editManager, property);
        if (editor instanceof PopupBasedGridEditor) {
            ((PopupBasedGridEditor) editor).showPopup(null);
        } else {
            editDispatcher.cancelEdit();
        }
    }

    @Override
    public void updateEditValue(Object value) {
    }

    @Override
    public void postDispatchResponse(ServerResponseResult response) {
    }

    @Override
    public Widget getComponent() {
        return button;
    }

    @Override
    public void setValue(Object value) {
        boolean enabled = value != null && (Boolean) value;
        button.setEnabled(enabled);
        button.setImagePath(property.getIconPath(enabled));
    }

    @Override
    public void setCaption(String caption) {
        button.setText(caption);
    }

    @Override
    public void updateCellBackgroundValue(Object value) {
        button.getElement().getStyle().setBorderColor(value == null ? null : value.toString());
    }

    @Override
    public void updateCellForegroundValue(Object value) {
        button.getElement().getStyle().setColor(value == null ? null : value.toString());
    }

    private class ActionEditManager extends EditManagerAdapter {
        @Override
        public GPropertyDraw getProperty(Cell.Context context) {
            return property;
        }

        @Override
        public void commitEditing(Object value) {
            editDispatcher.commitValue(value);
        }

        @Override
        public void cancelEditing() {
            editDispatcher.cancelEdit();
        }
    }
}
