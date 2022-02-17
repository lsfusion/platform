package lsfusion.gwt.client.form.design.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;

public class CustomContainerView extends GAbstractContainerView {

    private final FlexPanel panel;


    public CustomContainerView(GFormController formController, GContainer container) {
        super(container, formController);
        panel = new FlexPanel(container.isVertical());
        panel.getElement().setInnerHTML(getTagCustomDesign(container.getCustom()));
    }

    @Override
    protected void addImpl(int index, GComponent child, Widget view) {

        Element panelElement = panel.getElement();
        NodeList<Element> panelChildren = panelElement.getElementsByTagName(child.sID);
        Element parentElement = null;

        Widget viewParent = view.getParent();
        Widget widget = viewParent != null ? viewParent : view;

        if (panelChildren.getLength() == 1) {
            int childIndex = 0;
            Element elementToReplace = panelChildren.getItem(0);
            for (int i = 0; i < panelElement.getChildCount(); i++) { //get the index before which the element will be inserted
                childIndex = i;
                if (elementToReplace.getNodeName().equals(panelElement.getChild(i).getNodeName()))
                    break;
            }
            index = childIndex;
            parentElement = elementToReplace.getParentElement();
            elementToReplace.removeFromParent();
        }

        panel.add(widget, parentElement, index, child.getAlignment(), child.getFlex(), child.isShrink(), child.getSize(container.isVertical()));
    }

    public void updateCustom(String customDesign) {
        this.container.customDesign = customDesign;

        Element panelElement = panel.getElement();
        panelElement.setInnerHTML(getTagCustomDesign(customDesign));
        int childrenCount = getChildrenCount();
        for (int i = 0; i < childrenCount; i++) {
            GComponent child = getChild(i);
            NodeList<Element> elementsByTagName = panelElement.getElementsByTagName(child.sID);
            Widget childView = getChildView(child);
            Element childElement = childView.getElement();
            if (elementsByTagName.getLength() == 0) { //if the custom container contains an element that is not defined in html
                int i1 = child.container.children.indexOf(child);
                if (i1 == 0)
                    panelElement.insertFirst(childElement);
                else if (i1 == panelElement.getChildCount())
                    panelElement.appendChild(childElement);
                else
                    panelElement.insertAfter(childElement, getChildView(getChild(i1 - 1)).getElement());
            } else {
                for (int j = 0; j < elementsByTagName.getLength(); j++) {
                    Element elementToReplace = elementsByTagName.getItem(j);
                    Element parentElement = elementToReplace.getParentElement();
                    parentElement.replaceChild(childElement, elementToReplace);
                }
            }
        }
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
