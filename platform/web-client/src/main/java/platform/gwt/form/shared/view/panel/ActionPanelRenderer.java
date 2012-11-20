package platform.gwt.form.shared.view.panel;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form.client.HotkeyManager;
import platform.gwt.form.client.form.dispatch.GEditPropertyDispatcher;
import platform.gwt.form.client.form.dispatch.GEditPropertyHandler;
import platform.gwt.form.client.form.ui.GFormController;
import platform.gwt.form.shared.view.GEditBindingMap;
import platform.gwt.form.shared.view.GKeyStroke;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.classes.GType;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.editor.PopupBasedGridEditor;

import static platform.gwt.form.client.HotkeyManager.Binding;

public class ActionPanelRenderer implements PanelRenderer, GEditPropertyHandler {

    private final GFormController form;
    private final GEditPropertyDispatcher editDispatcher;
    private final EditManager editManager = new ActionEditManager();
    private final GPropertyDraw property;
    private final GGroupObjectValue columnKey;

    private final ImageButton button;
    private boolean enabled = true;
    private EventTarget focusTargetAfterEdit;

    public ActionPanelRenderer(final GFormController iform, final GPropertyDraw iproperty, GGroupObjectValue icolumnKey) {
        this.form = iform;
        this.property = iproperty;
        this.columnKey = icolumnKey;
        this.editDispatcher = new GEditPropertyDispatcher(form, this);

        button = new ImageButton(property.getEditCaption(), property.iconPath);
        button.addStyleName("panelActionProperty");
        if (property.preferredHeight > -1) {
            button.setHeight(property.preferredHeight + "px");
        }
        button.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                click(null);
            }
        });

        if (property.editKey != null) {
            HotkeyManager.get().addHotkeyBinding(form.getElement(), property.editKey, new Binding() {
                @Override
                public boolean onKeyPress(NativeEvent event, GKeyStroke key) {
                    return enabled && click(event.getEventTarget());
                }
            });
        }
    }

    private boolean click(EventTarget ifocusTargetAfterEdit) {
        if (!form.isEditing()) {
            focusTargetAfterEdit = ifocusTargetAfterEdit;
            editDispatcher.executePropertyEditAction(property, columnKey, GEditBindingMap.CHANGE, null);
            return true;
        }
        return false;
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
    public Widget getComponent() {
        return button;
    }

    @Override
    public void setValue(Object value) {
        enabled = value != null && (Boolean) value;
        button.setEnabled(enabled);
        button.setImagePath(property.getIconPath(enabled));
    }

    @Override
    public void setCaption(String caption) {
        button.setText(property.getEditCaption(caption));
    }

    @Override
    public void updateCellBackgroundValue(Object value) {
        button.getElement().getStyle().setBorderColor(value == null ? null : value.toString());
    }

    @Override
    public void updateCellForegroundValue(Object value) {
        button.getElement().getStyle().setColor(value == null ? null : value.toString());
    }

    @Override
    public void focus() {
        button.setFocus(true);
    }

    @Override
    public void onEditFinished() {
        if (focusTargetAfterEdit != null) {
            Element.as(focusTargetAfterEdit).focus();
            focusTargetAfterEdit = null;
        } else {
            focus();
        }
    }

    private class ActionEditManager implements EditManager {
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
