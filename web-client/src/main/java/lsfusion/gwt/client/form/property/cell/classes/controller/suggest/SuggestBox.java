package lsfusion.gwt.client.form.property.cell.classes.controller.suggest;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.BaseStaticImage;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.popup.PopupMenuCallback;
import lsfusion.gwt.client.base.view.popup.PopupMenuItemValue;
import lsfusion.gwt.client.base.view.popup.PopupMenuPanel;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.property.cell.classes.controller.SimpleTextBasedCellEditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.gwt.dom.client.BrowserEvents.BLUR;

public abstract class SuggestBox {

    private static final ClientMessages messages = ClientMessages.Instance.get();

    private List<String> latestSuggestions = new ArrayList<>();

    private Element lastSuggestElement = null;

    private boolean selectsFirstItem = true;
    private final SuggestOracle oracle;
    private String currentText;
    private final PopupMenuPanel suggestionPopup;
    private final InputElement inputElement;
    private ArrayList<Widget> bottomPanels;
    private final boolean strict;

    protected SuggestPopupButton refreshButton;
    protected boolean refreshButtonPressed;

    public interface Callback {
        void onSuggestionsReady(SuggestBox.Request request, SuggestBox.Response response);
    }
    
    private final Callback callback = new Callback() {
        public void onSuggestionsReady(Request request, Response response) {
            showSuggestions(inputElement, response.initial, response.suggestions, selectsFirstItem, bottomPanels, popupMenuCallback);
        }
    };

    private final PopupMenuCallback popupMenuCallback;

    public SuggestBox(SuggestOracle oracle, InputElement inputElement, Element parent, boolean strict, PopupMenuCallback callback) {
        this.oracle = oracle;
        this.inputElement = inputElement;
        this.strict = strict;

        this.suggestionPopup = new PopupMenuPanel();
        this.popupMenuCallback = suggestion -> {
            focus();
            setNewSelection(suggestion);

            callback.onMenuItemSelected(suggestion);
        };

        this.bottomPanels = new ArrayList<>();
        bottomPanels.add(createButtonsPanel(parent));

        Widget infoPanel = createInfoPanel(parent);
        if(infoPanel != null)
            bottomPanels.add(infoPanel);
    }

    protected abstract Widget createButtonsPanel(Element parent);
    protected abstract Widget createInfoPanel(Element parent);

    protected boolean isLoading;
    public void updateDecoration(boolean isLoading) {
        if (this.isLoading != isLoading) {
            refreshButton.changeImage(isLoading ? StaticImage.LOADING_IMAGE_PATH : StaticImage.REFRESH_IMAGE_PATH);
            this.isLoading = isLoading;
        }
    }

    protected PopupMenuItemValue getCurrentSelection() {
        if (isSuggestionListShowing()) {
            return suggestionPopup.getSelectedItemValue();
        } else {
            return null;
        }
    }

    public Element getPopupElement() {
        return suggestionPopup.getElement();
    }

    public void clearSelectedItem() {
        suggestionPopup.clearSelectedItem();
    }

    protected void showSuggestions(final Element suggestElement, boolean initial, Collection<? extends PopupMenuItemValue> suggestions, boolean isAutoSelectEnabled, ArrayList<Widget> bottomPanels, final PopupMenuCallback callback) {
        suggestionPopup.clearItems();

        if (suggestions.isEmpty()) {
            //show empty item for initial loading
            suggestionPopup.addTextItem(initial ? "" : messages.noResults());
        }

        for (final PopupMenuItemValue suggestion : suggestions) {
            suggestionPopup.addItem(suggestion, callback);
        }

        for(Widget bottomPanel : bottomPanels)
            suggestionPopup.addBottomPanelItem(bottomPanel);

        if (isAutoSelectEnabled && suggestions.size() > 0) {
            suggestionPopup.selectFirstItem();
        }

        // Link the popup autoHide to the TextBox.
        if (lastSuggestElement != suggestElement) {
            // If the suggest box has changed, free the old one first.
            if (lastSuggestElement != null) {
                assert false;
                suggestionPopup.removeAutoHidePartner(lastSuggestElement);
            }
            lastSuggestElement = suggestElement;
            suggestionPopup.addAutoHidePartner(suggestElement);
        }

        suggestionPopup.setPopupPositionAndShow(suggestElement);
    }

    public void setLatestSuggestions(List<String> latestSuggestions) {
        this.latestSuggestions = latestSuggestions;
    }

    public boolean isValidValue(String value) {
        return value.isEmpty() || latestSuggestions.contains(value);
    }

    public void hideSuggestions() {
        suggestionPopup.hide();
    }

    public boolean isSuggestionListShowing() {
        return suggestionPopup.isShowing();
    }

    public void setAutoSelectEnabled(boolean selectsFirstItem) {
        this.selectsFirstItem = selectsFirstItem;
    }

    public void focus() {
        FocusUtils.focus(inputElement, FocusUtils.Reason.OTHER);
    }

    public void showSuggestionList(boolean all) {
        currentText = getCurrentText();
        if (all) {
            oracle.requestSuggestions(new Request(null), callback);
        } else {
            refreshSuggestionList();
        }
    }

    public void updateSuggestionList() {
        // Get the raw text.
        String text = getCurrentText();
        if (text.equals(currentText)) return;
        currentText = text;

        refreshSuggestionList();
    }

    public void refreshSuggestionList() {
        oracle.requestSuggestions(new Request(currentText), callback);
    }

    public String getCurrentText() {
        return SimpleTextBasedCellEditor.getInputValue(inputElement);
    }

    public void onBrowserEvent(EventHandler handler) {
        String type = handler.event.getType();

        if (BrowserEvents.KEYDOWN.equals(type)) {
            onKeyDown(handler);
        } else if(BrowserEvents.KEYUP.equals(type)) {
            onKeyUp(handler);
        }

        if (handler.consumed) return;

        if (GKeyStroke.isCharModifyKeyEvent(handler.event, null)) updateSuggestionList();

        if (!handler.consumed && BLUR.equals(handler.event.getType())) {
            //restore focus and ignore blur if refresh button pressed
            if (refreshButtonPressed) {
                refreshButtonPressed = false;
                handler.consume();
                focus();
            }
        }
    }

    public void onKeyDown(EventHandler handler) {
        switch (handler.event.getKeyCode()) {
            case KeyCodes.KEY_DOWN:
                if (isSuggestionListShowing()) {
                    suggestionPopup.moveSelectionDown();
                    updateSuggestBox();
                    handler.consume();
                }
                break;
            case KeyCodes.KEY_UP:
                if (isSuggestionListShowing()) {
                    suggestionPopup.moveSelectionUp();
                    updateSuggestBox();
                    handler.consume();
                }
                break;
            case KeyCodes.KEY_ENTER:
            case KeyCodes.KEY_TAB:
                PopupMenuItemValue suggestion = getCurrentSelection();
                if (suggestion != null && strict) {
                    setNewSelection(suggestion);
                }
                break;
        }
    }

    public void onKeyUp(EventHandler handler) {
        // After every user key input, refresh the popup's suggestions.
        if (GKeyStroke.isSuitableEditKeyEvent(handler.event)) {
            updateSuggestionList();
        }
    }

    private void updateSuggestBox() {
        if (!strict) {
            setSelection(getCurrentSelection());
        }
    }

    /**
     * Set the new suggestion in the text box.
     *
     * @param curSuggestion the new suggestion
     */
    private void setNewSelection(PopupMenuItemValue curSuggestion) {
        setSelection(curSuggestion);
        suggestionPopup.hide();
    }

    public void setSelection(PopupMenuItemValue suggestion) {
        currentText = suggestion != null ? suggestion.getReplacementString() : null;
        SimpleTextBasedCellEditor.setInputValue(inputElement, currentText);
    }

    public static abstract class SuggestOracle {
        public abstract void requestSuggestions(SuggestBox.Request request, SuggestBox.Callback callback);
    }

    public static class Request {
        public String query;

        public Request(String query) {
            this.query = query;
        }
    }

    public static class Response {
        ArrayList<PopupMenuItemValue> suggestions;
        boolean initial;

        public Response(ArrayList<PopupMenuItemValue> suggestions, boolean initial) {
            this.suggestions = suggestions;
            this.initial = initial;
        }
    }

    protected abstract static class SuggestPopupButton extends GToolbarButton {
        public SuggestPopupButton(BaseStaticImage image) {
            super(image);
            getElement().addClassName("suggestPopupButton");
        }

        @Override
        public void setFocus(boolean focused) {
            // in Firefox FocusImpl calls focus() immediately
            // (in suggest box blur event is called before button action performed, which leads to commit editing problems)
            // while in FocusImplSafari (Chrome) this is done with 0 delay timeout.
            // doing the same here for equal behavior (see also MenuBar.setFocus())
            Timer t = new Timer() {
                public void run() {
                    SuggestPopupButton.super.setFocus(focused);
                }
            };
            t.schedule(0);
        }
    }
}
