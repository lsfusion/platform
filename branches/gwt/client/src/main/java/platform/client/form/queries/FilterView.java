package platform.client.form.queries;

import platform.interop.KeyStrokes;

import javax.swing.*;

public class FilterView extends QueryView {

    protected Icon getApplyIcon() {
        return new ImageIcon(getClass().getResource("/images/filt.gif"));
    }

    protected Icon getAddConditionIcon() {
        return new ImageIcon(getClass().getResource("/images/filtadd.gif"));
    }

    protected KeyStroke getKeyStroke(int modifier) {
        return KeyStrokes.getFilterKeyStroke(modifier);
    }
}
