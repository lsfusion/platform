package lsfusion.client.form.design.view;

import lsfusion.client.form.design.view.widget.Widget;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.base.view.FlexLayout;

import java.awt.*;

public class FlexPanelImpl {

    private static FlexPanelImpl impl;

    public static FlexPanelImpl get() {
        if (impl == null) {
            impl = new FlexPanelImpl();
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

    protected String getAlignmentValue(FlexAlignment justify) {
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

/*    public void setupParentDiv(DivElement parent, boolean vertical, GFlexAlignment justify) {
        parent.getStyle().setProperty("display", getDisplayFlexValue());
        parent.getStyle().setProperty(getDirectionAttrName(), getDirectionValue(vertical));
        parent.getStyle().setProperty(getJustifyContentAttrName(), getAlignmentValue(justify));
    }*/

/*    public void setVisible(DivElement parent, boolean visible) {
        parent.getStyle().setProperty("display", visible ? getDisplayFlexValue() : "none");
    }*/

    public LayoutData insertChild(FlexLayout layout, Widget child, int beforeIndex, FlexAlignment alignment, double flex, boolean shrink, boolean alignShrink, Integer flexBasis, boolean vertical) {
        LayoutData layoutData = new LayoutData(alignment, flex, flexBasis, shrink, alignShrink);
        child.setLayoutData(layoutData);

        setFlex(layout, child, layoutData, vertical);
        setAlignment(alignment);

        //DOM.insertChild(parent.<com.google.gwt.user.client.Element>cast(), child.<com.google.gwt.user.client.Element>cast(), beforeIndex);

        return layoutData;
    }

/*    public void removeChild(FlexPanel.LayoutData layoutData) {
        layoutData.child.removeFromParent();
    }*/

    public String getFlexBasisString(Integer flexBasis) {
        return flexBasis == null ? "auto" : flexBasis + "px";
    }

    /*    public int getFullSize(Element child, boolean vertical) {
        return vertical ? GwtClientUtils.getFullHeight(child) : GwtClientUtils.getFullWidth(child);
//        return child.getPropertyInt(vertical ? "offsetHeight" : "offsetWidth");
    }*/

    public int getSize(Component child, boolean vertical) {
        return vertical ? child.getHeight() : child.getWidth();
    }

/*    public int getMargins(Element child, boolean vertical) {
        return child.getPropertyInt(vertical ? "marginTop" : "marginLeft") +
                    child.getPropertyInt(vertical ? "marginBottom" : "marginRight");
    }*/

    public void setFlex(FlexLayout layout, Widget child, LayoutData layoutData, boolean vertical) {
        setFlex(layout, child, layoutData.flexBasis, vertical);
    }
    public void setFlex(FlexLayout layout, Widget child, Integer flexBasis, boolean vertical) {
        // it's important to set min-width, min-height, because flex-basis is automatically set to min-height if it's smaller (test case in LinearContainerView)
        FlexPanel.setBaseSize(child, vertical, flexBasis); // last parameter is null because we're setting main size
        //child.getStyle().setProperty(getFlexAttrName(), getFlexValue(flex, getFlexBasisString(flexBasis)));
//        JComponent component = child.getComponent();
//        layout.setConstraints(component, new FlexConstraints(layoutData.alignment, layoutData.flex));
        child.getComponent().invalidate();
    }

/*    public void setFlexBasis(FlexPanel.LayoutData layoutData, Element child, int flexBasis, boolean vertical) {
        layoutData.setFlexBasis(flexBasis); // also sets base flex basis
        setFlex(child, layoutData.flex, flexBasis, vertical);
    }*/

 /*   public void fixFlexBasis(FlexPanel.LayoutData layoutData, FlexTabbedPanel child, boolean vertical) {
        if(layoutData.flexBasis != null)
            return;

        Element childElement = child.getElement();
        // фиксируем явную ширину composite'а (ставим базис равный ширине, flex - 0)
        int size = getSize(childElement, vertical);

        Element childWidgetElement = child.getElement();

        if(layoutData.flex == 0) // оптимизация если flex'а нет, этот размер и фиксируем 
            layoutData.setFlexBasis(size);
        else {
            setFlex(childElement, 0, size, vertical);

            // у widget'a comoposite'а убираем растягивание
            setFlex(childWidgetElement, 0, null, vertical);
            setAlignment(childWidgetElement, GFlexAlignment.START);

            // измеряем ширину, запоминаем в базис
            int calcSize = getSize(childWidgetElement, vertical);

            // возвращаем растягивание
            setFlex(childWidgetElement, 1, null, vertical);
            setAlignment(childWidgetElement, GFlexAlignment.STRETCH);

            // выставляем расчитанный базис (возвращаем flex)
            setFlexBasis(layoutData, childElement, calcSize, vertical);
        }
    }*/

    private String getFlexValue(double flex, String flexBasis) {
        return flex + " 0 " + flexBasis;
    }

    public void setAlignment(FlexAlignment alignment) {
        //child.getStyle().setProperty(getAlignAttrName(), getAlignmentValue(alignment));
    }
}
