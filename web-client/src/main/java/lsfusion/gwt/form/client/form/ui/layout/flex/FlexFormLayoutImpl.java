package lsfusion.gwt.form.client.form.ui.layout.flex;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.client.form.ui.layout.GAbstractContainerView;
import lsfusion.gwt.form.client.form.ui.layout.GFormLayoutImpl;
import lsfusion.gwt.form.shared.view.GContainer;
import lsfusion.gwt.form.shared.view.GGrid;

import static lsfusion.gwt.base.client.GwtClientUtils.setupFillParent;

public class FlexFormLayoutImpl extends GFormLayoutImpl {

    @Override
    public GAbstractContainerView createContainerView(GFormController form, GContainer container) {
        if (container.isLinear()) {
            return new FlexLinearContainerView(container);
        } else if (container.isSplit()) {
            return new FlexSplitContainerView(container);
        } else if (container.isTabbed()) {
            return new FlexTabbedContainerView(form, container);
        } else if (container.isColumns()) {
            return new FlexColumnsContainerView(container);
        } else {
            throw new IllegalStateException("Incorrect container type");
        }
    }

    @Override
    public void setupMainContainer(Widget mainContainerWidget) {
        setupFillParent(mainContainerWidget.getElement());
    }

    @Override
    public Panel createGridView(GGrid grid, Panel panel) {
        FlexPanel gridView = new FlexPanel(true);
        gridView.addFill(panel);
        return gridView;
    }
}
