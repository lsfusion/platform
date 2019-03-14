package lsfusion.server.physics.admin.logging;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.property.CalcPropertyClassImplement;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.property.PropertyClassImplement;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.property.infer.ClassType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.property.*;

import static lsfusion.server.logics.property.oraction.ActionOrPropertyUtils.mapCalcImplement;
import static lsfusion.server.logics.property.oraction.ActionOrPropertyUtils.readCalcImplements;

/// Common usage:
/// LP<?> property - logging property
/// LP<?> logValueProperty = addLProp(property);
/// ...
/// LogFormEntity logForm = new LogFormEntity("FormSID", "FormCaption", property, logValueProperty, SomeBusinessLogics.this);
/// addPropertyDraw(addMFAProp("Caption", logForm, logForm.params), paramObjectEntities);

public class LogFormEntity extends FormEntity {
    public ImOrderSet<ObjectEntity> params;
    SystemEventsLogicsModule systemEventsLM;
    ImOrderSet<ObjectEntity> entities;
    ObjectEntity objSession;
    LCP<?> logValueProperty;
    LCP<?> logWhereProperty;
    LCP<?> property;
    public boolean lazyInit;

    public LogFormEntity(String canonicalName, LocalizedString caption, LCP<?> property, LCP<?> logValueProperty, LCP<?> logWhereProperty, SystemEventsLogicsModule systemEventsLM) {
        super(canonicalName, caption, systemEventsLM.getVersion());

        this.systemEventsLM = systemEventsLM;
        this.logValueProperty = logValueProperty;
        this.logWhereProperty = logWhereProperty;
        this.property = property;

        Version version = getVersion();

        ValueClass[] classes = getValueClassesList(property);
        MOrderExclSet<ObjectEntity> mParams = SetFact.mOrderExclSet(classes.length);
        
        // не в одном group Object так как при поиске значений может давать "декартово произведение" (не все субд умеют нормально разбирать такие случаи), вообще должно решаться в общем случае, но пока так
        int index = 1;
        for (ValueClass valueClass : classes) {
            String sID = "param" + index;

            GroupObjectEntity paramGroup = new GroupObjectEntity(genID(), sID + "Group");
            paramGroup.setPanelClassView();

            ObjectEntity obj = new ObjectEntity(genID(), sID, valueClass, valueClass != null ? valueClass.getCaption() : LocalizedString.NONAME, valueClass == null);
            mParams.exclAdd(obj);
            paramGroup.add(obj);
            index++;

            addGroupObject(paramGroup, version);
        }

        params = mParams.immutableOrder();

        GroupObjectEntity logGroup = new GroupObjectEntity(genID(), "logGroup");
        objSession = new ObjectEntity(genID(), "session", systemEventsLM.session, LocalizedString.create("{form.entity.session}"));
        entities = params.addOrderExcl(objSession);
        logGroup.add(objSession);

        addGroupObject(logGroup, version);

        initProperties();
        
        // finalizeInit внутри initMainLogic
    }

    private Version getVersion() {
        return systemEventsLM.getVersion();
    }

    public void initProperties() {
        Version version = getVersion();

        for (ObjectEntity obj : entities) {
            addPropertyDraw(obj, version, systemEventsLM.baseLM.getRecognizeGroup());
        }

        addPropertyDraw(logValueProperty, version, entities);

        ImList<PropertyClassImplement> recognizePropImpls =
                systemEventsLM.baseLM.getRecognizeGroup().getProperties(property.property.getValueClass(ClassType.logPolicy), version);

        for (PropertyClassImplement impl : recognizePropImpls) {
            if(impl instanceof CalcPropertyClassImplement) {
                CalcPropertyClassImplement<?> calcImpl = ((CalcPropertyClassImplement)impl);
                int paramCnt = logValueProperty.property.interfaces.size();
                ImOrderSet<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(paramCnt);

                LCP lpMainProp = new LCP(calcImpl.property);

                Object[] params = new Object[paramCnt + 1];
                params[0] = logValueProperty;
                for (int i = 0; i < paramCnt; i++) {
                    params[i+1] = i+1;
                }
                JoinProperty<?> jProp = new JoinProperty(impl.property.caption,
                        listInterfaces, mapCalcImplement(lpMainProp, readCalcImplements(listInterfaces, params)));
                jProp.drawOptions.inheritDrawOptions(impl.property.drawOptions);
                LCP<?> ljProp = new LCP<>(jProp, listInterfaces);
                addPropertyDraw(ljProp, version, entities);
            }
        }

        addFixedFilter(new FilterEntity(addPropertyObject(logWhereProperty, entities)), version);

        setNFEditType(PropertyEditType.READONLY, version);

        finalizeInit(version);
    }

    private static ValueClass[] getValueClassesList(LCP<?> property) {
        ImMap<PropertyInterface, ValueClass> interfaces = (ImMap<PropertyInterface, ValueClass>) property.property.getInterfaceClasses(ClassType.logPolicy);
        ValueClass[] classes = new ValueClass[interfaces.size()];
        int index = 0;
        for (PropertyInterface pi : property.property.interfaces) {
            classes[index++] = interfaces.get(pi);
        }
        return classes;
    }
}
