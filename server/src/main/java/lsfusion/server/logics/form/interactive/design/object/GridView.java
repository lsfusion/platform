package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.server.base.version.NFLazy;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.struct.FormEntity;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.Supplier;

public class GridView extends GridPropertyView {

    public boolean tabVertical = false;
    private boolean quickSearch = false;

    public GroupObjectView groupObject;

    // lazy creation, since its usage is pretty rear
    public static class ExContainerView extends FormEntity.ExProp<ContainerView> {

        public ExContainerView(Supplier<ContainerView> supplier) {
            super(supplier);
        }

        public ExContainerView(FormEntity.ExProp<ContainerView> exProp, ObjectMapping mapping) {
            super(exProp, mapping::get, mapping.version);
        }
    }
    private final ExContainerView record;
    public ContainerView getRecord() { // assert that grid view is "finalized"
        return record.get();
    }
    @NFLazy
    public ContainerView getNFRecord(Version version) {
        return record.getNF(version);
    }

    public GridView(int ID, int recordID, GroupObjectView groupObject) {
        super(ID);
        this.groupObject = groupObject;

        record = new ExContainerView(() -> {
            ContainerView record = new ContainerView(recordID);
            record.recordContainer = this;
            return record;
        });
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

        pool.serializeObject(outStream, getRecord());

        pool.serializeObject(outStream, groupObject);
    }

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();

        ContainerView record = getRecord();
        if(record != null)
            record.finalizeAroundInit();
    }

    protected boolean isCustom() {
        return groupObject.entity.isCustom();
    }

    // copy-constructor
    public GridView(GridView src, ObjectMapping mapping) {
        super(src, mapping);

        tabVertical = src.tabVertical;
        quickSearch = src.quickSearch;

        groupObject = mapping.get(src.groupObject);
        record = mapping.get(src.record);
    }
}
