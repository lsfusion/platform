package lsfusion.gwt.form.client.form.ui.toolbar.preferences;

import com.allen_sauer.gwt.dnd.client.DragHandlerAdapter;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.base.client.ErrorHandlingCallback;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.base.client.ui.DialogBoxHelper;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GFlexAlignment;
import lsfusion.gwt.form.client.MainFrame;
import lsfusion.gwt.form.client.form.ui.GCaptionPanel;
import lsfusion.gwt.form.client.form.ui.GGridTable;
import lsfusion.gwt.form.client.form.ui.GGroupObjectController;
import lsfusion.gwt.form.client.form.ui.dialog.GResizableModalWindow;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.form.shared.view.GFont;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.gwt.form.shared.view.GFont.DEFAULT_FONT_FAMILY;
import static lsfusion.gwt.form.shared.view.GFont.DEFAULT_FONT_SIZE;

public abstract class GUserPreferencesDialog extends GResizableModalWindow {
    private static final String CSS_USER_PREFERENCES_DUAL_LIST = "userPreferencesDualList";

    private GGroupObjectController groupController;
    private GGridTable grid;

    private FocusPanel focusPanel;

    private ColumnsDualListBox columnsDualListBox;
    private TextBox sizeBox;
    private CheckBox boldBox;
    private CheckBox italicBox;

    public GUserPreferencesDialog(GGridTable grid, GGroupObjectController groupController) {
        super("Настройка таблицы");
        this.groupController = groupController;

        this.grid = grid;

        VerticalPanel preferencesPanel = new VerticalPanel();
        preferencesPanel.setSize("100%", "100%");

        // columns
        columnsDualListBox = new ColumnsDualListBox();
        columnsDualListBox.getDragController().addDragHandler(new DragHandlerAdapter());
        columnsDualListBox.addStyleName(CSS_USER_PREFERENCES_DUAL_LIST);

        preferencesPanel.add(columnsDualListBox);
        preferencesPanel.setCellHeight(columnsDualListBox, "100%");

        preferencesPanel.add(GwtClientUtils.createVerticalStrut(3));

        // font settings
        Label sizeLabel = new Label("Размер: ");
        sizeBox = new TextBox();
        sizeBox.addStyleName("userPreferencesFontSizeTextBox");

        boldBox = new CheckBox("Полужирный");
        italicBox = new CheckBox("Курсив");

        FlexPanel fontPanel = new FlexPanel(FlexPanel.Justify.CENTER);
        fontPanel.add(sizeLabel, GFlexAlignment.CENTER);
        fontPanel.add(sizeBox, GFlexAlignment.CENTER);
        fontPanel.add(GwtClientUtils.createHorizontalStrut(3));
        fontPanel.add(boldBox, GFlexAlignment.CENTER);
        fontPanel.add(GwtClientUtils.createHorizontalStrut(3));
        fontPanel.add(italicBox, GFlexAlignment.CENTER);

        GCaptionPanel fontSettingsPanel = new GCaptionPanel("Настройки шрифта", fontPanel);
        preferencesPanel.add(fontSettingsPanel);

        preferencesPanel.add(GwtClientUtils.createVerticalStrut(5));

        //save/reset buttons
        HorizontalPanel saveResetButtons = new HorizontalPanel();

        Button saveForUserButton = new Button("Сохранить настройки");
        saveForUserButton.setWidth("15em");
        saveForUserButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                savePressed(false);
            }
        });
        Button resetForUserButton = new Button("Сбросить настройки");
        resetForUserButton.setWidth("15em");
        resetForUserButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                resetPressed(false);
            }
        });
        VerticalPanel currentUserButtons = new VerticalPanel();
        currentUserButtons.add(saveForUserButton);
        currentUserButtons.add(resetForUserButton);
        GCaptionPanel titledPanel = new GCaptionPanel("Для текущего пользователя", currentUserButtons);
        titledPanel.setSize("100%", "100%");
        saveResetButtons.add(titledPanel);

        if (MainFrame.configurationAccessAllowed) {
            Button saveButton = new Button("Сохранить настройки");
            saveButton.setWidth("15em");
            saveButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    savePressed(true);
                }
            });
            Button resetButton = new Button("Сбросить настройки");
            resetButton.setWidth("15em");
            resetButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    resetPressed(true);
                }
            });
            VerticalPanel allUsersButtons = new VerticalPanel();
            allUsersButtons.add(saveButton);
            allUsersButtons.add(resetButton);

            titledPanel = new GCaptionPanel("Для всех пользователей", allUsersButtons);
            titledPanel.setSize("100%", "100%");
            saveResetButtons.add(titledPanel);
        }

        SimplePanel srbContainer = new SimplePanel(saveResetButtons);
        preferencesPanel.add(srbContainer);
        preferencesPanel.setCellHorizontalAlignment(srbContainer, HasAlignment.ALIGN_CENTER);

        preferencesPanel.add(GwtClientUtils.createVerticalStrut(5));

        // ok/cancel buttons
        Button okButton = new Button("OK");
        okButton.setWidth("6em");
        okButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                okPressed();
            }
        });

        Button cancelButton = new Button("Отмена");
        cancelButton.setWidth("6em");
        cancelButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                hide();
            }
        });

        HorizontalPanel buttonsPanel = new HorizontalPanel();
        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);
        buttonsPanel.addStyleName("floatRight");

        preferencesPanel.add(buttonsPanel);


        focusPanel = new FocusPanel(preferencesPanel);
        focusPanel.addStyleName("noOutline");
        focusPanel.setHeight("100%");
        focusPanel.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
                    hide();
                }
            }
        });

        VerticalPanel vp = new VerticalPanel();
        vp.add(focusPanel);
        vp.setCellHeight(focusPanel, "100%");

        setContentWidget(vp);

        refreshValues(mergeFont());
    }

    public void showDialog() {
        center();
        setContentSize(430, 400);
        center();
        focusPanel.setFocus(true);
    }

    private void okPressed() {
        for (Widget label : columnsDualListBox.getVisibleWidgets()) {
            GPropertyDraw property = ((PropertyLabel) label).getPropertyItem().property;
            grid.setUserOrder(property, columnsDualListBox.getVisibleIndex(label));
            grid.setUserHide(property, false);
        }

        for (Widget label : columnsDualListBox.getInvisibleWidgets()) {
            GPropertyDraw property = ((PropertyLabel) label).getPropertyItem().property;
            grid.setUserOrder(property, columnsDualListBox.getVisibleCount() + columnsDualListBox.getInvisibleIndex(label));
            grid.setUserHide(property, true);
        }

        GFont userFont = getUserFont();
        grid.setUserFont(userFont);
        grid.font = userFont;
        
        grid.setHasUserPreferences(true);
        
        grid.columnsPreferencesChanged();

        hide();
    }

    private GFont getUserFont() {
        GFont initialFont = getInitialFont();
        Integer size;
        try {
            size = Integer.parseInt(sizeBox.getValue());
        } catch(NumberFormatException e) {
            size = initialFont.size;
        }

        return new GFont(initialFont.family, size != 0 ? size : initialFont.size, boldBox.getValue(), italicBox.getValue());
    }

    private GFont getInitialFont() {
        GFont designFont = grid.getDesignFont();
        return designFont == null ? new GFont(grid.font != null ? grid.font.family : DEFAULT_FONT_FAMILY, GFont.DEFAULT_FONT_SIZE, false, false) : designFont;
    }

    private void resetPressed(boolean forAllUsers) {
        grid.resetPreferences(forAllUsers, createSaveCallback("Сброс настроек успешно завершен"));
    }

    private void savePressed(boolean forAllUsers) {
        Map<GPropertyDraw, Map<Boolean, Integer>> userSortDirections = new HashMap<GPropertyDraw, Map<Boolean, Integer>>();
        int i = 0;
        for (Map.Entry<Map<GPropertyDraw, GGroupObjectValue>, Boolean> entry : grid.getOrderDirections().entrySet()) {
            HashMap<Boolean, Integer> dirs = new HashMap<Boolean, Integer>();
            dirs.put(entry.getValue(), i);
            userSortDirections.put(entry.getKey().keySet().iterator().next(), dirs);
            i++;
        }

        for (Widget w : columnsDualListBox.getVisibleWidgets()) {
            GPropertyDraw property = ((PropertyLabel) w).getPropertyItem().property;
            refreshPropertyUserPreferences(property, false, columnsDualListBox.getVisibleIndex(w), userSortDirections.get(property));
        }

        for (Widget w : columnsDualListBox.getInvisibleWidgets()) {
            GPropertyDraw property = ((PropertyLabel) w).getPropertyItem().property;
            int propertyOrder = columnsDualListBox.getVisibleCount() + columnsDualListBox.getInvisibleIndex(w);
            refreshPropertyUserPreferences(property, true, propertyOrder, userSortDirections.get(property));
        }

        GFont userFont = getUserFont();
        grid.setUserFont(userFont);
        
        grid.saveCurrentPreferences(forAllUsers, createSaveCallback("Сохранение настроек успешно завершено"));
    }

    private void refreshPropertyUserPreferences(GPropertyDraw property, boolean hide, int propertyOrder, Map<Boolean, Integer> userSortDirections) {
        Boolean sortDirection = userSortDirections != null ? userSortDirections.keySet().iterator().next() : null;
        Integer sortIndex = userSortDirections != null ? userSortDirections.values().iterator().next() : null;
        grid.setUserHide(property, hide);
        grid.setUserOrder(property, propertyOrder);
        grid.setUserSort(property, sortDirection != null ? sortIndex : null);
        grid.setUserAscendingSort(property, sortDirection);
    }

    private Boolean getPropertyState(GPropertyDraw property) {
        if (groupController.isPropertyInGrid(property)) {
            return true;
        } else if (groupController.isPropertyInPanel(property)) {
            return false;
        }
        return null;
    }

    private void refreshValues(GFont font) {
        List<GPropertyDraw> orderedVisibleProperties = grid.getOrderedVisibleProperties(groupController.getGroupObjectProperties());

        columnsDualListBox.clearLists();

        for (GPropertyDraw property : orderedVisibleProperties) {
            columnsDualListBox.addVisible(new PropertyListItem(property, getPropertyState(property)));
        }
        for (GPropertyDraw property : groupController.getGroupObjectProperties()) {
            if (!orderedVisibleProperties.contains(property)) {
                columnsDualListBox.addInvisible(new PropertyListItem(property, getPropertyState(property)));
            }
        }

        sizeBox.setValue((font == null || font.size == null) ? DEFAULT_FONT_SIZE.toString() : font.size.toString());
        boldBox.setValue(font != null && font.bold);
        italicBox.setValue(font != null && font.italic);
    }
    
    private GFont mergeFont() {
        GGridUserPreferences prefs = grid.getCurrentPreferences();

        GFont font = getInitialFont();
        if (prefs.hasUserPreferences()) {
            font = prefs.font;
        }
        return font;
    } 

    private ErrorHandlingCallback<ServerResponseResult> createSaveCallback(final String successMessageText) {
        return new ErrorHandlingCallback<ServerResponseResult>() {
            @Override
            public void success(ServerResponseResult result) {
                GFont font = mergeFont();
                refreshValues(font);
                grid.font = font;
                grid.columnsPreferencesChanged();
                preferencesChanged();
                DialogBoxHelper.showMessageBox(false, "Изменение настроек", successMessageText, null);
            }

            @Override
            public void failure(Throwable caught) {
                GFont font = mergeFont();
                refreshValues(font);
                grid.font = font;
                grid.columnsPreferencesChanged();
            }
        };
    }
    
    public abstract void preferencesChanged();
}
