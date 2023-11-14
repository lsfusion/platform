package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.view.MainFrame;

public class ColorCellEditor extends TextBasedPopupCellEditor {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    public ColorCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    JavaScriptObject alwan = null;
    PValue prevValue = null;
    @Override
    protected Widget createPopupComponent(Element parent, PValue oldValue) {
        prevValue = oldValue;
        String defaultColor = null;
        if (oldValue != null) {
            try {
                defaultColor = PValue.getColorValue(oldValue).value;
            } catch (Exception e) {
                throw new IllegalStateException("can't convert string value to color");
            }
        }

        if(alwan == null) {
            alwan = initAlwan(RootPanel.get().getElement(), editBox, defaultColor,
                    messages.ok(), messages.cancel(), messages.reset());
            popup.addAutoHidePartner(getPickerElement(RootPanel.get().getElement()));
        }

        popup.setVisible(false);

        editBox.setAttribute("autocomplete", "off");
        editBox.click();

        return new SimplePanel();
    }

    @Override
    public PValue getCommitValue(Element parent, Integer contextAction) {
        String background = getColor(alwan);
        return background != null ? PValue.getPValue(new ColorDTO(background)) : null;
    }

    private void finishEditing(Element parent) {
        commitValue(parent, getCommitValue(parent, null));
    }

    private void cancelEditing(Element parent) {
        commitValue(parent, prevValue);
    }

    public void reset(Element parent) {
        commitValue(parent, (PValue) null);
    }

    private String getTheme() {
        return MainFrame.colorTheme.isLight() ? "light" : "dark";
    };

    private String getColor(String defaultColor) {
        return "#" + (defaultColor != null ? defaultColor : MainFrame.colorTheme.isLight() ? "fff" : "000");
    };

    private native JavaScriptObject initAlwan(Element root, Element input, String defaultColor,
                                              String okText, String cancelText, String resetText)/*-{
        var instance = this;

        var alwan = new $wnd.Alwan(input, {
            opacity: false,
            inputs: { hex: true, rgb: false,  hsl: false  },
            color: instance.@ColorCellEditor::getColor(Ljava/lang/String;)(defaultColor),
            theme: instance.@ColorCellEditor::getTheme(*)()
        });

        var picker = root.getElementsByClassName('alwan')[0];

        var okButton = document.createElement('button');
        okButton.setAttribute('type', 'button');
        okButton.classList.add('btn');
        okButton.textContent = okText;
        okButton.addEventListener('click', function() {
            instance.@ColorCellEditor::finishEditing(*)(input);
        });
        picker.appendChild(okButton);

        var cancelButton = document.createElement('button');
        cancelButton.setAttribute('type', 'button');
        cancelButton.classList.add('btn');
        cancelButton.textContent = cancelText;
        cancelButton.addEventListener('click', function() {
            instance.@ColorCellEditor::cancelEditing(*)(input);
        });
        picker.appendChild(cancelButton);

        var resetButton = document.createElement('button');
        resetButton.setAttribute('type', 'button');
        resetButton.classList.add('btn');
        resetButton.textContent = resetText;
        resetButton.addEventListener('click', function() {
            instance.@ColorCellEditor::reset(*)(input);
        });
        picker.appendChild(resetButton);

        alwan.open();

        return alwan;
    }-*/;

    private native String getColor(JavaScriptObject alwan)/*-{
        return alwan.getColor().hex.substring(1);
    }-*/;

    private native Element getPickerElement(Element root)/*-{
        return root.getElementsByClassName('alwan')[0];
    }-*/;


    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        destroyAlwan(alwan);
        alwan = null;
        super.stop(parent, cancel, blurred);
    }

    private native void destroyAlwan(JavaScriptObject alwan)/*-{
            alwan.destroy();
    }-*/;

}
