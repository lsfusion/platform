package lsfusion.client.form.filter.user;

import lsfusion.client.form.filter.user.controller.QueryController;
import lsfusion.client.form.filter.user.view.QueryView;
import lsfusion.interop.form.event.KeyStrokes;

import javax.swing.*;

public class FilterView extends QueryView {

    public static final String APPLY_ICON_PATH = "filtapply.png";

    public static final String ADD_ICON_PATH = "filtadd.png";

    public static final String FILTER_ICON_PATH = "filt.png";

    public FilterView(QueryController controller) {
        super(controller);
    }

//    public Icon getApplyIcon() {
//        return applyIcon;
//    }

//    public Icon getAddIcon() {
//        return addIcon;
//    }

//    public Icon getFilterIcon() {
//        return filterIcon;
//    }

    protected KeyStroke getKeyStroke(int modifier) {
        return KeyStrokes.getFilterKeyStroke(modifier);
    }
}
