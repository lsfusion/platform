package lsfusion.gwt.client.form.object.table.grid.user.design.view;

import com.allen_sauer.gwt.dnd.client.DragHandlerAdapter;
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

import static lsfusion.gwt.client.view.MainFrame.v5;

public abstract class GUserPreferencesDialog extends DialogModalWindow {
    private static final ClientMessages messages = ClientMessages.Instance.get();

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
        super(messages.formGridPreferences(), true, ModalWindowSize.LARGE);

        this.groupController = groupController;

        this.grid = grid;

        this.panelController = panelController;

        FlexPanel filterPanel = new FlexPanel();
        filterPanel.addCentered(createLabel(messages.formGridPreferencesFilter()));
        TextBox filterBox = createTextBox();
        filterBox.addKeyUpHandler(event -> columnsDualListBox.filterChanged(filterBox.getText()));
        filterPanel.add(filterBox);

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
        GwtClientUtils.addClassName(columnsDualListBox, "user-preferences-dual-list", "userPreferencesDualList", v5);

        // column caption settings        
        columnCaptionBox = createTextBox();
        columnCaptionBox.addKeyUpHandler(event -> columnsDualListBox.columnCaptionBoxTextChanged(columnCaptionBox.getText()));

        FlexPanel columnCaptionPanel = new FlexPanel();
        Label columnCaptionLabel = createLabel(messages.formGridPreferencesColumnCaption());
        columnCaptionPanel.addCentered(columnCaptionLabel);
        columnCaptionPanel.add(columnCaptionBox);

        // column pattern settings
        columnPatternBox = createTextBox();
        columnPatternBox.addChangeHandler(changeEvent -> columnsDualListBox.columnPatternBoxTextChanged(columnPatternBox.getText()));

        FlexPanel columnPatternPanel = new FlexPanel();
        columnPatternPanel.add(createLabel(messages.formGridPreferencesColumnPattern()), GFlexAlignment.CENTER);
        columnPatternPanel.add(columnPatternBox);

        FlexPanel columnSettingsPanel = new FlexPanel();
        columnSettingsPanel.add(columnCaptionPanel);
        columnSettingsPanel.add(columnPatternPanel);

        //page size settings
        pageSizeBox = createIntegralTextBox();
        FlexPanel pageSizePanel = new FlexPanel();
        pageSizePanel.add(createLabel(messages.formGridPreferencesPageSize()), GFlexAlignment.CENTER);
        pageSizePanel.add(pageSizeBox);

        //header height
        headerHeightBox = createIntegralTextBox();
        FlexPanel headerHeightPanel = new FlexPanel();
        headerHeightPanel.add(createLabel(messages.formGridPreferencesHeaderHeight()), GFlexAlignment.CENTER);
        headerHeightPanel.add(headerHeightBox);

        FlexPanel pageSizeHeaderHeightPanel = new FlexPanel();
        pageSizeHeaderHeightPanel.add(pageSizePanel);
        pageSizeHeaderHeightPanel.add(headerHeightPanel);

        // font settings
        sizeBox = createIntegralTextBox();
        boldBox = new FormCheckBox(messages.formGridPreferencesFontStyleBold());
        italicBox = new FormCheckBox(messages.formGridPreferencesFontStyleItalic());
        FlexPanel fontPanel = new FlexPanel();
        fontPanel.addCentered(createLabel(messages.formGridPreferencesFontSize()));
        fontPanel.addCentered(sizeBox);
        fontPanel.addCentered(boldBox);
        fontPanel.addCentered(italicBox);

        FlexPanel gridSettingsPanel = new FlexPanel(true);
        gridSettingsPanel.add(pageSizeHeaderHeightPanel);
        gridSettingsPanel.add(new CaptionPanel(messages.formGridPreferencesFont(), fontPanel));

        // ok/cancel buttons
        FormButton okButton = new FormButton(messages.ok(), FormButton.ButtonStyle.PRIMARY, event -> okPressed());
        FormButton cancelButton = new FormButton(messages.cancel(), FormButton.ButtonStyle.SECONDARY, event -> hide());
        addFooterWidget(okButton);
        addFooterWidget(cancelButton);

        FlexPanel preferencesPanel = new FlexPanel(true);
        preferencesPanel.setHeight("100%");
        preferencesPanel.add(filterPanel);
        preferencesPanel.add(columnsDualListBox);
        preferencesPanel.add(new CaptionPanel(messages.formGridPreferencesSelectedColumnSettings(), columnSettingsPanel));
        preferencesPanel.add(new CaptionPanel(messages.formGridPreferencesGridSettings(), gridSettingsPanel));
        if (canBeSaved) {
            FlexPanel buttonsPanel = new FlexPanel();

            FormButton saveButton = new FormButton(messages.formGridPreferencesSave(), FormButton.ButtonStyle.PRIMARY, event -> savePressed());
            GwtClientUtils.addClassName(saveButton, "panel-renderer-value", "panelRendererValue", v5);
            buttonsPanel.add(saveButton);

            FormButton resetButton = new FormButton(messages.formGridPreferencesReset(), FormButton.ButtonStyle.SECONDARY,  event -> resetPressed());
            GwtClientUtils.addClassName(resetButton, "panel-renderer-value", "panelRendererValue", v5);
            buttonsPanel.add(resetButton);

            preferencesPanel.add(buttonsPanel);
        }

        focusPanel = new FocusPanel(preferencesPanel);
        focusPanel.addKeyDownHandler(event -> {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
                GwtClientUtils.stopPropagation(event);
                hide();
            }
        });

        setBodyWidget(focusPanel);

        refreshValues(mergeFont());
    }

    public void showDialog() {
        show(new PopupOwner(grid.getPopupOwnerWidget()));
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

    private PopupOwner getPopupOwner() {
        return new PopupOwner(this);
    }

    private void resetPressed() {
        final GSaveResetConfirmDialog confirmDialog = new GSaveResetConfirmDialog(false);
        confirmDialog.show(getPopupOwner(), new Callback() {
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
        confirmDialog.show(getPopupOwner(), new Callback() {
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
            if (!orderedVisibleProperties.contains(property) && !property.remove) {
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
                DialogBoxHelper.showMessageBox(caption, message, getPopupOwner(), chosenOption -> focusPanel.setFocus(true));
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

    private Label createLabel(String text) {
        Label label = new Label(text + ":");
        GwtClientUtils.addClassNames(label, "panel-property-label", "wrap-text-not-empty", "grid-vert-center");
        return label;
    }

    private TextBox createIntegralTextBox() {
        return createTextBox(true);
    }

    private TextBox createTextBox() {
        return createTextBox(false);
    }

    private TextBox createTextBox(boolean integral) {
        TextBox textBox = new TextBox();
        GwtClientUtils.addClassNames(textBox, "form-control", "prop-size-value");
        if(integral) {
            GwtClientUtils.addClassName(textBox, "user-preferences-integral-text-box", "userPreferencesIntegralTextBox", v5);
        }
        return textBox;
    }

    public abstract void preferencesChanged();
}
