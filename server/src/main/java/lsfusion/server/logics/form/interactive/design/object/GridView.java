package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.identity.IDGenerator;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerFactory;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.IdentityView;
import lsfusion.server.logics.form.struct.FormEntity;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.Predicate;
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

    public GridView(IDGenerator idGenerator, ContainerFactory<ContainerView> containerFactory, GroupObjectView groupObject, Version version) {
        super(idGenerator, containerFactory, version);
        this.groupObject = groupObject;

        calculations = new CalculationsView(idGenerator, this);

        record = new ExContainerView(() -> {
            ContainerView<?> record = containerFactory.createContainer(null);
            record.recordContainer = this;
            record.setAddParent(this, pc -> pc.record.get());
            return record;
        });
    }

    // ITS RECORD - what a DESIGN puts into GRID(g) - has no container: it is in the grid (ContainerView.getHiddenContainer),
    // so every walk down the design goes into it as into a container's children, as the client's do (GGrid.getChildren):
    // what it holds is found by its id - a tab a client activates, a container it collapses, a component a react view
    // places or shows -, its readers are read and its visibility is sent. Whatever draws the grid: the platform's map,
    // calendar or custom view pops the record up for the current object, a react view draws or places what it holds
    @Override
    public ComponentView findById(int id) {
        ComponentView result = super.findById(id);
        ContainerView record = getRecord();
        return result == null && record != null ? record.findById(id) : result;
    }
    @Override
    public void fillPropertyComponents(MExclSet<ComponentView> mComponents) {
        super.fillPropertyComponents(mComponents);
        ContainerView record = getRecord();
        if (record != null)
            record.fillPropertyComponents(mComponents);
    }
    @Override
    public void fillBaseComponents(MExclSet<ComponentView> mComponents, boolean showIf, Predicate<ContainerView> reactShown) {
        ContainerView record = getRecord();
        if (record != null)
            record.fillBaseComponents(mComponents, showIf, reactShown);
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
        groupObject.entity.setPageSize(0, version);
    }

    @Override
    protected void finalizeChanges() {
        super.finalizeChanges();

        tabVertical.finalizeChanges();
        quickSearch.finalizeChanges();

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
