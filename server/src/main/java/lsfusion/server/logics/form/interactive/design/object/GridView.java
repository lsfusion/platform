package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.struct.FormEntity;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.Supplier;

import static lsfusion.base.BaseUtils.nvl;

public class GridView extends GridPropertyView {

    private NFProperty<Boolean> tabVertical = NFFact.property();
    private NFProperty<Boolean> quickSearch = NFFact.property();

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

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        outStream.writeBoolean(isTabVertical());
        outStream.writeBoolean(isQuickSearch());

        pool.serializeObject(outStream, getRecord());

        pool.serializeObject(outStream, groupObject);
    }

    public boolean isTabVertical() {
        return nvl(tabVertical.get(), false);
    }
    public void setTabVertical(Boolean value, Version version) {
        tabVertical.set(value, version);
    }

    public boolean isQuickSearch() {
        return nvl(quickSearch.get(), false);
    }
    //todo: формально временное решение:
    //todo: метод дизайна, который изменяет энтити => должно быть перенсено на уровень энтити
    public void setQuickSearch(Boolean value, Version version) {
        quickSearch.set(value, version);
        groupObject.entity.pageSize = 0;
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

        tabVertical.set(src.tabVertical, p -> p, mapping.version);
        quickSearch.set(src.quickSearch, p -> p, mapping.version);

        groupObject = mapping.get(src.groupObject);
        record = mapping.get(src.record);
    }
}
