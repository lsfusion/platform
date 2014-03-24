package lsfusion.server.classes;

import lsfusion.base.BaseUtils;
import lsfusion.base.ImmutableObject;
import lsfusion.base.SFunctionSet;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWSVSMap;
import lsfusion.interop.Data;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.IdentityStrongLazy;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.classes.sets.OrObjectClassSet;
import lsfusion.server.classes.sets.UpClassSet;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.instance.CustomObjectInstance;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.ServerResourceBundle;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.ChangeClassActionProperty;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public abstract class CustomClass extends ImmutableObject implements ObjectClass, ValueClass {

    public Type getType() {
        return ObjectType.instance;
    }
    public Stat getTypeStat(boolean forJoin) {
        return Stat.ALOT;
    }

    public boolean dialogReadOnly = false;

    public final Collection<CustomClass> parents;
    public final List<CustomClass> children;

    public String toString() {
        return caption + " (" + sID + ")";
    }

    public Integer ID;
    protected String sID;

    public String getSID() {
        return sID;
    }

    public String caption;
    public CustomClass(String sID, String caption, CustomClass... parents) {
        this.sID = sID;
        this.caption = caption;
        this.parents = new ArrayList<CustomClass>();
        children = new ArrayList<CustomClass>();

        for (CustomClass parent : parents) {
            addParentClass(parent);
        }
    }

    public final void addParentClass(CustomClass parent) {
        this.parents.add(parent);
        parent.children.add(this);
        assert checkParentChildsCaches(parent);
    }

    private boolean checkParentChildsCaches(CustomClass parent) {
        assert parent.allChildren == null;
        for(CustomClass recParent : parent.parents)
            checkParentChildsCaches(recParent);
        return true;
    }

    @Override
    public Object getDefaultValue() {
        return null;
    }

    @Override
    public String getCaption() {
        return caption;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public UpClassSet getUpSet() {
        return new UpClassSet(this);
    }

    public BaseClass getBaseClass() {
        return parents.iterator().next().getBaseClass();
    }

    public boolean isChild(CustomClass parentClass) {
        return parentClass.getAllChildren().contains(this);
    }

    public boolean isCompatibleParent(ValueClass remoteClass) {
        return remoteClass instanceof CustomClass && ((CustomClass)remoteClass).isChild(this);
    }

    public CustomClass findClassID(int idClass) {
        if(ID!=null && ID == idClass) return this; // проверка чисто для fillIDs

        for(CustomClass child : children) {
            CustomClass findClass = child.findClassID(idClass);
            if(findClass!=null) return findClass;
        }

        return null;
    }

    public ConcreteCustomClass findConcreteClassID(int idClass) {
        CustomClass cls = findClassID(idClass);
        if (cls == null)
            throw new RuntimeException(ServerResourceBundle.getString("classes.there.is.an.object.of.not.existing.class.in.the.database")+" : " + idClass + ")");
        if (! (cls instanceof ConcreteCustomClass))
            throw new RuntimeException(ServerResourceBundle.getString("classes.there.is.an.object.of.abstract.class.in.the.database")+" : " + idClass + ")");
        return (ConcreteCustomClass) cls;
    }

    public ImSet<CustomClass> commonParents(CustomClass toCommon) {
        MAddMap<CustomClass, Check> checks = commonClassSet1(true);
        toCommon.commonClassSet2(false,null,true,checks);

        MSet<CustomClass> result = SetFact.mSet();
        commonClassSet3(result,null,true,checks);
        return result.immutable();
    }

    private final static LRUWSVSMap<CustomClass, CustomClass, ImSet<CustomClass>> cacheChilds = new LRUWSVSMap<CustomClass, CustomClass, ImSet<CustomClass>>(LRUUtil.G2);

    // получает классы у которого есть оба интерфейса
    public ImSet<CustomClass> commonChilds(CustomClass toCommon) {
        ImSet<CustomClass> result;
        
        result = cacheChilds.get(this, toCommon);
        if(result!=null) return result;

        MAddMap<CustomClass, Check> checks = commonClassSet1(false);
        toCommon.commonClassSet2(false,null,false,checks);

        MSet<CustomClass> mResult = SetFact.mSet();
        commonClassSet3(mResult,null,false,checks);
        result = mResult.immutable();

        cacheChilds.put(this, toCommon, result);
        return result;
    }


    public void fillParents(MSet<CustomClass> parentSet) {
        if (parentSet.add(this)) return;

        for(CustomClass parent : parents)
            parent.fillParents(parentSet);
    }

    // заполняет список классов
    public void fillChilds(MSet<CustomClass> classSet) {
        classSet.add(this);

        for(CustomClass child : children)
            child.fillChilds(classSet);
    }

    private ImSet<CustomClass> allChildren = null;
    @ManualLazy
    public ImSet<CustomClass> getAllChildren() {
        if(allChildren ==null) {
            MSet<CustomClass> mChilds = SetFact.mSet();
            fillChilds(mChilds);
            allChildren = mChilds.immutable();
        }
        return allChildren;
    }

    private ImSet<CustomClass> allParents = null;
    @ManualLazy
    public ImSet<CustomClass> getAllParents() {
        if(allParents==null) {
            MSet<CustomClass> mParents = SetFact.mSet();
            fillParents(mParents);
            allParents = mParents.immutable();
        }
        return allParents;
    }

    // заполняет список классов
    public void fillConcreteChilds(MSet<ConcreteCustomClass> classSet) {
        if(this instanceof ConcreteCustomClass) {
            ConcreteCustomClass concreteThis = (ConcreteCustomClass) this;
            if(classSet.add(concreteThis)) return;
        }

        for(CustomClass child : children)
            child.fillConcreteChilds(classSet);
    }

    // заполняет все нижние классы имплементации
    public abstract void fillNextConcreteChilds(MSet<ConcreteCustomClass> mClassSet);

    public ImSet<ConcreteCustomClass> getConcreteChildren() {

        MSet<ConcreteCustomClass> mResult = SetFact.mSet();
        fillConcreteChilds(mResult);
        return mResult.immutable();
    }

    protected enum Check {
        FIRST, CLOSEST, COMMON 
    }
    
    // 1-й шаг расставляем пометки 1 
    protected MAddMap<CustomClass, Check> commonClassSet1(boolean up) {
        return MapFact.mAddOverrideMap((up ? getAllParents() : getAllChildren()).toMap(Check.FIRST));         
    }

    // 2-й шаг пометки
    // 2 - самый верхний \ нижний общий класс
    // 3 - остальные общие классы
    protected void commonClassSet2(boolean set, MSet<CustomClass> free,boolean up, MAddMap<CustomClass, Check> checks) {
        Check check = checks.get(this); 
        if(!set) {
            if(check!=null) {
                if(check != Check.FIRST) return;
                checks.add(this, Check.CLOSEST);
                set = true;
            } else
                if(free!=null) free.add(this);
        } else {
            if(check==Check.COMMON)
                return;
            
            checks.add(this, Check.COMMON);
            if(check==Check.CLOSEST)
                return;
        }

        for(CustomClass child : (up? parents : children))
            child.commonClassSet2(set,free,up,checks);
    }

    // 3-й шаг выводит в Set, и сбрасывает пометки
    protected void commonClassSet3(MSet<CustomClass> common,MSet<CustomClass> free,boolean up, MAddMap<CustomClass, Check> checks) {
        Check check = checks.get(this);
        if(check==null) return;
        if(common!=null && check==Check.CLOSEST) common.add(this);
        if(free!=null && check==Check.FIRST) free.add(this);

        for(CustomClass child : (up? parents : children))
            child.commonClassSet3(common,free,up,checks);
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(Data.OBJECT);
        outStream.writeBoolean(this instanceof ConcreteCustomClass);
        outStream.writeUTF(caption);
        outStream.writeInt(ID);
        outStream.writeUTF(getSID());

        outStream.writeInt(children.size());
        for (CustomClass cls : children)
            cls.serialize(outStream);
    }

    private FormEntity baseClassForm = null;

    public FormEntity getBaseClassForm(BaseLogicsModule LM) {
        if (baseClassForm == null) {
            baseClassForm = getListForm(LM).form;
            List<FormEntity> childrenList = new ArrayList<FormEntity>();
            for (CustomClass child : children) {
                FormEntity childForm = child.getBaseClassForm(LM);
                if (childForm.getParent() == null)
                    childrenList.add(childForm);
            }

            Collections.sort(childrenList, new FormEntityComparator());

            for (FormEntity childForm : childrenList) {
                baseClassForm.add(childForm);
            }
        }
        return baseClassForm;
    }

    static class FormEntityComparator implements Comparator<FormEntity> {
        public int compare(FormEntity f1, FormEntity f2) {
            if (f1.caption == null && f2.caption == null)
                return 0;
            if (f1.caption == null)
                return -1;
            if (f2.caption == null)
                return 1;
            return f1.caption.compareTo(f2.caption);
        }
    }

    // проверяет находятся ли он и все верхние в OrObjectClassSet'е
    public boolean upInSet(UpClassSet upSet, ImSet<ConcreteCustomClass> set) {
        if(upSet.has(this))
            return true; // по child'ам уже не идем они явно все тоже есть
        if(this instanceof ConcreteCustomClass && !set.contains((ConcreteCustomClass) this))
            return false;
        for(CustomClass child : children)
            if(!child.upInSet(upSet, set))
                return false;
        return true;
    }
    public boolean upInSet(CustomClass[] wheres, int numWheres, CustomClass[] proceeded, int numProceeded, CustomClass check) {
        if(isChild(check)) return true;
        for(int i=0;i<numWheres;i++) if(wheres[i]!=null && isChild(wheres[i])) return true;
        for(int i=0;i<numProceeded;i++) if(isChild(proceeded[i])) return true;

        if(this instanceof ConcreteCustomClass) return false;
        for(CustomClass child : children)
            if(!child.upInSet(wheres,numWheres, proceeded, numProceeded, check)) return false;
        return true;
    }

    public abstract ConcreteCustomClass getSingleClass();

    public ObjectInstance newInstance(ObjectEntity entity) {
        return new CustomObjectInstance(entity, this);
    }

    private abstract class ClassFormHolder {
        private ClassFormEntity form;

        public ClassFormEntity getForm(BaseLogicsModule LM) {
            if (form != null) {
                return form;
            }

            form = createDefaultForm(LM);
            LM.addFormEntity(form.form);

            return form;
        }

        public void setForm(ClassFormEntity form) {
            this.form = form;
        }

        protected abstract ClassFormEntity createDefaultForm(BaseLogicsModule LM);
    }

    private ClassFormHolder listFormHolder = new ClassFormHolder() {
        @Override
        protected ClassFormEntity createDefaultForm(BaseLogicsModule LM) {
            ListFormEntity listFormEntity = new ListFormEntity(LM, CustomClass.this);
            return new ClassFormEntity(listFormEntity, listFormEntity.object);
        }
    };

    private ClassFormHolder dialogFormHolder = new ClassFormHolder() {
        @Override
        protected ClassFormEntity createDefaultForm(BaseLogicsModule LM) {
            DialogFormEntity dialogFormEntity = new DialogFormEntity(LM, CustomClass.this);
            return new ClassFormEntity(dialogFormEntity, dialogFormEntity.object);
        }
    };

    private ClassFormHolder editFormHolder = new ClassFormHolder() {
        @Override
        protected ClassFormEntity createDefaultForm(BaseLogicsModule LM) {
            EditFormEntity editFormEntity = new EditFormEntity(LM, CustomClass.this);
            return new ClassFormEntity(editFormEntity, editFormEntity.object);
        }
    };

    /**
     * используются для классовых форм в навигаторе
     * @param LM
     */
    public ClassFormEntity getListForm(BaseLogicsModule LM) {
        return listFormHolder.getForm(LM);
    }

    public void setListForm(FormEntity form, ObjectEntity object) {
        listFormHolder.setForm(new ClassFormEntity(form, object));
    }

    /**
     * используются при редактировании свойства даного класса из диалога, т.е. фактически для выбора объекта данного класса
     * @param LM
     */
    public ClassFormEntity getDialogForm(BaseLogicsModule LM) {
        return dialogFormHolder.getForm(LM);
    }

    public void setDialogForm(FormEntity form, ObjectEntity object) {
        dialogFormHolder.setForm(new ClassFormEntity(form, object));
    }

    /**
     * используется для редактирования конкретного объекта данного класса
     * @param LM
     */
    public ClassFormEntity getEditForm(BaseLogicsModule LM) {
        return editFormHolder.getForm(LM);
    }

    public void setEditForm(FormEntity form, ObjectEntity object) {
        editFormHolder.setForm(new ClassFormEntity(form, object));
    }

    public static IsClassProperty getProperty(ValueClass valueClass) {
        return new IsClassProperty("cp_" + valueClass.getSID(), valueClass.getCaption() + ServerResourceBundle.getString("logics.pr"), valueClass);
    }

    private IsClassProperty property;
    @ManualLazy
    public IsClassProperty getProperty() {
        if(property==null)
            property = getProperty(this);
        return property;
    }

    public void fillChangedProps(MExclSet<CalcProperty> mSet, IncrementType type) { // использование в aspectChangeExtProps у ADDOBJ, ChangeClass и т.п.
        getProperty().fillChangedProps(mSet, type);
    }

    public static ImSet<IsClassProperty> getProperties(ImSet<? extends ValueClass> classes) {
        return ((ImSet<ValueClass>)classes).mapSetValues(new GetValue<IsClassProperty, ValueClass>() {
            public IsClassProperty getMapValue(ValueClass value) {
                return value.getProperty();
            }});
    }

    public static ImSet<ClassDataProperty> getDataProperties(ImSet<ConcreteCustomClass> classes) {
        return classes.mapMergeSetValues(new GetValue<ClassDataProperty, ConcreteCustomClass>() {
            public ClassDataProperty getMapValue(ConcreteCustomClass value) {
                return value.dataProperty;
            }});
    }

    public ImSet<CalcProperty> getChildProps() {
        ImSet<CustomClass> childs = getAllChildren();
        MExclSet<CalcProperty> mResult = SetFact.mExclSet();
        for(CustomClass customClass : childs) {
            customClass.fillChangedProps(mResult, IncrementType.SET);
            customClass.fillChangedProps(mResult, IncrementType.DROP);
        }
        return mResult.immutable();
    }

    public ImSet<CalcProperty> getChildDropProps(final ConcreteObjectClass toClass) {
        MExclSet<CalcProperty> mResult = SetFact.mExclSet();
        for(CustomClass child : getAllChildren())
            if(!(toClass instanceof CustomClass && ((CustomClass) toClass).isChild(child)))
                child.fillChangedProps(mResult, IncrementType.DROP);
        return mResult.immutable();
    }

    public ImSet<CalcProperty> getParentSetProps() {
        MExclSet<CalcProperty> mResult = SetFact.mExclSet();
        for(CustomClass parent : getAllParents())
            parent.fillChangedProps(mResult, IncrementType.SET);
        return mResult.immutable();
    }

    public static ImSet<CalcProperty> getChangeProperties(ImSet<CustomClass> addClasses, ImSet<CustomClass> removeClasses) {
        MExclSet<CalcProperty> mResult = SetFact.mExclSet();
        for(CustomClass addClass : addClasses)
            addClass.fillChangedProps(mResult, IncrementType.SET);
        for(CustomClass removeClass : removeClasses)
            removeClass.fillChangedProps(mResult, IncrementType.DROP);
        return mResult.immutable();
    }

    public static ImSet<CalcProperty<ClassPropertyInterface>> getProperties(ImSet<CustomClass> addClasses, ImSet<CustomClass> removeClasses, ImSet<ConcreteObjectClass> oldClasses, ImSet<ConcreteObjectClass> newClasses) {
        return SetFact.addExclSet(getProperties(addClasses.merge(removeClasses)), getDataProperties(BaseUtils.<ImSet<ConcreteCustomClass>>immutableCast(oldClasses.merge(newClasses).filterFn(new SFunctionSet<ConcreteObjectClass>() {
            public boolean contains(ConcreteObjectClass element) {
                return element instanceof ConcreteCustomClass;
            }
        }))));
    }

    public static ActionProperty getChangeClassAction(ObjectClass cls) {
        return ChangeClassActionProperty.create(cls, false, cls.getBaseClass());
    }

    @IdentityStrongLazy // для ID
    public ActionProperty getChangeClassAction() {
        return getChangeClassAction(this);
    }

    @IdentityLazy
    public ImMap<ClassField,ObjectValueClassSet> getUpTables() {
        MMap<ClassField, ObjectValueClassSet> mMap = MapFact.mMap(OrObjectClassSet.<ClassField>objectValueSetAdd());
        for(CustomClass customClass : children)
            mMap.addAll(customClass.getUpTables());
        if(this instanceof ConcreteCustomClass) // глуповато конечно,
            mMap.add(((ConcreteCustomClass)this).dataProperty, (ConcreteCustomClass)this);
        return mMap.immutable().toRevExclMap();
    }
}
