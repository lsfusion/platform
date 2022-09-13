/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package lsfusion.gwt.client.form.property.cell.classes.controller.suggest;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Callback;
import com.google.gwt.user.client.ui.SuggestOracle.Request;
import com.google.gwt.user.client.ui.SuggestOracle.Response;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.cell.classes.controller.SimpleTextBasedCellEditor;

import java.util.Collection;

public class SuggestBox {

    private static final ClientMessages messages = ClientMessages.Instance.get();

    /**
     * The callback used when a user selects a {@link Suggestion}.
     */
    public interface SuggestionCallback {
        void onSuggestionSelected(Suggestion suggestion);
    }

    public static class SuggestionDisplay {

        private final MenuBar suggestionMenu;
        private final PopupPanel suggestionPopup;

        /**
         * We need to keep track of the last {@link SuggestBox} because it acts as
         * an autoHide partner for the {@link PopupPanel}. If we use the same
         * display for multiple {@link SuggestBox}, we need to switch the autoHide
         * partner.
         */
        private Element lastSuggestElement = null;

        public SuggestionDisplay() {
            suggestionMenu = new MenuBar();
            suggestionPopup = new PopupPanel();
            suggestionPopup.add(suggestionMenu);
        }

        public void hideSuggestions() {
            suggestionPopup.hide();
        }

        private boolean isSuggestionListShowing() {
            return suggestionPopup.isShowing();
        }

        protected Suggestion getCurrentSelection() {
            if (isSuggestionListShowing()) {
                return suggestionMenu.getSelectedItemSuggestion();
            } else {
                return null;
            }
        }

        /**
         * Get the {@link PopupPanel} used to display suggestions.
         *
         * @return the popup panel
         */
        protected PopupPanel getPopupPanel() {
            return suggestionPopup;
        }

        protected void moveSelectionDown() {
            if (isSuggestionListShowing()) {
                suggestionMenu.moveSelectionDown();
            }
        }

        protected void moveSelectionUp() {
            if (isSuggestionListShowing()) {
                suggestionMenu.moveSelectionUp();
            }
        }

        protected void showSuggestions(final Element suggestElement, boolean emptyQuery, Collection<? extends Suggestion> suggestions, boolean isAutoSelectEnabled, FlexPanel bottomPanel, final SuggestionCallback callback) {
            // Hide the popup if there are no suggestions to display.

            // Hide the popup before we manipulate the menu within it. If we do not
            // do this, some browsers will redraw the popup as items are removed
            // and added to the menu.
            if (suggestionPopup.isAttached()) {
                suggestionPopup.hide();
            }

            suggestionMenu.clearItems();

            if (suggestions.isEmpty()) {
                //show empty item for initial loading
                suggestionMenu.addTextItem(emptyQuery ? "" : messages.noResults());
            }

            for (final Suggestion suggestion : suggestions) {
                suggestionMenu.addItem(suggestion, callback);
            }

            suggestionMenu.addBottomPanelItem(bottomPanel, suggestionPopup);

            if (isAutoSelectEnabled && suggestions.size() > 0) {
                suggestionMenu.selectFirstItem();
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
    }

    private boolean selectsFirstItem = true;
    private final SuggestOracle oracle;
    private String currentText;
    private final SuggestionDisplay display;
    private final InputElement inputElement;
    private FlexPanel bottomPanel;
    private final boolean strict;
    private final Callback callback = new Callback() {
        public void onSuggestionsReady(Request request, Response response) {
            display.showSuggestions(inputElement, request.getQuery() == null, response.getSuggestions(), selectsFirstItem, bottomPanel, suggestionCallback);
        }
    };
    private final SuggestionCallback suggestionCallback;

    public SuggestBox(SuggestOracle oracle, InputElement inputElement, SuggestionDisplay suggestDisplay, boolean strict, SuggestionCallback callback) {
        this.inputElement = inputElement;
        this.display = suggestDisplay;
        this.strict = strict;
        suggestionCallback = suggestion -> {
            focus();
            setNewSelection(suggestion);

            callback.onSuggestionSelected(suggestion);
        };

        this.oracle = oracle;
    }

    public void setBottomPanel(FlexPanel bottomPanel) {
        this.bottomPanel = bottomPanel;
    }

    public boolean isSuggestionListShowing() {
        return display.isSuggestionListShowing();
    }

    public void setAutoSelectEnabled(boolean selectsFirstItem) {
        this.selectsFirstItem = selectsFirstItem;
    }

    public void focus() {
        FocusUtils.focus(inputElement, FocusUtils.Reason.OTHER);
    }

    public void showSuggestionList(boolean all) {
        currentText = getCurrentText();
        if (all) oracle.requestDefaultSuggestions(new Request(null, 20), callback);
        else refreshSuggestionList();
    }

    public void updateSuggestionList() {
        // Get the raw text.
        String text = getCurrentText();
        if (text.equals(currentText)) return;
        currentText = text;

        refreshSuggestionList();
    }

    public void refreshSuggestionList() {
        oracle.requestSuggestions(new Request(currentText, 20), callback);
    }

    public String getCurrentText() {
        return SimpleTextBasedCellEditor.getInputValue(inputElement);
    }

//  private void addEventsToTextBox() {
//    inputElement.addKeyDownHandler(this::onKeyDown);
//    inputElement.addKeyUpHandler(this::onKeyUp);
//  }

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
                display.moveSelectionDown();
                if (isSuggestionListShowing()) {
                    handler.consume();
                }
                break;
            case KeyCodes.KEY_UP:
                display.moveSelectionUp();
                if (isSuggestionListShowing()) {
                    handler.consume();
                }
                break;
            case KeyCodes.KEY_ENTER:
            case KeyCodes.KEY_TAB:
                Suggestion suggestion = display.getCurrentSelection();
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

    /**
     * Set the new suggestion in the text box.
     *
     * @param curSuggestion the new suggestion
     */
    private void setNewSelection(Suggestion curSuggestion) {
        setSelection(curSuggestion);
        display.hideSuggestions();
    }

    public void setSelection(Suggestion suggestion) {
        currentText = suggestion != null ? suggestion.getReplacementString() : null;
        SimpleTextBasedCellEditor.setInputValue(inputElement, currentText);
    }
}
