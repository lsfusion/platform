package platform.server.form.entity;

import platform.base.Result;
import platform.interop.ClassViewType;
import platform.server.classes.ValueClass;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.BusinessLogics;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;

import java.util.Arrays;
import java.util.List;

import static platform.server.logics.PropertyUtils.mapImplement;
import static platform.server.logics.PropertyUtils.readImplements;

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
    LP<?> logProperty;
    LP<?> property;
    public boolean lazyInit;

    public LogFormEntity(String sID, String caption, LP<?> property, LP<?> logProperty, BaseLogicsModule<?> LM, boolean lazyInit) {
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
        objSession = new ObjectEntity(classes.length + 2, "session", LM.session, ServerResourceBundle.getString("form.entity.session"));
        entities[classes.length] = objSession;
        logGroup.add(objSession);

        addGroup(paramsGroup);
        addGroup(logGroup);

        if (!lazyInit)
            initProperties();
    }

    public void initProperties() {
        for (ObjectEntity obj : entities) {
            addPropertyDraw(obj, LM.recognizeGroup, true);
        }
        addPropertyDraw(objSession, LM.baseGroup);

        addPropertyDraw(logProperty, entities);

        Result<ValueClass> value = new Result<ValueClass>();
        property.getCommonClasses(value);
        List<PropertyClassImplement> recognizePropImpls =
                LM.recognizeGroup.getProperties(Arrays.asList(Arrays.asList(new ValueClassWrapper(value.result))), true);

        for (PropertyClassImplement impl : recognizePropImpls) {
            int paramCnt = logProperty.property.interfaces.size();
            List<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(paramCnt);

            LP lpMainProp = new LP(impl.property);

            Object[] params = new Object[paramCnt + 1];
            params[0] = logProperty;
            for (int i = 0; i < paramCnt; i++) {
                params[i+1] = i+1;
            }
            JoinProperty<?> jProp = new JoinProperty("LogForm_" + impl.property.getSID(), impl.property.caption,
                    listInterfaces, false, mapImplement(lpMainProp, readImplements(listInterfaces, params)));
            jProp.inheritFixedCharWidth(impl.property);
            LP<?> ljProp = new LP<JoinProperty.Interface>(jProp, listInterfaces);
            addPropertyDraw(ljProp, entities);
        }

        addFixedFilter(new NotNullFilterEntity(addPropertyObject(logProperty, entities)));

        setReadOnly(true);
    }

    private static ValueClass[] getValueClassesList(LP<?> property) {
        Property.CommonClasses<?> commonClasses = property.property.getCommonClasses();
        ValueClass[] classes = new ValueClass[commonClasses.interfaces.size()];
        int index = 0;
        for (PropertyInterface pi : property.property.interfaces) {
            classes[index++] = commonClasses.interfaces.get(pi);
        }
        return classes;
    }
}
