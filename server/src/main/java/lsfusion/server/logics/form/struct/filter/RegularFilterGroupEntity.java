package lsfusion.server.logics.form.struct.filter;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.identity.IdentityObject;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.interfaces.NFOrderSet;
import lsfusion.server.logics.mutables.interfaces.NFProperty;

public class RegularFilterGroupEntity extends IdentityObject {

    public NFOrderSet<RegularFilterEntity> filters = NFFact.orderSet();

    private NFProperty<Integer> defaultFilterIndex = NFFact.property();

    // конструктор нельзя удалять - нужен для сериализации
    public RegularFilterGroupEntity() {
    }

    public RegularFilterGroupEntity(int iID, Version version) {
        ID = iID;
        defaultFilterIndex.set(-1, version);
    }

    public void addFilter(RegularFilterEntity filter, Version version) {
        filters.add(filter, version);
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

        return form.getApplyObject(mGroupObjects.immutable());
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
}
