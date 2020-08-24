package lsfusion.gwt.client.base.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.DOM;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;

import static lsfusion.gwt.client.base.view.FlexPanel.Justify;

public class FlexPanelImpl {

    private static FlexPanelImpl impl;

    public static FlexPanelImpl get() {
        if (impl == null) {
            impl = GWT.create(FlexPanelImpl.class);
        }
        return impl;
    }

    protected String getDisplayFlexValue() {
        return "flex";
    }

    protected String getDirectionAttrName() {
        return "flexDirection";
    }

    protected String getDirectionValue(boolean vertical) {
        return vertical ? getVertDirectionValue() : getHorzDirectionValue();
    }

    protected String getHorzDirectionValue() {
        return "row";
    }

    protected String getVertDirectionValue() {
        return "column";
    }

    protected String getJustifyContentAttrName() {
        return "justifyContent";
    }

    protected String getJustifyValue(Justify justify) {
        switch (justify) {
            case START: return getStartAlignmentValue();
            case CENTER: return getCenterAlignmentValue();
            case END: return getEndAlignmentValue();
        }
        throw new IllegalStateException("Unknown alignment");
    }

    protected String getStartAlignmentValue() {
        return "flex-start";
    }

    protected String getCenterAlignmentValue() {
        return "center";
    }

    protected String getEndAlignmentValue() {
        return "flex-end";
    }

    protected String getStretchAlignmentValue() {
        return "stretch";
    }

    protected String getAlignAttrName() {
        return "alignSelf";
    }

    protected String getAlignmentValue(GFlexAlignment justify) {
        switch (justify) {
            case START: return getStartAlignmentValue();
            case CENTER: return getCenterAlignmentValue();
            case END: return getEndAlignmentValue();
            case STRETCH: return getStretchAlignmentValue();
        }
        throw new IllegalStateException("Unknown alignment");
    }

    protected String getFlexAttrName() {
        return "flex";
    }

    public void setupParentDiv(DivElement parent, boolean vertical, Justify justify) {
        parent.getStyle().setOverflow(Style.Overflow.HIDDEN);
        parent.getStyle().setProperty("display", getDisplayFlexValue());
        parent.getStyle().setProperty(getDirectionAttrName(), getDirectionValue(vertical));
        parent.getStyle().setProperty(getJustifyContentAttrName(), getJustifyValue(justify));
    }

    public void setVisible(DivElement parent, boolean visible) {
        parent.getStyle().setProperty("display", visible ? getDisplayFlexValue() : "none");
    }

    public FlexPanel.LayoutData insertChild(Element parent, Element child, int beforeIndex, GFlexAlignment alignment, double flex, Integer flexBasis) {
        FlexPanel.LayoutData layoutData = new FlexPanel.LayoutData(child, alignment, flex, flexBasis);

        setFlex(layoutData, child, flex, flexBasis);
        setAlignment(layoutData, child, alignment);

        DOM.insertChild(parent.<com.google.gwt.user.client.Element>cast(), child.<com.google.gwt.user.client.Element>cast(), beforeIndex);

        return layoutData;
    }

    public void removeChild(FlexPanel.LayoutData layoutData) {
        layoutData.child.removeFromParent();
    }

    public String getFlexBasisString(Integer flexBasis) {
        return flexBasis == null ? "auto" : flexBasis + "px";
    }

    public void setFlex(Element child, double flex, Integer flexBasis) {
        child.getStyle().setProperty(getFlexAttrName(), getFlexValue(flex, getFlexBasisString(flexBasis)));
    }
    
    public int getSize(Element child, boolean vertical) {
        return child.getPropertyInt(vertical ? "offsetHeight" : "offsetWidth");
    }

    public void setFlex(FlexPanel.LayoutData layoutData, Element child, double flex, Integer flexBasis) {
        layoutData.flex = flex;
        layoutData.flexBasis = flexBasis;
        setFlex(child, flex, flexBasis);
    }

    public void setFlex(FlexPanel.LayoutData layoutData, Element child, double flex) {
        layoutData.flex = flex;
        setFlex(child, flex, layoutData.flexBasis);
    }

    public void setFlex(FlexPanel.LayoutData layoutData, Element child) {
        setFlex(child, layoutData.flex, layoutData.flexBasis);
    }

    public void setFlexBasis(FlexPanel.LayoutData layoutData, Element child, Integer flexBasis) {
        layoutData.flexBasis = flexBasis;
        setFlex(child, layoutData.flex, flexBasis);
    }

    public void fixFlexBasis(FlexPanel.LayoutData layoutData, FlexTabbedPanel child, boolean vertical) {
        if(layoutData.flexBasis != null)
            return;

        Element childElement = child.getElement();
        // фиксируем явную ширину composite'а (ставим базис равный ширине, flex - 0)
        int size = getSize(childElement, vertical);

        Element childWidgetElement = child.panel.getElement();

        if(layoutData.flex == 0) // оптимизация если flex'а нет, этот размер и фиксируем 
            layoutData.flexBasis = size;
        else {
            setFlex(childElement, 0, size);

            // у widget'a comoposite'а убираем растягивание
            setFlex(childWidgetElement, 0, null);
            setAlignment(childWidgetElement, GFlexAlignment.START);

            // измеряем ширину, запоминаем в базис
            layoutData.flexBasis = getSize(childWidgetElement, vertical);

            // возвращаем растягивание
            setFlex(childWidgetElement, 1, null);
            setAlignment(childWidgetElement, GFlexAlignment.STRETCH);

            // выставляем расчитанный базис (возвращаем flex)
            setFlex(layoutData, childElement);
        }
    }

    public int getFlexBasis(FlexPanel.LayoutData layoutData, Element child, boolean vertical) {
        if(layoutData.flexBasis != null)
            return layoutData.flexBasis;

        setFlex(child, 0, null);
        return child.getPropertyInt(vertical ? "offsetHeight" : "offsetWidth");
    }

    private String getFlexValue(double flex, String flexBasis) {
        return flex + " 0 " + flexBasis;
    }

    public void setAlignment(FlexPanel.LayoutData layoutData, Element child, GFlexAlignment alignment) {
        layoutData.alignment = alignment;
        setAlignment(child, alignment);
    }

    public void setAlignment(Element child, GFlexAlignment alignment) {
        child.getStyle().setProperty(getAlignAttrName(), getAlignmentValue(alignment));
    }
}
