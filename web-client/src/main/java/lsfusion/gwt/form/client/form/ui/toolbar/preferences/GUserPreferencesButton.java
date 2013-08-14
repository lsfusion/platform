package lsfusion.gwt.form.client.form.ui.toolbar.preferences;

import com.allen_sauer.gwt.dnd.client.DragHandlerAdapter;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.base.client.ErrorHandlingCallback;
import lsfusion.gwt.base.client.ui.DialogBoxHelper;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.client.MainFrame;
import lsfusion.gwt.form.client.form.ui.GCaptionPanel;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.client.form.ui.GGridTable;
import lsfusion.gwt.form.client.form.ui.dialog.GResizableModalWindow;
import lsfusion.gwt.form.client.form.ui.toolbar.GToolbarButton;
import lsfusion.gwt.form.shared.view.GColumnUserPreferences;
import lsfusion.gwt.form.shared.view.GFormUserPreferences;
import lsfusion.gwt.form.shared.view.GGroupObjectUserPreferences;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;

import java.util.*;

public abstract class GUserPreferencesButton extends GToolbarButton {
    private static final String PREFERENCES_SAVED_ICON = "userPreferencesSaved.png";
    private static final String PREFERENCES_UNSAVED_ICON = "userPreferences.png";
    private static final String CSS_USER_PREFERENCES_DUAL_LIST = "userPreferencesDualList";

    public UserPreferencesDialog dialog;

    private Map<String, Integer> orderMap = new HashMap<String, Integer>();

    public GUserPreferencesButton(boolean hasUserPreferences) {
        super(hasUserPreferences ? PREFERENCES_SAVED_ICON : PREFERENCES_UNSAVED_ICON, "Настройка таблицы");
    }

    public class UserPreferencesDialog extends GResizableModalWindow {
        private GFormController form;
        private GGridTable grid;

        private FocusPanel focusPanel;

        ColumnsDualListBox columnsDualListBox;

        public UserPreferencesDialog(GGridTable grid, GFormController form) {
            super("Настройка таблицы");

            this.form = form;
            this.grid = grid;

            VerticalPanel preferencesPanel = new VerticalPanel();
            preferencesPanel.setSize("100%", "100%");

            // columns
            columnsDualListBox = new ColumnsDualListBox();
            columnsDualListBox.getDragController().addDragHandler(new DragHandlerAdapter());
            columnsDualListBox.addStyleName(CSS_USER_PREFERENCES_DUAL_LIST);

            preferencesPanel.add(columnsDualListBox);
            preferencesPanel.setCellHeight(columnsDualListBox, "100%");

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
                    cancelPressed();
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
                        cancelPressed();
                    }
                }
            });

            VerticalPanel vp = new VerticalPanel();
            vp.add(focusPanel);
            vp.setCellHeight(focusPanel, "100%");

            setContentWidget(vp);



            HashMap<GPropertyDraw, Integer> propertyOrderMap = new HashMap<GPropertyDraw, Integer>();
            ArrayList<GPropertyDraw> properties = grid.getProperties();
            for (int i = 0; i < properties.size(); i++) {
                if (properties.get(i).orderUser == null)
                    properties.get(i).orderUser = i;
                propertyOrderMap.put(properties.get(i), properties.get(i).orderUser);

            }
            ColumnsOrderComparator columnsOrderComparator = new ColumnsOrderComparator(propertyOrderMap);
            TreeMap<GPropertyDraw, Integer> propertyOrderTreeMap = new TreeMap<GPropertyDraw, Integer>(columnsOrderComparator);
            propertyOrderTreeMap.putAll(propertyOrderMap);

            int i = 0;
            for (Map.Entry<GPropertyDraw, Integer> entry : propertyOrderTreeMap.entrySet()) {
                String caption = GwtSharedUtils.nullTrim(entry.getKey().getNotEmptyCaption());
                orderMap.put(caption, i);

                Boolean needToHide = entry.getKey().hideUser == null ? false : entry.getKey().hideUser;
                if (!needToHide)
                    columnsDualListBox.addVisible(entry.getKey());
                else
                    columnsDualListBox.addInvisible(entry.getKey());
                i++;
            }
        }

        public void showDialog() {
            center();
            setContentSize(430, 400);
            center();
            focusPanel.setFocus(true);
        }

        private void okPressed() {
            int i = 0;
            for (Widget label : columnsDualListBox.getVisibleWidgets()) {
                GPropertyDraw property = ((PropertyLabel) label).getProperty();
                property.orderUser = i;
                property.hideUser = false;
                i++;
            }

            i = 0;
            for (Widget label : columnsDualListBox.getInvisibleWidgets()) {
                GPropertyDraw property = ((PropertyLabel) label).getProperty();
                property.orderUser = columnsDualListBox.getVisibleWidgets().size() + i;
                property.hideUser = true;
                i++;
            }

            grid.columnsPreferencesChanged();

            dialog.hide();
        }

        private void resetPressed(boolean forAllUsers) {
            Map<String, GColumnUserPreferences> preferences = new HashMap<String, GColumnUserPreferences>();

            for (Widget w : columnsDualListBox.getVisibleWidgets()) {
                GPropertyDraw property = ((PropertyLabel) w).getProperty();
                preferences.put(property.sID, resetPropertyUserPreferences(property));
            }

            for (Widget w : columnsDualListBox.getInvisibleWidgets()) {
                GPropertyDraw property = ((PropertyLabel) w).getProperty();
                preferences.put(property.sID, resetPropertyUserPreferences(property));
            }

            if (grid.getProperties().size() != 0) {
                List<GGroupObjectUserPreferences> groupObjectUserPreferencesList = new ArrayList<GGroupObjectUserPreferences>();
                groupObjectUserPreferencesList.add(new GGroupObjectUserPreferences(preferences, grid.getGroupObject().getSID(), false));
                savePreferences(groupObjectUserPreferencesList, forAllUsers, true, "Сброс настроек успешно завершен");
            }

            columnsDualListBox.clearLists();
            for (int i = 0; i < grid.getProperties().size(); i++) {
                columnsDualListBox.addVisible(grid.getProperties().get(i));
            }
        }

        private GColumnUserPreferences resetPropertyUserPreferences(GPropertyDraw property) {
            property.hideUser = null;
            property.widthUser = null;
            property.sortUser = null;
            property.ascendingSortUser = null;
            property.orderUser = property.ID;
            return new GColumnUserPreferences(null, null, null, null, null);
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

            HashMap<String, GColumnUserPreferences> columnPreferences = new HashMap<String, GColumnUserPreferences>();
            i = 0;
            for (Widget w : columnsDualListBox.getVisibleWidgets()) {
                GPropertyDraw property = ((PropertyLabel) w).getProperty();
                columnPreferences.put(property.sID, refreshPropertyUserPreferences(property, false, i, userSortDirections));
                i++;
            }

            i = 0;
            for (Widget w : columnsDualListBox.getInvisibleWidgets()) {
                GPropertyDraw property = ((PropertyLabel) w).getProperty();
                int propertyOrder = columnsDualListBox.getVisibleWidgets().size() + i;
                columnPreferences.put(property.sID, refreshPropertyUserPreferences(property, true, propertyOrder, userSortDirections));
                i++;
            }

            if (grid.getProperties().size() != 0) {
                List<GGroupObjectUserPreferences> groupObjectUserPreferencesList = new ArrayList<GGroupObjectUserPreferences>();
                groupObjectUserPreferencesList.add(new GGroupObjectUserPreferences(columnPreferences, grid.getGroupObject().getSID(), true));
                savePreferences(groupObjectUserPreferencesList, forAllUsers, false, "Сохранение настроек успешно завершено");
            }
        }

        private GColumnUserPreferences refreshPropertyUserPreferences(GPropertyDraw property, boolean hide, int propertyOrder, Map<GPropertyDraw, Map<Boolean, Integer>> userSortDirections) {
            Boolean sortDirection = userSortDirections.containsKey(property) ? userSortDirections.get(property).keySet().iterator().next() : null;
            Integer sortIndex = userSortDirections.containsKey(property) ? userSortDirections.get(property).values().iterator().next() : null;
            property.hideUser = hide;
            property.orderUser = propertyOrder;
            return new GColumnUserPreferences(hide, property.widthUser, propertyOrder, sortDirection != null ? sortIndex : null, sortDirection);
        }

        private void savePreferences(List<GGroupObjectUserPreferences> goPreferences, final boolean forAllUsers, final boolean reset, final String messageText) {
            form.saveUserPreferences(new GFormUserPreferences(goPreferences), forAllUsers, new ErrorHandlingCallback<VoidResult>() {
                @Override
                public void success(VoidResult result) {
                    grid.columnsPreferencesChanged();
                    DialogBoxHelper.showMessageBox(false, "Изменение настроек", messageText, null);
                    if (!reset) {
                        setModuleImagePath(PREFERENCES_SAVED_ICON);
                    } else if (forAllUsers) {
                        setModuleImagePath(PREFERENCES_UNSAVED_ICON);
                    }
                }
            });
        }
    }

    private void cancelPressed() {
        dialog.hide();
    }

    class ColumnsOrderComparator implements Comparator {
        Map<GPropertyDraw, Integer> base;

        public ColumnsOrderComparator(Map base) {
            this.base = base;
        }

        public int compare(Object a, Object b) {

            if (base.get(a) < base.get(b))
                return -1;
            else
                return 1;
        }
    }
}
