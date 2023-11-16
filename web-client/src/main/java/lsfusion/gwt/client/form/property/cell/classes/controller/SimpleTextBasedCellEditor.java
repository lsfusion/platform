package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.impl.TextBoxImpl;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.view.CopyPasteUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.popup.PopupMenuItemValue;
import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.classes.controller.suggest.GCompletionType;
import lsfusion.gwt.client.form.property.cell.classes.controller.suggest.SuggestBox;
import lsfusion.gwt.client.form.property.cell.classes.view.SimpleTextBasedCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.view.MainFrame;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static com.google.gwt.user.client.Event.ONPASTE;
import static lsfusion.gwt.client.base.GwtClientUtils.nvl;
import static lsfusion.gwt.client.form.filter.user.GCompare.CONTAINS;

// now it's a sort of mix of RequestKeepValueCellEditor and RequestReplaceValueCellEditor (depending on isInputAsRenderElement)
public abstract class SimpleTextBasedCellEditor extends RequestReplaceValueCellEditor {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private static final TextBoxImpl textBoxImpl = GWT.create(TextBoxImpl.class);

    protected final GPropertyDraw property;
    protected final EditContext editContext;

    private boolean hasList;
    protected GCompletionType completionType;
    private GInputListAction[] actions;
    protected GCompare compare;
    protected SuggestBox suggestBox = null;

    public SimpleTextBasedCellEditor(EditManager editManager, GPropertyDraw property) {
        this(editManager, property, null);
    }
    
    public SimpleTextBasedCellEditor(EditManager editManager, GPropertyDraw property, GInputList inputList) {
        this(editManager, property, inputList, null);
    }

    public SimpleTextBasedCellEditor(EditManager editManager, GPropertyDraw property, GInputList inputList, EditContext editContext) {
        super(editManager);
        this.property = property;
        this.editContext = editContext;

        if(inputList != null) {
            this.hasList = true;
            this.completionType = inputList.completionType;
            this.actions = inputList.actions;
            this.compare = inputList.compare;
        }
    }

    private boolean isRenderInputElement(Element cellParent) {
        return getRenderInputElement(cellParent) != null;
    }
    private InputElement getRenderInputElement(Element cellParent) {
        return SimpleTextBasedCellRenderer.getSimpleInputElement(cellParent);
    }

    @Override
    public boolean needReplace(Element cellParent, RenderContext renderContext) {
        return !isRenderInputElement(cellParent);
    }

    protected InputElement inputElement;
    private String oldStringValue;

    protected void onInputReady(Element parent, PValue oldValue) {
    }

    private boolean started;

    @Override
    public void start(EventHandler handler, Element parent, PValue oldValue) {

        if(GMouseStroke.isChangeEvent(handler.event)) {
            Integer dialogInputActionIndex = property.getDialogInputActionIndex(actions);
            if (dialogInputActionIndex != null) {
                commitFinish(parent, oldValue, dialogInputActionIndex, CommitReason.FORCED);
                return;
            }
        }
        started = true;

        boolean allSuggestions = true;
        if(inputElement == null) {
            inputElement = getRenderInputElement(parent);

            //getInputValue() must be before onInputReady as it affects daterangepicker behaviour. onInputReady trigger the creation dateTimePicker, if the date was null
            //then getInputElementValue saves today's date instead of null, and when you press clear, it calls stop, which takes the oldStringValue
            // and returns it back and we get today's date instead of null
            oldStringValue = getInputValue();

            onInputReady(parent, oldValue);

            //need for start editing by CHANGEKEY
            FocusUtils.focus(inputElement, FocusUtils.Reason.OTHER);

            parent.addClassName("property-hide-toolbar");

            handler.consume(true, false);
        } else {
            onInputReady(parent, oldValue);

            //we need this order (focus before setValue) for single click editing IntegralCellEditor (type=number)
            FocusUtils.focus(inputElement, FocusUtils.Reason.OTHER);
            boolean selectAll = !GKeyStroke.isChangeAppendKeyEvent(handler.event);

            String value = checkStartEvent(handler.event, parent, this::checkInputValidity);
            if(value != null) {
                allSuggestions = false;
                selectAll = false;
            } else
                value = (property.clearText ? "" : tryFormatInputText(oldValue));

            setInputValue(parent, value);

            if (selectAll)
                inputElement.select();
        }

        if (hasList) {
            suggestBox = createSuggestBox(inputElement, parent);
            
            // don't update suggestions if editing started with char key event. as editor text is empty on init - request is being sent twice
            // wait for editor key listener to catch the event
            // UPD: for now is reproducible only in panel properties
            if (!GKeyStroke.isCharAddKeyEvent(handler.event)) {
                suggestBox.showSuggestionList(allSuggestions);
            }
        }
    }

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        if (started) {
            if (hasList) {
                suggestBox.hideSuggestions();
                suggestBox = null;
            }

            if (isRenderInputElement(parent)) {
                parent.removeClassName("property-hide-toolbar");

                setInputValue(parent, oldStringValue);
            }

            inputElement = null;
            started = false;
        }
    }

    protected void setInputValue(Element parent, String value) {
        setInputValue(inputElement, value);
    }
    public static void setInputValue(InputElement element, String value) {
        element.setValue(value);
    }
    private String getInputValue() {
        return getInputValue(inputElement);
    }
    public static String getInputValue(InputElement element) {
        return element.getValue();
    }

    boolean plainPaste = false;
    private void addPasteListener(InputElement inputElement) {
        GwtClientUtils.setEventListener(inputElement, ONPASTE, event -> {
            if (editContext != null && event.getTypeInt() == ONPASTE && !plainPaste) {
                String cbText = CopyPasteUtils.getEventClipboardData(event);
                String modifiedPastedText = (String) editContext.modifyPastedString(cbText);
                if (modifiedPastedText != null && !modifiedPastedText.equals(cbText)) { // to paste via default mechanism otherwise
                    pasteClipboardText(event, modifiedPastedText);
                }
            }
            plainPaste = false;
        });
    }

    protected native boolean pasteClipboardText(Event event, String pastedText)/*-{
        var eventTarget = event.target;
        var cursorPosStart = eventTarget.selectionStart;
        var cursorPosEnd = eventTarget.selectionEnd;
        var v = eventTarget.value;
        var mergedText = v.substring(0, cursorPosStart) + pastedText + v.substring(cursorPosEnd, v.length);
        if (this.@SimpleTextBasedCellEditor::isStringValid(*)(mergedText)) {
            event.stopPropagation();
            event.preventDefault();
            eventTarget.value = mergedText;
            eventTarget.selectionStart = eventTarget.selectionEnd = cursorPosStart + pastedText.length;
            return true;
        }
        return false;
    }-*/;

    @Override
    public void onBrowserEvent(Element parent, EventHandler handler) {
        if(hasList && isThisCellEditor()) {
            suggestBox.onBrowserEvent(handler);
            if(handler.consumed)
                return;
        }

        Event event = handler.event;
        if (GKeyStroke.isInputKeyEvent(event, this::isMultiLine) || GMouseStroke.isEvent(event) || GMouseStroke.isContextMenuEvent(event)) {
            boolean isCorrect = true;

            String stringToAdd = null;
            if(GKeyStroke.isCharAddKeyEvent(event))
                stringToAdd = String.valueOf((char) event.getCharCode());
            else if(GKeyStroke.isPasteFromClipboardEvent(event))
                stringToAdd = CopyPasteUtils.getEventClipboardData(event);
            if(stringToAdd != null && !checkInputValidity(parent, stringToAdd))
                isCorrect = false; // this thing is needed to disable inputting incorrect symbols

            handler.consume(isCorrect, false);
        } else if (GKeyStroke.isPlainPasteEvent(event)) {
            plainPaste = true;
        } else {
            Integer inputActionIndex = property.getInputActionIndex(event, true);
            if(inputActionIndex != null) {
                validateAndCommit(parent, inputActionIndex, true, CommitReason.FORCED);
                return;
            }
        }

        super.onBrowserEvent(parent, handler);
    }

    protected boolean checkInputValidity(Element parent, String stringToAdd) {
        int cursorPosition = textBoxImpl.getCursorPos(inputElement);
        int selectionLength = textBoxImpl.getSelectionLength(inputElement);
        String currentValue = getInputValue();
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

    public static InputElement renderInputElement(Element cellParent, GPropertyDraw property, boolean multiLine, RenderContext renderContext, boolean removeAllPMB) {
        RendererType rendererType = renderContext.getRendererType();
        InputElement inputElement = SimpleTextBasedCellRenderer.createInputElement(property, rendererType);

        CellRenderer.renderTextAlignment(property, inputElement, true, rendererType);

        SimpleTextBasedCellRenderer.render(property, inputElement, renderContext, multiLine);

        if(removeAllPMB)
            inputElement.addClassName("remove-all-pmb");
        cellParent.appendChild(inputElement);

        // the problem of the height:100% is that: when it is set for the input element, line-height is ignored (and line-height is used for example in bootstrap, so when the data is drawn with the regular div, but input with the input element, they have different sizes, which is odd)
        // but flex works fine for the input element, but we cannot set display flex for the table cell element
        // so we'll use 100% for table cells (it's not that big deal for table cells, because usually there are a lot of cells without input in a row, and they will respect line-height)
        if(GwtClientUtils.isTDorTH(cellParent))
            GwtClientUtils.setupPercentParent(inputElement);
        else
            GwtClientUtils.setupFlexParent(inputElement);

        return inputElement;
    }

    public static void clearInputElement(Element cellParent) {
        if(!GwtClientUtils.isTDorTH(cellParent))
            GwtClientUtils.clearFlexParentElement(cellParent);
    }

    @Override
    public void render(Element cellParent, RenderContext renderContext, PValue oldValue, Integer renderedWidth, Integer renderedHeight) {
        assert !isRenderInputElement(cellParent);

        SimpleTextBasedCellRenderer.setPadding(cellParent); // paddings should be for the element itself (and in general do not change), because the size is set for this element and reducing paddings will lead to changing element size

        boolean multiLine = isMultiLine();

        inputElement = renderInputElement(cellParent, property, multiLine, renderContext, true);
        addPasteListener(inputElement);

        // input doesn't respect justify-content, stretch, plus we want to include paddings in input (to avoid having "selection border")
        // we have to set sizes that were rendered, since input elements have really unpredicatble content sizes
        if(renderedWidth != null)
            inputElement.getStyle().setWidth(renderedWidth, Style.Unit.PX);
        if(renderedHeight != null) {
            inputElement.getStyle().setHeight(renderedHeight, Style.Unit.PX);
            if(multiLine)
                // https://stackoverflow.com/questions/7144843/extra-space-under-textarea-differs-along-browsers
                inputElement.getStyle().setVerticalAlign(Style.VerticalAlign.TOP);
        }
    }

    protected boolean isMultiLine() {
        return false;
    }

    @Override
    public void clearRender(Element cellParent, RenderContext renderContext, boolean cancel) {
        assert !isRenderInputElement(cellParent);

        SimpleTextBasedCellRenderer.clearPadding(cellParent);

//        TextBasedCellRenderer.clearBasedTextFonts(property, element.getStyle(), renderContext);

//        TextBasedCellRenderer.clearRender(property, element.getStyle(), renderContext);

        clearInputElement(cellParent);

        super.clearRender(cellParent, renderContext, cancel);
    }

    public PValue getCommitValue(Element parent, Integer contextAction) throws InvalidEditException {
        if (hasList) {
            PopupMenuItemValue selectedItem = suggestBox.selectedItem;
            if (selectedItem != null)
                return selectedItem.getPValue();

            if(contextAction == null && completionType.isOnlyCommitSelection())
                throw new InvalidEditException();
        }

        String stringValue = getInputValue();
        if (hasList && contextAction == null && completionType.isCheckCommitInputInList() && !suggestBox.isValidValue(stringValue)) {
            throw new InvalidEditException();
        }
        try {
            return tryParseInputText(stringValue, true); //if button pressed, input element is button
        } catch (ParseException e) {
            throw new InvalidEditException();
        }
    }

    protected boolean isThisCellEditor() {
        assert hasList;
        return suggestBox != null;
    }

    private SuggestBox createSuggestBox(InputElement element, Element parent) {
        return new SuggestBox(new SuggestBox.SuggestOracle() {
            private Timer delayTimer;
            private SuggestBox.Request currentRequest; // current pending request
            private SuggestBox.Callback currentCallback;

            private String prevSucceededEmptyQuery;

            @Override
            public void requestSuggestions(SuggestBox.Request request, SuggestBox.Callback callback) {
                currentRequest = request;
                currentCallback = callback;

                if(delayTimer == null)
                    updateAsyncValues();
            }

            private void updateAsyncValues() {
                final SuggestBox.Request request = currentRequest;
                currentRequest = null;
                final SuggestBox.Callback callback = currentCallback;
                currentCallback = null;

                boolean emptyQuery = request.query == null;
                String query = nvl(request.query, "");
//                if(prevSucceededEmptyQuery != null && query.startsWith(prevSucceededEmptyQuery))
//                    return;

                suggestBox.updateDecoration(true);

                //disable selection while loading (delayed)
                Result<Boolean> resultReceived = new Result<>(false);

                // add timer to avoid blinking:
                // popup, when empty popup is followed by non-empty one
                // active row, when there are results updated
                new Timer() {
                    @Override
                    public void run() {
                        if(isThisCellEditor()) {
                            if (emptyQuery && !suggestBox.isSuggestionListShowing()) {
                                callback.onSuggestionsReady(request, new SuggestBox.Response(new ArrayList<>(), false, true));
                                setMinWidth(element, suggestBox, false);
                            }
                            if (!resultReceived.result)
                                suggestBox.clearSelectedItem();
                        }
                    }
                }.schedule(100);

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

                editManager.getAsyncValues(query, new AsyncCallback<GFormController.GAsyncResult>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        if (isThisCellEditor()) //  && suggestBox.isSuggestionListShowing()
                            cancelAndFlushDelayed(execTimer);
                        resultReceived.set(true);
                    }

                    @Override
                    public void onSuccess(GFormController.GAsyncResult result) {
                        if (isThisCellEditor()) { //  && suggestBox.isSuggestionListShowing() in desktop this check leads to "losing" result, since suggest box can be not shown yet (!), however maybe in web-client it's needed for some reason (but there can be the risk of losing result)
                            suggestBox.setAutoSelectEnabled(completionType.isAutoSelection() && !(isFilterList() && query.contains(MainFrame.matchSearchSeparator)) && !emptyQuery);

                            boolean succeededEmpty = false;
                            if(result.asyncs != null) {
                                List<String> rawSuggestions = new ArrayList<>();
                                ArrayList<PopupMenuItemValue> suggestionList = new ArrayList<>();
                                for (GAsync suggestion : result.asyncs) {
                                    PValue rawValue = PValue.escapeSeparator(suggestion.getRawValue(), compare);
                                    String rawString = PValue.getStringValue(rawValue);
                                    rawSuggestions.add(rawString);
                                    suggestionList.add(new PopupMenuItemValue() {
                                        @Override
                                        public String getDisplayString() {
                                            return PValue.getStringValue(suggestion.getDisplayValue());
                                        }

                                        @Override
                                        public PValue getPValue() {
                                            return rawValue;
                                        }

                                        @Override
                                        public String getReplacementString() {
                                            return rawString;
                                        }
                                    });
                                }
                                suggestBox.setLatestSuggestions(rawSuggestions);
                                callback.onSuggestionsReady(request, new SuggestBox.Response(suggestionList, result.needMoreSymbols, false));
                                setMinWidth(element, suggestBox, true);

                                succeededEmpty = suggestionList.isEmpty();
                            }

                            suggestBox.updateDecoration(result.moreRequests);

                            if(!result.moreRequests && !result.needMoreSymbols) {
                                if (succeededEmpty)
                                    prevSucceededEmptyQuery = query;
                                else
                                    prevSucceededEmptyQuery = null;
                            }

                            cancelAndFlushDelayed(execTimer);
                            resultReceived.set(true);
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

                if(currentRequest != null && suggestBox != null) // there was pending request
                    updateAsyncValues();
            }

            private void setMinWidth(InputElement inputElement, SuggestBox suggestBox, boolean offsets) {
                setMinWidth(suggestBox.getPopupElement(), inputElement.getOffsetWidth() - (offsets ? 8 : 0)); //8 = offsets
            }

            private native void setMinWidth(Element element, int minWidth) /*-{
                Array.prototype.forEach.call(element.getElementsByClassName("dropdown-item"), function(item) {
                    item.style.minWidth = minWidth + "px";
                });
            }-*/;
        }, element, parent, completionType, (suggestion, commitReason) -> validateAndCommit(parent, true, commitReason)) {

            @Override
            protected Widget createButtonsPanel(Element parent) {
//                FlexPanel bottomPanel = new FlexPanel(true);

                FlexPanel buttonsPanel = new FlexPanel();
                buttonsPanel.getElement().addClassName("dropdown-menu-button-panel");

                // block mouse down events to prevent focus issues
                buttonsPanel.addDomHandler(GwtClientUtils::stopPropagation, MouseDownEvent.getType());

                buttonsPanel.add(refreshButton = new SuggestPopupButton(StaticImage.REFRESH_IMAGE_PATH) {
                    @Override
                    public ClickHandler getClickHandler() {
                        return event -> {
                            refreshButtonPressed = true;
                            suggestBox.refreshSuggestionList();
                        };
                    }
                });

                for (GInputListAction action : actions) {
                    SuggestPopupButton actionButton = new SuggestPopupButton(action.action) {
                        @Override
                        public ClickHandler getClickHandler() {
                            return event -> validateAndCommit(parent, action.index, true, CommitReason.FORCED);
                        }
                    };
                    buttonsPanel.add(actionButton);

                    String tooltip = property.getQuickActionTooltipText(action.keyStroke);
                    if (tooltip != null) {
                        TooltipManager.initTooltip(actionButton.getElement(), new TooltipManager.TooltipHelper() {
                            public String getTooltip() {
                                return tooltip;
                            }
                        });
                    }
                }

                return buttonsPanel;
            }

            protected Widget createInfoPanel(Element parent) {
                if (isFilterList()) {
                    HTML tip = new HTML(compare == CONTAINS ? messages.suggestBoxContainsTip() : messages.suggestBoxMatchTip(MainFrame.matchSearchSeparator));
                    tip.getElement().addClassName("dropdown-menu-tip");
                    tip.getElement().addClassName("text-secondary");
                    return tip;
                }
                return null;
            }

            @Override
            public void hideSuggestions() { // in theory should be in SuggestOracle, but now it's readonly
                // canceling query
//                    assert isThisCellEditor(); // can be broken when for example tab is changed, it sets display to none before blur occurs
                if (isLoading)
                    editManager.getAsyncValues(null, new AsyncCallback<GFormController.GAsyncResult>() {
                        @Override
                        public void onFailure(Throwable caught) {
                        }

                        @Override
                        public void onSuccess(GFormController.GAsyncResult result) {
                            // assert CANCELED returned
                        }
                    });

                super.hideSuggestions();
            }
        };
    }

    private boolean isFilterList() {
        return compare != null && compare.escapeSeparator();
    }

    protected PValue tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        if(inputText == null || inputText.isEmpty())
            return null;

        if(this instanceof FormatCellEditor) {
            GFormatType formatType = ((FormatCellEditor) this).getFormatType();
            return formatType.parseString(inputText, property.pattern);
        }
        return PValue.getPValue(inputText);
    }

    protected String tryFormatInputText(PValue value) {
        if(value == null)
            return "";

        if(this instanceof FormatCellEditor) {
            GFormatType formatType = ((FormatCellEditor) this).getFormatType();
            return formatType.formatString(value, property.pattern);
        }
        return PValue.getStringValue(value);
    }
}
