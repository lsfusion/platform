package lsfusion.gwt.client.form.design.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.flex.LayoutContainerView;

public class CustomContainerView extends LayoutContainerView {

    private final ResizableComplexPanel panel;
    private final GFormController formController;
    private String currentCustomDesign = "";

    public CustomContainerView(GFormController formController, GContainer container) {
        super(container, formController);

        this.formController = formController;
        panel = new ResizableComplexPanel();
    }

    @Override
    public void updateLayout(long requestIndex, boolean[] childrenVisible) {
        if (!currentCustomDesign.equals(container.getCustomDesign())) {
            currentCustomDesign = container.getCustomDesign();
            childrenViews.forEach(childrenView -> formController.getFormLayout().recordViews.add(childrenView));

            Element panelElement = panel.getElement();
            panelElement.setInnerHTML(getTagCustomDesign(container.getCustomDesign()));
            for (GComponent child : children) {
                Element panelChild = panelElement.getElementsByTagName(child.sID).getItem(0);
                if (panelChild != null)
                    panelChild.getParentElement().replaceChild(getChildView(child).getElement(), panelChild);
            }
        }
        super.updateLayout(requestIndex, childrenVisible);
    }

    public void updateCustomDesign(String customDesign) {
        this.container.setCustomDesign(customDesign);
    }

    private String getTagCustomDesign(String rawCustomDesign) {
        while (true) {
            int openBracket = rawCustomDesign.indexOf("[");
            int closeBracket = rawCustomDesign.indexOf("]");
            if (openBracket == -1 || closeBracket == -1) {
                break;
            } else {
                String tagName = rawCustomDesign.substring(openBracket + 1, closeBracket);
                rawCustomDesign = rawCustomDesign.replace("[" + tagName + "]", "<" + tagName + "></" + tagName + ">");
            }
        }

        return rawCustomDesign;
    }

    @Override
    protected void removeImpl(int index, GComponent child) {
        panel.remove(getChildView(child));
    }

    @Override
    public Widget getView() {
        return panel;
    }
}
