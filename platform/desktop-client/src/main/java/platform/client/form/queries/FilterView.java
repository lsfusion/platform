package platform.client.form.queries;

import platform.interop.KeyStrokes;

import javax.swing.*;

public class FilterView extends QueryView {

    private static final ImageIcon applyIcon = new ImageIcon(FilterView.class.getResource("/images/filt.png"));

    private static final ImageIcon addConditionIcon = new ImageIcon(FilterView.class.getResource("/images/filtadd.png"));

    FilterView(QueryController controller) {
        super(controller);
    }

    protected Icon getApplyIcon() {
        return applyIcon;
    }

    protected Icon getAddConditionIcon() {
        return addConditionIcon;
    }

    protected KeyStroke getKeyStroke(int modifier) {
        return KeyStrokes.getFilterKeyStroke(modifier);
    }
}
