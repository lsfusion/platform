package platform.gwt.form.client.form.ui;

import com.smartgwt.client.widgets.layout.*;
import platform.gwt.view.*;

public class GFormContainer extends GAbstractFormContainer {

    public GFormContainer(GContainer key) {
        this.key = key;

        if (key.gwtIsLayout)
            if (key.gwtVertical)
                containerComponent = new VLayout();
            else
                containerComponent = new HLayout(10);
        else
        if (key.gwtVertical)
            containerComponent = new VStack();
        else
            containerComponent = new HStack(10);

        containerComponent.setAlign(key.hAlign.getSmartGWTAlignment());
        addBorder();
    }
}
