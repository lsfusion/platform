package lsfusion.client.form.object.panel.controller;

import lsfusion.base.file.RawFileData;
import lsfusion.base.lambda.Callback;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.view.ClientFormLayout;
import lsfusion.client.form.design.view.JComponentPanel;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.ImagePropertyRenderer;
import lsfusion.client.form.property.panel.view.PanelView;
import lsfusion.interop.base.view.FlexConstraints;
import lsfusion.interop.form.design.Alignment;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertyPanelController {
    private final ClientFormController form;
    private final PanelController panelController;
    private final ClientPropertyDraw property;

    private List<ClientGroupObjectValue> columnKeys;
    private Map<ClientGroupObjectValue, Object> values;
    private Map<ClientGroupObjectValue, Object> captions;
    private Map<ClientGroupObjectValue, Object> showIfs;
    private Map<ClientGroupObjectValue, Object> readOnly;
    private Map<ClientGroupObjectValue, Object> cellBackgroundValues;
    private Map<ClientGroupObjectValue, Object> cellForegroundValues;
    private Map<ClientGroupObjectValue, Object> imageValues;

    private Map<ClientGroupObjectValue, PanelView> views;

    private JComponentPanel viewsPanel;

    public PropertyPanelController(final ClientFormController form, final PanelController panelController, ClientPropertyDraw property) {

        this.form = form;
        this.panelController = panelController;
        this.property = property;

        form.addPropertyBindings(property, () -> new ClientFormController.Binding(property.groupObject, 0) {
            public boolean pressed(KeyEvent ke) {
                return forceEdit();
            }
            public boolean showing() {
                return views.values().iterator().next().isShowing();
            }
        });

        viewsPanel = new JComponentPanel(property.columnKeysVertical, Alignment.START);
    }

    public boolean forceEdit() {
        if (views != null && !views.isEmpty()) {
            return views.values().iterator().next().forceEdit();
        }
        return false;
    }

    public boolean requestFocusInWindow() {
        if (views != null && !views.isEmpty()) {
            views.values().iterator().next().getFocusComponent().requestFocusInWindow();
            return true;
        }
        return false;
    }

    public void addView(ClientFormLayout formLayout) {
        formLayout.add(property, viewsPanel);
    }

    public void removeView(ClientFormLayout formLayout) {
        formLayout.remove(property, viewsPanel);
    }

    public void setVisible(boolean visible) {
        viewsPanel.setVisible(visible);
    }

    public void setPropertyValues(Map<ClientGroupObjectValue, Object> valueMap, boolean update) {
        if (update) {
            values.putAll(valueMap);
        } else {
            values = valueMap;
        }
    }

    public void setPropertyCaptions(Map<ClientGroupObjectValue, Object> captions) {
        this.captions = captions;
    }

    public void setReadOnlyValues(Map<ClientGroupObjectValue, Object> readOnlyValues) {
        this.readOnly = readOnlyValues;
    }

    public void setShowIfs(Map<ClientGroupObjectValue, Object> showIfs) {
        this.showIfs = showIfs;
    }

    public void setColumnKeys(List<ClientGroupObjectValue> columnKeys) {
        this.columnKeys = columnKeys;
    }

    public void setCellBackgroundValues(Map<ClientGroupObjectValue, Object> cellBackgroundValues) {
        this.cellBackgroundValues = cellBackgroundValues;
    }

    public void setCellForegroundValues(Map<ClientGroupObjectValue, Object> cellForegroundValues) {
        this.cellForegroundValues = cellForegroundValues;
    }

    public void setImageValues(Map<ClientGroupObjectValue, Object> imageValues) {
        this.imageValues = imageValues;
    }

    void update(Color rowBackground, Color rowForeground) {
        Map<ClientGroupObjectValue, PanelView> newViews = new HashMap<>();

        List<ClientGroupObjectValue> columnKeys = this.columnKeys != null ? this.columnKeys : ClientGroupObjectValue.SINGLE_EMPTY_KEY_LIST;
        for (final ClientGroupObjectValue columnKey : columnKeys) {
            if (showIfs == null || showIfs.get(columnKey) != null) {
                if (!property.hide) {
                    PanelView view = property.getPanelView(form, columnKey);
                    view.setReadOnly(property.isReadOnly());
                    newViews.put(columnKey, view);

                    view.getEditPropertyDispatcher().setUpdateEditValueCallback(new Callback<Object>() {
                        @Override
                        public void done(Object result) {
                            values.put(columnKey, result);
                        }
                    });

                    panelController.addGroupObjectActions(view.getComponent());
                }
            }
        }

        if(views != null) {
            views.values().forEach(panelView -> viewsPanel.remove(panelView.getComponent()));
        }
        views = newViews;

        //вообще надо бы удалять всё, и добавлять заново, чтобы соблюдался порядок,
        //но при этом будет терятся фокус с удалённых компонентов, поэтому пока забиваем на порядок
//        viewsPanel.removeAll();

        for (ClientGroupObjectValue columnKey : columnKeys) {
            PanelView view = views.get(columnKey);
            if (view != null && view.getComponent().getParent() != viewsPanel) {
                viewsPanel.add(view.getComponent(), new FlexConstraints(property.getAlignment(), property.getValueWidth(viewsPanel)));
            }
        }

        for (Map.Entry<ClientGroupObjectValue, PanelView> e : views.entrySet()) {
            updatePanelView(e.getKey(), e.getValue(), rowBackground, rowForeground);
        }

        if (property.drawAsync && !views.isEmpty()) {
            form.setAsyncView(views.get(columnKeys.get(0)));
        }
    }

    private void updatePanelView(ClientGroupObjectValue columnKey, PanelView view, Color rowBackground, Color rowForeground) {
        if (values != null) {
            Object value = values.get(columnKey);

            view.setValue(value);
        }

        if (readOnly != null) {
            view.setReadOnly(readOnly.get(columnKey) != null);
        }

        Color background = rowBackground;
        if (background == null && cellBackgroundValues != null) {
            background = (Color) cellBackgroundValues.get(columnKey);
        }
        view.setBackgroundColor(background);

        Color foreground = rowForeground;
        if (foreground == null && cellForegroundValues != null) {
            foreground = (Color) cellForegroundValues.get(columnKey);
        }
        view.setForegroundColor(foreground);

        if(imageValues != null) {
            RawFileData image = (RawFileData) imageValues.get(columnKey);
            if(image != null) {
                view.setImage(ImagePropertyRenderer.convertValue(image));
            }
        }

        if (captions != null) {
            String caption = property.getDynamicCaption(captions.get(columnKey));
            view.setCaption(caption);
            view.setToolTip(caption);
        }
    }
}