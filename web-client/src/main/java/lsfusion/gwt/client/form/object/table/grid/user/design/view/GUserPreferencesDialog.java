package lsfusion.gwt.client.form.object.table.grid.user.design.view;

import com.allen_sauer.gwt.dnd.client.DragHandlerAdapter;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.Callback;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.CaptionPanel;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.panel.controller.GPanelController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGridUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.PropertyListItem;
import lsfusion.gwt.client.form.object.table.grid.view.GGridTable;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.gwt.client.base.GwtClientUtils.createHorizontalStrut;
import static lsfusion.gwt.client.base.GwtClientUtils.createVerticalStrut;

public abstract class GUserPreferencesDialog extends DialogModalWindow {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private static final String CSS_USER_PREFERENCES_DUAL_LIST = "userPreferencesDualList";

    private GGridController groupController;
    private GGridTable grid;
    private GPanelController panelController;

    private FocusPanel focusPanel;

    private ColumnsDualListBox columnsDualListBox;
    private TextBox pageSizeBox;
    private TextBox headerHeightBox;
    private TextBox sizeBox;
    private CheckBox boldBox;
    private CheckBox italicBox;

    private TextBox columnCaptionBox;
    private TextBox columnPatternBox;

    public GUserPreferencesDialog(GGridTable grid, GGridController groupController, GPanelController panelController, boolean canBeSaved) {
        super(true, ModalWindowSize.LARGE);

        setCaption(messages.formGridPreferences());

        this.groupController = groupController;

        this.grid = grid;

        this.panelController = panelController;

        // columns
        columnsDualListBox = new ColumnsDualListBox() {
            @Override
            public void setColumnCaptionBoxText(String text) {
                columnCaptionBox.setText(text);
            }
            @Override
            public void setColumnPatternBoxText(String pattern) {
                columnPatternBox.setText(pattern);
            }
        };
        columnsDualListBox.getDragController().addDragHandler(new DragHandlerAdapter());
        columnsDualListBox.addStyleName(CSS_USER_PREFERENCES_DUAL_LIST);

        // column caption settings        
        columnCaptionBox = new TextBox();
        columnCaptionBox.addStyleName("userPreferencesColumnTextBox");
        columnCaptionBox.addKeyUpHandler(event -> columnsDualListBox.columnCaptionBoxTextChanged(columnCaptionBox.getText()));

        FlexPanel columnCaptionPanel = new FlexPanel();
        columnCaptionPanel.add(new Label(messages.formGridPreferencesColumnCaption() + ":"), GFlexAlignment.CENTER);
        columnCaptionPanel.add(createHorizontalStrut(2));
        columnCaptionPanel.add(columnCaptionBox);

        // column pattern settings
        columnPatternBox = new TextBox();
        columnPatternBox.addStyleName("userPreferencesColumnTextBox");
        columnPatternBox.addChangeHandler(changeEvent -> columnsDualListBox.columnPatternBoxTextChanged(columnPatternBox.getText()));

        FlexPanel columnPatternPanel = new FlexPanel();
        columnPatternPanel.add(new Label(messages.formGridPreferencesColumnPattern() + ":"), GFlexAlignment.CENTER);
        columnPatternPanel.add(createHorizontalStrut(2));
        columnPatternPanel.add(columnPatternBox);

        VerticalPanel columnSettingsPanel = new VerticalPanel();
        columnSettingsPanel.setSpacing(2);
        columnSettingsPanel.setWidth("100%");
        columnSettingsPanel.add(columnCaptionPanel);
        columnSettingsPanel.add(columnPatternPanel);

        //page size settings
        pageSizeBox = new TextBox();
        pageSizeBox.addStyleName("userPreferencesIntegralTextBox");
        FlexPanel pageSizePanel = new FlexPanel();
        pageSizePanel.add(new Label(messages.formGridPreferencesPageSize() + ":"), GFlexAlignment.CENTER);
        pageSizePanel.add(createHorizontalStrut(2));
        pageSizePanel.add(pageSizeBox);

        //header height
        headerHeightBox = new TextBox();
        headerHeightBox.addStyleName("userPreferencesIntegralTextBox");
        FlexPanel headerHeightPanel = new FlexPanel();
        headerHeightPanel.add(new Label(messages.formGridPreferencesHeaderHeight() + ":"), GFlexAlignment.CENTER);
        headerHeightPanel.add(createHorizontalStrut(2));
        headerHeightPanel.add(headerHeightBox);
        
        // font settings
        sizeBox = new TextBox();
        sizeBox.addStyleName("userPreferencesIntegralTextBox");
        boldBox = new FormCheckBox(messages.formGridPreferencesFontStyleBold());
        italicBox = new FormCheckBox(messages.formGridPreferencesFontStyleItalic());
        FlexPanel fontPanel = new FlexPanel();
        fontPanel.getElement().getStyle().setMargin(2, Style.Unit.PX);
        Label fontLabel = new Label(messages.formGridPreferencesFontSize() + ":");
        fontLabel.addStyleName("userPreferencesFontLabel");
        fontPanel.addCentered(fontLabel);
        fontPanel.add(createHorizontalStrut(2));
        fontPanel.addCentered(sizeBox);
        fontPanel.add(createHorizontalStrut(6));
        fontPanel.addCentered(boldBox);
        fontPanel.add(createHorizontalStrut(6));
        fontPanel.addCentered(italicBox);
        fontPanel.add(createHorizontalStrut(2));

        VerticalPanel gridSettingsPanel = new VerticalPanel();
        gridSettingsPanel.setSpacing(2);
        gridSettingsPanel.add(pageSizePanel);
        gridSettingsPanel.add(headerHeightPanel);
        gridSettingsPanel.add(new CaptionPanel(messages.formGridPreferencesFont(), fontPanel));

        // ok/cancel buttons
        FormButton okButton = new FormButton(messages.ok(), FormButton.ButtonStyle.PRIMARY, event -> okPressed());
        FormButton cancelButton = new FormButton(messages.cancel(), FormButton.ButtonStyle.SECONDARY, event -> hide());
        addFooterWidget(okButton);
        addFooterWidget(cancelButton);

        VerticalPanel preferencesPanel = new VerticalPanel();
        preferencesPanel.setSpacing(3);
        preferencesPanel.setSize("100%", "100%");
        preferencesPanel.add(columnsDualListBox);
        preferencesPanel.setCellHeight(columnsDualListBox, "100%");
        preferencesPanel.add(GwtClientUtils.createVerticalStrut(3));
        preferencesPanel.add(new CaptionPanel(messages.formGridPreferencesSelectedColumnSettings(), columnSettingsPanel));
        preferencesPanel.add(createVerticalStrut(3));
        preferencesPanel.add(new CaptionPanel(messages.formGridPreferencesGridSettings(), gridSettingsPanel));
        preferencesPanel.add(createVerticalStrut(5));
        if (canBeSaved) {
            FormButton saveButton = new FormButton(messages.formGridPreferencesSave(), event -> savePressed());
            saveButton.addStyleName("userPreferencesSaveResetButton");
            preferencesPanel.add(saveButton);

            FormButton resetButton = new FormButton(messages.formGridPreferencesReset(), event -> resetPressed());
            resetButton.addStyleName("userPreferencesSaveResetButton");
            preferencesPanel.add(resetButton);
        }

        focusPanel = new FocusPanel(preferencesPanel);
        focusPanel.addStyleName("noOutline");
        focusPanel.setSize("100%", "100%");
        focusPanel.addKeyDownHandler(event -> {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
                GwtClientUtils.stopPropagation(event);
                hide();
            }
        });

        ResizableComplexPanel mainContainer = new ResizableComplexPanel();
        mainContainer.setStyleName("dialog-user-preferences-container");
        mainContainer.add(focusPanel);

        setBodyWidget(mainContainer);

        refreshValues(mergeFont());
    }

    public void showDialog() {
        show();
        focusPanel.setFocus(true);
    }

    private void okPressed() {
        for (Widget label : columnsDualListBox.getVisibleWidgets()) {
            PropertyListItem property = ((PropertyLabel) label).getPropertyItem();
            grid.setColumnSettings(property.property, property.getUserCaption(true), property.getUserPattern(),
                    columnsDualListBox.getVisibleIndex(label), false);
        }

        String[] hiddenPropSids = new String[columnsDualListBox.getInvisibleWidgets().size()];
        for (int i = 0; i < columnsDualListBox.getInvisibleWidgets().size(); i++) {
            Widget label = columnsDualListBox.getInvisibleWidgets().get(i);
            PropertyListItem property = ((PropertyLabel) label).getPropertyItem();
            grid.setColumnSettings(property.property, property.getUserCaption(true), property.getUserPattern(),
                    columnsDualListBox.getVisibleCount() + i, true);
            if (property.inGrid == null || property.inGrid) {
                hiddenPropSids[i] = property.property.propertyFormName;
            }
        }

        GFont userFont = getUserFont();
        if (userFont.size <= 0) {
            userFont = userFont.deriveFont(userFont.bold, userFont.italic);
        }
        grid.setUserFont(userFont);
        grid.font = userFont;
        
        Integer userPageSize = getUserPageSize();
        grid.setUserPageSize(userPageSize);

        grid.setUserHeaderHeight(getUserHeaderHeight());
        grid.columnsChanged();

        grid.setHasUserPreferences(true);

        grid.columnsPreferencesChanged();
        
        grid.refreshUPHiddenProps(hiddenPropSids);

        hide();
    }

    private GFont getUserFont() {
        GFont initialFont = getInitialFont();
        int size;
        try {
            size = Integer.parseInt(sizeBox.getValue());
            if (size <= 0) {
                size = initialFont.size;
            }
        } catch(NumberFormatException e) {
            size = -1;
        }

        return new GFont(initialFont.family, size, boldBox.getValue(), italicBox.getValue());
    }

    private Integer getUserPageSize() {
        Integer pageSize;
        try {
            pageSize = Integer.parseInt(pageSizeBox.getValue());
        } catch(NumberFormatException e) {
            return null;
        }
        return pageSize != 0 ? pageSize : null;
    }

    private Integer getUserHeaderHeight() {
        Integer headerHeight;
        try {
            headerHeight = Integer.parseInt(headerHeightBox.getValue());
        } catch(NumberFormatException e) {
            return null;
        }
        return headerHeight >= 0 ? headerHeight : null;
    }

    private GFont getInitialFont() {
        GFont designFont = grid.getDesignFont();
        return designFont == null ? new GFont(grid.font != null ? grid.font.family : "", 0, false, false) : designFont;
    }
    
    private void resetPressed() {
        final GSaveResetConfirmDialog confirmDialog = new GSaveResetConfirmDialog(false);
        confirmDialog.show(new Callback() {
            @Override
            public void onSuccess() {
                columnCaptionBox.setText(null);
                columnPatternBox.setText(null);
                grid.resetPreferences(confirmDialog.forAll, confirmDialog.complete, createChangeCallback(false));
            }

            @Override
            public void onFailure() {
                focusPanel.setFocus(true);
            }
        });
    }

    private void savePressed() {
        final GSaveResetConfirmDialog confirmDialog = new GSaveResetConfirmDialog(true);
        confirmDialog.show(new Callback() {
            @Override
            public void onSuccess() {
                Map<GPropertyDraw, Map<Boolean, Integer>> userSortDirections = new HashMap<>();
                int i = 0;
                for (Map.Entry<Map<GPropertyDraw, GGroupObjectValue>, Boolean> entry : grid.getOrderDirections().entrySet()) {
                    HashMap<Boolean, Integer> dirs = new HashMap<>();
                    dirs.put(entry.getValue(), i);
                    userSortDirections.put(entry.getKey().keySet().iterator().next(), dirs);
                    i++;
                }
        
                for (Widget w : columnsDualListBox.getVisibleWidgets()) {
                    PropertyListItem property = ((PropertyLabel) w).getPropertyItem();
                    refreshPropertyUserPreferences(property, false, columnsDualListBox.getVisibleIndex(w), userSortDirections.get(property.property));
                }
        
                for (Widget w : columnsDualListBox.getInvisibleWidgets()) {
                    PropertyListItem property = ((PropertyLabel) w).getPropertyItem();
                    int propertyOrder = columnsDualListBox.getVisibleCount() + columnsDualListBox.getInvisibleIndex(w);
                    refreshPropertyUserPreferences(property, true, propertyOrder, userSortDirections.get(property.property));
                }
        
                GFont userFont = getUserFont();
                grid.setUserFont(userFont);
                
                Integer userPageSize = getUserPageSize();
                grid.setUserPageSize(userPageSize);
        
                grid.setUserHeaderHeight(getUserHeaderHeight());
                grid.columnsChanged();
                
                grid.saveCurrentPreferences(confirmDialog.forAll, createChangeCallback(true));
            }

            @Override
            public void onFailure() {
                focusPanel.setFocus(true);
            }
        });
    }

    private void refreshPropertyUserPreferences(PropertyListItem property, boolean hide, int propertyOrder, Map<Boolean, Integer> userSortDirections) {
        Boolean sortDirection = userSortDirections != null ? userSortDirections.keySet().iterator().next() : null;
        Integer sortIndex = userSortDirections != null ? userSortDirections.values().iterator().next() : null;
        grid.setColumnSettings(property.property, property.getUserCaption(true), property.getUserPattern(), propertyOrder, hide);
        grid.setUserSort(property.property, sortDirection != null ? sortIndex : null);
        grid.setUserAscendingSort(property.property, sortDirection);
    }

    private Boolean getPropertyState(GPropertyDraw property) {
        if (groupController.isPropertyInGrid(property)) {
            return true;
        } else if (panelController.containsProperty(property)) {
            return false;
        }
        return null;
    }

    private void refreshValues(GFont font) {
        List<GPropertyDraw> orderedVisibleProperties = grid.getOrderedVisibleProperties(groupController.getGroupObjectProperties());
        GGridUserPreferences currentPreferences = grid.getCurrentPreferences();
        columnsDualListBox.clearLists();

        for (GPropertyDraw property : orderedVisibleProperties) {
            columnsDualListBox.addVisible(new PropertyListItem(property, currentPreferences.getUserCaption(property),
                    currentPreferences.getUserPattern(property), getPropertyState(property)));
        }
        for (GPropertyDraw property : groupController.getGroupObjectProperties()) {
            if (!orderedVisibleProperties.contains(property)) {
                columnsDualListBox.addInvisible(new PropertyListItem(property, currentPreferences.getUserCaption(property),
                        currentPreferences.getUserPattern(property), getPropertyState(property)));
            }
        }

        sizeBox.setValue((font == null || font.size <= 0) ? "" : String.valueOf(font.size));
        boldBox.setValue(font != null && font.bold);
        italicBox.setValue(font != null && font.italic);

        Integer currentPageSize = currentPreferences.pageSize;
        pageSizeBox.setValue(currentPageSize == null ? "" : String.valueOf(currentPageSize));

        Integer currentHeaderHeight = currentPreferences.headerHeight;
        headerHeightBox.setValue(currentHeaderHeight == null ? "" : String.valueOf(currentHeaderHeight));
    }
    
    private GFont mergeFont() {
        GGridUserPreferences prefs = grid.getCurrentPreferences();

        GFont font = getInitialFont();
        if (prefs.hasUserPreferences()) {
            font = prefs.font;
        }
        return font;
    } 

    private AsyncCallback<ServerResponseResult> createChangeCallback(final boolean save) {
        return new AsyncCallback<ServerResponseResult>() {
            @Override
            public void onSuccess(ServerResponseResult result) {
                GFont font = mergeFont();
                refreshValues(font);
                grid.font = font;
                grid.columnsPreferencesChanged();
                grid.setUserHeaderHeight(getUserHeaderHeight());
                grid.columnsChanged();
                preferencesChanged();
                String caption = save ? messages.formGridPreferencesSaving() : messages.formGridPreferencesResetting();
                String message = save ? messages.formGridPreferencesSaveSuccess() : messages.formGridPreferencesResetSuccess();
                DialogBoxHelper.showMessageBox(false, caption, message, chosenOption -> focusPanel.setFocus(true));
            }

            @Override
            public void onFailure(Throwable caught) {
                GFont font = mergeFont();
                refreshValues(font);
                grid.font = font;
                grid.columnsPreferencesChanged();
                grid.columnsChanged();
                focusPanel.setFocus(true);
            }
        };
    }
    
    public abstract void preferencesChanged();
}
