package lsfusion.server.logics.classes;

import lsfusion.base.BaseUtils;
import lsfusion.base.mutability.ImmutableObject;
import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWSVSMap;
import lsfusion.interop.form.property.DataType;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.caches.IdentityStartLazy;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.logics.classes.sets.OrObjectClassSet;
import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.logics.classes.sets.ResolveUpClassSet;
import lsfusion.server.logics.classes.sets.UpClassSet;
import lsfusion.server.base.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.form.interactive.instance.object.CustomObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.CanonicalNameUtils;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.form.auto.ClassFormEntity;
import lsfusion.server.logics.form.auto.DialogFormEntity;
import lsfusion.server.logics.form.auto.EditFormEntity;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.physics.dev.debug.ClassDebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.language.linear.LAP;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.interfaces.NFDefault;
import lsfusion.server.logics.mutables.interfaces.NFOrderSet;
import lsfusion.server.logics.mutables.interfaces.NFProperty;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.action.change.ChangeClassActionProperty;
import lsfusion.server.logics.action.flow.CaseActionProperty;
import lsfusion.server.logics.property.cases.ActionCase;
import lsfusion.server.logics.action.session.DataSession;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;

public abstract class CustomClass extends ImmutableObject implements ObjectClass, ValueClass {

    private ClassDebugInfo debugInfo;

    public Type getType() {
        return ObjectType.instance;
    }
    public Stat getTypeStat(boolean forJoin) {
        return Stat.ALOT;
    }

    public boolean dialogReadOnly = false;

    public final NFOrderSet<CustomClass> parents = NFFact.orderSet();
    public Iterable<CustomClass> getParentsIt() { // есть риск нарваться на NF, хотя может и нет так как классы первым прогоном собираются
        return parents.getIt();
    }
    public Iterable<CustomClass> getParentsListIt() {
        return parents.getListIt();
    }        
    public boolean containsNFParents(CustomClass customClass, Version version) {
        return parents.containsNF(customClass, version);
    }
    public Iterable<CustomClass> getNFParentsIt(Version version) {
        return parents.getNFIt(version);
    }

    public final NFOrderSet<CustomClass> children = NFFact.orderSet();
    public ImSet<CustomClass> getChildren() {
        return children.getSet();
    }
    public Iterable<CustomClass> getChildrenIt() {
        return children.getIt();
    }

    public String toString() {
        String result = getCanonicalName();
        if (caption != null) {
            result += " '" + ThreadLocalContext.localize(caption) + "'";
        }
        if (debugInfo != null) {
            result += " [" + debugInfo + "]";
        }
        return result;
    }

    public Long ID;
    private String canonicalName;

    public String getSID() {
        return CanonicalNameUtils.toSID(canonicalName);
    }
    
    public String getCanonicalName() {
        return canonicalName;
    }

    public String getName() {
        return CanonicalNameUtils.getName(canonicalName);
    }
    
    public String getParsedName() {
        return getCanonicalName();
    }

    public LocalizedString caption;
    public CustomClass(String canonicalName, LocalizedString caption, Version version, ImList<CustomClass> parents) {
        this.canonicalName = canonicalName;
        this.caption = caption;

        for (CustomClass parent : parents) {
            addParentClass(parent, version);
        }
    }

    public final void addParentClass(CustomClass parent, Version version) {
        this.parents.add(parent, version);
        parent.children.add(this, version);
        assert checkParentChildsCaches(parent, version);
    }

    private boolean checkParentChildsCaches(CustomClass parent, Version version) {
        assert parent.allChildren == null;
        for(CustomClass recParent : parent.getNFParentsIt(version))
            checkParentChildsCaches(recParent, version);
        return true;
    }

    @Override
    public Object getDefaultValue() {
        return null;
    }

    @Override
    public LocalizedString getCaption() {
        return caption;
    } 

    public boolean hasChildren() {
        return !getChildren().isEmpty();
    }

    private UpClassSet up = null;
    @ManualLazy
    public UpClassSet getUpSet() {
        if(up == null)
            up = new UpClassSet(this);
        return up;
    }

    @Override
    public ResolveClassSet getResolveSet() {
        return new ResolveUpClassSet(this);
    }

    public BaseClass getBaseClass() {
        return getParentsIt().iterator().next().getBaseClass();
    }

    public boolean isChild(CustomClass parentClass) {
        return parentClass.getAllChildren().contains(this);
    }

    public boolean isCompatibleParent(ValueClass remoteClass) {
        return remoteClass instanceof CustomClass && ((CustomClass)remoteClass).isChild(this);
    }

    public CustomClass findClassID(long idClass) {
        if(ID!=null && ID == idClass) return this; // проверка чисто для fillIDs

        for(CustomClass child : getChildrenIt()) {
            CustomClass findClass = child.findClassID(idClass);
            if(findClass!=null) return findClass;
        }

        return null;
    }

    public ConcreteCustomClass findConcreteClassID(long idClass) {
        CustomClass cls = findClassID(idClass);
        if (cls == null)
            throw new RuntimeException(ThreadLocalContext.localize("{classes.there.is.an.object.of.not.existing.class.in.the.database} : " + idClass + ")"));
        if (! (cls instanceof ConcreteCustomClass))
            throw new RuntimeException(ThreadLocalContext.localize("{classes.there.is.an.object.of.abstract.class.in.the.database} : " + idClass + ")"));
        return (ConcreteCustomClass) cls;
    }

    private final static LRUWSVSMap<CustomClass, CustomClass, ImSet<CustomClass>> cacheChilds = new LRUWSVSMap<>(LRUUtil.G2);

    public void fillParents(MSet<CustomClass> parentSet) {
        if (parentSet.add(this)) return;

        for(CustomClass parent : getParentsIt())
            parent.fillParents(parentSet);
    }

    // заполняет список классов
    public void fillChilds(MSet<CustomClass> classSet) {
        classSet.add(this);

        for(CustomClass child : getChildrenIt())
            child.fillChilds(classSet);
    }

    private ImSet<CustomClass> allChildren = null;
    @ManualLazy
    public ImSet<CustomClass> getAllChildren() {
        if(allChildren ==null) {
            allChildren = BaseUtils.getAllChildren(this, getChildren);
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

    @IdentityStartLazy
    public ImSet<CustomClass> getAllChildrenParents() {
        ImSet<CustomClass> children = getChildren();
        if(children.isEmpty())
            return getAllParents();
        MSet<CustomClass> mResult = SetFact.mSet();
        for(CustomClass child : children)
            mResult.addAll(child.getAllChildrenParents());
        return mResult.immutable();            
    }

    // заполняет список классов
    public void fillConcreteChilds(MSet<ConcreteCustomClass> classSet) {
        if(this instanceof ConcreteCustomClass) {
            ConcreteCustomClass concreteThis = (ConcreteCustomClass) this;
            if(classSet.add(concreteThis)) return;
        }

        for(CustomClass child : getChildrenIt())
            child.fillConcreteChilds(classSet);
    }

    // заполняет все нижние классы имплементации
    public abstract void fillNextConcreteChilds(MSet<ConcreteCustomClass> mClassSet);

    private static BaseUtils.ExChildrenInterface<CustomClass> getChildren = new BaseUtils.ExChildrenInterface<CustomClass>() {
        public ImSet<CustomClass> getAllChildren(CustomClass element) {
            return element.getAllChildren();
        }

        public Iterable<CustomClass> getChildrenIt(CustomClass element) {
            return element.getChildrenIt();
        }
    };

    protected static BaseUtils.ExChildrenInterface<CustomClass> getParents = new BaseUtils.ExChildrenInterface<CustomClass>() {
        public ImSet<CustomClass> getAllChildren(CustomClass element) {
            return element.getAllParents();
        }

        public Iterable<CustomClass> getChildrenIt(CustomClass element) {
            return element.getParentsIt();
        }
    };

    // получает классы у которого есть оба интерфейса
    public ImSet<CustomClass> commonChilds(CustomClass toCommon) {
        ImSet<CustomClass> result;

        result = cacheChilds.get(this, toCommon);
        if(result!=null) return result;
        
        result = BaseUtils.commonChildren(this, toCommon, getChildren);
        cacheChilds.put(this, toCommon, result);
        return result;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(DataType.OBJECT);
        outStream.writeBoolean(this instanceof ConcreteCustomClass);
        outStream.writeUTF(caption.getSourceString());
        outStream.writeLong(ID);
        outStream.writeUTF(getSID());

        ImSet<CustomClass> children = getChildren();
        outStream.writeInt(children.size());
        for (CustomClass cls : children)
            cls.serialize(outStream);
    }

    public ClassDebugInfo getDebugInfo() {
        return debugInfo;
    }

    public void setDebugInfo(ClassDebugInfo debugInfo) {
        this.debugInfo = debugInfo;
    }

    // проверяет находятся ли он и все верхние в OrObjectClassSet'е
    public boolean upInSet(UpClassSet upSet, ImSet<ConcreteCustomClass> set) {
        if(upSet.has(this))
            return true; // по child'ам уже не идем они явно все тоже есть
        if(this instanceof ConcreteCustomClass && !set.contains((ConcreteCustomClass) this))
            return false;
        for(CustomClass child : getChildrenIt())
            if(!child.upInSet(upSet, set))
                return false;
        return true;
    }
    public boolean upInSet(CustomClass[] wheres, int numWheres, CustomClass[] proceeded, int numProceeded, CustomClass check) {
        if(isChild(check)) return true;
        for(int i=0;i<numWheres;i++) if(wheres[i]!=null && isChild(wheres[i])) return true;
        for(int i=0;i<numProceeded;i++) if(isChild(proceeded[i])) return true;

        if(this instanceof ConcreteCustomClass) return false;
        for(CustomClass child : getChildrenIt())
            if(!child.upInSet(wheres,numWheres, proceeded, numProceeded, check)) return false;
        return true;
    }

    public abstract ConcreteCustomClass getSingleClass();

    public ObjectInstance newInstance(ObjectEntity entity) {
        return new CustomObjectInstance(entity, this);
    }

    public boolean disableSingleApply() {
        return this instanceof BaseClass; // тут возможно стоило бы еще другие абстрактные классы с большим количеством children'ов исключить, но кроме Object'а пока таких нет 
    }

    private abstract class ClassFormHolder {
        private NFProperty<ClassFormEntity> form = NFFact.property();
        private boolean isUsed = false;

        public ClassFormEntity getForm(final BaseLogicsModule LM) {
            return form.getDefault(new NFDefault<ClassFormEntity>() {
                public ClassFormEntity create() {
                    return addDefaultForm(LM);
                }
            });
        }

        public ClassFormEntity getPolyForm(final BaseLogicsModule LM, ConcreteCustomClass concreteClass) {
            // deprecated ветка 
//            if(CustomClass.this instanceof ConcreteCustomClass && !hasPolyForm())
//                return getForm(LM);
            
            if(concreteClass == null) // нет объекта вызываем default (по сути если не передается объект)
                return getForm(LM);

            assert concreteClass.isChild(CustomClass.this);
            ClassFormEntity result = getFormHolder(concreteClass).getRecPolyForm(CustomClass.this);
            if(result == null)
                result = getFormHolder(concreteClass).getRecDefaultForm(CustomClass.this, LM);
            return result;
        }
        
        protected abstract LAP<?> getPolyAction(BaseLogicsModule LM);

        // если есть хоть один child poly form или реализация в полиморфного метода
        protected boolean hasImplementation(BaseLogicsModule LM) {
            if(hasPolyForm())
                return true;

            CaseActionProperty polyAction = (CaseActionProperty) getPolyAction(LM).property;
            ImList<ActionCase<PropertyInterface>> cases = polyAction.getOptimizedCases(MapFact.singleton(polyAction.interfaces.single(), getUpSet()), SetFact.<PropertyInterface>EMPTY());
            if(cases.size() > 1) // если есть edit кроме default'ого поведения
                return true;
            
            return false;            
        }
        
        @IdentityLazy
        protected ClassFormEntity getRecDefaultForm(CustomClass baseClass, BaseLogicsModule LM) {
            if(isUsed) { // this heuristics is needed because if we have abstract A and concrete B with specified edit form / action, we don't want to show edit form for A because it's definitely not what developer meant 
                if(hasImplementation(LM))
                    return null; // optimization, exiting because if this class has implementation, isUsed parents will also have it
                return getFormHolder(CustomClass.this).getForm(LM);
            }
            
            for(CustomClass parentClass : getParentsIt()) 
                if(parentClass.isChild(baseClass)) {
                    ClassFormEntity parentForm = getFormHolder(parentClass).getRecDefaultForm(baseClass, LM);
                    if(parentForm != null)
                        return parentForm;
                }
            
            return null;            
        }
        
        @IdentityLazy
        protected boolean hasPolyForm() { // есть ли у child'ов хоть одна определенная форма
            ClassFormEntity setForm = form.get(); // хотя с default'ом все равно есть опасность для верхних классов у которых изменится логика polyForm, лучше отдельно кэш сделать
            if(setForm != null)
                return true;
            
            for(CustomClass child : getChildrenIt())
                if(getFormHolder(child).hasPolyForm())
                    return true;
            return false;
        }
        
        protected abstract ClassFormHolder getFormHolder(CustomClass cls);

        // бежим по children в детерминированном порядке от которого зависит concreteClass (если он есть) и ищем подходящую форму
        @IdentityLazy
        private ClassFormEntity getRecPolyForm(CustomClass baseClass) {
            ClassFormEntity polyForm = form.get();  // хотя с default'ом все равно есть опасность для верхних классов у которых изменится логика polyForm, лучше отдельно кэш сделать
            if(polyForm != null)
                return polyForm;

            if(BaseUtils.hashEquals(CustomClass.this, baseClass)) // оптимизация
                return null;
                
            for(CustomClass parentClass : getParentsListIt())
                if(parentClass.isChild(baseClass)) {
                    ClassFormEntity parentForm = getFormHolder(parentClass).getRecPolyForm(baseClass);
                    if(parentForm != null)
                        return parentForm;
                }
            
            return null;
        }

        @IdentityStrongLazy
        private ClassFormEntity addDefaultForm(BaseLogicsModule LM) {
            ClassFormEntity classForm = createDefaultForm(LM);
            LM.addFormEntity(classForm.form);
            return classForm;
        }

        public void setForm(ClassFormEntity form, Version version) {
            this.form.set(form, version);
        }

        protected abstract ClassFormEntity createDefaultForm(BaseLogicsModule LM);
    }

    private ClassFormHolder dialogFormHolder = new ClassFormHolder() {
        @Override
        protected ClassFormHolder getFormHolder(CustomClass cls) {
            return cls.dialogFormHolder;
        }

        @Override
        protected LAP<?> getPolyAction(BaseLogicsModule LM) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected ClassFormEntity createDefaultForm(BaseLogicsModule LM) {
            DialogFormEntity dialogFormEntity = new DialogFormEntity(LM, CustomClass.this);
            return new ClassFormEntity(dialogFormEntity, dialogFormEntity.object);
        }
    };

    private ClassFormHolder editFormHolder = new ClassFormHolder() {
        @Override
        protected ClassFormHolder getFormHolder(CustomClass cls) {
            return cls.editFormHolder;
        }

        @Override
        protected LAP<?> getPolyAction(BaseLogicsModule LM) {
            return LM.getPolyEdit();
        }

        @Override
        protected ClassFormEntity createDefaultForm(BaseLogicsModule LM) {
            EditFormEntity editFormEntity = new EditFormEntity(LM, CustomClass.this);
            return new ClassFormEntity(editFormEntity, editFormEntity.object);
        }
    };


    public void markUsed(boolean edit) {
        if(this instanceof BaseClass) // default реализацию edit исключаем из этой эвристики (SHOW OBJECT Object) 
            return;
        
        if(edit)
            editFormHolder.isUsed = true;
        else
            dialogFormHolder.isUsed = true;
    }

    /**
     * используются при редактировании свойства даного класса из диалога, т.е. фактически для выбора объекта данного класса
     * @param LM
     */
    public ClassFormEntity getDialogForm(BaseLogicsModule LM) {
        return dialogFormHolder.getForm(LM);
    }
//    public ClassFormEntity getDialogForm(BaseLogicsModule LM, Version version) {
//        return dialogFormHolder.getNFForm(LM, version);
//    }

    public void setDialogForm(FormEntity form, ObjectEntity object, Version version) {
        dialogFormHolder.setForm(new ClassFormEntity(form, object), version);
    }

    /**
     * используется для редактирования конкретного объекта данного класса
     * @param LM
     * @param session
     */
    public ClassFormEntity getEditForm(BaseLogicsModule LM, DataSession session, ObjectValue concreteObject) throws SQLException, SQLHandledException {
        ConcreteCustomClass concreteCustomClass = null;
        
        ConcreteClass concreteClass = null;
        if(concreteObject instanceof DataObject && (concreteClass = session.getCurrentClass((DataObject)concreteObject)) instanceof ConcreteCustomClass)
            concreteCustomClass = (ConcreteCustomClass) concreteClass;
            
        return editFormHolder.getPolyForm(LM, concreteCustomClass);
    }

    public void setEditForm(FormEntity form, ObjectEntity object, Version version) {
        editFormHolder.setForm(new ClassFormEntity(form, object), version);
    }

    public static IsClassProperty getProperty(ValueClass valueClass) {
        return new IsClassProperty(LocalizedString.concat(valueClass.getCaption(), LocalizedString.create("{logics.pr}")), valueClass);
    }

    private IsClassProperty property;
    @ManualLazy
    public IsClassProperty getProperty() {
        if(property==null)
            property = getProperty(this);
        return property;
    }

    // использование в aspectChangeExtProps у NEW, ChangeClass и т.п.
    public void fillChangedProps(MExclSet<CalcProperty> mSet, IncrementType type) {
        getProperty().fillChangedProps(mSet, type);
    }
    public ImSet<ClassDataProperty> getUpDataProps() {
        return getUpObjectClassFields().keys().mapSetValues(new GetValue<ClassDataProperty, ObjectClassField>() {
            public ClassDataProperty getMapValue(ObjectClassField value) {
                return value.getProperty();
            }});
    }

    public ImSet<CalcProperty> getUpAllChangedProps() {
        ImSet<CustomClass> childs = getAllChildren();
        MExclSet<CalcProperty> mResult = SetFact.mExclSet();
        for(CustomClass customClass : childs) {
            customClass.fillChangedProps(mResult, IncrementType.SET);
            customClass.fillChangedProps(mResult, IncrementType.DROP);
        }
        return mResult.immutable();
    }

//    public ImSet<CalcProperty> getChildDropProps(final ConcreteObjectClass toClass) {
//        MExclSet<CalcProperty> mResult = SetFact.mExclSet();
//        for(CustomClass child : getAllChildren())
//            if(!(toClass instanceof CustomClass && ((CustomClass) toClass).isChild(child)))
//                child.fillChangedProps(mResult, IncrementType.DROP);
//        return mResult.immutable();
//    }

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

    public static ActionProperty getChangeClassAction(ObjectClass cls) {
        return ChangeClassActionProperty.create(cls, false, cls.getBaseClass());
    }

    @IdentityStrongLazy // для ID
    public ActionProperty getChangeClassAction() {
        return getChangeClassAction(this);
    }

    public ImMap<ObjectClassField,ObjectValueClassSet> getUpObjectClassFields() {
        return BaseUtils.immutableCast(getUpClassFields(true));
    }

    public ImMap<IsClassField,ObjectValueClassSet> getUpIsClassFields() {
        return getUpClassFields(false);
    }

        // в отличии от getMapTables, для того чтобы ходить "вверх" было удобно
    private IsClassField isClassField;

    public void setIsClassField(IsClassField isClassField) {
        this.isClassField = isClassField;
    }

    @IdentityLazy
    public ImMap<IsClassField,ObjectValueClassSet> getUpClassFields(boolean onlyObjectClassFields) {
        if(isClassField != null && (!onlyObjectClassFields || isClassField instanceof ObjectClassField)) // последняя проверка оптимизационная
            return MapFact.<IsClassField, ObjectValueClassSet>singleton(isClassField, getUpSet());

        MMap<IsClassField, ObjectValueClassSet> mMap = MapFact.mMap(OrObjectClassSet.<IsClassField>objectValueSetAdd());
        for(CustomClass customClass : getChildrenIt())
            mMap.addAll(customClass.getUpClassFields(onlyObjectClassFields));
        if(this instanceof ConcreteCustomClass) // глуповато конечно,
            mMap.add(((ConcreteCustomClass)this).dataProperty, (ConcreteCustomClass)this);
        return pack(mMap.immutable().toRevExclMap(), onlyObjectClassFields, getUpSet());
    }


    public static ImRevMap<IsClassField, ObjectValueClassSet> pack(ImRevMap<IsClassField, ObjectValueClassSet> map, boolean onlyObjectClassFields, ObjectValueClassSet baseClassSet) {
        // паковка по идее должна включать в себя случай, когда есть ClassField который полностью покрывает одну из таблиц, то эффективнее ее долить в ClassField, аналогичная оптимизация на количества ClassValueWhere
        // но пока из способа определения ClassField'а и методологии определения FULL, это большая редкость

        // во многом оптимизация, но важно что дает детерменированность что для Abstract не вернется Concrete, а для Concrete - Abstract что приведет к бесконечной паковке, например в GroupExpr.followFalse
        if(map.size() == 1) {
            assert map.singleValue().containsAll(baseClassSet, false) && baseClassSet.containsAll(map.singleValue(), false);
            return MapFact.singletonRev(map.singleKey(), baseClassSet);
        }

        return map;
    }

    public boolean isComplex;

    @IdentityLazy
    public boolean hasComplex(boolean up) {
        if(isComplex)
            return true;

        for(CustomClass customClass : (up ? getParentsIt() : getChildrenIt()))
            if(customClass.hasComplex(up))
                return true;
        return false;
    }

    public boolean hasComplex() {
        return hasComplex(true) || hasComplex(false);
    }
    
    public ImSet<CalcProperty> aggrProps = SetFact.EMPTY(); // все свойство с одним параметром

    @IdentityLazy
    public ImSet<CalcProperty> getUpAggrProps() {
        MSet<CalcProperty> mUpAggrProps = SetFact.mSet(aggrProps);
        for(CustomClass parent : getParentsIt())
            mUpAggrProps.addAll(parent.getUpAggrProps());        
        final ImSet<CalcProperty> upAggrProps = mUpAggrProps.immutable();
        
        // вырезаем те для кого implements уже есть
        return upAggrProps.filterFn(new SFunctionSet<CalcProperty>() {
            public boolean contains(CalcProperty element) {
                return !upAggrProps.intersect(((CalcProperty<?>)element).getImplements());
            }
        });
    }

}
