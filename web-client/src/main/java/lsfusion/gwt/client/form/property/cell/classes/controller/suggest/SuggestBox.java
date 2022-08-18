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
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.cell.classes.controller.SimpleTextBasedCellEditor;

import java.util.Collection;

public class SuggestBox {

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
      suggestionPopup = createPopup();
      suggestionPopup.setWidget(decorateSuggestionList(suggestionMenu));
    }

    public void hideSuggestions() {
      suggestionPopup.hide();
    }

    public boolean isSuggestionListShowing() {
      return suggestionPopup.isShowing();
    }

    protected PopupPanel createPopup() {
      PopupPanel p = new PopupPanel();
      p.setStyleName("gwt-SuggestBoxPopup");
      p.setAnimationType(PopupPanel.AnimationType.ROLL_DOWN);
      return p;
    }

    /**
     * Wrap the list of suggestions before adding it to the popup. You can
     * override this method if you want to wrap the suggestion list in a
     * decorator.
     *
     * @param suggestionList the widget that contains the list of suggestions
     * @return the suggestList, optionally inside of a wrapper
     */
    protected Widget decorateSuggestionList(Widget suggestionList) {
      return suggestionList;
    }

    protected Suggestion getCurrentSelection() {
      if (!isSuggestionListShowing()) {
        return null;
      }
      MenuItem item = suggestionMenu.getSelectedItem();
      return item == null ? null : item.getSuggestion();
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
      // Make sure that the menu is actually showing. These keystrokes
      // are only relevant when choosing a suggestion.
      if (isSuggestionListShowing()) {
        // If nothing is selected, getSelectedItemIndex will return -1 and we
        // will select index 0 (the first item) by default.
        suggestionMenu.selectItem(suggestionMenu.getSelectedItemIndex() + 1);
      }
    }

    protected void moveSelectionUp() {
      // Make sure that the menu is actually showing. These keystrokes
      // are only relevant when choosing a suggestion.
      if (isSuggestionListShowing()) {
        // if nothing is selected, then we should select the last suggestion by
        // default. This is because, in some cases, the suggestions menu will
        // appear above the text box rather than below it (for example, if the
        // text box is at the bottom of the window and the suggestions will not
        // fit below the text box). In this case, users would expect to be able
        // to use the up arrow to navigate to the suggestions.
        if (suggestionMenu.getSelectedItemIndex() == -1) {
          suggestionMenu.selectItem(suggestionMenu.getItems().size() - 1);
        } else {
          suggestionMenu.selectItem(suggestionMenu.getSelectedItemIndex() - 1);
        }
      }
    }

    protected void showSuggestions(final Element suggestElement,
        Collection<? extends Suggestion> suggestions,
        boolean isDisplayStringHTML, boolean isAutoSelectEnabled,
        final SuggestionCallback callback) {
      // Hide the popup if there are no suggestions to display.

      // Hide the popup before we manipulate the menu within it. If we do not
      // do this, some browsers will redraw the popup as items are removed
      // and added to the menu.
      if (suggestionPopup.isAttached()) {
        suggestionPopup.hide();
      }

      suggestionMenu.clearItems();

      for (final Suggestion curSuggestion : suggestions) {
        final MenuItem menuItem = new MenuItem(curSuggestion, isDisplayStringHTML);
        menuItem.setScheduledCommand(() -> callback.onSuggestionSelected(curSuggestion));

        suggestionMenu.addItem(menuItem);
      }

      if (isAutoSelectEnabled && suggestions.size() > 0) {
        // Select the first item in the suggestion menu.
        suggestionMenu.selectItem(0);
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

      suggestionPopup.showRelativeTo(suggestElement);
    }
  }

  private boolean selectsFirstItem = true;
  private final SuggestOracle oracle;
  private String currentText;
  private final SuggestionDisplay display;
  private final InputElement inputElement;
  private final boolean strict;
  private final Callback callback = new Callback() {
    public void onSuggestionsReady(Request request, Response response) {
      display.showSuggestions(inputElement, response.getSuggestions(),
          oracle.isDisplayStringHTML(), selectsFirstItem,
          suggestionCallback);
    }
  };
  private final SuggestionCallback suggestionCallback;

  public SuggestBox(SuggestOracle oracle, InputElement inputElement,
      SuggestionDisplay suggestDisplay, boolean strict, SuggestionCallback callback) {
    this.inputElement = inputElement;
    this.display = suggestDisplay;
    this.strict = strict;
    suggestionCallback = suggestion -> {
      focus();
      setNewSelection(suggestion);

      callback.onSuggestionSelected(suggestion);
    };

//    addEventsToTextBox();

    this.oracle = oracle;
  }

  /**
   * Check if the {@link SuggestionDisplay} is showing.
   *
   * @return true if the list of suggestions is currently showing, false if not
   */
  public boolean isSuggestionListShowing() {
    return display.isSuggestionListShowing();
  }

  /**
   * Turns on or off the behavior that automatically selects the first suggested
   * item. This behavior is on by default.
   *
   * @param selectsFirstItem Whether or not to automatically select the first
   *          suggestion
   */
  public void setAutoSelectEnabled(boolean selectsFirstItem) {
    this.selectsFirstItem = selectsFirstItem;
  }

  public void focus() {
    FocusUtils.focus(inputElement, FocusUtils.Reason.OTHER);
  }

  public void showSuggestionList(boolean all) {
    currentText = getCurrentText();
    if(all)
      oracle.requestDefaultSuggestions(new Request(null, 20), callback);
    else
      refreshSuggestionList();
  }
  public void updateSuggestionList() {
    // Get the raw text.
    String text = getCurrentText();
    if (text.equals(currentText))
      return;
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

    if(BrowserEvents.KEYDOWN.equals(type))
        onKeyDown(handler);

    if(handler.consumed)
        return;

    if (GKeyStroke.isCharModifyKeyEvent(handler.event, null))
        updateSuggestionList();
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
        //todo: replaced, do not hide if no results
        if (suggestion != null && strict) {
          setNewSelection(suggestion);
        }
        /*if (suggestion == null) {
          display.hideSuggestions();
        } else {
          setNewSelection(suggestion);
        }*/
        break;
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
