package lsfusion.gwt.client.form.design.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;

public class CustomContainerView extends GAbstractContainerView {

    private final ResizableComplexPanel panel;
    private final GFormController formController;

    public CustomContainerView(GFormController formController, GContainer container) {
        super(container, formController);

        this.formController = formController;
        panel = new ResizableComplexPanel();
    }

    @Override
    protected void addImpl(int index, GComponent child, Widget view) {
        //adding is done in updateLayout()
    }

    @Override
    public void updateLayout(long requestIndex, boolean[] childrenVisible) {
        childrenViews.forEach(childrenView -> formController.getFormLayout().recordViews.add(childrenView));

        Element panelElement = panel.getElement();
        panelElement.setInnerHTML(getTagCustomDesign(container.getCustom()));

        for (int i = 0; i < getChildrenCount(); i++) {
            GComponent child = getChild(i);
            NodeList<Element> panelChildren = panelElement.getElementsByTagName(child.sID);
            int childrenLength = panelChildren.getLength();
            if (childrenLength > 0) {
                for (int j = 0; j < childrenLength; j++) {
                    Element elementToReplace = panelChildren.getItem(j);
                    Widget childView = getChildView(child);
                    childView.setVisible(childrenVisible[i]);
                    elementToReplace.getParentElement().replaceChild(childView.getElement(), elementToReplace);
                }
            }
        }
        super.updateLayout(requestIndex, childrenVisible);
    }

    public void updateCustom(String customDesign) {
        this.container.customDesign = customDesign;
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
