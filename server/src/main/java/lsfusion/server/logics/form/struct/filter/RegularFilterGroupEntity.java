package lsfusion.server.logics.form.struct.filter;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.identity.IDGenerator;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFOrderSet;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterGroupView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.IdentityEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;

public class RegularFilterGroupEntity extends IdentityEntity<RegularFilterGroupEntity, GroupObjectEntity> {

    public NFOrderSet<RegularFilterEntity> filters = NFFact.orderSet();

    private NFProperty<Integer> defaultFilterIndex = NFFact.property();

    public boolean noNull;

    @Override
    protected String getDefaultSIDPrefix() {
        return "regularFilter";
    }

    public RegularFilterGroupEntity(IDGenerator ID, String sID, boolean noNull, Version version) {
        super(ID, sID, null);
        this.defaultFilterIndex.set(noNull ? 0 : -1, version);
        this.noNull = noNull;
    }

    public void addFilter(RegularFilterEntity filter, boolean setDefault, Version version) {
        if (setDefault) {
            setDefault(filters.size(version), version);
        }
        filters.add(filter, version);
    }

    public void setDefault(int index, Version version) {
        defaultFilterIndex.set(index, version);
    }

    public int getDefault() {
        return defaultFilterIndex.get();
    }
    
    public int getFiltersCount(Version version) {
        return filters.size(version);
    }

    public ImList<RegularFilterEntity> getNFFilters(Version version) {
        return filters.getNFOrderSet(version);
    }

    public ImList<RegularFilterEntity> getFiltersList() {
        return filters.getList();
    }

    public GroupObjectEntity getToDraw(FormEntity form) {
        MSet<ObjectEntity> mGroupObjects = SetFact.mSet();

        // ищем самый нижний GroupObjectInstance, к которому применяется фильтр
        for (RegularFilterEntity regFilter : getFiltersList()) {
            mGroupObjects.addAll(regFilter.filter.getObjects());
        }

        return form.getApplyObject(mGroupObjects.immutable(), SetFact.EMPTY());
    }

    public GroupObjectEntity getNFToDraw(FormEntity form, Version version) {
        MSet<ObjectEntity> mGroupObjects = SetFact.mSet();

        // ищем самый нижний GroupObjectInstance, к которому применяется фильтр
        for (RegularFilterEntity regFilter : filters.getNFOrderSet(version)) {
            mGroupObjects.addAll(regFilter.filter.getObjects());
        }

        return form.getNFApplyObject(mGroupObjects.immutable(), version);
    }

    public void finalizeAroundInit() {
        filters.finalizeChanges();
        defaultFilterIndex.finalizeChanges();
    }

    public RegularFilterGroupView view;

    // copy-constructor
    protected RegularFilterGroupEntity(RegularFilterGroupEntity src, ObjectMapping mapping) {
        super(src, mapping);

        noNull = src.noNull;
        view = mapping.get(src.view);
    }

    @Override
    public void extend(RegularFilterGroupEntity src, ObjectMapping mapping) {
        super.extend(src, mapping);

        mapping.sets(defaultFilterIndex, src.defaultFilterIndex);
    }

    @Override
    public void add(RegularFilterGroupEntity src, ObjectMapping mapping) {
        super.add(src, mapping);

        mapping.add(filters, src.filters);
    }

    @Override
    public RegularFilterGroupEntity copy(ObjectMapping mapping) {
        return new RegularFilterGroupEntity(this, mapping);
    }
}
