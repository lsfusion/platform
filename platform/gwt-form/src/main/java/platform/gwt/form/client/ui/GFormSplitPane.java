package platform.gwt.form.client.ui;

import com.smartgwt.client.types.LayoutResizeBarPolicy;
import com.smartgwt.client.widgets.layout.*;
import platform.gwt.view.GContainer;

public class GFormSplitPane extends GAbstractFormContainer {
    public GFormSplitPane(GContainer key) {
        this.key = key;

        if (key.gwtVertical) {
            containerComponent = new VLayout();
        } else {
            containerComponent = new HLayout();
        }

        containerComponent.setDefaultResizeBars(LayoutResizeBarPolicy.MIDDLE);
        addBorder();
    }
}
