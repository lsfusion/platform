package lsfusion.server.form.view;

import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GridView extends ComponentView {

    public boolean tabVertical = false;
    private boolean quickSearch = false;
    public int headerHeight = -1;

    public GroupObjectView groupObject;

    public GridView() {
        
    }

    public GridView(int ID, GroupObjectView groupObject) {
        super(ID);
        this.groupObject = groupObject;
    }

    @Override
    public double getBaseDefaultFlex(FormEntity formEntity) {
        return 1;
    }

    @Override
    public FlexAlignment getBaseDefaultAlignment(FormEntity formEntity) {
        return FlexAlignment.STRETCH;
    }

    //todo: формально временное решение:
    //todo: метод дизайна, который изменяет энтити => должно быть перенсено на уровень энтити
    public void setQuickSearch(boolean quickSearch) {
        this.quickSearch = quickSearch;
        groupObject.entity.pageSize = 0;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        outStream.writeBoolean(tabVertical);
        outStream.writeBoolean(quickSearch);
        outStream.writeInt(headerHeight);

        pool.serializeObject(outStream, groupObject);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        tabVertical = inStream.readBoolean();
        quickSearch = inStream.readBoolean();
        headerHeight = inStream.readInt();

        groupObject = pool.deserializeObject(inStream);
    }
}
