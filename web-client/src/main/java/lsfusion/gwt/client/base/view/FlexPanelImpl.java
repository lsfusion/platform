package lsfusion.gwt.client.base.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;

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

    public void setupParentDiv(DivElement parent, boolean vertical, GFlexAlignment justify) {
        parent.getStyle().setProperty("display", getDisplayFlexValue());
        parent.getStyle().setProperty(getDirectionAttrName(), getDirectionValue(vertical));
        parent.getStyle().setProperty(getJustifyContentAttrName(), getAlignmentValue(justify));
    }

    public void setVisible(DivElement parent, boolean visible) {
        parent.getStyle().setProperty("display", visible ? getDisplayFlexValue() : "none");
    }

    public FlexPanel.LayoutData insertChild(Element parent, Element child, int beforeIndex, GFlexAlignment alignment, double flex, Integer flexBasis, boolean vertical) {
        FlexPanel.LayoutData layoutData = new FlexPanel.LayoutData(child, alignment, flex, flexBasis);

        updateFlex(layoutData, child, vertical);
        updateAlignment(layoutData, child);

        DOM.insertChild(parent.<com.google.gwt.user.client.Element>cast(), child.<com.google.gwt.user.client.Element>cast(), beforeIndex);

        return layoutData;
    }

    public void removeChild(FlexPanel.LayoutData layoutData) {
        layoutData.child.removeFromParent();
    }

    public String getFlexBasisString(Integer flexBasis) {
        return flexBasis == null ? "auto" : flexBasis + "px";
    }

    public void setFlex(Element child, double flex, Integer flexBasis, boolean vertical) {
        // it's important to set min-width, min-height, because flex-basis is automatically set to min-height if it's smaller (test case in LinearContainerView)
        FlexPanel.setBaseSize(child, vertical, flexBasis, false); // last parameter is false because we're setting main size
        child.getStyle().setProperty(getFlexAttrName(), getFlexValue(flex, getFlexBasisString(flexBasis)));
    }

    public int getFullSize(Element child, boolean vertical) {
        return vertical ? GwtClientUtils.getFullHeight(child) : GwtClientUtils.getFullWidth(child);
//        return child.getPropertyInt(vertical ? "offsetHeight" : "offsetWidth");
    }

    public int getSize(Element child, boolean vertical) {
        return vertical ? GwtClientUtils.getHeight(child) : GwtClientUtils.getWidth(child);
//        return child.getPropertyInt(vertical ? "clientHeight" : "clientWidth") -
//                (child.getPropertyInt(vertical ? "paddingTop" : "paddingLeft") +
//                        child.getPropertyInt(vertical ? "paddingBottom" : "paddingRight"));
    }

    public int getMargins(Element child, boolean vertical) {
        return child.getPropertyInt(vertical ? "marginTop" : "marginLeft") +
                    child.getPropertyInt(vertical ? "marginBottom" : "marginRight");
    }

    public void updateFlex(FlexPanel.LayoutData layoutData, Element child, boolean vertical) {
        setFlex(child, layoutData.flex, layoutData.flexBasis, vertical);
    }

    public void updateAlignment(FlexPanel.LayoutData layoutData, Element child) {
        child.getStyle().setProperty(getAlignAttrName(), getAlignmentValue(layoutData.alignment));
    }

    public void setFlexBasis(FlexPanel.LayoutData layoutData, Element child, int flexBasis, boolean vertical) {
        layoutData.setFlexBasis(flexBasis); // also sets base flex basis
        updateFlex(layoutData, child, vertical);
    }
    public void setFlex(FlexPanel.LayoutData layoutData, Element child, double flex, Integer flexBasis, boolean vertical) {
        layoutData.flex = flex;
        layoutData.flexBasis = flexBasis;
        updateFlex(layoutData, child, vertical);
    }

    public void setStretchFlex(FlexPanel.LayoutData layoutData, Element child, boolean set, boolean vertical) {
        if(!layoutData.isFlex()) {
            double newFlex = set ? 1 : layoutData.baseFlex;
            if(layoutData.flex != newFlex) { // for optimization purposes + there might be problems with setBaseSize, since some data components use it explicitly without setting LayoutData
                layoutData.flex = newFlex;
                updateFlex(layoutData, child, vertical);
            }
        }
    }

    public void setStretchAlignment(FlexPanel.LayoutData layoutData, Element child, boolean set, boolean vertical) {
        if(!layoutData.baseAlignment.equals(GFlexAlignment.STRETCH)) {
            GFlexAlignment newAlignment = set ? GFlexAlignment.STRETCH : layoutData.baseAlignment;
            if(!newAlignment.equals(layoutData.alignment)) {
                layoutData.alignment = newAlignment;
                updateAlignment(layoutData, child);
            }
        }
    }

    public void fixFlexBasis(FlexTabbedPanel child, boolean vertical) {
        FlexPanel.LayoutData layoutData = (FlexPanel.LayoutData) child.getLayoutData();
        if(layoutData.flexBasis != null)
            return;

        Element childElement = child.getElement();
        // фиксируем явную ширину composite'а (ставим базис равный ширине, flex - 0)
        int size = getSize(childElement, vertical);

        Element childWidgetElement = child.getElement();

        if(!layoutData.isFlex()) // оптимизация если flex'а нет, этот размер и фиксируем
            layoutData.setFlexBasis(size);
        else {
            setFlex(childElement, 0, size, vertical);

            // у widget'a comoposite'а убираем растягивание
            setFlex(childWidgetElement, 0, null, vertical);
//            setAlignment(childWidgetElement, GFlexAlignment.START);

            // измеряем ширину, запоминаем в базис
            int calcSize = getSize(childWidgetElement, vertical);

            // возвращаем растягивание
            setFlex(childWidgetElement, 1, null, vertical);
//            setAlignment(childWidgetElement, GFlexAlignment.STRETCH);

            // выставляем расчитанный базис (возвращаем flex)
            setFlexBasis(layoutData, childElement, calcSize, vertical);
        }
    }

    private String getFlexValue(double flex, String flexBasis) {
        return flex + " 0 " + flexBasis;
    }
}
