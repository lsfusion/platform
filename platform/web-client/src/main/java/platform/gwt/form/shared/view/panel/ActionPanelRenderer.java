package platform.gwt.form.shared.view.panel;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form.client.form.dispatch.GEditPropertyDispatcher;
import platform.gwt.form.client.form.dispatch.GEditPropertyHandler;
import platform.gwt.form.client.form.ui.GFormController;
import platform.gwt.form.client.form.ui.GPropertyContextMenuPopup;
import platform.gwt.form.shared.view.GEditBindingMap;
import platform.gwt.form.shared.view.GKeyStroke;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.classes.GType;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.DialogBasedGridCellEditor;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.editor.PopupBasedGridCellEditor;

import static platform.gwt.base.client.GwtClientUtils.stopPropagation;
import static platform.gwt.base.shared.GwtSharedUtils.nullEquals;
import static platform.gwt.form.client.HotkeyManager.Binding;

public class ActionPanelRenderer implements PanelRenderer, GEditPropertyHandler {

    private final GFormController form;
    private final GEditPropertyDispatcher editDispatcher;
    private final EditManager editManager = new ActionEditManager();
    private final GPropertyContextMenuPopup contextMenuPopup = new GPropertyContextMenuPopup();
    private final GPropertyDraw property;
    private final GGroupObjectValue columnKey;

    private final ImageButton button;
    private boolean isValueTrue = true;
    private boolean readOnly = false;
    private boolean enabled = true;
    private EventTarget focusTargetAfterEdit;

    private Object background;
    private Object foreground;

    public ActionPanelRenderer(final GFormController iform, final GPropertyDraw iproperty, GGroupObjectValue icolumnKey) {
        this.form = iform;
        this.property = iproperty;
        this.columnKey = icolumnKey;
        this.editDispatcher = new GEditPropertyDispatcher(form, this);

        button = new ImageButton(property.getEditCaption(), property.icon);
        button.addStyleName("panelActionProperty");
        if (property.getPreferredPixelHeight() > -1) {
            button.setHeight(property.getPreferredHeight());
        }
        if (property.font != null) {
            button.getLabel().getElement().getStyle().setProperty("font", property.font.getFullFont());
        }
        button.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                click(null);
            }
        });
        button.addDomHandler(new ContextMenuHandler() {
            @Override
            public void onContextMenu(ContextMenuEvent event) {
                contextMenuPopup.show(property, event.getNativeEvent().getClientX(), event.getNativeEvent().getClientY(), new GPropertyContextMenuPopup.ItemSelectionListener() {
                    @Override
                    public void onMenuItemSelected(String actionSID) {
                        onContextMenuItemSelected(actionSID);
                    }
                });
                stopPropagation(event);
            }
        }, ContextMenuEvent.getType());

        button.getElement().setPropertyObject("groupObject", property.groupObject);
        if (property.editKey != null) {
            iform.addHotkeyBinding(property.groupObject, property.editKey, new Binding() {
                @Override
                public boolean onKeyPress(NativeEvent event, GKeyStroke key) {
                    return enabled && click(event.getEventTarget());
                }
            });
        }
        button.setFocusable(property.focusable);
    }

    private void onContextMenuItemSelected(String actionSID) {
        if (!form.isEditing()) {
            editDispatcher.executePropertyEditAction(property, columnKey, actionSID, null);
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
        if (editor instanceof PopupBasedGridCellEditor) {
            ((PopupBasedGridCellEditor) editor).showPopup(null);
        } else if (editor instanceof DialogBasedGridCellEditor) {
            editor.startEditing(null, null, null, null);
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
        this.isValueTrue = value != null && (Boolean) value;
        updateEnabled();
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        updateEnabled();
    }

    private void updateEnabled() {
        boolean newEnabled = isValueTrue && !readOnly;
        if (enabled != newEnabled) {
            enabled = newEnabled;
            button.setEnabled(enabled);
            button.setAppImagePath(property.getIconPath(enabled));
        }
    }

    @Override
    public void setCaption(String caption) {
        button.setText(property.getEditCaption(caption));
    }

    @Override
    public void setDefaultIcon() {
        button.setImage(property.icon);
    }

    @Override
    public void setIcon(String iconPath) {
        button.setModuleImagePath(iconPath);
    }

    @Override
    public void updateCellBackgroundValue(Object value) {
        if (!nullEquals(background, value)) {
            background = value;
            button.getElement().getStyle().setBorderColor(value == null ? null : value.toString());
        }
    }

    @Override
    public void updateCellForegroundValue(Object value) {
        if (!nullEquals(foreground, value)) {
            foreground = value;
            button.getElement().getStyle().setColor(value == null ? null : value.toString());
        }
    }

    @Override
    public void focus() {
        button.setFocus(true);
    }

    @Override
    public String getWidth() {
        return null;
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

        @Override
        public void selectNextCellInColumn(boolean down) {
        }
    }
}
