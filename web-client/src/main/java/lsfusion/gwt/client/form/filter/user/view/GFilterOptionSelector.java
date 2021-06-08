package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.PopupDialogPanel;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.GFontMetrics;
import lsfusion.gwt.client.form.design.GFontWidthString;
import lsfusion.gwt.client.form.event.GKeyStroke;

public abstract class GFilterOptionSelector<T> extends TextBox {
    protected PopupDialogPanel popup = new PopupDialogPanel();
    protected MenuBar menuBar = new MenuBar(true);
    protected T currentValue;

    public GFilterOptionSelector(T[] values) {
        addStyleName("userFilterSelector");
        setReadOnly(true);

        menuBar.addHandler(event -> {
            if (GKeyStroke.isEscapeKeyEvent(event.getNativeEvent())) {
                GwtClientUtils.stopPropagation(event);
                popup.hide();
            }
        }, KeyDownEvent.getType());
        
        addMouseDownHandler(event -> showMenu());
        addKeyPressHandler(event -> showMenu());

        for (T value : values) {
            addMenuItem(value, value.toString());
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

    @Override
    public void setText(String text) {
        String notNullText = text != null ? text : "";
        super.setText(notNullText);
        setTitle(notNullText);
        setWidth(GFontMetrics.getStringWidth(new GFontWidthString(GFont.DEFAULT_FONT, notNullText)) + "px");
    }

    protected Widget getPopupContent() {
        return menuBar;
    }

    private void showMenu() {
        GwtClientUtils.showPopupInWindow(popup, getPopupContent(), getAbsoluteLeft(), getAbsoluteTop() + getOffsetHeight());
    }

    public void add(T value, String caption) {
        addMenuItem(value, caption);
    }

    protected void addMenuItem(T value, String caption) {
        MenuItem menuItem = new MenuItem(caption, () -> {
            popup.hide();
            setSelectedValue(value, caption);
            valueChanged(value);
        });

        menuBar.addItem(menuItem);
    }

    public abstract void valueChanged(T value);
}