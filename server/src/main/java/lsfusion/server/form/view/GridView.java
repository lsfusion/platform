package lsfusion.server.form.view;

import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GridView extends ComponentView {

    public boolean tabVertical = false;
    public boolean autoHide = false;
    private boolean quickSearch = false;
    public int headerHeight = 0;

    public GroupObjectView groupObject;

    public GridView() {
        
    }

    public GridView(int ID, GroupObjectView groupObject) {
        super(ID);
        this.groupObject = groupObject;
        flex = 1;
        alignment = FlexAlignment.STRETCH;
    }

    //todo: формально временное решение:
    //todo: метод дизайна, который изменяет энтити => должно быть перенсено на уровень энтити
    public void setQuickSearch(boolean quickSearch) {
        this.quickSearch = quickSearch;
        groupObject.entity.pageSize = 0;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        outStream.writeBoolean(tabVertical);
        outStream.writeBoolean(autoHide);
        outStream.writeBoolean(quickSearch);
        outStream.writeInt(headerHeight);

        pool.serializeObject(outStream, groupObject);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        tabVertical = inStream.readBoolean();
        autoHide = inStream.readBoolean();
        quickSearch = inStream.readBoolean();
        headerHeight = inStream.readInt();

        groupObject = pool.deserializeObject(inStream);
    }
}
