package lsfusion.interop.form.design;

import lsfusion.interop.base.view.FlexAlignment;

public interface AbstractComponent {
    void setFlex(double flex);
    void setAlignment(FlexAlignment alignment);
    void setShrink(boolean shrink);
    void setMarginTop(int marginTop);
    void setMarginBottom(int marginBottom);
    void setMarginLeft(int marginLeft);
    void setMarginRight(int marginRight);
    void setMargin(int margin);
}
