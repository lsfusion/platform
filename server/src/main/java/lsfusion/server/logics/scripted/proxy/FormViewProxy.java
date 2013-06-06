package lsfusion.server.logics.scripted.proxy;

import lsfusion.server.form.view.FormView;

import javax.swing.*;

public class FormViewProxy extends ViewProxy<FormView> {
    public FormViewProxy(FormView target) {
        super(target);
    }

    public void setKeyStroke(KeyStroke keyStroke) {
        target.keyStroke = keyStroke;
    }

    public void setCaption(String caption) {
        target.caption = caption;
    }

    public void setOverridePageWidth(Integer overridePageWidth) {
        target.overridePageWidth = overridePageWidth;
    }

    public void setGwtAllowScrollSplits(boolean gwtAllowScrollSplits) {
        target.gwtAllowScrollSplits = gwtAllowScrollSplits;
    }
}
