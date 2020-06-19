package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
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
import lsfusion.gwt.client.form.property.cell.classes.view.ActionCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.*;
import lsfusion.gwt.client.form.property.table.view.GPropertyContextMenuPopup;

import static lsfusion.gwt.client.base.GwtClientUtils.isShowing;
import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;
import static lsfusion.gwt.client.base.GwtSharedUtils.nullEquals;
import static lsfusion.gwt.client.base.view.ColorUtils.getDisplayColor;
import static lsfusion.gwt.client.view.StyleDefaults.BUTTON_HORIZONTAL_PADDING;

public class ActionPanelRenderer extends PanelRenderer {

//    private final GFormController form;

    public ActionPanelRenderer(final GFormController form, final GPropertyDraw property, GGroupObjectValue columnKey) {
        super(form, property, columnKey);

        finalizeInit();
    }

    @Override
    public Widget getComponent() {
        return value;
    }

    // hack, assert that render element is rendered with ActionCellRenderer
    private AppImageButton getButton() {
        return ((AppImageButton) value.getRenderElement().getPropertyObject(ActionCellRenderer.WIDGET));
    }

    @Override
    protected Widget getTooltipWidget() {
        return value;
    }

    @Override
    protected void setLabelText(String text) {
        getButton().setText(text);
    }

    // interface for refresh button
    public void setImage(String iconPath) {
        AppImageButton button = getButton();
        if(iconPath != null)
            button.setModuleImagePath(iconPath);
        else
            button.setDefaultImage();
    }
}
