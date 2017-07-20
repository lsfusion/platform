package lsfusion.interop.form.layout;

public interface AbstractComponent<C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> {
    void setFlex(double flex);
    void setAlignment(FlexAlignment alignment);
    void setMarginTop(int marginTop);
    void setMarginBottom(int marginBottom);
    void setMarginLeft(int marginLeft);
    void setMarginRight(int marginRight);
    void setMargin(int margin);
}
