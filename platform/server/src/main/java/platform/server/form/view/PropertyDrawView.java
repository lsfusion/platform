package platform.server.form.view;

import platform.interop.form.ReportConstants;
import platform.interop.form.layout.SimplexConstraints;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenConstraints;
import platform.server.classes.ValueClass;
import platform.server.data.type.Type;
import platform.server.data.type.TypeSerializer;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.form.view.report.ReportDrawField;
import platform.server.logics.property.ExecuteProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.table.MapKeysTable;
import platform.server.serialization.SerializationType;
import platform.server.serialization.ServerSerializationPool;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;

public class PropertyDrawView extends ComponentView {

    public PropertyDrawEntity<?> entity;

    /**
     * Usage example:
     * <pre><code>
     *  LP someLP = ...;
     *  ...
     *  PropertyDrawEntity propertyDraw = getPropertyDraw(someLP);
     *  design.get(propertyDraw).autoHide = true;
     *  </code></pre>
     */
    public boolean autoHide = false;

    public Color highlightColor;

    public Dimension minimumSize;
    public Dimension maximumSize;
    public Dimension preferredSize;

    private int minimumCharWidth;
    private int maximumCharWidth;
    private int preferredCharWidth;

    public KeyStroke editKey;
    public boolean showEditKey = true;

    public Format format;

    public Boolean focusable;

    public boolean panelLabelAbove = false;

    public ExternalScreen externalScreen;
    public ExternalScreenConstraints externalScreenConstraints = new ExternalScreenConstraints();

    public String caption;
    public boolean clearText;

    @SuppressWarnings({"UnusedDeclaration"})
    public PropertyDrawView() {

    }

    public PropertyDrawView(PropertyDrawEntity entity) {
        super(entity.ID);
        this.entity = entity;
    }

    @Override
    public SimplexConstraints<ComponentView> getDefaultConstraints() {
        return SimplexConstraints.getPropertyDrawDefaultConstraints(super.getDefaultConstraints());
    }

    public Type getType() {
        return entity.propertyObject.property.getType();
    }

    public String getSID() {
        return entity.getSID();
    }

    public String getDefaultCaption() {
        return entity.propertyObject.property.caption;
    }

    private String getCaption() {
        return caption != null
                ? caption
                : entity.propertyCaption == null
                ? getDefaultCaption()
                : null;
    }

    public ReportDrawField getReportDrawField() {

        ReportDrawField reportField = new ReportDrawField(getSID(), getCaption());

        Type type = getType();

        reportField.minimumWidth = type.getMinimumWidth();
        reportField.setPreferredWidth(type.getPreferredWidth());

        if (getPreferredCharWidth() != 0) {
            reportField.fixedCharWidth = getPreferredCharWidth();
        }

        Format format = type.getReportFormat();
        if (format instanceof DecimalFormat) {
            reportField.pattern = ((DecimalFormat) format).toPattern();
        }
        if (format instanceof SimpleDateFormat) {
            reportField.pattern = ((SimpleDateFormat) format).toPattern();
        }

        reportField.hasColumnGroupObjects = !entity.columnGroupObjects.isEmpty();
        reportField.hasCaptionProperty = (entity.propertyCaption != null);

        // определяем класс заголовка
        if (reportField.hasCaptionProperty) {
            ReportDrawField captionField = new ReportDrawField(getSID() + ReportConstants.captionSuffix, "");
            entity.propertyCaption.property.getType().fillReportDrawField(captionField);
            reportField.captionClass = captionField.valueClass;
        } else {
            reportField.captionClass = java.lang.String.class;
        }

        if (!getType().fillReportDrawField(reportField)) {
            return null;
        }

        return reportField;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.writeString(outStream, SerializationType.VISUAL_SETUP.equals(serializationType)
                ? caption
                : getCaption());

        pool.writeObject(outStream, minimumSize);
        pool.writeObject(outStream, maximumSize);
        pool.writeObject(outStream, preferredSize);

        outStream.writeInt(getMinimumCharWidth());
        outStream.writeInt(getMaximumCharWidth());
        outStream.writeInt(getPreferredCharWidth());

        pool.writeObject(outStream, editKey);

        outStream.writeBoolean(showEditKey);

        pool.writeObject(outStream, format);
        pool.writeObject(outStream, focusable);
        outStream.writeBoolean(entity.readOnly);

        outStream.writeBoolean(panelLabelAbove);

        outStream.writeBoolean(externalScreen != null);
        if (externalScreen != null) {
            outStream.writeInt(externalScreen.getID());
        }
        pool.writeObject(outStream, externalScreenConstraints);

        outStream.writeBoolean(autoHide);

        pool.writeObject(outStream, highlightColor);

        //entity часть
        TypeSerializer.serializeType(outStream, getType());

        pool.writeString(outStream, entity.propertyObject.property.sID);
        pool.writeString(outStream, entity.propertyObject.property.toolTip);
        pool.serializeObject(outStream, pool.context.view.getGroupObject(
                SerializationType.VISUAL_SETUP.equals(serializationType) ? entity.toDraw : entity.getToDraw(pool.context.view.entity)));

        outStream.writeInt(entity.columnGroupObjects.size());
        for (GroupObjectEntity groupEntity : entity.columnGroupObjects) {
            pool.serializeObject(outStream, pool.context.view.getGroupObject(groupEntity));
        }

        outStream.writeBoolean(!(entity.propertyObject.property instanceof ExecuteProperty)); //checkEquals
        outStream.writeBoolean(entity.propertyObject.property.askConfirm);
        outStream.writeBoolean(clearText);

        MapKeysTable<? extends PropertyInterface> mapTable = entity.propertyObject.property.mapTable;
        pool.writeString(outStream, mapTable != null ? mapTable.table.name : null);

        Iterator<ValueClass> classesIt = entity.propertyObject.property.getCommonClasses().interfaces.values().iterator();
        Collection<PropertyObjectInterfaceEntity> interfacesEntities = entity.propertyObject.mapping.values();
        outStream.writeInt(interfacesEntities.size());
        for (PropertyObjectInterfaceEntity interfaceEntity : interfacesEntities) {
            assert classesIt.hasNext();
            ValueClass valueClass = classesIt.next();

            pool.writeString(outStream, interfaceEntity.toString());
            valueClass.serialize(outStream);
        }

        entity.propertyObject.property.getCommonClasses().value.serialize(outStream);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        caption = pool.readString(inStream);

        minimumSize = pool.readObject(inStream);
        maximumSize = pool.readObject(inStream);
        preferredSize = pool.readObject(inStream);

        setMinimumCharWidth(inStream.readInt());
        setMaximumCharWidth(inStream.readInt());
        setPreferredCharWidth(inStream.readInt());

        editKey = pool.readObject(inStream);
        showEditKey = inStream.readBoolean();

        format = pool.readObject(inStream);

        focusable = pool.readObject(inStream);

        panelLabelAbove = inStream.readBoolean();

        if (inStream.readBoolean()) {
            externalScreen = pool.context.BL.getExternalScreen(inStream.readInt());
        }

        externalScreenConstraints = pool.readObject(inStream);

        autoHide = inStream.readBoolean();

        highlightColor = pool.readObject(inStream);

        entity = pool.context.entity.getPropertyDraw(inStream.readInt());
    }

    @Override
    public String toString() {
        return getCaption();
    }

    public int getMinimumCharWidth() {
        return minimumCharWidth != 0 ? minimumCharWidth : entity.propertyObject.property.minimumCharWidth;
    }

    public void setMinimumCharWidth(int minimumCharWidth) {
        this.minimumCharWidth = minimumCharWidth;
    }

    public int getMaximumCharWidth() {
        return maximumCharWidth != 0 ? maximumCharWidth : entity.propertyObject.property.maximumCharWidth;
    }

    public void setMaximumCharWidth(int maximumCharWidth) {
        this.maximumCharWidth = maximumCharWidth;
    }

    public int getPreferredCharWidth() {
        return preferredCharWidth != 0 ? preferredCharWidth : entity.propertyObject.property.preferredCharWidth;
    }

    public void setPreferredCharWidth(int preferredCharWidth) {
        this.preferredCharWidth = preferredCharWidth;
    }
}
