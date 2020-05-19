package lsfusion.server.language.form;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.interop.form.property.PropertyGroupType;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import javax.swing.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.base.BaseUtils.isRedundantString;
import static lsfusion.base.BaseUtils.nvl;

public class FormPropertyOptions {
    private PropertyEditType editType;
    private Boolean isSelector;    
    private Boolean hintNoUpdate;
    private Boolean hintTable;
    private Boolean optimisticAsync;
    private Columns columns;
    private PropertyObjectEntity showIf;
    private PropertyObjectEntity readOnlyIf;
    private PropertyObjectEntity background;
    private PropertyObjectEntity foreground;
    private PropertyObjectEntity header;
    private PropertyObjectEntity footer;
    private ClassViewType viewType;
    private GroupObjectEntity toDraw;
    private OrderedMap<String, LocalizedString> contextMenuBindings;
    private Map<KeyStroke, String> keyBindings;
    private Map<String, ActionObjectEntity> eventActions;
    private String eventId;
    private String integrationSID;
    private PropertyDrawEntity neighbourPropertyDraw;
    private Boolean order;
    private boolean filter;
    private boolean pivotRow;
    private boolean pivotColumn;
    private boolean pivotMeasure;

    // for pivoting
    public String formula;
    public ImList<PropertyDrawEntity> formulaOperands;

    public PropertyGroupType aggrFunc;

    public ImList<PropertyObjectEntity> lastAggrColumns;
    public Boolean lastAggrDesc;

    private PropertyDrawEntity quickFilterPropertyDraw;
    private String neighbourPropertyText;
    private Boolean isRightNeighbour;
    
    private Boolean newSession;
    private Boolean isNested;

    //integration options
    private Boolean attr;
    private String groupName;

    public void setAggrFunc(PropertyGroupType aggrFunc) {
        this.aggrFunc = aggrFunc;
    }

    public void setLastAggr(List<PropertyObjectEntity> lastAggrColumns, boolean lastAggrDesc) {
        this.lastAggrColumns = ListFact.fromJavaList(lastAggrColumns);
        this.lastAggrDesc = lastAggrDesc;
    }

    public void setFormula(String formula, List<PropertyDrawEntity> formulaOperands) {
        this.formula = formula;
        this.formulaOperands = ListFact.fromJavaList(formulaOperands);
    }

    public void setNewSession(Boolean newSession) {
        this.newSession = newSession;
    }
    
    public void setNested(Boolean isNested) {
        this.isNested = isNested;
    }
    
    public Boolean isNewSession() {
        return newSession;
    }
    
    public Boolean isNested() {
        return isNested;
    }

    public Boolean getSelector() {
        return isSelector;
    }

    public void setSelector(Boolean selector) {
        isSelector = selector;
    }

    public PropertyEditType getEditType() {
        return editType;
    }

    public void setEditType(PropertyEditType editType) {
        this.editType = editType;
    }

    public Columns getColumns() {
        return columns;
    }

    public void setColumns(Columns columns) {
        this.columns = columns;
    }

    public void setColumns(String columnsName, List<GroupObjectEntity> columns) {
        this.columns = new Columns(columnsName, columns);
    }

    public PropertyObjectEntity getShowIf() {
        return showIf;
    }

    public void setShowIf(PropertyObjectEntity showIf) {
        this.showIf = showIf;
    }

    public PropertyObjectEntity getReadOnlyIf() {
        return readOnlyIf;
    }

    public void setReadOnlyIf(PropertyObjectEntity readOnlyIf) {
        this.readOnlyIf = readOnlyIf;
    }

    public PropertyObjectEntity getBackground() {
        return background;
    }

    public void setBackground(PropertyObjectEntity background) {
        this.background = background;
    }

    public PropertyObjectEntity getForeground() {
        return foreground;
    }

    public void setForeground(PropertyObjectEntity foreground) {
        this.foreground = foreground;
    }

    public PropertyObjectEntity getHeader() {
        return header;
    }

    public void setHeader(PropertyObjectEntity header) {
        this.header = header;
    }

    public PropertyObjectEntity getFooter() {
        return footer;
    }

    public void setFooter(PropertyObjectEntity footer) {
        this.footer = footer;
    }

    public ClassViewType getViewType() {
        return viewType;
    }

    public void setViewType(ClassViewType viewType) {
        this.viewType = viewType;
    }

    public void setToDraw(GroupObjectEntity toDraw) {
        this.toDraw = toDraw;
    }

    public GroupObjectEntity getToDraw() {
        return toDraw;
    }

    public Boolean getHintNoUpdate() {
        return hintNoUpdate;
    }

    public void setHintNoUpdate(Boolean hintNoUpdate) {
        this.hintNoUpdate = hintNoUpdate;
    }

    public Boolean getHintTable() {
        return hintTable;
    }

    public void setHintTable(Boolean hintTable) {
        this.hintTable = hintTable;
    }

    public Boolean getOptimisticAsync() {
        return optimisticAsync;
    }

    public void setOptimisticAsync(Boolean optimisticAsync) {
        this.optimisticAsync = optimisticAsync;
    }

    public void addEventAction(String actionSID, ActionObjectEntity action) {
        if (action != null) {
            if (eventActions == null) {
                eventActions = new HashMap<>();
            }
            eventActions.put(actionSID, action);
        }
    }

    public void addContextMenuBinding(String actionSID, LocalizedString caption) {
        if (contextMenuBindings == null) {
            contextMenuBindings = new OrderedMap<>();
        }
        contextMenuBindings.put(actionSID, caption);
    }

    public void addContextMenuAction(LocalizedString caption, ActionObjectEntity action) {
        if (action != null) {
            Action property = (Action) action.property;

            addEventAction(property.getSID(), action);
            addContextMenuBinding(property.getSID(), getContextMenuCaption(caption, property));
        }
    }

    public static LocalizedString getContextMenuCaption(LocalizedString caption, Action property) {
        if (caption == null || isRedundantString(caption.getSourceString())) {
            caption = property.caption;
        }
        if (caption == null || isRedundantString(caption.getSourceString())) {
            caption = LocalizedString.create(property.getSID());
        }
        return caption;
    }

    public OrderedMap<String, LocalizedString> getContextMenuBindings() {
        return contextMenuBindings;
    }

    public void setContextMenuBindings(OrderedMap<String, LocalizedString> contextMenuBindings) {
        this.contextMenuBindings = contextMenuBindings;
    }
    
    public void addKeyPressAction(String key, ActionObjectEntity action) {
        if (action != null) {
            String propertySID = action.property.getSID();
            addEventAction(propertySID, action);
            addKeyBinding(KeyStroke.getKeyStroke(key), propertySID);
        }
    }

    public void addKeyBinding(KeyStroke key, String actionSID) {
        if (keyBindings == null) {
            keyBindings = new HashMap<>();
        }
        keyBindings.put(key, actionSID);
    }
    
    public Map<KeyStroke, String> getKeyBindings() {
        return keyBindings;
    }
    
    public void setKeyBindings(Map<KeyStroke, String> keyBindings) {
        this.keyBindings = keyBindings;
    } 

    public Map<String, ActionObjectEntity> getEventActions() {
        return eventActions;
    }

    public void setEventActions(Map<String, ActionObjectEntity> eventActions) {
        this.eventActions = eventActions;
    }

    public PropertyDrawEntity getNeighbourPropertyDraw() {
        return neighbourPropertyDraw;
    }

    public String getNeighbourPropertyText() {
        return neighbourPropertyText;
    }

    public void setNeighbourPropertyDraw(PropertyDrawEntity neighbourPropertyDraw, String propText) {
        this.neighbourPropertyDraw = neighbourPropertyDraw;
        this.neighbourPropertyText = propText;
    }

    public PropertyDrawEntity getQuickFilterPropertyDraw() {
        return quickFilterPropertyDraw;
    }

    public void setQuickFilterPropertyDraw(PropertyDrawEntity quickFilterPropertyDraw) {
        this.quickFilterPropertyDraw = quickFilterPropertyDraw;
    }

    public Boolean isRightNeighbour() {
        return isRightNeighbour;
    }

    public void setNeighbourType(Boolean isRightNeighbour) {
        this.isRightNeighbour = isRightNeighbour;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getIntegrationSID() {
        return integrationSID;
    }

    public void setIntegrationSID(String integrationSID) {
        this.integrationSID = integrationSID;
    }

    public Boolean getAttr() {
        return attr;
    }

    public void setAttr(Boolean attr) {
        this.attr = attr;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Boolean getOrder() {
        return order;
    }

    public void setOrder(Boolean order) {
        this.order = order;
    }

    public boolean isFilter() {
        return filter;
    }

    public void setFilter(boolean filter) {
        this.filter = filter;
    }

    public boolean isPivotRow() {
        return pivotRow;
    }

    public void setPivotRow(boolean pivotRow) {
        this.pivotRow = pivotRow;
    }

    public boolean isPivotColumn() {
        return pivotColumn;
    }

    public void setPivotColumn(boolean pivotColumn) {
        this.pivotColumn = pivotColumn;
    }

    public boolean isPivotMeasure() {
        return pivotMeasure;
    }

    public void setPivotMeasure(boolean pivotMeasure) {
        this.pivotMeasure = pivotMeasure;
    }

    public FormPropertyOptions overrideWith(FormPropertyOptions overrides) {
        FormPropertyOptions merged = new FormPropertyOptions();

        merged.setNewSession(nvl(overrides.isNewSession(), newSession));
        merged.setNested(nvl(overrides.isNested(), isNested));
        merged.setEditType(nvl(overrides.getEditType(), editType));
        merged.setSelector(nvl(overrides.getSelector(), isSelector));
        merged.setHintNoUpdate(nvl(overrides.getHintNoUpdate(), hintNoUpdate));
        merged.setHintTable(nvl(overrides.getHintTable(), hintTable));
        merged.setOptimisticAsync(nvl(overrides.getOptimisticAsync(), optimisticAsync));
        merged.setColumns(nvl(overrides.getColumns(), columns));
        merged.setShowIf(nvl(overrides.getShowIf(), showIf));
        merged.setReadOnlyIf(nvl(overrides.getReadOnlyIf(), readOnlyIf));
        merged.setBackground(nvl(overrides.getBackground(), background));
        merged.setForeground(nvl(overrides.getForeground(), foreground));
        merged.setHeader(nvl(overrides.getHeader(), header));
        merged.setFooter(nvl(overrides.getFooter(), footer));
        merged.setViewType(nvl(overrides.getViewType(), viewType));
        merged.setToDraw(nvl(overrides.getToDraw(), toDraw));
        merged.setEventActions(nvl(overrides.getEventActions(), eventActions));
        merged.setContextMenuBindings(nvl(overrides.getContextMenuBindings(), contextMenuBindings));
        merged.setKeyBindings(nvl(overrides.getKeyBindings(), keyBindings));
        merged.setEventId(nvl(overrides.getEventId(), eventId));
        merged.setIntegrationSID(nvl(overrides.getIntegrationSID(), integrationSID));
        merged.setNeighbourPropertyDraw(nvl(overrides.getNeighbourPropertyDraw(), neighbourPropertyDraw), nvl(overrides.getNeighbourPropertyText(), neighbourPropertyText));
        merged.setNeighbourType(nvl(overrides.isRightNeighbour(), isRightNeighbour));

        merged.formula = nvl(overrides.formula, formula);
        merged.formulaOperands = nvl(overrides.formulaOperands, formulaOperands);
        merged.aggrFunc = nvl(overrides.aggrFunc, aggrFunc);
        merged.lastAggrColumns = nvl(overrides.lastAggrColumns, lastAggrColumns);
        merged.lastAggrDesc = nvl(overrides.lastAggrDesc, lastAggrDesc);

        merged.setQuickFilterPropertyDraw(nvl(overrides.getQuickFilterPropertyDraw(), quickFilterPropertyDraw));

        merged.setAttr(nvl(overrides.getAttr(), attr));
        merged.setGroupName(nvl(overrides.getGroupName(), groupName));

        merged.setOrder(nvl(overrides.getOrder(), order));
        merged.setFilter(nvl(overrides.isFilter(), filter));

        merged.setPivotColumn(nvl(overrides.isPivotColumn(), pivotColumn));
        merged.setPivotRow(nvl(overrides.isPivotRow(), pivotRow));
        merged.setPivotMeasure(nvl(overrides.isPivotMeasure(), pivotMeasure));

        return merged;
    }
    
    public static class Columns {
        public final String columnsName;
        public final List<GroupObjectEntity> columns;

        public Columns(String columnsName, List<GroupObjectEntity> columns) {
            this.columnsName = columnsName;
            this.columns = columns;
        }
    }
}
