package platform.client.descriptor.editor.base;

import javax.swing.*;

import java.awt.event.ItemEvent;

import static platform.client.descriptor.editor.base.Tristate.*;

//Обычный checkBox с 3-мя состоянями, который циклично их меняет
public class TristateCheckBox extends AbstractTristateCheckBox {
    public TristateCheckBox() {
        super();
    }

    public TristateCheckBox(String text) {
        super(text);
    }

    public TristateCheckBox(String text, Icon icon, Tristate initial) {
        super(text, icon, initial);
    }

    public TristateCheckBox(String text, Tristate initial) {
        super(text, initial);
    }

    @Override
    protected void onChange() {
        switch (getState()) {
            case NOT_SELECTED:
                setState(SELECTED);
                break;
            case SELECTED:
                setState(MIXED);
                break;
            case MIXED:
                setState(NOT_SELECTED);
                break;
        }
    }

    public Boolean getStateAsBoolean() {
        switch (getState()) {
            case NOT_SELECTED:
                return false;
            case SELECTED:
                return true;
            case MIXED:
                return null;
        }
        return null;
    }

    public void setStateFromBoolean(Boolean state) {
        setState(state == null
                 ? Tristate.MIXED
                 : state
                   ? Tristate.SELECTED
                   : Tristate.NOT_SELECTED);
    }
}
