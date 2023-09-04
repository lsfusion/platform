package lsfusion.server.logics.form.interactive.design.property;

import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.base.AppServerImage;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.MouseInputEvent;
import lsfusion.interop.form.print.ReportFieldExtraType;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeSerializer;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.classes.data.integral.IntegralClass;
import lsfusion.server.logics.classes.data.time.DateClass;
import lsfusion.server.logics.classes.data.time.DateTimeClass;
import lsfusion.server.logics.classes.data.time.TimeClass;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.async.AsyncInput;
import lsfusion.server.logics.form.interactive.action.async.AsyncNoWaitExec;
import lsfusion.server.logics.form.interactive.action.async.AsyncSerializer;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
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

import static lsfusion.base.BaseUtils.nvl;
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

    public int charWidth;
    private int charHeight;

    public Dimension valueSize;
    public Integer valueWidth;
    public Integer valueHeight;

    public Integer captionWidth;
    public Integer captionHeight;

    private Boolean valueFlex;

    public String tag;
    public String valueElementClass;
    public String captionElementClass;

    public KeyInputEvent changeKey;
    public Integer changeKeyPriority;
    public Boolean showChangeKey;
    public MouseInputEvent changeMouse;
    public Integer changeMousePriority;

    public boolean drawAsync = false;

    public String pattern;

    public Boolean focusable;

    public boolean panelCaptionVertical = false;
    public Boolean panelCaptionLast;
    public FlexAlignment panelCaptionAlignment;

    public boolean panelColumnVertical = false;

    public FlexAlignment valueAlignment;

    public LocalizedString comment;
    public String commentElementClass;
    public boolean panelCommentVertical;
    public boolean panelCommentFirst;
    public FlexAlignment panelCommentAlignment;

    public LocalizedString placeholder;

    public LocalizedString caption;
    public AppServerImage.Reader image;
    public boolean clearText;
    public boolean notSelectAll;
    public String toolTip;

    public Boolean toolbar;

    public boolean notNull;

    public Boolean sticky;
    public Boolean sync;

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
        return entity.getIntegrationSID();
    }

    public Type getType(FormInstanceContext context) {
        return entity.getType(context);
    }

    public boolean isProperty(FormInstanceContext context) {
        return entity.isProperty(context);
    }

    public int getValueWidth(FormInstanceContext context) {
        if(valueWidth != null)
            return valueWidth;

        return -1;
    }

    public int getValueHeight(FormInstanceContext context) {
        if(valueHeight != null)
            return valueHeight;

        if(isAutoSize(context)) {
            if(!isProperty(context) && !entity.hasDynamicImage()) // we want vertical size for action to be equal to text fields
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
    public Type getExternalChangeType(FormInstanceContext context) {
        return getChangeType(context, true);
    }

    public Type getChangeType(FormInstanceContext context, boolean externalChange) {
        AsyncEventExec asyncEventExec = entity.getAsyncEventExec(context, CHANGE, externalChange);
        return asyncEventExec instanceof AsyncInput ? ((AsyncInput) asyncEventExec).changeType : null;
    }

    @Override
    public double getDefaultFlex(FormInstanceContext context) {
        ContainerView container = getLayoutParamContainer();
        if(((container != null && container.isHorizontal()) || entity.isList(context)) && isHorizontalValueFlex(context))
            return -2; // flex = width
        return super.getDefaultFlex(context);
    }

    @Override
    protected boolean isDefaultShrink(FormInstanceContext context, boolean explicit) {
        ContainerView container = getLayoutParamContainer();
        if(container != null && container.isHorizontal() && container.isWrap() && isHorizontalValueShrink(context))
            return true;
        return super.isDefaultShrink(context, explicit);
    }

    @Override
    public FlexAlignment getDefaultAlignment(FormInstanceContext context) {
        ContainerView container = getLayoutParamContainer();
        if (container != null && !container.isHorizontal() && isHorizontalValueFlex(context))
            return FlexAlignment.STRETCH;
        return super.getDefaultAlignment(context);
    }

    @Override
    protected boolean isDefaultAlignShrink(FormInstanceContext context, boolean explicit) {
        // actually not needed mostly since for STRETCH align shrink is set, but just in case
        ContainerView container = getLayoutParamContainer();
        if (container != null && !container.isHorizontal() && isHorizontalValueShrink(context))
            return true;
        return super.isDefaultAlignShrink(context, explicit);
    }

    private String getCustomRenderFunction(FormInstanceContext context) {
        return entity.getCustomRenderFunction(context);
    }

    public static final boolean defaultSync = true;

    public Map<String, AsyncEventExec> getAsyncEventExec(FormInstanceContext context) {
        Map<String, AsyncEventExec> asyncExecMap = new HashMap<>();
        Boolean sync = getSync();
        if(sync == null || !sync) { // if WAIT we don't want any asyncs
            for (String actionId : entity.getAllPropertyEventActions(context)) {
                AsyncEventExec asyncEventExec = entity.getAsyncEventExec(context, actionId, false);
                if (asyncEventExec == null && (sync != null || !defaultSync)) // explicit NOWAIT or not default sync
                    asyncEventExec = AsyncNoWaitExec.instance;
                if (asyncEventExec != null)
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

    public AppServerImage.AutoName getAutoName() {
        return AppServerImage.getAutoName(this::getCaption, entity.getInheritedProperty()::getName);
    }

    public void setImage(String image) {
        this.image = AppServerImage.createPropertyImage(image, this);
    }

    public AppServerImage getImage(FormInstanceContext context) {
        if(this.image != null)
            return this.image.get(context);

        AppServerImage.Reader entityImage = entity.getImage();
        if(entityImage != null)
            return entityImage.get(context);

        return getDefaultImage(context);
    }

    private AppServerImage getDefaultImage(FormInstanceContext context) {
        return ActionOrProperty.getDefaultImage(AppServerImage.AUTO, getAutoName(), Settings.get().getDefaultPropertyImageRankingThreshold(), Settings.get().isDefaultPropertyImage(), context);
    }

    // we return to the client null, if we're sure that caption is always empty (so we don't need to draw label)
    public String getDrawCaption() {
        LocalizedString caption = getCaption();
        if(hasNoCaption(caption, entity.getPropertyExtra(CAPTION)))
            return null;

        return ThreadLocalContext.localize(caption);
    }

    public static boolean hasNoCaption(LocalizedString caption, PropertyObjectEntity<?> propertyCaption) {
        return ((caption == null || caption.isEmpty()) && propertyCaption == null) || (propertyCaption != null && propertyCaption.property.isExplicitNull()); // isEmpty can be better, but we just want to emulate NULL to be like NULL caption
    }

    public boolean isNotNull() {
        return notNull || entity.isNotNull();
    }

    public boolean isSticky(FormInstanceContext context) {
        if (entity.sticky != null)
            return entity.sticky;

        if (sticky != null)
            return sticky;

        if(!isProperty(context))
            return false;

        if(entity.getAssertProperty(context).isValueUnique(entity.getToDraw(context.entity), Property.ValueUniqueType.STICKY))
            return true;

        if(ThreadLocalContext.getBaseLM().isRecognize(entity.getInheritedProperty()))
            return true;

        return false;
    }

    public Boolean getSync() {
        return nvl(entity.sync, sync);
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
        
        reportField.pattern = getPattern();

        type.fillReportDrawField(reportField);
        return reportField;
    }

    private void setupGeometry(ReportDrawField reportField, int scale) {
        reportField.scale = scale;

        if(entity.isStaticProperty()) {
            Type type = entity.getStaticType();
            reportField.minimumWidth = type.getReportMinimumWidth() * scale;
            reportField.preferredWidth = type.getReportPreferredWidth() * scale;
        }
        int reportCharWidth = charWidth;
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

        outStream.writeBoolean(isAutoSize(pool.context));

        pool.writeString(outStream, getDrawCaption());
        AppServerImage.serialize(getImage(pool.context), outStream, pool);
        pool.writeString(outStream, regexp);
        pool.writeString(outStream, regexpMessage);
        pool.writeLong(outStream, maxValue);
        outStream.writeBoolean(echoSymbols);
        outStream.writeBoolean(noSort);

        Compare defaultCompare = getDefaultCompare(pool.context);
        if(defaultCompare != null)
            defaultCompare.serialize(outStream);
        else
            outStream.writeByte(-1);

        outStream.writeInt(getCharHeight());
        outStream.writeInt(getCharWidth(pool.context));

        outStream.writeInt(getValueWidth(pool.context));
        outStream.writeInt(getValueHeight(pool.context));

        outStream.writeInt(getCaptionWidth(pool.context.entity));
        outStream.writeInt(getCaptionHeight(pool.context.entity));

        pool.writeObject(outStream, changeKey);
        pool.writeInt(outStream, changeKeyPriority);
        outStream.writeBoolean(showChangeKey);
        pool.writeObject(outStream, changeMouse);
        pool.writeInt(outStream, changeMousePriority);

        outStream.writeBoolean(drawAsync);

        pool.writeObject(outStream, getFormat(pool.context));

        outStream.writeBoolean(entity.isList(pool.context));

        pool.writeObject(outStream, focusable);
        outStream.writeByte(entity.getEditType().serialize());

        outStream.writeBoolean(panelCaptionVertical);
        pool.writeObject(outStream, panelCaptionLast);
        pool.writeObject(outStream, panelCaptionAlignment);

        outStream.writeBoolean(panelColumnVertical);
        
        pool.writeObject(outStream, getValueAlignment(pool.context));

        pool.writeString(outStream, ThreadLocalContext.localize(comment));
        pool.writeString(outStream, commentElementClass);
        outStream.writeBoolean(panelCommentVertical);
        outStream.writeBoolean(panelCommentFirst);
        pool.writeObject(outStream, panelCommentAlignment);

        pool.writeString(outStream, ThreadLocalContext.localize(placeholder));

        pool.writeObject(outStream, getChangeOnSingleClick(pool.context));
        outStream.writeBoolean(hide);

        //entity часть
        if(isProperty(pool.context)) {
            Type type = getType(pool.context);
            // however this hack helps only in some rare cases, since baseType is used in a lot of places
            if(type == null) // temporary hack, will not be needed when expression will be automatically patched with "IS Class"
                type = IntegerClass.instance;
            TypeSerializer.serializeType(outStream, type);
        } else {
            outStream.writeByte(1);
            outStream.writeByte(DataType.ACTION);
        }

        pool.writeString(outStream, getTag(pool.context));
        pool.writeString(outStream, getValueElementClass(pool.context));
        pool.writeString(outStream, getCaptionElementClass(pool.context));
        pool.writeBoolean(outStream, hasToolbar(pool.context));

        Type externalChangeType = getExternalChangeType(pool.context);
        outStream.writeBoolean(externalChangeType != null);
        if (externalChangeType != null) {
            TypeSerializer.serializeType(outStream, externalChangeType);
        }

        Map<String, AsyncEventExec> asyncExecMap = getAsyncEventExec(pool.context);
        outStream.writeInt(asyncExecMap.size());
        for (Map.Entry<String, AsyncEventExec> entry : asyncExecMap.entrySet()) {
            pool.writeString(outStream, entry.getKey());
            AsyncSerializer.serializeEventExec(entry.getValue(), pool.context, outStream);
        }

        outStream.writeBoolean(entity.askConfirm);
        if(entity.askConfirm)
            pool.writeString(outStream, getAskConfirmMessage(pool.context));
        outStream.writeBoolean(hasEditObjectAction(pool.context));
        outStream.writeBoolean(hasChangeAction(pool.context));
        outStream.writeBoolean(entity.hasDynamicImage());

        ActionOrProperty inheritedProperty = entity.getInheritedProperty();
        outStream.writeBoolean(inheritedProperty instanceof Property && ((Property<?>) inheritedProperty).disableInputList);

        ActionOrPropertyObjectEntity<?, ?> debug = entity.getDebugActionOrProperty(pool.context); // only for tooltip
        ActionOrProperty<?> debugBinding = entity.getDebugBindingProperty(); // only for tooltip

        pool.writeString(outStream, debugBinding.getNamespace());
        pool.writeString(outStream, getSID());
        pool.writeString(outStream, debugBinding.getCanonicalName());
        pool.writeString(outStream, getPropertyFormName());
        pool.writeString(outStream, getIntegrationSID());
        pool.writeString(outStream, toolTip);
        pool.serializeObject(outStream, pool.context.view.getGroupObject(entity.getToDraw(pool.context.entity)));

        pool.writeString(outStream, entity.columnsName);
        ImOrderSet<GroupObjectEntity> columnGroupObjects = entity.getColumnGroupObjects();
        outStream.writeInt(columnGroupObjects.size());
        for (GroupObjectEntity groupEntity : columnGroupObjects) {
            pool.serializeObject(outStream, pool.context.view.getGroupObject(groupEntity));
        }

        outStream.writeBoolean(isProperty(pool.context));
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

        MapKeysTable<? extends PropertyInterface> mapTable = isProperty(pool.context) ?
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

        if(debug instanceof PropertyObjectEntity) {
            ValueClass valueClass = ((PropertyObjectEntity<?>) debug).property.getValueClass(ClassType.formPolicy);
            outStream.writeBoolean(valueClass != null);
            if(valueClass != null)
                valueClass.serialize(outStream);
        } else {
            outStream.writeBoolean(true);
            outStream.writeByte(DataType.ACTION);
        }
        
        pool.writeString(outStream, getCustomRenderFunction(pool.context));

        pool.writeString(outStream, entity.eventID);

        pool.writeString(outStream, debug.getCreationScript());
        pool.writeString(outStream, debug.getCreationPath());
        pool.writeString(outStream, debug.getPath());
        pool.writeString(outStream, entity.getFormPath());

        pool.writeString(outStream, entity.getMouseBinding(pool.context));

        ImMap<KeyStroke, String> keyBindings = entity.getKeyBindings(pool.context);
        outStream.writeInt(keyBindings == null ? 0 : keyBindings.size());
        if (keyBindings != null) {
            for (int i=0,size=keyBindings.size();i<size;i++) {
                pool.writeObject(outStream, keyBindings.getKey(i));
                pool.writeString(outStream, keyBindings.getValue(i));
            }
        }

        OrderedMap<String, LocalizedString> contextMenuBindings = filterContextMenuItems(entity.getContextMenuBindings(pool.context), pool.context);
        outStream.writeInt(contextMenuBindings == null ? 0 : contextMenuBindings.size());
        if (contextMenuBindings != null) {
            for (int i = 0; i < contextMenuBindings.size(); ++i) {
                pool.writeString(outStream, contextMenuBindings.getKey(i));
                pool.writeString(outStream, ThreadLocalContext.localize(contextMenuBindings.getValue(i)));
            }
        }

        outStream.writeBoolean(isNotNull());
        outStream.writeBoolean(isSticky(pool.context));
        outStream.writeBoolean(entity.getPropertyExtra(PropertyDrawExtraType.FOOTER) != null);
    }

    private OrderedMap<String, LocalizedString> filterContextMenuItems(OrderedMap<String, LocalizedString> contextMenuBindings, FormInstanceContext context) {
        if (contextMenuBindings == null || contextMenuBindings.size() == 0) {
            return null;
        }

        OrderedMap<String, LocalizedString> contextMenuItems = new OrderedMap<>();
        for (int i = 0; i < contextMenuBindings.size(); ++i) {
            String actionSID = contextMenuBindings.getKey(i);
            LocalizedString caption = contextMenuBindings.getValue(i);
            ActionObjectEntity<?> eventAction = entity.getCheckedEventAction(actionSID, context);
            if (eventAction != null && context.securityPolicy.checkPropertyViewPermission(eventAction.property)) {
                contextMenuItems.put(actionSID, caption);
            }
        }
        return contextMenuItems;
    }

    public String getPattern() {
        return pattern;
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

        pattern = pool.readObject(inStream);

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

    public Compare getDefaultCompare(FormInstanceContext context) {
        if(defaultCompare != null)
            return defaultCompare;

        if(isProperty(context)) {
            Type type = getType(context);
            if (type != null) {
                return type.getDefaultCompare();
            }
        }

        return null;
    }

    public int getCharHeight() {
        return charHeight;
    }

    public void setCharHeight(int charHeight) {
        this.charHeight = charHeight;
    }

    public int getCharWidth(FormInstanceContext context) {
        PropertyDrawEntity.Select select = entity.getSelectProperty(context);
        if(select != null) {
            if(select.elementType.equals("Input"))
                return (charWidth != 0 ? charWidth : select.length / select.count) * 4;

            if (!entity.isList(context) && isAutoSize(context))
                return 0;

            if (select.elementType.startsWith("Button"))
                return (charWidth != 0 && !select.actual ? charWidth * select.count : select.length) + select.count * (select.elementType.startsWith("ButtonGroup") ? 4 : 6); // couple of symbols for padding
        }

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

    public boolean isHorizontalValueFlex(FormInstanceContext context) {
        if(valueFlex != null)
            return valueFlex;
        Type type;
        return isProperty(context) && (type = getType(context)) != null && type.isFlex();
    }

    public boolean isHorizontalValueShrink(FormInstanceContext context) {
//        if(valueFlex != null)
//            return valueFlex;
        Type type;
        return isProperty(context) && (type = getType(context)) != null && type.isFlex();
    }

    public boolean isDefaultShrinkOverflowVisible(FormInstanceContext context) {
        return !entity.isList(context) && (isTagInput(context) || entity.getSelectProperty(context) != null); // inputs (incl. multi) + radio + select use shadows
    }

    public String getAskConfirmMessage(FormInstanceContext context) {
        assert entity.askConfirm;
        if (entity.askConfirmMessage != null)
            return entity.askConfirmMessage;
        
        LocalizedString msg;
        if (isProperty(context)) {
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
    
    public boolean hasChangeAction(FormInstanceContext context) {
        return hasAction(context, CHANGE);
    }
    public boolean hasEditObjectAction(FormInstanceContext context) {
        return hasAction(context, EDIT_OBJECT);
    }

    public boolean hasAction(FormInstanceContext context, String actionID) {
        ActionObjectEntity<?> eventAction = entity.getCheckedEventAction(actionID, context);
        if(eventAction != null)
            return eventAction.property.hasFlow(ChangeFlowType.ANYEFFECT);
        return false;
    }
    public boolean hasFlow(FormInstanceContext context, ChangeFlowType type) {
        ActionObjectEntity<?> eventAction = entity.getCheckedEventAction(CHANGE, context);
        if(eventAction != null)
            return eventAction.property.hasFlow(type);
        return false;
    }


    private Format format;
    private boolean formatCalculated;
    @ManualLazy
    public Format getFormat(FormInstanceContext context) {
        if(pattern != null) {
            if(!formatCalculated) {
                if(isProperty(context)) {
                    Type type = getType(context);
                    if (type instanceof IntegralClass) {
                        format = new DecimalFormat(pattern);
                    } else if (type instanceof DateClass || type instanceof TimeClass || type instanceof DateTimeClass) {
                        format = new SimpleDateFormat(pattern);
                    }
                }
                formatCalculated = true;
            }
            return format;
        }

        return null;
    }

    public Boolean getChangeOnSingleClick(FormInstanceContext context) {
        if(changeOnSingleClick != null)
            return changeOnSingleClick;

        if(isProperty(context)) {
            if (getType(context) instanceof LogicalClass)
                return Settings.get().getChangeBooleanOnSingleClick();
        } else
            return Settings.get().getChangeActionOnSingleClick();

        return null;
    }

    public FlexAlignment getValueAlignment(FormInstanceContext context) {
        if (valueAlignment == null && isProperty(context)) {
            Type type = getType(context);
            if(type != null)
                return type.getValueAlignment();
            return FlexAlignment.START;
        }
        return valueAlignment;
    }

    public String getTag(FormInstanceContext context) {
        if(tag != null)
            return tag;

        if(isCustom(context)) {
            PropertyDrawEntity.Select select = entity.getSelectProperty(context);
            if (select != null && select.elementType.equals("Dropdown") && Settings.get().isNoToolbarForSelectDropdownInPanel())
                return "select";
            return null;
        }

        Type changeType = getChangeType(context, false);
        if (isProperty(context)) {
            Type type = getType(context);
            if((type != null && changeType != null && type.getCompatible(changeType) != null &&
                    type.useInputTag(!entity.isList(context), context.useBootstrap,
                            entity.getPropertyExtra(BACKGROUND) != null || design.background != null))
                    || placeholder != null)
                return "input";

            if(isLink(context) && !hasFlow(context, ChangeFlowType.INPUT))
                return "a";
        } else {
            if(changeType == null && hasFlow(context, ChangeFlowType.ANYEFFECT))
                return "button";
        }

        return null;
    }

    private boolean isLink(FormInstanceContext context) {
        return hasFlow(context, ChangeFlowType.INTERACTIVEFORM) && !hasFlow(context, ChangeFlowType.READONLYCHANGE);
    }

    public String getCaptionElementClass(FormInstanceContext context) {
        if (captionElementClass != null)
            return captionElementClass;

        if(isProperty(context)) {
            String valueElementClass = getValueElementClass(context);
            // shortcut for the toggle button checkbox
            if(valueElementClass != null && valueElementClass.contains("btn-check"))
                return "btn btn-outline-primary";

            if (valueElementClass == null && isSimpleText(context))
                return "text-secondary";
        }

        return null;
    }

    @Override
    protected String getDefaultElementClass(FormInstanceContext context) {
        if(isProperty(context)) {
            Type type = getType(context);
            if (type instanceof LogicalClass && entity.isPredefinedSwitch())
                return "form-switch";
        }

        return null;
    }

    public String getValueElementClass(FormInstanceContext context) {
        if(valueElementClass != null)
            return valueElementClass;

        if (isProperty(context)) {
            if(isSimpleText(context)) {
                // if we're in panel and there is no decoration, nor other styling, nor custom view, making label gray to distinguish it from the value
                if(!context.useBootstrap)
                    return "form-control";

                if (hasFlow(context, ChangeFlowType.INPUT))
                    return "form-control";

                if (hasChangeAction(context))
                    return "btn btn-light";
            }
        } else {
            if(hasFlow(context, ChangeFlowType.PRIMARY))
                return "btn-primary";

            if(isLink(context))
                return "btn-link text-decoration-none";

            if(hasFlow(context, ChangeFlowType.ANYEFFECT))
                return "btn-outline-secondary";
        }

        return null;
    }

    private boolean isSimpleText(FormInstanceContext context) {
        return getTag(context) == null && !entity.isList(context) && !isCustom(context);
    }

    public boolean hasToolbar(FormInstanceContext context) {
        if(toolbar != null)
            return toolbar;

        if(isCustom(context) && entity.getSelectProperty(context) == null) // we want loading for select props, (entity.isReadOnly(context) || !hasChangeAction(context)) the problem of using hasChangeAction is that for JSON property it is always generated but it's impossible to understand if it is used
            return false;

        if(!isProperty(context))
            return true;

        Type type = getType(context);
        if(type != null)
            return type.hasToolbar(!entity.isList(context) && isTagInput(context));

        return true;
    }

    private boolean isTagInput(FormInstanceContext context) {
        String tag = getTag(context);
        return tag != null && tag.equals("input");
    }

    public Boolean boxed;

    public Boolean autoSize;

    public boolean isAutoSize(FormInstanceContext context) {
        if(autoSize != null)
            return autoSize;

        return isCustom(context) || !isProperty(context);
    }

    protected boolean isCustom(FormInstanceContext context) {
        return getCustomRenderFunction(context) != null;
    }
}