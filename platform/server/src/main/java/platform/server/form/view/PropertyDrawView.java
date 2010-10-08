package platform.server.form.view;

import platform.server.data.type.Type;
import platform.server.data.type.TypeSerializer;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.view.report.ReportDrawField;
import platform.server.logics.property.ExecuteProperty;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenConstraints;
import platform.server.serialization.ServerSerializationPool;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.awt.*;
import java.text.Format;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class PropertyDrawView extends ComponentView implements ClientSerialize {

    private FormView form;
    public PropertyDrawEntity<?> entity;
    
    /**
     * Example of use:
     *  <pre><code>
     *  LP someLP = ...;
     *  ...
     *  PropertyDrawEntity propertyDraw = getPropertyDraw(someLP);
     *  design.get(propertyDraw).autoHide = true;
     *  </code></pre>
     */
    public boolean autoHide = false;

    public GroupObjectEntity keyBindingGroup = null;

    public PropertyDrawView() {
        
    }
    
    public PropertyDrawView(int ID, FormView form, PropertyDrawEntity entity) {
        super(ID);
        this.form = form;
        this.entity = entity;
    }

    public Type getType() {
        return entity.propertyObject.property.getType();
    }

    public int getID() {
        return entity.getID();
    }

    public String getSID() {
        return entity.getSID();
    }

    public String getDefaultCaption() {
        return entity.propertyObject.property.caption;
    }

    public Dimension minimumSize;
    public Dimension maximumSize;
    public Dimension preferredSize;

    public KeyStroke editKey;
    public boolean showEditKey = true;

    public Format format;

    public Boolean focusable;

    public Boolean readOnly = Boolean.FALSE;

    public boolean panelLabelAbove = false;

    public ExternalScreen externalScreen;
    public ExternalScreenConstraints externalScreenConstraints = new ExternalScreenConstraints();

    public String caption;
    String getCaption() {
        if (caption != null)
            return caption;
        else
            return getDefaultCaption();
    }

    public ReportDrawField getReportDrawField() {

        ReportDrawField reportField = new ReportDrawField(getSID(), getCaption());

        Type type = getType();

        reportField.minimumWidth = type.getMinimumWidth();
        reportField.preferredWidth = type.getPreferredWidth();

        Format format = type.getDefaultFormat();
        if (format instanceof DecimalFormat) {
            reportField.pattern = ((DecimalFormat)format).toPattern();
        }
        if (format instanceof SimpleDateFormat) {
            reportField.pattern = ((SimpleDateFormat)format).toPattern();
        }

        if (!getType().fillReportDrawField(reportField))
            return null;

        return reportField;
    }


    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeUTF(getCaption());

        TypeSerializer.serializeType(outStream,getType());

        new ObjectOutputStream(outStream).writeObject(minimumSize);
        new ObjectOutputStream(outStream).writeObject(maximumSize);
        new ObjectOutputStream(outStream).writeObject(preferredSize);

        new ObjectOutputStream(outStream).writeObject(editKey);
        outStream.writeBoolean(showEditKey);

        new ObjectOutputStream(outStream).writeObject(format);

        new ObjectOutputStream(outStream).writeObject(focusable);

        new ObjectOutputStream(outStream).writeObject(readOnly);

        outStream.writeBoolean(panelLabelAbove);

        outStream.writeBoolean(externalScreen != null);
        if (externalScreen != null)
            outStream.writeInt(externalScreen.getID());

        outStream.writeBoolean(externalScreenConstraints != null);
        if (externalScreenConstraints != null)
            new ObjectOutputStream(outStream).writeObject(externalScreenConstraints);

        outStream.writeInt(entity.getID());
        outStream.writeUTF(entity.propertyObject.property.sID);
        outStream.writeBoolean(entity.toDraw!=null);
        if(entity.toDraw!=null) {
            outStream.writeInt(entity.toDraw.getID());
        }
        outStream.writeBoolean(keyBindingGroup!=null);
        if(keyBindingGroup!=null) {
            outStream.writeInt(keyBindingGroup.getID());
        }

        outStream.writeInt(entity.columnGroupObjects.size());
        for (GroupObjectEntity columnGroupObject : entity.columnGroupObjects) {
            outStream.writeInt(columnGroupObject.getID());
        }

        outStream.writeBoolean(autoHide);

        outStream.writeBoolean(!(entity.propertyObject.property instanceof ExecuteProperty)); //
        outStream.writeBoolean(entity.propertyObject.property.askConfirm);
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.writeString(outStream, getCaption());
        TypeSerializer.serializeType(outStream, getType());

        pool.writeObject(outStream, minimumSize);
        pool.writeObject(outStream, maximumSize);
        pool.writeObject(outStream, preferredSize);

        pool.writeObject(outStream, editKey);

        outStream.writeBoolean(showEditKey);

        pool.writeObject(outStream, format);
        pool.writeObject(outStream, focusable);
        pool.writeObject(outStream, readOnly);

        outStream.writeBoolean(panelLabelAbove);

        outStream.writeBoolean(externalScreen != null);
        if (externalScreen != null) {
            outStream.writeInt(externalScreen.getID());
        }
        pool.writeObject(outStream, externalScreenConstraints);

        outStream.writeInt(entity.getID());
        pool.writeString(outStream, entity.propertyObject.property.sID);
        pool.serializeObject(outStream, form.getGroupObject(entity.toDraw));
        pool.serializeObject(outStream, form.getGroupObject(keyBindingGroup));

        outStream.writeInt(entity.columnGroupObjects.size());
        for (GroupObjectEntity groupEntity : entity.columnGroupObjects) {
            pool.serializeObject(outStream, form.getGroupObject(groupEntity));
        }

        outStream.writeBoolean(autoHide);

        outStream.writeBoolean(!(entity.propertyObject.property instanceof ExecuteProperty)); //checkEquals
        outStream.writeBoolean(entity.propertyObject.property.askConfirm);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        //todo:
//        super.customDeserialize(pool, iID, inStream);
//
//        caption = pool.readString(inStream);
//
//        baseType = ClientTypeSerializer.deserialize(inStream);
//
//        minimumSize = pool.readObject(inStream);
//        maximumSize = pool.readObject(inStream);
//        preferredSize = pool.readObject(inStream);
//
//        editKey = pool.readObject(inStream);
//        showEditKey = inStream.readBoolean();
//
//        format = pool.readObject(inStream);
//
//        focusable = pool.readObject(inStream);
//        readOnly = pool.readObject(inStream);
//        if (readOnly == null) {
//            readOnly = true;
//        }
//
//        panelLabelAbove = inStream.readBoolean();
//
//        if (inStream.readBoolean()) {
//            externalScreen = ClientExternalScreen.getScreen(inStream.readInt());
//        }
//
//        externalScreenConstraints = pool.readObject(inStream);
//
//        ID = inStream.readInt();
//        sID = pool.readString(inStream);
//
//        groupObject = pool.deserializeObject(inStream);
//        keyBindingGroup = pool.deserializeObject(inStream);
//
//        columnGroupObjects = pool.deserializeList(inStream);
//
//        autoHide = inStream.readBoolean();
//
//        checkEquals = inStream.readBoolean();
//        askConfirm = inStream.readBoolean();
    }
}
