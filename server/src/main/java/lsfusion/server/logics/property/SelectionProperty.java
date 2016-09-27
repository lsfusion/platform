package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.interop.KeyStrokes;
import lsfusion.server.classes.LogicalClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.entity.filter.NotNullFilterEntity;
import lsfusion.server.form.entity.filter.RegularFilterEntity;
import lsfusion.server.form.entity.filter.RegularFilterGroupEntity;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.mutables.Version;

public class SelectionProperty extends SessionDataProperty {

    ValueClass[] classes;
    BaseLogicsModule LM;

    public SelectionProperty(ValueClass[] classes, BaseLogicsModule LM) {
        super(LocalizedString.create("{logics.property.select}"), classes, LogicalClass.instance);
        this.classes = classes;
        this.LM = LM;
    }

    @Override
    public boolean isField() {
        return true;
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> entity, FormEntity<?> form, Version version) {
        super.proceedDefaultDraw(entity, form, version);
        RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(form.genID(), version);
        PropertyObjectEntity<ClassPropertyInterface, ?> po = entity.propertyObject;
        filterGroup.addFilter(new RegularFilterEntity(form.genID(),
                new NotNullFilterEntity((CalcPropertyObjectEntity) po),
                LocalizedString.create("{logics.property.selected}"),
                KeyStrokes.getSelectionFilterKeyStroke()), false, version);
        form.addRegularFilterGroup(filterGroup, version);

        ImOrderMap<ClassPropertyInterface, ObjectEntity> objects = po.getMapObjectInstances().toOrderMap();
        Object[] params = new Object[objects.size() + 2];
        params[0] = LM.baseLM.defaultOverrideBackgroundColor;
        ObjectEntity[] map = new ObjectEntity[po.mapping.size()];
        for (int i = 0; i < objects.size(); i++) {
            params[i + 2] = i + 1;
            map[i] = objects.getValue(i);
        }
        if(po.property instanceof CalcProperty)
            params[1] = new LCP<>((CalcProperty<ClassPropertyInterface>) po.property, objects.keyOrderSet());
        else
            params[1] = new LAP<>((ActionProperty<ClassPropertyInterface>) po.property, objects.keyOrderSet()); 
        entity.getNFToDraw(form, version).propertyBackground = form.addPropertyObject(LM.addJProp(LM.and1, params), map);
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        propertyView.editKey = KeyStrokes.getSelectionPropertyKeyStroke();
        propertyView.editOnSingleClick = true;
    }
}
