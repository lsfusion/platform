package lsfusion.gwt.client.form.design.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.flex.LayoutContainerView;

public class CustomContainerView extends LayoutContainerView {

    private final ResizableComplexPanel panel;
    private String currentCustom = "";

    private final boolean simpleCustom;

    public CustomContainerView(GFormController formController, GContainer container) {
        super(container, formController);
        simpleCustom = "".equals(container.getCustom());
        panel = new ResizableComplexPanel();
        GwtClientUtils.addClassName(panel, "panel-custom");
    }

    protected void addImpl(int index) {
        ComponentViewWidget childView = getCustomChildView(index);
        if(simpleCustom)
            childView.add(panel, getChildPosition(index));
        else // in theory can be attached to the panel
            childView.attach(formController.getFormLayout().attachContainer);
    }

    @Override
    public void updateLayout(long requestIndex, boolean[] childrenVisible) {
        if (!simpleCustom && !currentCustom.equals(container.getCustom())) {
            currentCustom = container.getCustom();

            panel.getElement().setInnerHTML(getTagCustom(container.getCustom()));
            for (int i = 0, childrenSize = children.size(); i < childrenSize; i++)
                getCustomChildView(i).replace(panel, children.get(i).sID);
        }
        super.updateLayout(requestIndex, childrenVisible);
    }

    public void updateCustom(String custom) {
        this.container.setCustom(custom);
    }

    private String getTagCustom(String rawCustom) {
        while (true) {
            int openBracket = rawCustom.indexOf("[");
            int closeBracket = rawCustom.indexOf("]");
            if (openBracket == -1 || closeBracket == -1) {
                break;
            } else {
                String tagName = rawCustom.substring(openBracket + 1, closeBracket);
                rawCustom = rawCustom.replace("[" + tagName + "]", "<" + tagName + "></" + tagName + ">");
            }
        }

        return rawCustom;
    }

    @Override
    protected void removeImpl(int index) {
        ComponentViewWidget childView = getCustomChildView(index);
        if(simpleCustom)
            childView.remove(panel, getChildPosition(index));
        else // really odd method / behaviour (because something else should happen, but not sure what exactly)
            childView.remove(panel);
    }

    private ComponentViewWidget getCustomChildView(int index) {
        return getChildView(index);
    }

    @Override
    public Widget getView() {
        return panel;
    }
}
