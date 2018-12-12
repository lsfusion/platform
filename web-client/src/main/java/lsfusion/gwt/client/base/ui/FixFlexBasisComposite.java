package lsfusion.gwt.client.base.ui;

import com.google.gwt.user.client.ui.Widget;

// вообще логически это Composite
// но нужен функционал по определению basis, а для этого нужен контейнер в который можно докинуть новый FlexPanel, переместить туда элемент этого Composite так чтобы layout'инг верхних элементов не поменялся (иначе могут быть "скачки" в элементах со скроллами)
// пока для этого используем FlexPanel
public class FixFlexBasisComposite extends FlexPanel {

    public Widget widget;
    
    public FixFlexBasisComposite() {
        super(true);
    }
    
    protected void initWidget(Widget widget) {
        this.widget = widget;
        addFill(widget);
    }
}
