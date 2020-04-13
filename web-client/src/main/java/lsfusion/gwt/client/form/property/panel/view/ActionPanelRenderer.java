package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.ImageHolder;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.base.view.AppImageButton;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GKeyInputEvent;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.controller.DialogBasedGridCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.PopupBasedGridCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.GEditPropertyHandler;
import lsfusion.gwt.client.form.property.cell.controller.GridCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.NativeEditEvent;
import lsfusion.gwt.client.form.property.cell.controller.dispatch.GEditPropertyDispatcher;
import lsfusion.gwt.client.form.property.table.view.GPropertyContextMenuPopup;

import static lsfusion.gwt.client.base.GwtClientUtils.*;
import static lsfusion.gwt.client.base.GwtSharedUtils.nullEquals;
import static lsfusion.gwt.client.form.property.cell.GEditBindingMap.getPropertyKeyPressActionSID;

public class ActionPanelRenderer implements PanelRenderer, GEditPropertyHandler {

    private final GFormController form;
    private final GEditPropertyDispatcher editDispatcher;
    private final EditManager editManager = new ActionEditManager();
    private final GPropertyContextMenuPopup contextMenuPopup = new GPropertyContextMenuPopup();
    private final GPropertyDraw property;
    private String tooltip;

    private final GGroupObjectValue columnKey;
    private final ActionButton button;

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

        button = new ActionButton(property.imageHolder, property.getEditCaption());
        button.addStyleName("actionPanelRenderer");
        Style buttonStyle = button.getElement().getStyle();
        buttonStyle.setPaddingTop(0, Style.Unit.PX);
        buttonStyle.setPaddingBottom(0, Style.Unit.PX);
        buttonStyle.setPaddingLeft(BUTTON_HORIZONTAL_PADDING, Style.Unit.PX);
        buttonStyle.setPaddingRight(BUTTON_HORIZONTAL_PADDING, Style.Unit.PX);

        setTooltip(property.caption);

        TooltipManager.registerWidget(button, new TooltipManager.TooltipHelper() {
            @Override
            public String getTooltip() {
                return property.getTooltipText(tooltip);
            }

            @Override
            public boolean stillShowTooltip() {
                return button.isAttached() && button.isVisible();
            }
        });

        int borderWidth = 2;
        int baseHeight = property.getValueHeight(null);
        if (baseHeight > -1) {
            // min-height instead of height to be able to stretch vertically
            buttonStyle.setProperty("minHeight", baseHeight + borderWidth + "px");
        }
        int baseWidth = property.valueWidth;
        if (baseWidth > -1) {
            // min-width instead of width to be able to stretch horizontally
            buttonStyle.setProperty("minWidth", baseWidth + borderWidth + "px");
        }
        if (property.font != null) {
            property.font.apply(button.getLabel().getElement().getStyle());
        }

        button.getElement().setPropertyObject("groupObject", property.groupObject);

        button.setFocusable(property.focusable);

        initUIHandlers(iform);
    }

    private void initUIHandlers(GFormController iform) {

        //we have 'ENTER' binding for tab action, so this 'ENTER' binding should have higher priority
        form.addBinding(new GKeyInputEvent(new GKeyStroke(KeyCodes.KEY_ENTER, false, false, false), null), new GFormController.Binding(property.groupObject, 0, eventObject -> eventObject.getEventTarget().cast() == button.getElement()) {
            @Override
            public void pressed(EventTarget eventTarget) {
                click(eventTarget);
            }

            @Override
            public boolean showing() {
                return isShowing(button);
            }

            @Override
            public boolean enabled() {
                return enabled;
            }
        });

        button.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                assert !form.isEditing();
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
        
        button.addDomHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (!form.isEditing()) {
                    final String actionSID = getPropertyKeyPressActionSID(new NativeEditEvent(event.getNativeEvent()), property);
                    if (actionSID != null) {
                        editDispatcher.executePropertyEventAction(property, columnKey, actionSID);
                    }
                }
            }
        }, KeyDownEvent.getType());

        iform.addPropertyBindings(property, () -> new GFormController.Binding(property.groupObject) {
            @Override
            public void pressed(EventTarget eventTarget) {
                click(eventTarget);
            }
            @Override
            public boolean showing() {
                return isShowing(button);
            }
            @Override
            public boolean enabled() {
                return enabled;
            }
        });
    }

    private void onContextMenuItemSelected(String actionSID) {
        if (!form.isEditing()) {
            editDispatcher.executePropertyEventAction(property, columnKey, actionSID);
        }
    }

    private void click(EventTarget ifocusTargetAfterEdit) {
        focusTargetAfterEdit = ifocusTargetAfterEdit;
        editDispatcher.executePropertyEventAction(property, columnKey, GEditBindingMap.CHANGE);
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
            button.setImage(property.getImage(enabled));
        }
    }

    @Override
    public void setCaption(String caption) {
        button.setText(property.getEditCaption(caption));
        setTooltip(caption);
    }

    public void setTooltip(String caption) {
        tooltip = !GwtSharedUtils.isRedundantString(property.toolTip) ? property.toolTip : caption;
    }

    @Override
    public void setDefaultIcon() {
        button.setDefaultImage();
    }

    @Override
    public void setImage(String iconPath) {
        button.setModuleImagePath(iconPath);
    }

    @Override
    public void updateCellBackgroundValue(Object value) {
        if (!nullEquals(background, value)) {
            background = value;
            button.getElement().getStyle().setBackgroundColor(value == null ? null : value.toString());
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
    public void takeFocusAfterEdit() {
        if (focusTargetAfterEdit != null) {
            Element.as(focusTargetAfterEdit).focus();
            focusTargetAfterEdit = null;
        } else {
            focus();
        }
    }

    @Override
    public Object getEditValue() {
        return null;
    }

    private class ActionButton extends AppImageButton {
        private ActionButton(ImageHolder imageHolder, String caption) {
            super(imageHolder, caption);
            sinkEvents(Event.ONBLUR | Event.ONFOCUS);
        }

        @Override
        public final void onBrowserEvent(Event event) {
            // Verify that the target is still a child of this widget. IE fires focus
            // events even after the element has been removed from the DOM.
            EventTarget eventTarget = event.getEventTarget();
            if (!Element.is(eventTarget) || !getElement().isOrHasChild(Element.as(eventTarget))) {
                return;
            }
            super.onBrowserEvent(event);
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
        public void selectNextRow(boolean down) {
        }

        @Override
        public void selectNextCellInColumn(boolean forward) {
        }
    }
}
