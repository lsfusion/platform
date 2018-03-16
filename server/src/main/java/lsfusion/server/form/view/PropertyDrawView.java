package lsfusion.server.form.view;

import lsfusion.base.OrderedMap;
import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.Compare;
import lsfusion.interop.form.ReportConstants;
import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.interop.form.screen.ExternalScreen;
import lsfusion.interop.form.screen.ExternalScreenConstraints;
import lsfusion.server.Settings;
import lsfusion.server.auth.ChangePropertySecurityPolicy;
import lsfusion.server.classes.ActionClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeSerializer;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.view.report.ReportDrawField;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassType;
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
        return entity.propertyObject.property.getType();
    }

    public Type getChangeType(FormEntity form) {
        return entity.getRequestInputType(form);
    }
    
    public Type getChangeWYSType(FormEntity form) {
        return entity.getWYSRequestInputType(form);
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

    public Pair<ObjectEntity, Boolean> getAddRemove(FormEntity form) {
        return entity.getAddRemove(form);        
    }

    public LocalizedString getDefaultCaption() {
        return entity.propertyObject.property.caption;
    }

    public LocalizedString getCaption() {
        return caption != null
                ? caption
                : getDefaultCaption();
    }

    //Для Jasper'а экранируем кавычки
    public String getReportCaption() {
        LocalizedString caption = getCaption();
        return caption == null ? null : ThreadLocalContext.localize(caption).replace("\"", "\\\"");
    }

    public ReportDrawField getReportDrawField(int charWidth, int scale) {

        ReportDrawField reportField = new ReportDrawField(getPropertyFormName(), getReportCaption(), charWidth);

        Type type = getType();

        reportField.scale = scale;
        reportField.minimumWidth = type.getReportMinimumWidth() * scale;
        reportField.preferredWidth = type.getReportPreferredWidth() * scale;
        int reportCharWidth = getCharWidth();
        if (reportCharWidth != 0) {
            reportField.fixedCharWidth = reportCharWidth * scale;
        }

        reportField.hasColumnGroupObjects = !entity.getColumnGroupObjects().isEmpty();
        if (reportField.hasColumnGroupObjects) {
            reportField.columnGroupName = entity.columnsName;
        }
        reportField.hasCaptionProperty = (entity.propertyCaption != null);
        reportField.hasFooterProperty = (entity.propertyFooter != null);

        // определяем класс заголовка
        if (reportField.hasCaptionProperty) {
            ReportDrawField captionField = new ReportDrawField(getPropertyFormName() + ReportConstants.headerSuffix, "", charWidth);
            entity.propertyCaption.property.getType().fillReportDrawField(captionField);
            reportField.captionClass = captionField.valueClass;
        } else {
            reportField.captionClass = java.lang.String.class;
        }

        // определяем класс футера
        if (reportField.hasFooterProperty) {
            ReportDrawField footerField = new ReportDrawField(getPropertyFormName() + ReportConstants.footerSuffix, "", charWidth);
            entity.propertyFooter.property.getType().fillReportDrawField(footerField);
            reportField.footerClass = footerField.valueClass;
        } else {
            reportField.footerClass = java.lang.String.class;
        }

        reportField.pattern = getFormatPattern();
        
        if (!getType().fillReportDrawField(reportField)) {
            return null;
        }

        return reportField;
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
        serializeCompare(outStream);
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
        TypeSerializer.serializeType(outStream, getType());

        // асинхронные интерфейсы

        Type changeType = getChangeType(pool.context.view.entity);
        outStream.writeBoolean(changeType != null);
        if (changeType != null) {
            TypeSerializer.serializeType(outStream, changeType);
        }

        Type changeWYSType = getChangeWYSType(pool.context.view.entity);
        outStream.writeBoolean(changeWYSType != null);
        if (changeWYSType != null) {
            TypeSerializer.serializeType(outStream, changeWYSType);
        }

        Pair<ObjectEntity, Boolean> addRemove = getAddRemove(pool.context.view.entity);
        outStream.writeBoolean(addRemove != null);
        if(addRemove!=null) {
            pool.serializeObject(outStream, pool.context.view.getObject(addRemove.first));
            outStream.writeBoolean(addRemove.second);
        }

        outStream.writeBoolean(entity.askConfirm);
        if(entity.askConfirm)
            pool.writeString(outStream, getAskConfirmMessage());
        outStream.writeBoolean(entity.hasEditObjectAction());
        outStream.writeBoolean(hasChangeAction(pool.context));

        pool.writeString(outStream, entity.getNamespace());
        pool.writeString(outStream, getSID());
        pool.writeString(outStream, entity.propertyObject.property.getOrCanonicalName());
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

        outStream.writeBoolean(entity.propertyObject.property.checkEquals());
        outStream.writeBoolean(clearText);

        pool.serializeObject(outStream, pool.context.view.get(entity.quickFilterProperty));

        MapKeysTable<? extends PropertyInterface> mapTable = entity.propertyObject.property instanceof CalcProperty ?
                        ((CalcProperty<?>)entity.propertyObject.property).mapTable : null;
        pool.writeString(outStream, mapTable != null ? mapTable.table.getName() : null);

        ImMap<PropertyInterface, ValueClass> interfaceClasses = (ImMap<PropertyInterface, ValueClass>) entity.propertyObject.property.getInterfaceClasses(ClassType.formPolicy);
        ImMap<PropertyInterface, PropertyObjectInterfaceEntity> interfaceEntities = (ImMap<PropertyInterface, PropertyObjectInterfaceEntity>) entity.propertyObject.mapping;
        outStream.writeInt(entity.propertyObject.property.interfaces.size());
        for (PropertyInterface iFace : entity.propertyObject.property.interfaces) {
            pool.writeString(outStream, interfaceEntities.get(iFace).toString());
            
            ValueClass paramClass = interfaceClasses.get(iFace);
            outStream.writeBoolean(paramClass != null);
            if (paramClass != null) {
                paramClass.serialize(outStream);
            }
        }

        entity.propertyObject.property.getValueClass(ClassType.formPolicy).serialize(outStream); // только показать пользователю
        pool.writeString(outStream, entity.eventID);

        pool.writeString(outStream, entity.propertyObject.getCreationScript());
        pool.writeString(outStream, entity.propertyObject.getCreationPath());
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

        outStream.writeBoolean(notNull || entity.propertyObject.property.isSetNotNull());
    }

    private void serializeCompare(DataOutputStream outStream) throws IOException {
        if(defaultCompare != null)
            defaultCompare.serialize(outStream);
        else if(entity.propertyObject.property.drawOptions.getDefaultCompare() != null)
            entity.propertyObject.property.drawOptions.getDefaultCompare().serialize(outStream);
        else if(Settings.get().isDefaultCompareForStringContains() && getType() instanceof StringClass)
            Compare.CONTAINS.serialize(outStream);
        else
            outStream.writeByte(-1);
    }

    private OrderedMap<String, LocalizedString> filterContextMenuItems(OrderedMap<String, LocalizedString> contextMenuBindings, ServerContext context) {
        if (contextMenuBindings == null || contextMenuBindings.size() == 0) {
            return null;
        }

        OrderedMap<String, LocalizedString> contextMenuItems = new OrderedMap<>();
        for (int i = 0; i < contextMenuBindings.size(); ++i) {
            String actionSID = contextMenuBindings.getKey(i);
            LocalizedString caption = contextMenuBindings.getValue(i);
            ActionPropertyObjectEntity<?> editAction = entity.getEditAction(actionSID, context.entity);
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
        return getType().isFlex();
    }

    public String getAskConfirmMessage() {
        assert entity.askConfirm;
        if (entity.askConfirmMessage != null)
            return entity.askConfirmMessage;
        
        LocalizedString msg;
        if (entity.propertyObject.property.getType() instanceof ActionClass) {
            msg = LocalizedString.create("{form.instance.do.you.really.want.to.take.action}");
        } else {
            msg = LocalizedString.create("{form.instance.do.you.really.want.to.edit.property}");
        }
        LocalizedString caption = getCaption();
        if (!caption.isEmpty()) {
            msg = LocalizedString.concatList(msg, " \"", caption, "\"?");
        }

        return ThreadLocalContext.localize(msg);
    }
    
    public boolean hasChangeAction(ServerContext context) {
        ActionPropertyObjectEntity<?> editAction = entity.getChangeAction(context.entity);
        if (editAction != null) {
            boolean readOnly = (((ActionProperty) editAction.property).checkReadOnly && entity.propertyReadOnly != null && entity.propertyReadOnly.property.checkAlwaysNull(false));
            ChangePropertySecurityPolicy changePropertySecurityPolicy = context.securityPolicy.property.change;
            boolean securityPermission = changePropertySecurityPolicy.checkPermission(editAction.property) && changePropertySecurityPolicy.checkPermission(entity.propertyObject.property);
            return !readOnly && securityPermission;
        }
        return false;    
    }
}