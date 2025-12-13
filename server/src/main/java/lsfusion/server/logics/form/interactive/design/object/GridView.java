package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.base.identity.IDGenerator;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.IdentityView;
import lsfusion.server.logics.form.struct.FormEntity;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.Supplier;

import static lsfusion.base.BaseUtils.nvl;

public class GridView extends GridPropertyView<GridView, GroupObjectView>{

    private NFProperty<Boolean> tabVertical = NFFact.property();
    private NFProperty<Boolean> quickSearch = NFFact.property();

    public GroupObjectView groupObject;

    public CalculationsView calculations;

    // lazy creation, since its usage is pretty rear
    public static class ExContainerView<AddParent extends IdentityView<AddParent, ?>> extends FormEntity.ExMapProp<ContainerView<AddParent>, ExContainerView<AddParent>> {

        public ExContainerView(Supplier<ContainerView<AddParent>> supplier) {
            super(supplier);
        }

        public ExContainerView(ExContainerView<AddParent> exProp, ObjectMapping mapping) {
            super(exProp, mapping);
        }

        @Override
        public ExContainerView<AddParent> get(ObjectMapping mapping) {
            return new ExContainerView<>(this, mapping);
        }
    }
    private final ExContainerView<?> record;
    public ContainerView getRecord() { // assert that grid view is "finalized"
        return record.get();
    }
    @NFLazy
    public ContainerView getNFRecord(Version version) {
        return record.getNF(version);
    }

    public GridView(IDGenerator idGenerator, GroupObjectView groupObject, Version version) {
        super(idGenerator, version);
        this.groupObject = groupObject;

        calculations = new CalculationsView(idGenerator, this);

        record = new ExContainerView(() -> {
            ContainerView<?> record = new ContainerView(idGenerator);
            record.recordContainer = this;
            record.setAddParent(this, pc -> pc.record.get());
            return record;
        });
    }

    @Override
    public String getPropertyGroupContainerSID() {
        return groupObject.getSID();
    }

    @Override
    public String getPropertyGroupContainerName() {
        return groupObject.getSID();
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

        calculations.finalizeAroundInit();

        ContainerView record = getRecord();
        if(record != null)
            record.finalizeAroundInit();
    }

    protected boolean isCustom() {
        return groupObject.entity.isCustom();
    }

    // copy-constructor
    protected GridView(GridView src, ObjectMapping mapping) {
        super(src, mapping);

        calculations = mapping.get(src.calculations);
        groupObject = mapping.get(src.groupObject);
        record = mapping.get((ExContainerView)src.record);
    }

    @Override
    public void extend(GridView src, ObjectMapping mapping) {
        super.extend(src, mapping);

        mapping.sets(tabVertical, src.tabVertical);
        mapping.sets(quickSearch, src.quickSearch);
    }

    @Override
    public GroupObjectView getAddParent(ObjectMapping mapping) {
        return groupObject;
    }
    @Override
    public GridView getAddChild(GroupObjectView groupObjectView, ObjectMapping mapping) {
        return groupObjectView.grid;
    }
    @Override
    public GridView copy(ObjectMapping mapping) {
        return new GridView(this, mapping);
    }
}
