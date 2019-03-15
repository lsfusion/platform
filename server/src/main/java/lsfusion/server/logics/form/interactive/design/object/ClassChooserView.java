package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.struct.object.ObjectEntity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClassChooserView extends ComponentView {
    public ClassChooserView() {

    }

    public ObjectView object;
    public boolean visible = true;
    
    public ClassChooserView(int ID, ObjectEntity view, ObjectView object) {
        super(ID);
        this.visible = view.baseClass instanceof CustomClass && !((CustomClass)view.baseClass).getChildren().isEmpty();

        this.object = object;

        setFlex(0.2);
        setAlignment(FlexAlignment.STRETCH);
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        pool.serializeObject(outStream, object);
        outStream.writeBoolean(visible);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        object = pool.deserializeObject(inStream);
        visible = inStream.readBoolean();
    }
}
