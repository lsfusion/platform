package platform.client.form.queries;

import platform.interop.KeyStrokes;

import javax.swing.*;

public class FindView extends QueryView {
    private static final ImageIcon applyIcon = new ImageIcon(FilterView.class.getResource("/images/find.png"));
    private static final ImageIcon addConditionIcon = new ImageIcon(FilterView.class.getResource("/images/findadd.png"));

    FindView(QueryController controller) {
        super(controller);
    }

    protected Icon getApplyIcon() {
        return applyIcon;
    }

    protected Icon getAddConditionIcon() {
        return addConditionIcon;
    }

    protected KeyStroke getKeyStroke(int modifier) {
        return KeyStrokes.getFindKeyStroke(modifier);
    }
}
