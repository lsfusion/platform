package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.view.DivWidget;
import lsfusion.gwt.client.base.view.PopupDialogPanel;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.GFontMetrics;
import lsfusion.gwt.client.form.design.GFontWidthString;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.view.StyleDefaults;

import java.util.Collections;
import java.util.List;

public abstract class GFilterOptionSelector<T> extends FocusWidget {
    protected PopupDialogPanel popup = new PopupDialogPanel();
    protected MenuBar menuBar = new MenuBar(true);
    protected T currentValue;
    
    public GFilterOptionSelector() {
        this(Collections.emptyList(), Collections.emptyList());
    }

    public GFilterOptionSelector(List<T> values, List<String> popupCaptions) {
        setElement(Document.get().createDivElement());

        addStyleName("userFilterSelector");
        setHeight(StyleDefaults.VALUE_HEIGHT_STRING);
//        setReadOnly(true);

        Style menuBarStyle = menuBar.getElement().getStyle();
        menuBarStyle.setProperty("maxHeight", StyleDefaults.VALUE_HEIGHT * 12, Style.Unit.PX); // 12 rows
        menuBarStyle.setOverflowY(Style.Overflow.AUTO);

        menuBar.addHandler(event -> {
            if (GKeyStroke.isEscapeKeyEvent(event.getNativeEvent())) {
                GwtClientUtils.stopPropagation(event);
                popup.hide();
            }
        }, KeyDownEvent.getType());
        
        addMouseDownHandler(event -> showMenu());
        addKeyPressHandler(event -> showMenu());

        for (T value : values) {
            addMenuItem(value, value.toString(), popupCaptions.get(values.indexOf(value)));
        }
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        setTabIndex(-1); // makes non-focusable
    }

    public void setSelectedValue(T value) {
        setSelectedValue(value, value != null ? value.toString() : null);
    }
    
    public void setSelectedValue(T value, String caption) {
        currentValue = value;
        setText(caption);
    }

    public void setText(String text) {
        getElement().setInnerText(GwtSharedUtils.nullTrim(text));
    }

    protected Widget getPopupContent() {
        return menuBar;
    }

    private void showMenu() {
        GwtClientUtils.showPopupInWindow(popup, getPopupContent(), getAbsoluteLeft(), getAbsoluteTop() + getOffsetHeight());
    }

    public void add(T value, String caption) {
        addMenuItem(value, caption, caption);
    }
    
    public void add(T value, String caption, String popupCaption) {
        addMenuItem(value, caption, popupCaption);
    }

    protected MenuItem addMenuItem(T value, String caption, String popupCaption) {
        MenuItem menuItem = new MenuItem(popupCaption, () -> {
            popup.hide();
            setSelectedValue(value, caption);
            valueChanged(value);
        });

        menuBar.addItem(menuItem);
        
        return menuItem;
    }

    public void hidePopup() {
        popup.hide();
    }

    public abstract void valueChanged(T value);
}