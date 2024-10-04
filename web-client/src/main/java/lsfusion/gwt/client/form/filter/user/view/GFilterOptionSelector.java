package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FocusWidget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.view.popup.PopupMenuItemValue;
import lsfusion.gwt.client.base.view.popup.PopupMenuPanel;
import lsfusion.gwt.client.form.event.GKeyStroke;

import java.util.Collections;
import java.util.List;

public abstract class GFilterOptionSelector<T> extends FocusWidget {
    protected PopupMenuPanel popup = new PopupMenuPanel( true);
    protected T currentValue;

    public GFilterOptionSelector(GFilterConditionView.UIHandler uiHandler) {
        this(uiHandler, Collections.emptyList(), Collections.emptyList());
    }

    public GFilterOptionSelector(GFilterConditionView.UIHandler uiHandler, List<T> values, List<String> popupCaptions) {
        setElement(Document.get().createDivElement());

       GwtClientUtils.addClassNames(this, "form-control", "filter-selector");
        
        addMouseDownHandler(event -> showMenu());
        addKeyPressHandler(event -> {
            showMenu();
        });
        addKeyDownHandler(event -> {
            
            if (GKeyStroke.isEscapeKeyEvent(event.getNativeEvent())) {
                if (popup.isShowing()) {
                    hidePopup();
                } else {
                    uiHandler.resetConditions();
                }
                GwtClientUtils.stopPropagation(event);
            } else {
                popup.onBrowserEvent(Event.as(event.getNativeEvent()));
            }
        });

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

    private void showMenu() {
        popup.setPopupPositionAndShow(getElement());
        popup.selectFirstItem();
    }

    public void add(T value, String caption) {
        addMenuItem(value, caption, caption);
    }
    
    public void add(T value, String caption, String popupCaption) {
        addMenuItem(value, caption, popupCaption);
    }

    protected void addMenuItem(T value, String caption, String popupCaption) {
        popup.addItem(createItem(value, caption, popupCaption), suggestion -> {
            popup.hide();
            setSelectedValue(value, caption);
            valueChanged(value);
        });
    }
    
    protected PopupMenuItemValue createItem(T value, String caption, String popupCaption) {
        return new PopupMenuItemValue() {
            @Override
            public String getDisplayString() {
                return popupCaption;
            }

            @Override
            public String getReplacementString() {
                return caption;
            }
        };
    }

    public void hidePopup() {
        popup.hide();
    }

    public abstract void valueChanged(T value);
}