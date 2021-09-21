package lsfusion.server.logics.form.interactive.design.filter;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFList;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class FilterView extends ComponentView {

    public boolean visible = true;
    
    public NFList<PropertyDrawView> properties = NFFact.list();

    public FilterView() {
    }

    public FilterView(int ID) {
        super(ID);

        setAlignment(FlexAlignment.STRETCH);
    }
    
    public void addProperty(PropertyDrawView property, Version version) {
        properties.add(property, version);
    }  

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        outStream.writeBoolean(visible);
        pool.serializeCollection(outStream, properties.getList());
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        visible = inStream.readBoolean();
        properties = NFFact.finalList(pool.deserializeList(inStream));
    }

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();
        
        properties.finalizeChanges();
    }
}
