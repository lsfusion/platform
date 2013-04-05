package platform.server.form.entity.drilldown;

import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.MMap;
import platform.server.classes.ValueClass;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.*;

import java.util.ArrayList;
import java.util.List;

import static platform.base.BaseUtils.capitalize;
import static platform.base.BaseUtils.isRedundantString;
import static platform.server.logics.ServerResourceBundle.getString;

public class SumUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<SumUnionProperty.Interface, SumUnionProperty> {

    public SumUnionDrillDownFormEntity(String sID, String caption, SumUnionProperty property, BusinessLogics BL) {
        super(sID, caption, property, BL);
    }
}
