package lsfusion.client.form.object.panel.controller;

import lsfusion.base.Pair;
import lsfusion.base.file.RawFileData;
import lsfusion.client.base.focus.FocusComponentProvider;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.view.ClientFormLayout;
import lsfusion.client.form.design.view.FlexPanel;
import lsfusion.client.form.design.view.flex.CaptionContainerHolder;
import lsfusion.client.form.design.view.flex.LinearCaptionContainer;
import lsfusion.client.form.design.view.widget.Widget;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.ImagePropertyRenderer;
import lsfusion.client.form.property.panel.view.PanelView;
import lsfusion.interop.base.view.FlexAlignment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
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

    private final Panel renderersPanel;

    public PropertyPanelController(final ClientFormController form, final PanelController panelController, ClientPropertyDraw property) {

        this.form = form;
        this.panelController = panelController;
        this.property = property;

        form.addPropertyBindings(property, () -> new ClientFormController.Binding(property.groupObject, 0) {
            public boolean pressed(InputEvent ke) {
                return forceEdit();
            }
            public boolean showing() {
                if(views != null && !views.isEmpty())
                    return views.values().iterator().next().isShowing();
                return false;
            }
        });

        renderersPanel = new Panel(property.panelColumnVertical);
        renderersPanel.setDebugContainer("CONTROLLER [" + property + "]");
    }

    public boolean forceEdit() {
        if (views != null && !views.isEmpty()) {
            return views.values().iterator().next().forceEdit();
        }
        return false;
    }
    
    public JComponent getFocusComponent() {
        if (views != null && !views.isEmpty()) {
            return views.values().iterator().next().getFocusComponent();
        }
        return null;
    }

    public boolean requestFocusInWindow() {
        JComponent focusComponent = getFocusComponent();
        if (focusComponent != null) {
            return focusComponent.requestFocusInWindow();
        }
        return false;
    }

    public void addView(ClientFormLayout formLayout) {
        formLayout.addBaseComponent(property, renderersPanel, (FocusComponentProvider) PropertyPanelController.this::getFocusComponent);
    }

    public void removeView(ClientFormLayout formLayout) {
        formLayout.removeBaseComponent(property, renderersPanel);
    }

    public void setVisible(boolean visible) {
        renderersPanel.setVisible(visible);
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

    public interface CaptionContainer {
        void put(Widget widget, Pair<Integer, Integer> valueSizes, FlexAlignment alignment);
    }

    public static class Panel extends FlexPanel implements CaptionContainerHolder {

        public Panel(boolean vertical) {
            super(vertical);
        }

        public LinearCaptionContainer captionContainer;

        @Override
        public void setCaptionContainer(LinearCaptionContainer captionContainer) {
            this.captionContainer = captionContainer; 
        }

        @Override
        public FlexAlignment getCaptionHAlignment() {
            return FlexAlignment.START;
        }
    }

    public void update(Color rowBackground, Color rowForeground) {
        Map<ClientGroupObjectValue, PanelView> newViews = new HashMap<>();

        List<ClientGroupObjectValue> columnKeys = this.columnKeys != null ? this.columnKeys : ClientGroupObjectValue.SINGLE_EMPTY_KEY_LIST;
        for (final ClientGroupObjectValue columnKey : columnKeys) {
            if (showIfs == null || showIfs.get(columnKey) != null) {
                PanelView view = views != null ? views.remove(columnKey) : null;
                if (view == null && (!property.hide || property.changeKey != null)) {
                    view = property.getPanelView(form, columnKey, renderersPanel.captionContainer);
                    view.setReadOnly(property.isReadOnly());

                    view.getEditPropertyDispatcher().setUpdateEditValueCallback(result -> values.put(columnKey, result));

                    panelController.addGroupObjectActions(view.getWidget().getComponent());
                }
                if(view != null) {
                    newViews.put(columnKey, view);
                }
            }
        }

        if(views != null && !property.hide) {
            views.values().forEach(panelView -> renderersPanel.remove(panelView.getWidget()));
        }
        views = newViews;

        //вообще надо бы удалять всё, и добавлять заново, чтобы соблюдался порядок,
        //но при этом будет терятся фокус с удалённых компонентов, поэтому пока забиваем на порядок
//        viewsPanel.removeAll();

        if (!property.hide) {
            for (ClientGroupObjectValue columnKey : columnKeys) {
                PanelView view = views.get(columnKey);
                if (view != null && view.getWidget().getParent() != renderersPanel.getComponent()) {
//                    viewsPanel.add(view.getComponent(), new FlexConstraints(property.getAlignment(), property.getValueWidth(viewsPanel)));
                    renderersPanel.addFill(view.getWidget());
                }
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