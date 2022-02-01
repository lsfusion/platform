package lsfusion.server.logics.form.interactive.design.property;

import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.MouseInputEvent;
import lsfusion.interop.form.print.ReportFieldExtraType;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeSerializer;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.form.interactive.action.async.AsyncChange;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.async.AsyncSerializer;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.BaseComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.stat.print.design.ReportDrawField;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawExtraType;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.exec.db.table.MapKeysTable;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.interop.action.ServerResponse.CHANGE;
import static lsfusion.interop.action.ServerResponse.EDIT_OBJECT;
import static lsfusion.server.logics.form.struct.property.PropertyDrawExtraType.*;

public class PropertyDrawView extends BaseComponentView {

    public PropertyDrawEntity<?> entity;

    public Boolean changeOnSingleClick;
    public boolean hide;
    public String regexp;
    public String regexpMessage;
    public Long maxValue;
    public Boolean echoSymbols;
    public boolean noSort;
    public Compare defaultCompare;

    private int charWidth;
    private int charHeight;

    public Dimension valueSize;
    public Integer valueWidth;
    public Integer valueHeight;

    public Integer captionWidth;
    public Integer captionHeight;

    private Boolean valueFlex;

    public KeyInputEvent changeKey;
    public Integer changeKeyPriority;
    public Boolean showChangeKey;
    public MouseInputEvent changeMouse;
    public Integer changeMousePriority;

    public boolean drawAsync = false;

    public Format format;

    public Boolean focusable;

    public boolean panelCaptionVertical = false;
    public Boolean panelCaptionLast;
    public FlexAlignment panelCaptionAlignment;

    public boolean panelColumnVertical = false;

    public FlexAlignment valueAlignment;

    public LocalizedString caption;
    public boolean clearText;
    public boolean notSelectAll;
    public String toolTip;

    public boolean notNull;

    public Boolean sticky;

    @SuppressWarnings({"UnusedDeclaration"})
    public PropertyDrawView() {

    }

    public PropertyDrawView(PropertyDrawEntity entity) {
        super(entity.ID);
        this.entity = entity;
        setMargin(2);
        setSID("PROPERTY(" + entity.getSID() + ")");
    }
    
    public String getPropertyFormName() {
        return entity.getSID();
    }

    public String getIntegrationSID() {
        return entity.integrationSID;
    }

    public Type getType() {
        return entity.getType();
    }

    public boolean isProperty() {
        return entity.isProperty();
    }

    public int getValueWidth(FormEntity entity) {
        if(valueWidth != null)
            return valueWidth;

//        if(isAutoSize(entity)) {
//            if(!isProperty())
//                return -1;
//        }

        return -1;
    }

    public int getValueHeight(FormEntity entity) {
        if(valueHeight != null)
            return valueHeight;

        if(isAutoSize(entity)) {
            if(!isProperty()) // we want vertical size for action to be equal to text fields
                return -2;
        }

        return -1;
    }

    public int getCaptionWidth(FormEntity entity) {
        if(captionWidth != null)
            return captionWidth;

        return -1;
    }

    public int getCaptionHeight(FormEntity entity) {
        if(captionHeight != null)
            return captionHeight;

        return -1;
    }

    // we force optimistic async event scheme for external calls (since this calls assume that async push should exist)
    // for that purpose we have to send to client that type to do parsing, rendering, etc.
    public Type getExternalChangeType(ServerContext context) {
        AsyncEventExec asyncEventExec = entity.getAsyncEventExec(context.entity, context.securityPolicy, CHANGE, true);
        return asyncEventExec instanceof AsyncChange ? ((AsyncChange) asyncEventExec).changeType : null;
    }

    @Override
    public double getDefaultFlex(FormEntity formEntity) {
        ContainerView container = getLayoutParamContainer();
        if(((container != null && container.isHorizontal()) || entity.isList(formEntity)) && isHorizontalValueFlex())
            return -2; // flex = width
        return super.getDefaultFlex(formEntity);
    }

    @Override
    protected boolean isDefaultShrink(FormEntity formEntity, boolean explicit) {
        ContainerView container = getLayoutParamContainer();
        if(container != null && container.isHorizontal() && container.isWrap() && isHorizontalValueShrink())
            return true;
        return super.isDefaultShrink(formEntity, explicit);
    }

    @Override
    public FlexAlignment getDefaultAlignment(FormEntity formEntity) {
        ContainerView container = getLayoutParamContainer();
        if (container != null && !container.isHorizontal() && isHorizontalValueFlex())
            return FlexAlignment.STRETCH;
        return super.getDefaultAlignment(formEntity);
    }

    @Override
    protected boolean isDefaultAlignShrink(FormEntity formEntity, boolean explicit) {
        // actually not needed mostly since for STRETCH align shrink is set, but just in case
        ContainerView container = getLayoutParamContainer();
        if (container != null && !container.isHorizontal() && isHorizontalValueShrink())
            return true;
        return super.isDefaultAlignShrink(formEntity, explicit);
    }

    public Map<String, AsyncEventExec> getAsyncEventExec(ServerContext context) {
        Map<String, AsyncEventExec> asyncExecMap = new HashMap<>();
        for (String actionId : ServerResponse.events) {
            AsyncEventExec asyncEventExec = entity.getAsyncEventExec(context.entity, context.securityPolicy, actionId, false);
            if (asyncEventExec != null) {
                asyncExecMap.put(actionId, asyncEventExec);
            }
        }
        return asyncExecMap;
    }

    public LocalizedString getCaption() {
        return caption != null
                ? caption
                : entity.getCaption();
    }

    // we return to the client null, if we're sure that caption is always empty (so we don't need to draw label)
    public String getDrawCaption() {
        LocalizedString caption = getCaption();
        if(hasNoCaption(caption.isEmpty() ? null : caption, entity.getPropertyExtra(CAPTION)))
            return null;

        return ThreadLocalContext.localize(caption);
    }

    public static boolean hasNoCaption(LocalizedString caption, PropertyObjectEntity<?> propertyCaption) {
        return (caption == null && propertyCaption == null) || (propertyCaption != null && propertyCaption.property.isExplicitNull()); // isEmpty can be better, but we just want to emulate NULL to be like NULL caption
    }

    public boolean isNotNull() {
        return notNull || entity.isNotNull();
    }

    public boolean isSticky(FormEntity formEntity) {
        return entity.sticky != null ? entity.sticky : sticky != null ? sticky : isProperty() && entity.getPropertyObjectEntity().isValueUnique(entity.getToDraw(formEntity));
    }

    //Для Jasper'а экранируем кавычки
    public String getReportCaption() {
        LocalizedString caption = getCaption();
        return caption == null ? null : ThreadLocalContext.localize(caption).replace("\"", "\\\"");
    }

    public ReportDrawField getReportDrawField(int charWidth, int scale, Type type) {
        ReportDrawField reportField = new ReportDrawField(getPropertyFormName(), getReportCaption(), charWidth);

        setupGeometry(reportField, scale);
        setupColumnGroupObjects(reportField);

        PropertyDrawExtraType[] setupTypes = {CAPTION, FOOTER, BACKGROUND, FOREGROUND, IMAGE};
        for (PropertyDrawExtraType setupType : setupTypes) {
            setupExtra(reportField, setupType);
        }
        setupShowIf(reportField);
        
        reportField.pattern = getFormatPattern();

        type.fillReportDrawField(reportField);
        return reportField;
    }

    private void setupGeometry(ReportDrawField reportField, int scale) {
        Type type = getType();

        reportField.scale = scale;
        reportField.minimumWidth = type.getReportMinimumWidth() * scale;
        reportField.preferredWidth = type.getReportPreferredWidth() * scale;
        int reportCharWidth = getCharWidth();
        if (reportCharWidth != 0) {
            reportField.fixedCharWidth = reportCharWidth * scale;
        }
    }

    private void setupColumnGroupObjects(ReportDrawField reportField) {
        if (!entity.getColumnGroupObjects().isEmpty()) {
            reportField.hasColumnGroupObjects = true;
            reportField.columnGroupName = entity.columnsName;
        }
    }

    private void setupExtra(ReportDrawField field, PropertyDrawExtraType type) {
        ReportFieldExtraType reportType = type.getReportExtraType();
        if (entity.hasPropertyExtra(type)) {
            field.addExtraType(reportType);
            field.setExtraTypeClass(reportType, getPropertyClass(entity.getPropertyExtra(type)));
        } else {
            field.setExtraTypeClass(reportType, String.class);            
        }
    }
    
    private void setupShowIf(ReportDrawField field) {
        if (entity.hasPropertyExtra(PropertyDrawExtraType.SHOWIF)) {
            field.addExtraType(ReportFieldExtraType.SHOWIF);
            field.setExtraTypeClass(ReportFieldExtraType.SHOWIF, getPropertyClass(entity.getPropertyExtra(PropertyDrawExtraType.SHOWIF)));
        }
    }

    private Class getPropertyClass(PropertyObjectEntity<?> property) {
        ReportDrawField field = new ReportDrawField("", "", charWidth);
        Type type = property.property.getType();
        if (type != null) {
            type.fillReportDrawField(field);
        }
        return field.valueClass;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        outStream.writeBoolean(isAutoSize(pool.context.entity));
        outStream.writeBoolean(isBoxed(pool.context.entity));

        pool.writeString(outStream, getDrawCaption());
        pool.writeString(outStream, regexp);
        pool.writeString(outStream, regexpMessage);
        pool.writeLong(outStream, maxValue);
        outStream.writeBoolean(echoSymbols);
        outStream.writeBoolean(noSort);

        if(defaultCompare != null)
            defaultCompare.serialize(outStream);
        else if(Settings.get().isDefaultCompareForStringContains() && isProperty() && getType() instanceof StringClass)
            Compare.MATCH.serialize(outStream);
        else
            outStream.writeByte(-1);

        outStream.writeInt(getCharHeight());
        outStream.writeInt(getCharWidth());

        outStream.writeInt(getValueWidth(pool.context.entity));
        outStream.writeInt(getValueHeight(pool.context.entity));

        outStream.writeInt(getCaptionWidth(pool.context.entity));
        outStream.writeInt(getCaptionHeight(pool.context.entity));

        pool.writeObject(outStream, changeKey);
        pool.writeInt(outStream, changeKeyPriority);
        outStream.writeBoolean(showChangeKey);
        pool.writeObject(outStream, changeMouse);
        pool.writeInt(outStream, changeMousePriority);

        outStream.writeBoolean(drawAsync);

        pool.writeObject(outStream, format);

        outStream.writeBoolean(entity.isList(pool.context.view.entity));

        pool.writeObject(outStream, focusable);
        outStream.writeByte(entity.getEditType().serialize());

        outStream.writeBoolean(panelCaptionVertical);
        pool.writeObject(outStream, panelCaptionLast);
        pool.writeObject(outStream, panelCaptionAlignment);

        outStream.writeBoolean(panelColumnVertical);
        
        pool.writeObject(outStream, getValueAlignment());

        pool.writeObject(outStream, changeOnSingleClick);
        outStream.writeBoolean(hide);

        //entity часть
        if(isProperty())
            TypeSerializer.serializeType(outStream, getType());
        else {
            outStream.writeByte(1);
            outStream.writeByte(DataType.ACTION);
        }

        Type externalChangeType = getExternalChangeType(pool.context);
        outStream.writeBoolean(externalChangeType != null);
        if (externalChangeType != null) {
            TypeSerializer.serializeType(outStream, externalChangeType);
        }

        Map<String, AsyncEventExec> asyncExecMap = getAsyncEventExec(pool.context);
        outStream.writeInt(asyncExecMap.size());
        for (Map.Entry<String, AsyncEventExec> entry : asyncExecMap.entrySet()) {
            pool.writeString(outStream, entry.getKey());
            AsyncSerializer.serializeEventExec(entry.getValue(), outStream);
        }

        outStream.writeBoolean(entity.askConfirm);
        if(entity.askConfirm)
            pool.writeString(outStream, getAskConfirmMessage());
        outStream.writeBoolean(hasEditObjectAction(pool.context));
        outStream.writeBoolean(hasChangeAction(pool.context));
        outStream.writeBoolean(entity.hasDynamicImage);

        ActionOrPropertyObjectEntity<?, ?> debug = entity.getDebugProperty(); // only for tooltip
        ActionOrProperty<?> debugBinding = entity.getDebugBindingProperty(); // only for tooltip

        pool.writeString(outStream, debugBinding.getNamespace());
        pool.writeString(outStream, getSID());
        pool.writeString(outStream, debugBinding.getCanonicalName());
        pool.writeString(outStream, getPropertyFormName());
        pool.writeString(outStream, getIntegrationSID());
        pool.writeString(outStream, toolTip);
        pool.serializeObject(outStream, pool.context.view.getGroupObject(entity.getToDraw(pool.context.view.entity)));

        pool.writeString(outStream, entity.columnsName);
        ImOrderSet<GroupObjectEntity> columnGroupObjects = entity.getColumnGroupObjects();
        outStream.writeInt(columnGroupObjects.size());
        for (GroupObjectEntity groupEntity : columnGroupObjects) {
            pool.serializeObject(outStream, pool.context.view.getGroupObject(groupEntity));
        }

        outStream.writeBoolean(isProperty());
        outStream.writeBoolean(clearText);
        outStream.writeBoolean(notSelectAll);

        // for pivoting
        pool.writeString(outStream, entity.formula);
        if(entity.formula != null) {
            ImList<PropertyDrawEntity> formulaOperands = entity.formulaOperands;
            outStream.writeInt(formulaOperands.size());
            for (PropertyDrawEntity formulaOperand : formulaOperands)
                pool.serializeObject(outStream, pool.context.view.get(formulaOperand));
        }

        pool.writeString(outStream, entity.aggrFunc != null ? entity.aggrFunc.toString() : null);
        outStream.writeInt(entity.lastAggrColumns.size());
        outStream.writeBoolean(entity.lastAggrDesc);

        pool.serializeObject(outStream, pool.context.view.get(entity.quickFilterProperty));

        MapKeysTable<? extends PropertyInterface> mapTable = isProperty() ?
                        ((Property<?>)debugBinding).mapTable : null;
        pool.writeString(outStream, mapTable != null ? mapTable.table.getName() : null);

        ImMap<PropertyInterface, ValueClass> interfaceClasses = (ImMap<PropertyInterface, ValueClass>) debug.property.getInterfaceClasses(ClassType.formPolicy);
        ImMap<PropertyInterface, ObjectEntity> interfaceEntities = (ImMap<PropertyInterface, ObjectEntity>) debug.mapping;
        outStream.writeInt(debug.property.interfaces.size());
        for (PropertyInterface iFace : debug.property.interfaces) {
            pool.writeString(outStream, interfaceEntities.get(iFace).toString());
            
            ValueClass paramClass = interfaceClasses.get(iFace);
            outStream.writeBoolean(paramClass != null);
            if (paramClass != null) {
                paramClass.serialize(outStream);
            }
        }

        if(debug instanceof PropertyObjectEntity)
            ((PropertyObjectEntity<?>)debug).property.getValueClass(ClassType.formPolicy).serialize(outStream);
        else
            outStream.writeByte(DataType.ACTION);
        
        pool.writeString(outStream, entity.customRenderFunction);
        pool.writeString(outStream, entity.customEditorFunction);

        pool.writeString(outStream, entity.eventID);

        pool.writeString(outStream, debug.getCreationScript());
        pool.writeString(outStream, debug.getCreationPath());
        pool.writeString(outStream, debug.getPath());
        pool.writeString(outStream, entity.getFormPath());

        pool.writeString(outStream, entity.getMouseBinding());

        ImMap<KeyStroke, String> keyBindings = entity.getKeyBindings();
        outStream.writeInt(keyBindings == null ? 0 : keyBindings.size());
        if (keyBindings != null) {
            for (int i=0,size=keyBindings.size();i<size;i++) {
                pool.writeObject(outStream, keyBindings.getKey(i));
                pool.writeString(outStream, keyBindings.getValue(i));
            }
        }

        OrderedMap<String, LocalizedString> contextMenuBindings = filterContextMenuItems(entity.getContextMenuBindings(), pool.context);
        outStream.writeInt(contextMenuBindings == null ? 0 : contextMenuBindings.size());
        if (contextMenuBindings != null) {
            for (int i = 0; i < contextMenuBindings.size(); ++i) {
                pool.writeString(outStream, contextMenuBindings.getKey(i));
                pool.writeString(outStream, ThreadLocalContext.localize(contextMenuBindings.getValue(i)));
            }
        }

        outStream.writeBoolean(isNotNull());
        outStream.writeBoolean(isSticky(pool.context.view.entity));
        outStream.writeBoolean(entity.getPropertyExtra(PropertyDrawExtraType.FOOTER) != null);
    }

    private OrderedMap<String, LocalizedString> filterContextMenuItems(OrderedMap<String, LocalizedString> contextMenuBindings, ServerContext context) {
        if (contextMenuBindings == null || contextMenuBindings.size() == 0) {
            return null;
        }

        OrderedMap<String, LocalizedString> contextMenuItems = new OrderedMap<>();
        for (int i = 0; i < contextMenuBindings.size(); ++i) {
            String actionSID = contextMenuBindings.getKey(i);
            LocalizedString caption = contextMenuBindings.getValue(i);
            ActionObjectEntity<?> eventAction = entity.getEventAction(actionSID, context.entity, context.securityPolicy);
            if (eventAction != null && context.securityPolicy.checkPropertyViewPermission(eventAction.property)) {
                contextMenuItems.put(actionSID, caption);
            }
        }
        return contextMenuItems;
    }

    public String getFormatPattern() {
        if (format != null) {
            if (format instanceof DecimalFormat)
                return ((DecimalFormat) format).toPattern();
            else if (format instanceof SimpleDateFormat)
                return ((SimpleDateFormat) format).toPattern();
        }
        return null;
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        autoSize = inStream.readBoolean();
        boxed = inStream.readBoolean();

        caption = LocalizedString.create(pool.readString(inStream));
        regexp = pool.readString(inStream);
        regexpMessage = pool.readString(inStream);
        maxValue = pool.readLong(inStream);
        echoSymbols = inStream.readBoolean();
        noSort = inStream.readBoolean();
        defaultCompare = Compare.deserialize(inStream);

        setCharHeight(inStream.readInt());
        setCharWidth(inStream.readInt());
        setValueSize(pool.readObject(inStream));

        setCaptionWidth(inStream.readInt());
        setCaptionHeight(inStream.readInt());

        changeKey = pool.readObject(inStream);
        changeKeyPriority = pool.readInt(inStream);
        showChangeKey = inStream.readBoolean();
        changeMouse = pool.readObject(inStream);
        changeMousePriority = pool.readInt(inStream);

        format = pool.readObject(inStream);

        focusable = pool.readObject(inStream);

        panelCaptionVertical = inStream.readBoolean();
        panelCaptionLast = pool.readObject(inStream);
        panelCaptionAlignment = pool.readObject(inStream);

        panelColumnVertical = inStream.readBoolean();

        valueAlignment = pool.readObject(inStream);

        changeOnSingleClick = pool.readObject(inStream);
        hide = inStream.readBoolean();

        entity = pool.context.entity.getPropertyDraw(inStream.readInt());
    }

    @Override
    public String toString() {
        return ThreadLocalContext.localize(getCaption()) + " " + super.toString();
    }

    public int getCharHeight() {
        return charHeight;
    }

    public void setCharHeight(int charHeight) {
        this.charHeight = charHeight;
    }

    public int getCharWidth() {
        return charWidth;
    }

    public void setCharWidth(int minimumCharWidth) {
        this.charWidth = minimumCharWidth;
    }

    public Dimension getValueSize() {
        return valueSize;
    }

    public void setValueSize(Dimension valueSize) {
        this.valueWidth = valueSize.width;
        this.valueHeight = valueSize.height;
    }

    public void setValueWidth(Integer valueWidth) {
        this.valueWidth = valueWidth;
    }

    public void setValueHeight(Integer valueHeight) {
        this.valueHeight = valueHeight;
    }

    public void setCaptionWidth(Integer captionWidth) {
        this.captionWidth = captionWidth;
    }

    public void setCaptionHeight(Integer captionHeight) {
        this.captionHeight = captionHeight;
    }

    public Boolean getValueFlex() {
        return valueFlex;
    }

    public void setValueFlex(Boolean valueFlex) {
        this.valueFlex = valueFlex;
    }

    public boolean isHorizontalValueFlex() {
        if(valueFlex != null)
            return valueFlex;
        Type type;
        return isProperty() && (type = getType()) != null && type.isFlex();
    }

    public boolean isHorizontalValueShrink() {
//        if(valueFlex != null)
//            return valueFlex;
        Type type;
        return isProperty() && (type = getType()) != null && type.isFlex();
    }

    public String getAskConfirmMessage() {
        assert entity.askConfirm;
        if (entity.askConfirmMessage != null)
            return entity.askConfirmMessage;
        
        LocalizedString msg;
        if (isProperty()) {
            msg = LocalizedString.create("{form.instance.do.you.really.want.to.edit.property}");
        } else {
            msg = LocalizedString.create("{form.instance.do.you.really.want.to.take.action}");
        }
        LocalizedString caption = getCaption();
        if (!caption.isEmpty()) {
            msg = LocalizedString.concatList(msg, " \"", caption, "\"?");
        }

        return ThreadLocalContext.localize(msg);
    }
    
    public boolean hasChangeAction(ServerContext context) {
        return entity.getEventAction(CHANGE, context.entity, context.securityPolicy) != null;
    }
    public boolean hasEditObjectAction(ServerContext context) {
        return entity.getEventAction(EDIT_OBJECT, context.entity, context.securityPolicy) != null;
    }

    public FlexAlignment getValueAlignment() {
        if (valueAlignment == null && isProperty()) {
            Type type = getType();
            if(type != null)
                return type.getValueAlignment();
            return FlexAlignment.START;
        }
        return valueAlignment;
    }

    public Boolean boxed;

    public boolean isBoxed(FormEntity entity) {
        if(boxed != null)
            return boxed;

        return true;
    }

    public Boolean autoSize;

    public boolean isAutoSize(FormEntity entity) {
        if(autoSize != null)
            return autoSize;

        return isCustom() || !isProperty();
    }

    protected boolean isCustom() {
        return entity.customRenderFunction != null;
    }
}