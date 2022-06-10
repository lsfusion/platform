package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.impl.TextBoxImpl;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GAsync;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.base.view.CopyPasteUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.classes.controller.suggest.GCompletionType;
import lsfusion.gwt.client.form.property.cell.classes.controller.suggest.SuggestBox;
import lsfusion.gwt.client.form.property.cell.classes.controller.suggest.TextBox;
import lsfusion.gwt.client.form.property.cell.classes.view.TextBasedCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.gwt.dom.client.BrowserEvents.BLUR;
import static lsfusion.gwt.client.base.GwtClientUtils.isShowing;
import static lsfusion.gwt.client.base.GwtClientUtils.nvl;

public abstract class TextBasedCellEditor extends RequestReplaceValueCellEditor {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private static final String LOADING_IMAGE_PATH = "loading.gif";
    private static final String REFRESH_IMAGE_PATH = "refresh.png";
    private static TextBoxImpl textBoxImpl = GWT.create(TextBoxImpl.class);

    protected final GPropertyDraw property;

    boolean hasList;
    GCompletionType completionType;
    GInputListAction[] actions;
    CustomSuggestBox suggestBox = null;
    boolean escapeComma;

    public TextBasedCellEditor(EditManager editManager, GPropertyDraw property) {
        this(editManager, property, null);
    }

    public TextBasedCellEditor(EditManager editManager, GPropertyDraw property, GInputList inputList) {
        this(editManager, property, inputList, false);
    }

    public TextBasedCellEditor(EditManager editManager, GPropertyDraw property, GInputList inputList, boolean escapeComma) {
        super(editManager);
        this.property = property;
        this.hasList = inputList != null && !disableSuggest();
        this.completionType = inputList != null ? inputList.completionType : GCompletionType.NON_STRICT;
        this.actions = inputList != null ? inputList.actions : null;
        this.escapeComma = escapeComma;
    }

    protected boolean disableSuggest() {
        return false;
    }

    @Override
    public void start(Event event, Element parent, Object oldValue) {
        String value = property.clearText ? "" : escapeComma(tryFormatInputText(oldValue), GMouseStroke.isEvent(event));
        if(hasList) {
            suggestBox.showSuggestionList();
            suggestBox.setValue(value);
        }
        InputElement inputElement = getInputElement(parent);
        String startEventValue = checkStartEvent(event, parent, this::checkInputValidity);
        boolean selectAll = startEventValue == null;
        value = startEventValue != null ? startEventValue : value;

        //we need this order (focus before setValue) for single click editing IntegralCellEditor (type=number)
        inputElement.focus();
        setInputValue(inputElement, value);

        if (selectAll) {
            inputElement.select();
        }
    }

    protected void setInputValue(InputElement element, String value) {
        element.setValue(value);
    }

    @Override
    public void onBrowserEvent(Element parent, EventHandler handler) {
        Event event = handler.event;
        String type = event.getType();
        if (GKeyStroke.isCharAddKeyEvent(event) || GKeyStroke.isCharDeleteKeyEvent(event) ||
                GKeyStroke.isCharNavigateKeyEvent(event) || GMouseStroke.isEvent(event) || GKeyStroke.isPasteFromClipboardEvent(event) || GMouseStroke.isContextMenuEvent(event)) {
            boolean isCorrect = true;

            String stringToAdd = null;
            if(GKeyStroke.isCharAddKeyEvent(event))
                stringToAdd = String.valueOf((char) event.getCharCode());
            else if(GKeyStroke.isPasteFromClipboardEvent(event))
                stringToAdd = CopyPasteUtils.getEventClipboardData(event);
            if(stringToAdd != null && !checkInputValidity(parent, stringToAdd))
                isCorrect = false; // this thing is needed to disable inputting incorrect symbols

            handler.consume(isCorrect, false);
        } else if (BLUR.equals(type)) {
            //restore focus and ignore blur if refresh button pressed
            if(hasList && suggestBox.display.isRefreshButtonPressed()) {
                suggestBox.display.setRefreshButtonPressed(false);
                suggestBox.setFocus(true);
                return;
            }
        } else {
            Integer inputActionIndex = property.getInputActionIndex(event, true);
            if(inputActionIndex != null) {
                validateAndCommit(suggestBox.getElement(), inputActionIndex, true, CommitReason.OTHER);
                return;
            }
        }

        super.onBrowserEvent(parent, handler);
    }
    
    private boolean checkInputValidity(Element parent, String stringToAdd) {
        InputElement input = getInputElement(parent);
        int cursorPosition = textBoxImpl.getCursorPos(input);
        int selectionLength = textBoxImpl.getSelectionLength(input);
        String currentValue = getCurrentText(parent);
        String firstPart = currentValue == null ? "" : currentValue.substring(0, cursorPosition);
        String secondPart = currentValue == null ? "" : currentValue.substring(cursorPosition + selectionLength);

        return isStringValid(firstPart + stringToAdd + secondPart);
    }
    
    protected boolean isStringValid(String string) {
        try {
            tryParseInputText(string, false);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    protected Element setupInputElement(RenderContext renderContext){
        Element inputElement = createInputElement();

        Style.TextAlign textAlign = property.getTextAlignStyle();
        if(textAlign != null)
            inputElement.getStyle().setTextAlign(textAlign);

        // input doesn't respect justify-content, stretch, plus we want to include paddings in input (to avoid having "selection border")
        GwtClientUtils.setupPercentParent(inputElement);

        TextBasedCellRenderer.render(property, inputElement, renderContext, isMultiLine());

        return inputElement;
    }

    @Override
    public void render(Element cellParent, RenderContext renderContext, Pair<Integer, Integer> renderedSize, Object oldValue) {
        if(property.autoSize) { // we have to set sizes that were rendered, since input elements have really unpredicatble sizes
            // wrapping element since otherwise it's not clear how to restore height (sometimes element has it set)
            // actually it seems that we can avoid wrapping asserting that upper element is div set with setPercentMain, but for now it's not that important
            Element div = Document.get().createDivElement();
            div.getStyle().setHeight(renderedSize.second, Style.Unit.PX);
            div.getStyle().setWidth(renderedSize.first, Style.Unit.PX);
            cellParent.appendChild(div);

            cellParent = div;
        }

        cellParent.appendChild(setupInputElement(renderContext));
    }

    protected boolean isMultiLine() {
        return false;
    }

    @Override
    public void clearRender(Element cellParent, RenderContext renderContext, boolean cancel) {
        if(hasList)
            suggestBox.hideSuggestions();

        super.clearRender(cellParent, renderContext, cancel);
    }

    public Object getValue(Element parent, Integer contextAction) {
        String stringValue = contextAction != null ? suggestBox.getValue() : getCurrentText(parent);
        if(hasList && completionType.isStrict() && contextAction == null && !suggestBox.isValidValue(stringValue))
            return RequestValueCellEditor.invalid;
        try {
            return tryParseInputText(stringValue, true); //if button pressed, input element is button
        } catch (ParseException e) {
            return RequestValueCellEditor.invalid;
        }
    }

    protected Element createTextInputElement() {
        return Document.get().createTextInputElement();
    }
    protected ValueBoxBase<String> createTextBoxBase() {
        return new TextBox();
    }

    protected boolean isThisCellEditor() {
        assert hasList;
        boolean showing = isShowing(suggestBox);
//        assert (editManager.isEditing() && this == editManager.getCellEditor()) == showing;
        return showing;
    }

    public Element createInputElement() {
        if(hasList) {
            suggestBox = new CustomSuggestBox(new SuggestOracle() {
                private Timer delayTimer;
                private Request currentRequest; // current pending request
                private Callback currentCallback;

                private String prevSucceededEmptyQuery;

                @Override
                public void requestDefaultSuggestions(Request request, Callback callback) {
                    requestAsyncSuggestions(request, callback);
                }

                @Override
                public void requestSuggestions(Request request, Callback callback) {
                    requestAsyncSuggestions(request, callback);
                }

                private void requestAsyncSuggestions(Request request, Callback callback) {
                    currentRequest = request;
                    currentCallback = callback;

                    if(delayTimer == null)
                        updateAsyncValues();
                }

                private void updateAsyncValues() {
                    final Request request = currentRequest;
                    currentRequest = null;
                    final Callback callback = currentCallback;
                    currentCallback = null;

                    boolean emptyQuery = request.getQuery() == null;
                    String query = nvl(request.getQuery(), "");
                    if(prevSucceededEmptyQuery != null && query.startsWith(prevSucceededEmptyQuery))
                        return;

                    suggestBox.updateDecoration(false, true, true);

                    if (emptyQuery) { // to show empty popup immediately
                        // add timer to avoid blinking when empty popup is followed by non-empty one
                        Timer t = new Timer() {
                            @Override
                            public void run() {
                                if (isThisCellEditor() && !suggestBox.isSuggestionListShowing()) {
                                    callback.onSuggestionsReady(request, new Response(new ArrayList<>()));
                                    setMinWidth(suggestBox, false);
                                }
                            }
                        };
                        t.schedule(100);
                    }

                    assert delayTimer == null;
                    // we're sending a request, so we want to delay all others for at least 100ms
                    // also we're using timer to identify the call in cancelAndFlushDelayed
                    Timer execTimer = new Timer() {
                        public void run() {
                            flushDelayed();
                        }
                    };
                    execTimer.schedule(1000);
                    delayTimer = execTimer;

                    editManager.getAsyncValues(query, new AsyncCallback<Pair<ArrayList<GAsync>, Boolean>>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            if (isThisCellEditor()) //  && suggestBox.isSuggestionListShowing()
                                cancelAndFlushDelayed(execTimer);
                        }

                        @Override
                        public void onSuccess(Pair<ArrayList<GAsync>, Boolean> result) {
                            if (isThisCellEditor()) { //  && suggestBox.isSuggestionListShowing() in desktop this check leads to "losing" result, since suggest box can be not shown yet (!), however maybe in web-client it's needed for some reason (but there can be the risk of losing result)
                                suggestBox.setAutoSelectEnabled(completionType.isAnyStrict() && !emptyQuery);

                                boolean succeededEmpty = false;
                                if(result.first != null) {
                                    List<String> rawSuggestions = new ArrayList<>();
                                    List<Suggestion> suggestionList = new ArrayList<>();
                                    for (GAsync suggestion : result.first) {
                                        rawSuggestions.add(suggestion.rawString);
                                        suggestionList.add(new Suggestion() {
                                            @Override
                                            public String getDisplayString() {
                                                return suggestion.displayString; // .replace("<b>", "<strong>").replace("</b>", "</strong>");
                                            }

                                            @Override
                                            public String getReplacementString() {
                                                return suggestion.rawString;
                                            }
                                        });
                                    }
                                    suggestBox.setLatestSuggestions(rawSuggestions);
                                    callback.onSuggestionsReady(request, new Response(suggestionList));
                                    setMinWidth(suggestBox, true);

                                    succeededEmpty = suggestionList.isEmpty();
                                }

                                suggestBox.updateDecoration(succeededEmpty, false, result.second);

                                if(!result.second) {
                                    if (succeededEmpty)
                                        prevSucceededEmptyQuery = query;
                                    else
                                        prevSucceededEmptyQuery = null;
                                }

                                cancelAndFlushDelayed(execTimer);
                            }
                        }
                    });
                }

                private void cancelAndFlushDelayed(Timer execTimer) {
                    if(delayTimer == execTimer) { // we're canceling only if the current timer has not changed
                        delayTimer.cancel();

                        flushDelayed();
                    }
                }

                private void flushDelayed() {
                    // assert that delaytimer is equal to execTimer
                    delayTimer = null;

                    if(currentRequest != null) // there was pending request
                        updateAsyncValues();
                }

                private void setMinWidth(CustomSuggestBox suggestBox, boolean offsets) {
                    setMinWidth(suggestBox.getPopupElement(), suggestBox.getOffsetWidth() - (offsets ? 8 : 0)); //8 = offsets
                }

                private native void setMinWidth(Element element, int minWidth) /*-{
                    Array.prototype.forEach.call(element.getElementsByClassName("item"), function(item) {
                        item.style.minWidth = minWidth + "px";
                    });
                }-*/;

                @Override
                public boolean isDisplayStringHTML() {
                    return true;
                }
            }, createTextBoxBase(), new DefaultSuggestionDisplayString()) {
                @Override
                public void hideSuggestions() { // in theory should be in SuggestOracle, but now it's readonly
                    // canceling query
//                    assert isThisCellEditor(); // can be broken when for example tab is changed, it sets display to none before blur occurs
                    if (isLoading())
                        editManager.getAsyncValues(null, new AsyncCallback<Pair<ArrayList<GAsync>, Boolean>>() {
                            @Override
                            public void onFailure(Throwable caught) {
                            }

                            @Override
                            public void onSuccess(Pair<ArrayList<GAsync>, Boolean> result) {
                                // assert CANCELED returned
                            }
                        });

                    super.hideSuggestions();
                }
            };
            return suggestBox.getElement();
        } else {
            return createTextInputElement();
        }
    }

    protected InputElement getInputElement(Element parent) {
        if(property.autoSize)
            parent = parent.getFirstChildElement();
        return parent.getFirstChild().cast();
    }

    private String getCurrentText(Element parent) {
        return getInputElement(parent).getValue();
    }

    protected Object tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        if(inputText == null || inputText.isEmpty())
            return null;

        if(this instanceof FormatCellEditor) {
            GFormatType formatType = ((FormatCellEditor) this).getFormatType();
            return formatType.parseString(inputText, property.pattern);
        }
        return inputText;
    }

    protected String tryFormatInputText(Object value) {
        if(value == null)
            return "";

        if(this instanceof FormatCellEditor) {
            GFormatType formatType = ((FormatCellEditor) this).getFormatType();
            return formatType.formatString(value, property.pattern);
        }
        return value.toString();
    }

    private String escapeComma(String value, boolean ignoreEscape) {
        return escapeComma && !ignoreEscape ? value.replace(",", "\\,") : value;
    }

    private class CustomSuggestBox extends SuggestBox {
        private DefaultSuggestionDisplayString display;
        private List<String> latestSuggestions = new ArrayList<>();

        public CustomSuggestBox(SuggestOracle oracle, ValueBoxBase<String> valueBox, DefaultSuggestionDisplayString display) {
            super(oracle, valueBox, display, completionType.isAnyStrict());
            this.display = display;
            onAttach();
            getElement().removeClassName("gwt-SuggestBox");
        }

        public void setLatestSuggestions(List<String> latestSuggestions) {
            this.latestSuggestions = latestSuggestions;
        }

        public boolean isValidValue(String value) {
            return value.isEmpty() || latestSuggestions.contains(value);
        }

        public void hideSuggestions() {
            display.hideSuggestions();
        }

        protected boolean isLoading() {
            return display.isLoading;
        }
        public void updateDecoration(boolean showNoResult, boolean showEmptyLabel, boolean isLoading) {
            display.updateDecoration(showNoResult, showEmptyLabel, isLoading);
        }

        public Element getPopupElement() {
            return display.getPopupElement();
        }
    }

    private class DefaultSuggestionDisplayString extends SuggestBox.DefaultSuggestionDisplay {
        private Label noResultsLabel;
        private Label emptyLabel; //for loading
        private GToolbarButton refreshButton;
        private boolean refreshButtonPressed;

        public DefaultSuggestionDisplayString() {
            setSuggestionListHiddenWhenEmpty(false);
        }

        public String getReplacementString() {
            SuggestOracle.Suggestion selection = getCurrentSelection();
            return selection != null ? escapeComma(selection.getReplacementString(), false) : null;
        }

        @Override
        protected Widget decorateSuggestionList(Widget suggestionList) {
            VerticalPanel panel = new VerticalPanel();
            panel.add(suggestionList);

            noResultsLabel = new Label(messages.noResults());
            noResultsLabel.getElement().addClassName("item"); //to be like suggestion item
            noResultsLabel.getElement().addClassName("noResultsLabel");
            panel.add(noResultsLabel);

            emptyLabel = new Label();
            emptyLabel.getElement().addClassName("item"); //to be like suggestion item
            panel.add(emptyLabel);

            HorizontalPanel bottomPanel = new HorizontalPanel();
            bottomPanel.setWidth("100%");
            bottomPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
            bottomPanel.getElement().addClassName("suggestPopupBottomPanel");
            // block mouse down events to prevent focus issues
            bottomPanel.addDomHandler(GwtClientUtils::stopPropagation, MouseDownEvent.getType());
            panel.add(bottomPanel);

            HorizontalPanel buttonsPanel = new HorizontalPanel();

            buttonsPanel.add(refreshButton = new SuggestPopupButton(REFRESH_IMAGE_PATH) {
                @Override
                public ClickHandler getClickHandler() {
                    return event -> {
                        refreshButtonPressed = true;
                        suggestBox.showSuggestionList();
                    };
                }
            });

            for(int i = 0; i < actions.length; i++) {
                int index = i;
                SuggestPopupButton actionButton = new SuggestPopupButton(actions[index].action + ".png") {
                    @Override
                    public ClickHandler getClickHandler() {
                        return event -> validateAndCommit(suggestBox.getElement(), index, true, CommitReason.OTHER);
                    }
                };
                buttonsPanel.add(actionButton);

                String tooltip = property.getQuickActionTooltipText(actions[index].keyStroke);
                if(tooltip != null) {
                    TooltipManager.registerWidget(actionButton, new TooltipManager.TooltipHelper() {
                        public String getTooltip() {
                            return tooltip;
                        }

                        public boolean stillShowTooltip() {
                            return actionButton.isAttached() && actionButton.isVisible();
                        }

                        public String getPath() {
                            return null;
                        }

                        public String getCreationPath() {
                            return null;
                        }
                    });
                }
            }

            bottomPanel.add(buttonsPanel);

            return panel;
        }

        @Override
        protected void showSuggestions(SuggestBox suggestBox, Collection<? extends SuggestOracle.Suggestion> suggestions, boolean isDisplayStringHTML, boolean isAutoSelectEnabled, SuggestBox.SuggestionCallback callback) {
            super.showSuggestions(suggestBox, suggestions, isDisplayStringHTML, isAutoSelectEnabled, suggestion -> {
                callback.onSuggestionSelected(suggestion);
                validateAndCommit(suggestBox.getElement().getParentElement(),  true);
            });
        }

        public boolean isLoading;
        public void updateDecoration(boolean showNoResult, boolean showEmptyLabel, boolean isLoading) {
            noResultsLabel.setVisible(showNoResult);
            emptyLabel.setVisible(showEmptyLabel);
            if (this.isLoading != isLoading) {
                refreshButton.setModuleImagePath(isLoading ? LOADING_IMAGE_PATH : REFRESH_IMAGE_PATH);
                this.isLoading = isLoading;
            }
        }

        @Override
        protected void moveSelectionDown() {
            super.moveSelectionDown();
            if(!completionType.isAnyStrict()) {
                suggestBox.setValue(getReplacementString());
            }
        }

        @Override
        protected void moveSelectionUp() {
            super.moveSelectionUp();
            if(!completionType.isAnyStrict()) {
                suggestBox.setValue(getReplacementString());
            }
        }

        public boolean isRefreshButtonPressed() {
            return refreshButtonPressed;
        }

        public void setRefreshButtonPressed(boolean refreshButtonPressed) {
            this.refreshButtonPressed = refreshButtonPressed;
        }

        public Element getPopupElement() {
            return getPopupPanel().getElement();
        }
    }

    private abstract static class SuggestPopupButton extends GToolbarButton {
        public SuggestPopupButton(String imagePath) {
            super(imagePath);
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
