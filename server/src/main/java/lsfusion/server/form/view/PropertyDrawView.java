package lsfusion.server.form.view;

import lsfusion.base.OrderedMap;
import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.Compare;
import lsfusion.interop.Data;
import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.interop.form.screen.ExternalScreen;
import lsfusion.interop.form.screen.ExternalScreenConstraints;
import lsfusion.server.Settings;

import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeSerializer;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.view.report.ReportDrawField;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.table.MapKeysTable;
import lsfusion.server.serialization.SerializationType;
import lsfusion.server.serialization.ServerContext;
import lsfusion.server.serialization.ServerSerializationPool;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Map;

import static lsfusion.interop.form.ServerResponse.CHANGE;
import static lsfusion.interop.form.ServerResponse.EDIT_OBJECT;

public class PropertyDrawView extends ComponentView {

    public PropertyDrawEntity<?> entity;

    public boolean panelCaptionAfter;
    public boolean editOnSingleClick;
    public boolean hide;
    public String regexp;
    public String regexpMessage;
    public Long maxValue;
    public Boolean echoSymbols;
    public boolean noSort;
    public Compare defaultCompare;

    private int numRowHeight;
    public Dimension valueSize;
    private int charWidth;
    private Boolean valueFlex;

    public KeyStroke changeKey;
    public Boolean showChangeKey;

    public boolean drawAsync = false;

    public Format format;

    public Boolean focusable;

    public boolean panelCaptionAbove = false;

    public ExternalScreen externalScreen;
    public ExternalScreenConstraints externalScreenConstraints = new ExternalScreenConstraints();

    public LocalizedString caption;
    public boolean clearText;
    public boolean notSelectAll;
    public String toolTip;

    public boolean notNull;

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

    public Type getType() {
        return entity.getType();
    }
    
    public boolean isCalcProperty() {
        return entity.isCalcProperty();
    }

    public Type getChangeType(ServerContext context) {
        return entity.getRequestInputType(context.securityPolicy);
    }
    
    public Type getChangeWYSType(ServerContext context) {
        return entity.getWYSRequestInputType(context.securityPolicy);
    }

    @Override
    public double getBaseDefaultFlex(FormEntity formEntity) {
        ContainerView container = getContainer();
        if(((container != null && container.isHorizontal()) || entity.isGrid(formEntity)) && isHorizontalValueFlex()) // если верхний контейнер горизонтальный или grid и свойство - flex, возвращаем -2 
            return -2; // выставляем flex - равный ширине
        return super.getBaseDefaultFlex(formEntity);
    }

    @Override
    public FlexAlignment getBaseDefaultAlignment(FormEntity formEntity) {
        ContainerView container = getContainer();
        if (container != null && container.isVertical() && isHorizontalValueFlex())
            return FlexAlignment.STRETCH;
        return super.getBaseDefaultAlignment(formEntity);
    }

    public Pair<ObjectEntity, Boolean> getAddRemove(ServerContext context) {
        return entity.getAddRemove(context.entity, context.securityPolicy);        
    }

    public LocalizedString getCaption() {
        return caption != null
                ? caption
                : entity.getCaption();
    }

    public boolean isNotNull() {
        return notNull || entity.isNotNull();
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

        setupHeader(reportField);
        setupFooter(reportField);
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

    private void setupHeader(ReportDrawField field) {
        if (entity.propertyCaption != null) {
            field.hasHeaderProperty = true;
            field.headerClass = getPropertyClass(entity.propertyCaption);
        } else {
            field.headerClass = java.lang.String.class;
        }
    }

    private void setupFooter(ReportDrawField field) {
        if (entity.propertyFooter != null) {
            field.hasFooterProperty = true;
            field.footerClass = getPropertyClass(entity.propertyFooter);
        } else {
            field.footerClass = java.lang.String.class;
        }
    }

    private void setupShowIf(ReportDrawField field) {
        // At the moment, only showif with no params is passing to the report
        if (entity.propertyShowIf != null && entity.propertyShowIf.property.interfaces.size() == 0) {
            field.hasShowIfProperty = true;
            field.showIfClass = getPropertyClass(entity.propertyShowIf);
        }
    }

    private Class getPropertyClass(CalcPropertyObjectEntity<?> property) {
        ReportDrawField field = new ReportDrawField("", "", charWidth);
        property.property.getType().fillReportDrawField(field);
        return field.valueClass;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.writeString(outStream, ThreadLocalContext.localize(SerializationType.VISUAL_SETUP.equals(serializationType) ? caption : getCaption()));
        pool.writeString(outStream, regexp);
        pool.writeString(outStream, regexpMessage);
        pool.writeLong(outStream, maxValue);
        outStream.writeBoolean(echoSymbols);
        outStream.writeBoolean(noSort);

        if(defaultCompare != null)
            defaultCompare.serialize(outStream);
        else if(Settings.get().isDefaultCompareForStringContains() && isCalcProperty() && getType() instanceof StringClass)
            Compare.CONTAINS.serialize(outStream);
        else
            outStream.writeByte(-1);

        outStream.writeInt(getNumRowHeight());
        outStream.writeInt(getCharWidth());
        pool.writeObject(outStream, getValueSize());

        pool.writeObject(outStream, changeKey);

        outStream.writeBoolean(showChangeKey);

        outStream.writeBoolean(drawAsync);

        pool.writeObject(outStream, format);
        pool.writeObject(outStream, focusable);
        outStream.writeByte(entity.getEditType().serialize());

        outStream.writeBoolean(panelCaptionAbove);

        outStream.writeBoolean(externalScreen != null);
        if (externalScreen != null) {
            outStream.writeInt(externalScreen.getID());
        }
        pool.writeObject(outStream, externalScreenConstraints);

        outStream.writeBoolean(panelCaptionAfter);
        outStream.writeBoolean(editOnSingleClick);
        outStream.writeBoolean(hide);

        //entity часть
        if(isCalcProperty())
            TypeSerializer.serializeType(outStream, getType());
        else {
            outStream.writeByte(1);
            outStream.writeByte(Data.ACTION);
        }

        // асинхронные интерфейсы

        Type changeType = getChangeType(pool.context);
        outStream.writeBoolean(changeType != null);
        if (changeType != null) {
            TypeSerializer.serializeType(outStream, changeType);
        }

        Type changeWYSType = getChangeWYSType(pool.context);
        outStream.writeBoolean(changeWYSType != null);
        if (changeWYSType != null) {
            TypeSerializer.serializeType(outStream, changeWYSType);
        }

        Pair<ObjectEntity, Boolean> addRemove = getAddRemove(pool.context);
        outStream.writeBoolean(addRemove != null);
        if(addRemove!=null) {
            pool.serializeObject(outStream, pool.context.view.getObject(addRemove.first));
            outStream.writeBoolean(addRemove.second);
        }

        outStream.writeBoolean(entity.askConfirm);
        if(entity.askConfirm)
            pool.writeString(outStream, getAskConfirmMessage());
        outStream.writeBoolean(hasEditObjectAction(pool.context));
        outStream.writeBoolean(hasChangeAction(pool.context));

        PropertyObjectEntity<?, ?> debug = entity.getDebugProperty(); // only for tooltip
        Property<?> debugBinding = entity.getDebugBindingProperty(); // only for tooltip

        pool.writeString(outStream, debugBinding.getNamespace());
        pool.writeString(outStream, getSID());
        pool.writeString(outStream, debugBinding.getCanonicalName());
        pool.writeString(outStream, getPropertyFormName());
        pool.writeString(outStream, toolTip);
        pool.serializeObject(outStream, pool.context.view.getGroupObject(
                SerializationType.VISUAL_SETUP.equals(serializationType) ? entity.toDraw : entity.getToDraw(pool.context.view.entity)));

        pool.writeString(outStream, entity.columnsName);
        ImOrderSet<GroupObjectEntity> columnGroupObjects = entity.getColumnGroupObjects();
        outStream.writeInt(columnGroupObjects.size());
        for (GroupObjectEntity groupEntity : columnGroupObjects) {
            pool.serializeObject(outStream, pool.context.view.getGroupObject(groupEntity));
        }

        outStream.writeBoolean(isCalcProperty());
        outStream.writeBoolean(clearText);
        outStream.writeBoolean(notSelectAll);

        pool.serializeObject(outStream, pool.context.view.get(entity.quickFilterProperty));

        MapKeysTable<? extends PropertyInterface> mapTable = isCalcProperty() ?
                        ((CalcProperty<?>)debugBinding).mapTable : null;
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

        if(debug instanceof CalcPropertyObjectEntity)
            ((CalcPropertyObjectEntity<?>)debug).property.getValueClass(ClassType.formPolicy).serialize(outStream);
        else 
            outStream.writeByte(Data.ACTION);
        
        pool.writeString(outStream, entity.eventID);

        pool.writeString(outStream, debug.getCreationScript());
        pool.writeString(outStream, debug.getCreationPath());
        pool.writeString(outStream, entity.getFormPath());

        pool.writeString(outStream, entity.getMouseBinding());

        Map<KeyStroke, String> keyBindings = entity.getKeyBindings();
        outStream.writeInt(keyBindings == null ? 0 : keyBindings.size());
        if (keyBindings != null) {
            for (Map.Entry<KeyStroke, String> e : keyBindings.entrySet()) {
                pool.writeObject(outStream, e.getKey());
                pool.writeString(outStream, e.getValue());
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
    }

    private OrderedMap<String, LocalizedString> filterContextMenuItems(OrderedMap<String, LocalizedString> contextMenuBindings, ServerContext context) {
        if (contextMenuBindings == null || contextMenuBindings.size() == 0) {
            return null;
        }

        OrderedMap<String, LocalizedString> contextMenuItems = new OrderedMap<>();
        for (int i = 0; i < contextMenuBindings.size(); ++i) {
            String actionSID = contextMenuBindings.getKey(i);
            LocalizedString caption = contextMenuBindings.getValue(i);
            ActionPropertyObjectEntity<?> editAction = entity.getEditAction(actionSID, context.securityPolicy);
            if (editAction != null && context.securityPolicy.property.view.checkPermission(editAction.property)) {
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

        caption = LocalizedString.create(pool.readString(inStream));
        regexp = pool.readString(inStream);
        regexpMessage = pool.readString(inStream);
        maxValue = pool.readLong(inStream);
        echoSymbols = inStream.readBoolean();
        noSort = inStream.readBoolean();
        defaultCompare = Compare.deserialize(inStream);

        setNumRowHeight(inStream.readInt());
        setCharWidth(inStream.readInt());
        setValueSize(pool.<Dimension>readObject(inStream));

        changeKey = pool.readObject(inStream);
        showChangeKey = inStream.readBoolean();

        format = pool.readObject(inStream);

        focusable = pool.readObject(inStream);

        panelCaptionAbove = inStream.readBoolean();

        if (inStream.readBoolean()) {
            externalScreen = pool.context.BL.getExternalScreen(inStream.readInt());
        }

        externalScreenConstraints = pool.readObject(inStream);

        panelCaptionAfter = inStream.readBoolean();
        editOnSingleClick = inStream.readBoolean();
        hide = inStream.readBoolean();

        entity = pool.context.entity.getPropertyDraw(inStream.readInt());
    }

    @Override
    public String toString() {
        return ThreadLocalContext.localize(getCaption()) + " " + super.toString();
    }

    public int getNumRowHeight() {
        return numRowHeight;
    }

    public void setNumRowHeight(int numRowHeight) {
        this.numRowHeight = numRowHeight;
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

    public void setValueSize(Dimension minimumValueSize) {
        this.valueSize = minimumValueSize;
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
        return isCalcProperty() && getType().isFlex();
    }

    public String getAskConfirmMessage() {
        assert entity.askConfirm;
        if (entity.askConfirmMessage != null)
            return entity.askConfirmMessage;
        
        LocalizedString msg;
        if (isCalcProperty()) {
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
        return entity.getEditAction(CHANGE, context.securityPolicy) != null;    
    }
    public boolean hasEditObjectAction(ServerContext context) {
        return entity.getEditAction(EDIT_OBJECT, context.securityPolicy) != null;    
    }
}