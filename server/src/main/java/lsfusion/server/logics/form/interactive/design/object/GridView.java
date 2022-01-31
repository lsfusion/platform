package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.struct.FormEntity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GridView extends GridPropertyView {

    public boolean tabVertical = false;
    private boolean quickSearch = false;

    public GroupObjectView groupObject;

    protected ContainerView record; // lazy creation, since its usage is pretty rear
    public ContainerView getRecord() { // assert that grid view is "finalized"
        return record;
    }
    @NFLazy
    public ContainerView getNFRecord(FormView formView) {
        if(record == null) {
            record = formView.createContainer();
            record.recordContainer = this;
        }
        return record;
    }

    public GridView() {
        
    }

    public GridView(int ID, GroupObjectView groupObject) {
        super(ID);
        this.groupObject = groupObject;
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

        outStream.writeBoolean(isAutoSize(pool.context.entity));
        outStream.writeBoolean(boxed != null);
        if(boxed != null)
            outStream.writeBoolean(boxed);

        outStream.writeBoolean(tabVertical);
        outStream.writeBoolean(quickSearch);
        outStream.writeInt(headerHeight);

        outStream.writeInt(getLineWidth());
        outStream.writeInt(getLineHeight());

        pool.serializeObject(outStream, getRecord());

        pool.serializeObject(outStream, groupObject);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        autoSize = inStream.readBoolean();
        boxed = inStream.readBoolean() ? inStream.readBoolean() : null;

        tabVertical = inStream.readBoolean();
        quickSearch = inStream.readBoolean();
        headerHeight = inStream.readInt();

        lineWidth = inStream.readInt();
        lineHeight = inStream.readInt();

        record = pool.deserializeObject(inStream);

        groupObject = pool.deserializeObject(inStream);
    }

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();

        if(record != null)
            record.finalizeAroundInit();
    }

    protected boolean isCustom() {
        return groupObject.entity.isCustom();
    }
}
