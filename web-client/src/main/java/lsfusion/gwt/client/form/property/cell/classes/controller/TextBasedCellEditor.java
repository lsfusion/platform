package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.impl.TextBoxImpl;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.CopyPasteUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.controller.suggest.SuggestBox;
import lsfusion.gwt.client.form.property.cell.classes.controller.suggest.TextBox;
import lsfusion.gwt.client.form.property.cell.classes.view.TextBasedCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.ReplaceCellEditor;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static com.google.gwt.dom.client.BrowserEvents.BLUR;
import static com.google.gwt.dom.client.BrowserEvents.KEYDOWN;
import static lsfusion.gwt.client.base.GwtClientUtils.nvl;

public abstract class TextBasedCellEditor implements ReplaceCellEditor {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private static TextBoxImpl textBoxImpl = GWT.create(TextBoxImpl.class);

    protected final GPropertyDraw property;
    protected final EditManager editManager;
    protected final String inputElementTagName;

    boolean hasList;
    boolean strict;
    String[] actions;
    CustomSuggestBox suggestBox = null;

    public TextBasedCellEditor(EditManager editManager, GPropertyDraw property) {
        this(editManager, property, null);
    }

    public TextBasedCellEditor(EditManager editManager, GPropertyDraw property, GInputList inputList) {
        this(editManager, property, "input", inputList);
    }

    public TextBasedCellEditor(EditManager editManager, GPropertyDraw property, String inputElementTagName, GInputList inputList) {
        this.inputElementTagName = inputElementTagName;
        this.editManager = editManager;
        this.property = property;
        this.hasList = inputList != null;
        this.strict = inputList != null && inputList.strict;
        this.actions = inputList != null ? inputList.actions : null;
    }

    @Override
    public void startEditing(Event event, Element parent, Object oldValue) {
        String value = tryFormatInputText(oldValue);
        if(hasList) {
            suggestBox.refreshSuggestionList(); //show suggestions for empty value
            suggestBox.setValue(value);
        }
        InputElement inputElement = getInputElement(parent);
        boolean selectAll = true;
        if (GKeyStroke.isCharDeleteKeyEvent(event)) {
            value = "";
            selectAll = false;
        } else if (GKeyStroke.isCharAddKeyEvent(event)) {
            String input = String.valueOf((char) event.getCharCode());
            value = checkInputValidity(parent, input) ? input : "";
            selectAll = false;
        }
        //we need this order (focus before setValue) for single click editing IntegralCellEditor (type=number)
        inputElement.focus();
        setValue(inputElement, value);

        if (selectAll) {
            inputElement.select();
        }
    }

    private native void setValue(Element element, Object value) /*-{
        element.value = value;
    }-*/;

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
        } else if (KEYDOWN.equals(type)) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyCodes.KEY_ENTER) {
                enterPressed(handler, parent);
            } else if (keyCode == KeyCodes.KEY_ESCAPE) {
                escapePressed(handler, parent);
            }
//                else
//                if (keyCode == KeyCodes.KEY_DOWN) {
//                    handler.consume();
//                    arrowPressed(event, parent, true);
//                } else if (keyCode == KeyCodes.KEY_UP) {
//                    handler.consume();
//                    arrowPressed(event, parent, false);
//                }
        } else if (BLUR.equals(type)) {
            // Cancel the change. Ensure that we are blurring the input element and
            // not the parent element itself.
            EventTarget eventTarget = event.getEventTarget();
            if (Element.is(eventTarget)) {
                Element target = Element.as(eventTarget);
                if (inputElementTagName.equals(target.getTagName().toLowerCase())) {
                    validateAndCommit(parent, true, true);
                }
            }
        }
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

    protected boolean checkEnterEvent(Event event) {
        return GKeyStroke.isPlainKeyEvent(event);
    }
    protected void enterPressed(EventHandler handler, Element parent) {
        if(checkEnterEvent(handler.event)) {
            handler.consume();
            validateAndCommit(parent, false, false);
        }
    }
    protected void escapePressed(EventHandler handler, Element parent) {
        if(hasList) {
            suggestBox.display.hideSuggestions();
        }
        if(GKeyStroke.isPlainKeyEvent(handler.event)) {
            handler.consume();
            editManager.cancelEditing();
        }
    }

    protected Element setupInputElement(Element cellParent, RenderContext renderContext, Pair<Integer, Integer> renderedSize){
        Element inputElement = createInputElement();
        // without setting boxSized class textarea and input behaviour is pretty odd when text is very large or inside td (position of textarea / input is really unpredictable)
        inputElement.addClassName("boxSized");

        Style.TextAlign textAlign = property.getTextAlignStyle();
        if(textAlign != null)
            inputElement.getStyle().setTextAlign(textAlign);

        inputElement.getStyle().setHeight(100, Style.Unit.PCT);
        inputElement.getStyle().setWidth(100, Style.Unit.PCT); // input doesn't respect justify-content, stretch, plus we want to include paddings in input (to avoid having "selection border")

        TextBasedCellRenderer.render(property, inputElement, renderContext, isMultiLine(), false);

        if(property.autoSize) { // we have to set sizes that were rendered, since input elements have really unpredicatble sizes
            cellParent = GwtClientUtils.wrapDiv(cellParent); // wrapping element since otherwise it's not clear how to restore height (sometimes element has it set)
            cellParent.getStyle().setHeight(renderedSize.second, Style.Unit.PX);
            cellParent.getStyle().setWidth(renderedSize.first, Style.Unit.PX);
        }

        return inputElement;
    }

    @Override
    public void render(Element cellParent, RenderContext renderContext, Pair<Integer, Integer> renderedSize) {
        cellParent.appendChild(setupInputElement(cellParent, renderContext, renderedSize));
    }

    @Override
    public void clearRender(Element cellParent, RenderContext renderContext) {
        GwtClientUtils.removeAllChildren(cellParent);
    }

    protected boolean isMultiLine() {
        return false;
    }

    public void commitEditing(Element parent) {
        validateAndCommit(parent, true, false);
    }

    public void validateAndCommit(Element parent, boolean cancelIfInvalid, boolean blurred) {
        validateAndCommit(parent, null, cancelIfInvalid, blurred);
    }

    public void validateAndCommit(Element parent, Integer contextAction, boolean cancelIfInvalid, boolean blurred) {
        if(contextAction == null && hasList && strict && !suggestBox.isValidText()) {
            if (cancelIfInvalid) {
                editManager.cancelEditing();
            }
            return;
        }

        String stringValue = suggestBox != null ? suggestBox.getValue() : getCurrentText(parent);
        try {
            suggestBox.display.hideSuggestions();
            editManager.commitEditing(new GUserInputResult(tryParseInputText(stringValue, true), contextAction), blurred);
        } catch (ParseException ignore) {
            if (cancelIfInvalid) {
                editManager.cancelEditing();
            }
        }
    }

    private String prevQuery = null;

    protected Element createTextInputElement() {
        return Document.get().createTextInputElement();
    }
    protected ValueBoxBase<String> createTextBoxBase() {
        return new TextBox();
    }

    public Element createInputElement() {
        if(hasList) {
            suggestBox = new CustomSuggestBox(new SuggestOracle() {

                @Override
                public void requestDefaultSuggestions(Request request, Callback callback) {
                    requestAsyncSuggestions(request, callback);
                }

                @Override
                public void requestSuggestions(Request request, Callback callback) {
                    requestAsyncSuggestions(request, callback);
                }

                private void requestAsyncSuggestions(Request request, Callback callback) {
                    String query = nvl(request.getQuery(), "");
                    prevQuery = query;
                    editManager.getAsyncValues(query, new AsyncCallback<Pair<ArrayList<String>, Boolean>>() {
                        @Override
                        public void onFailure(Throwable caught) {
                        }

                        @Override
                        public void onSuccess(Pair<ArrayList<String>, Boolean> result) {
                            suggestBox.setLatestSuggestions(result.first);
                            List<Suggestion> suggestionList = new ArrayList<>();
                            for (String suggestion : result.first) {
                                suggestionList.add(new Suggestion() {
                                    @Override
                                    public String getDisplayString() {
                                        int start = suggestion.toLowerCase().indexOf(query.toLowerCase());
                                        int end = start + query.length();
                                        return suggestion.substring(0, start) + "<strong>" + suggestion.substring(start, end) + "</strong>" + suggestion.substring(end);
                                    }

                                    @Override
                                    public String getReplacementString() {
                                        return suggestion;
                                    }
                                });
                            }
                            suggestBox.updateDecoration(suggestionList.isEmpty(), result.second);
                            callback.onSuggestionsReady(request, new Response(suggestionList));
                        }
                    });

                }

                @Override
                public boolean isDisplayStringHTML() {
                    return true;
                }
            }, createTextBoxBase(), new DefaultSuggestionDisplayString());
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

    protected abstract Object tryParseInputText(String inputText, boolean onCommit) throws ParseException;

    protected String tryFormatInputText(Object value) {
        return value == null ? "" : value.toString();
    }

    private class CustomSuggestBox extends SuggestBox {
        public DefaultSuggestionDisplayString display;
        private List<String> latestSuggestions = new ArrayList<>();

        public CustomSuggestBox(SuggestOracle oracle, ValueBoxBase<String> valueBox, DefaultSuggestionDisplayString display) {
            super(oracle, valueBox, display);
            this.display = display;
            onAttach();
            getElement().removeClassName("gwt-SuggestBox");
            setAutoSelectEnabled(strict);

            refreshSuggestionList();
        }

        public void setLatestSuggestions(List<String> latestSuggestions) {
            this.latestSuggestions = latestSuggestions;
        }

        public boolean isValidText() {
            return latestSuggestions.contains(getText());
        }

        public void updateDecoration(boolean showNoResult, boolean showRefresh) {
            display.updateDecoration(showNoResult, showRefresh);
        }
    }

    private class DefaultSuggestionDisplayString extends SuggestBox.DefaultSuggestionDisplay {
        private Label noResultsLabel;
        private PushButton refreshButton;

        public DefaultSuggestionDisplayString() {
            setSuggestionListHiddenWhenEmpty(false);
        }

        public String getReplacementString() {
            SuggestOracle.Suggestion selection = getCurrentSelection();
            return selection != null ? selection.getReplacementString() : null;
        }

        @Override
        protected Widget decorateSuggestionList(Widget suggestionList) {
            VerticalPanel panel = new VerticalPanel();
            panel.add(suggestionList);
            panel.add(noResultsLabel = new Label(messages.nothingFound()));

            HorizontalPanel bottomPanel = new HorizontalPanel();
            bottomPanel.setWidth("100%");
            bottomPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
            panel.add(bottomPanel);

            HorizontalPanel buttonsPanel = new HorizontalPanel();
            for(int i = 0; i < actions.length; i++) {
                int index = i;
                GwtClientUtils.setThemeImage(actions[index] + ".png", (image -> buttonsPanel.add(new PushButton(new Image(image)) {
                    @Override
                    protected void onClickStart() {
                        suggestBox.display.hideSuggestions();
                        validateAndCommit(getElement(), index, true, false);
                    }
                })));
            }
            GwtClientUtils.setThemeImage("refresh.png", (image -> buttonsPanel.add(refreshButton = new PushButton(new Image(image)) {
                @Override
                protected void onClickStart() {
                    suggestBox.display.hideSuggestions();
                    suggestBox.forceRefreshSuggestions(prevQuery);
                }
            })));
            bottomPanel.add(buttonsPanel);

            return panel;
        }

        public void updateDecoration(boolean showNoResult, boolean showRefresh) {
            noResultsLabel.setVisible(showNoResult);
            refreshButton.setVisible(showRefresh);
        }

        @Override
        protected void moveSelectionDown() {
            super.moveSelectionDown();
            if(!strict) {
                suggestBox.setValue(getReplacementString());
            }
        }

        @Override
        protected void moveSelectionUp() {
            super.moveSelectionUp();
            if(!strict) {
                suggestBox.setValue(getReplacementString());
            }
        }
    }
}
