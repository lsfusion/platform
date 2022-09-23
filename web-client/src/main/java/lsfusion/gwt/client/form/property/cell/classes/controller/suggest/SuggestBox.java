package lsfusion.gwt.client.form.property.cell.classes.controller.suggest;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.KeyCodes;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.cell.classes.controller.SimpleTextBasedCellEditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SuggestBox {

    private static final ClientMessages messages = ClientMessages.Instance.get();

    private List<String> latestSuggestions = new ArrayList<>();

    private Element lastSuggestElement = null;

    private boolean selectsFirstItem = true;
    private final SuggestOracle oracle;
    private String currentText;
    private final PopupPanel suggestionPopup;
    private final InputElement inputElement;
    private FlexPanel bottomPanel;
    private final boolean strict;

    public interface Callback {
        void onSuggestionsReady(SuggestBox.Request request, SuggestBox.Response response);
    }
    
    private final Callback callback = new Callback() {
        public void onSuggestionsReady(Request request, Response response) {
            showSuggestions(inputElement, response.initial, response.suggestions, selectsFirstItem, bottomPanel, suggestionCallback);
        }
    };

    public interface SuggestionCallback {
        void onSuggestionSelected(Suggestion suggestion);
    }

    private final SuggestionCallback suggestionCallback;

    public SuggestBox(SuggestOracle oracle, InputElement inputElement, boolean strict, SuggestionCallback callback) {
        this.oracle = oracle;
        this.inputElement = inputElement;
        this.strict = strict;

        this.suggestionPopup = new PopupPanel();
        this.suggestionCallback = suggestion -> {
            focus();
            setNewSelection(suggestion);

            callback.onSuggestionSelected(suggestion);
        };
    }

    protected Suggestion getCurrentSelection() {
        if (isSuggestionListShowing()) {
            return suggestionPopup.getSelectedItemSuggestion();
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

    protected void showSuggestions(final Element suggestElement, boolean initial, Collection<? extends Suggestion> suggestions, boolean isAutoSelectEnabled, FlexPanel bottomPanel, final SuggestionCallback callback) {
        suggestionPopup.clearItems();

        if (suggestions.isEmpty()) {
            //show empty item for initial loading
            suggestionPopup.addTextItem(initial ? "" : messages.noResults());
        }

        for (final Suggestion suggestion : suggestions) {
            suggestionPopup.addItem(suggestion, callback);
        }

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

    public void setBottomPanel(FlexPanel bottomPanel) {
        this.bottomPanel = bottomPanel;
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
                Suggestion suggestion = getCurrentSelection();
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
    private void setNewSelection(Suggestion curSuggestion) {
        setSelection(curSuggestion);
        suggestionPopup.hide();
    }

    public void setSelection(Suggestion suggestion) {
        currentText = suggestion != null ? suggestion.getReplacementString() : null;
        SimpleTextBasedCellEditor.setInputValue(inputElement, currentText);
    }

    public static abstract class SuggestOracle {
        public abstract void requestSuggestions(SuggestBox.Request request, SuggestBox.Callback callback);
    }

    public interface Suggestion {
        String getDisplayString();

        String getReplacementString();
    }
    
    public static class Request {
        public String query;

        public Request(String query) {
            this.query = query;
        }
    }

    public static class Response {
        ArrayList<Suggestion> suggestions;
        boolean initial;

        public Response(ArrayList<Suggestion> suggestions, boolean initial) {
            this.suggestions = suggestions;
            this.initial = initial;
        }
    }
}
