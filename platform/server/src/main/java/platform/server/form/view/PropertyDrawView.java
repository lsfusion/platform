package platform.server.form.view;

import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenConstraints;
import platform.server.data.type.Type;
import platform.server.data.type.TypeSerializer;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.view.report.ReportDrawField;
import platform.server.logics.property.ExecuteProperty;
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

public class PropertyDrawView extends ComponentView {

    private FormView form;
    public PropertyDrawEntity<?> entity;

    /**
     * Example of use:
     * <pre><code>
     *  LP someLP = ...;
     *  ...
     *  PropertyDrawEntity propertyDraw = getPropertyDraw(someLP);
     *  design.get(propertyDraw).autoHide = true;
     *  </code></pre>
     */
    public boolean autoHide = false;

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

    String getCaption() {
        if (caption != null) {
            return caption;
        } else {
            return getDefaultCaption();
        }
    }

    public ReportDrawField getReportDrawField() {

        ReportDrawField reportField = new ReportDrawField(getSID(), getCaption());

        Type type = getType();

        reportField.minimumWidth = type.getMinimumWidth();
        reportField.preferredWidth = type.getPreferredWidth();

        Format format = type.getDefaultFormat();
        if (format instanceof DecimalFormat) {
            reportField.pattern = ((DecimalFormat) format).toPattern();
        }
        if (format instanceof SimpleDateFormat) {
            reportField.pattern = ((SimpleDateFormat) format).toPattern();
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

        outStream.writeBoolean(autoHide);

        pool.serializeObject(outStream, form.getGroupObject(keyBindingGroup));

        outStream.writeInt(entity.getID());

        //entity часть
        TypeSerializer.serializeType(outStream, getType());

        pool.writeString(outStream, entity.propertyObject.property.sID);
        pool.serializeObject(outStream, form.getGroupObject(entity.toDraw));

        outStream.writeInt(entity.columnGroupObjects.size());
        for (GroupObjectEntity groupEntity : entity.columnGroupObjects) {
            pool.serializeObject(outStream, form.getGroupObject(groupEntity));
        }

        outStream.writeBoolean(!(entity.propertyObject.property instanceof ExecuteProperty)); //checkEquals
        outStream.writeBoolean(entity.propertyObject.property.askConfirm);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, iID, inStream);

        caption = pool.readString(inStream);

        minimumSize = pool.readObject(inStream);
        maximumSize = pool.readObject(inStream);
        preferredSize = pool.readObject(inStream);

        editKey = pool.readObject(inStream);
        showEditKey = inStream.readBoolean();

        format = pool.readObject(inStream);

        focusable = pool.readObject(inStream);
        readOnly = pool.readObject(inStream);
        if (readOnly == null) {
            readOnly = true;
        }

        panelLabelAbove = inStream.readBoolean();

        if (inStream.readBoolean()) {
            externalScreen = pool.context.BL.getExternalScreen(inStream.readInt());
        }

        externalScreenConstraints = pool.readObject(inStream);

        autoHide = inStream.readBoolean();

        form = pool.deserializeObject(inStream);

        GroupObjectView keyBindingGroupView = pool.deserializeObject(inStream);
        if (keyBindingGroupView != null) {
            keyBindingGroup = keyBindingGroupView.entity;
        }

        entity = pool.context.form.getPropertyDraw(inStream.readInt());
    }
}
