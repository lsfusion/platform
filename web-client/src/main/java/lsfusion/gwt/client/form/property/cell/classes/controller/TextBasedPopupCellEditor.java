package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

import java.text.ParseException;

public abstract class TextBasedPopupCellEditor extends TextBasedCellEditor {

    protected JavaScriptObject popup;
    protected InputElement editBox;
    public TextBasedPopupCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    // it seems that it's needed only for editBox.click() and selectAll
    @Override
    protected void onInputReady(Element parent, PValue oldValue) {
        super.onInputReady(parent, oldValue);

        editBox = inputElement;

        if(!inputElementType.hasNativePopup()) {
            popup = GwtClientUtils.showTippyPopup(RootPanel.get().getElement(), parent, createPopupComponent(parent, oldValue).getElement(), false);
        }
    }

    protected abstract Widget createPopupComponent(Element parent, PValue oldValue);
    protected abstract void removePopupComponent(Element parent);
    protected abstract PValue getPopupValue();

    @Override
    public void clearRender(Element cellParent, RenderContext renderContext, boolean cancel) {
        super.clearRender(cellParent, renderContext, cancel);
        GwtClientUtils.hideTippyPopup(popup);
    }

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        if(popup != null)
            removePopupComponent(parent);
        super.stop(parent, cancel, blurred);
    }

    protected PValue tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        //to be able to enter the date from keyboard
        if (onCommit) {
            try {
                return super.tryParseInputText(inputText, true);
            } catch (ParseException e) {
                if(popup != null)
                    return getPopupValue();
                else
                    throw e;
            }
        }

        return PValue.getPValue(inputText);
    }
}
