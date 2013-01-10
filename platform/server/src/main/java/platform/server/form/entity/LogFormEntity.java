package platform.server.form.entity;

import org.apache.poi.hssf.record.LeftMarginRecord;
import platform.base.col.ListFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.interop.ClassViewType;
import platform.interop.PropertyEditType;
import platform.server.classes.ValueClass;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.BusinessLogics;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static platform.server.logics.PropertyUtils.mapCalcImplement;
import static platform.server.logics.PropertyUtils.readCalcImplements;

/**
 * User: DAle
 * Date: 29.04.11
 * Time: 15:43
 */

/// Common usage:
/// LP<?> property - logging property
/// LP<?> logProperty = addLProp(property);
/// ...
/// LogFormEntity logForm = new LogFormEntity("FormSID", "FormCaption", property, logProperty, SomeBusinessLogics.this);
/// addPropertyDraw(addMFAProp("Caption", logForm, logForm.params), paramObjectEntities);

public class LogFormEntity<T extends BusinessLogics<T>> extends FormEntity<T> {
    public ObjectEntity[] params;
    BaseLogicsModule<?> LM;
    ObjectEntity[] entities;
    ObjectEntity objSession;
    LCP<?> logProperty;
    LCP<?> property;
    public boolean lazyInit;

    public LogFormEntity(String sID, String caption, LCP<?> property, LCP<?> logProperty, BaseLogicsModule<?> LM, boolean lazyInit) {
        super(sID, caption);
        this.LM = LM;
        this.logProperty = logProperty;
        this.property = property;
        this.lazyInit = lazyInit;

        ValueClass[] classes = getValueClassesList(property);
        entities = new ObjectEntity[classes.length + 1];

        GroupObjectEntity paramsGroup = new GroupObjectEntity(0, "paramsGroup");
        paramsGroup.setInitClassView(ClassViewType.PANEL);

        int index = 1;
        for (ValueClass valueClass : classes) {
            ObjectEntity obj = new ObjectEntity(index, "param" + index, valueClass, valueClass.getCaption());
            entities[index-1] = obj;
            paramsGroup.add(obj);
            index++;
        }

        params = Arrays.copyOf(entities, classes.length);

        GroupObjectEntity logGroup = new GroupObjectEntity(classes.length + 1, "logGroup");
        objSession = new ObjectEntity(classes.length + 2, "session", LM.getBL().systemEventsLM.session, ServerResourceBundle.getString("form.entity.session"));
        entities[classes.length] = objSession;
        logGroup.add(objSession);

        addGroupObject(paramsGroup);
        addGroupObject(logGroup);

        if (!lazyInit)
            initProperties();
    }

    public void initProperties() {
        for (ObjectEntity obj : entities) {
            addPropertyDraw(obj, LM.recognizeGroup, true);
        }

        addPropertyDraw(logProperty, entities);

        ImList<PropertyClassImplement> recognizePropImpls =
                LM.recognizeGroup.getProperties(SetFact.singleton(SetFact.singleton(new ValueClassWrapper(property.property.getValueClass()))), true);

        for (PropertyClassImplement impl : recognizePropImpls) {
            if(impl instanceof CalcPropertyClassImplement) {
                CalcPropertyClassImplement<?> calcImpl = ((CalcPropertyClassImplement)impl);
                int paramCnt = logProperty.property.interfaces.size();
                ImOrderSet<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(paramCnt);

                LCP lpMainProp = new LCP(calcImpl.property);

                Object[] params = new Object[paramCnt + 1];
                params[0] = logProperty;
                for (int i = 0; i < paramCnt; i++) {
                    params[i+1] = i+1;
                }
                JoinProperty<?> jProp = new JoinProperty("LogForm_" + impl.property.getSID(), impl.property.caption,
                        listInterfaces, false, mapCalcImplement(lpMainProp, readCalcImplements(listInterfaces, params)));
                jProp.inheritFixedCharWidth(impl.property);
                LCP<?> ljProp = new LCP<JoinProperty.Interface>(jProp, listInterfaces);
                addPropertyDraw(ljProp, entities);
            }
        }

        addFixedFilter(new NotNullFilterEntity(addPropertyObject(logProperty, entities)));

        setEditType(PropertyEditType.READONLY);
    }

    private static ValueClass[] getValueClassesList(LCP<?> property) {
        ImMap<PropertyInterface, ValueClass> interfaces = (ImMap<PropertyInterface, ValueClass>) property.property.getInterfaceClasses();
        ValueClass[] classes = new ValueClass[interfaces.size()];
        int index = 0;
        for (PropertyInterface pi : property.property.interfaces) {
            classes[index++] = interfaces.get(pi);
        }
        return classes;
    }
}
