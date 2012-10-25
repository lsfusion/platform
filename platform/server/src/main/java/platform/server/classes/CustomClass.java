package platform.server.classes;

import platform.base.BaseUtils;
import platform.base.ImmutableObject;
import platform.base.QuickSet;
import platform.interop.Data;
import platform.server.auth.SecurityPolicy;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ManualLazy;
import platform.server.classes.sets.ConcreteCustomClassSet;
import platform.server.classes.sets.CustomClassSet;
import platform.server.classes.sets.UpClassSet;
import platform.server.data.expr.query.Stat;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.form.entity.*;
import platform.server.form.instance.CustomObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.navigator.NavigatorElement;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.BusinessLogics;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.ChangeClassActionProperty;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public abstract class CustomClass extends ImmutableObject implements ObjectClass, ValueClass {

    public Type getType() {
        return ObjectType.instance;
    }
    public Stat getTypeStat() {
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
        assert parent.childs==null;
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
        return parentClass.getChilds().contains(this);
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

    public CustomClassSet commonParents(CustomClass toCommon) {
        commonClassSet1(true);
        toCommon.commonClassSet2(false,null,true);

        CustomClassSet result = new CustomClassSet();
        commonClassSet3(result,null,true);
        return result;
    }

    Map<CustomClass,CustomClassSet> cacheChilds = new HashMap<CustomClass,CustomClassSet>();

    // получает классы у которого есть оба интерфейса
    public CustomClassSet commonChilds(CustomClass toCommon) {
        CustomClassSet result = null;
        if(BusinessLogics.activateCaches) result = cacheChilds.get(toCommon);
        if(result!=null) return result;
        result = new CustomClassSet();
        commonClassSet1(false);
        toCommon.commonClassSet2(false,null,false);

        commonClassSet3(result,null,false);
        if(BusinessLogics.activateCaches) cacheChilds.put(toCommon,result);
        return result;
    }


    public void fillParents(Collection<CustomClass> parentSet) {
        if (parentSet.contains(this)) return;
        parentSet.add(this);

        for(CustomClass parent : parents)
            parent.fillParents(parentSet);
    }

    // заполняет список классов
    public void fillChilds(Set<CustomClass> classSet) {
        classSet.add(this);

        for(CustomClass child : children)
            child.fillChilds(classSet);
    }

    private Set<CustomClass> childs = null;
    @ManualLazy
    public Set<CustomClass> getChilds() {
        if(childs==null) {
            childs = new HashSet<CustomClass>();
            fillChilds(childs);
        }
        return childs;
    }

    // заполняет список классов
    public void fillConcreteChilds(Collection<ConcreteCustomClass> classSet) {
        if(this instanceof ConcreteCustomClass) {
            ConcreteCustomClass concreteThis = (ConcreteCustomClass) this;
            if(classSet.contains(concreteThis)) return;
            classSet.add(concreteThis);
        }

        for(CustomClass child : children)
            child.fillConcreteChilds(classSet);
    }

    // заполняет все нижние классы имплементации
    public abstract void fillNextConcreteChilds(ConcreteCustomClassSet classSet);

    public Collection<ConcreteCustomClass> getConcreteChildren() {

        Collection<ConcreteCustomClass> result = new ArrayList<ConcreteCustomClass>();
        fillConcreteChilds(result);
        return result;
    }

    public void getDiffSet(ConcreteObjectClass diffClass,Collection<CustomClass> addClasses,Collection<CustomClass> removeClasses) {
        if(diffClass instanceof UnknownClass) { // если неизвестный то все добавляем
            fillParents(addClasses);
            return;
        }

        commonClassSet1(true); // check
        if(diffClass!=null) ((CustomClass)diffClass).commonClassSet2(false,removeClasses,true);

        commonClassSet3(null,addClasses,true);
    }


    int check = 0;
    // 1-й шаг расставляем пометки 1
    private void commonClassSet1(boolean up) {
        if(check ==1) return;
        check = 1;
        for(CustomClass child : (up? parents : children))
            child.commonClassSet1(up);
    }

    // 2-й шаг пометки
    // 2 - верхний общий класс
    // 3 - просто общий класс
    private void commonClassSet2(boolean set,Collection<CustomClass> free,boolean up) {
        if(!set) {
            if(check >0) {
                if(check !=1) return;
                check = 2;
                set = true;
            } else
                if(free!=null) free.add(this);
        } else {
            if(check ==3 || check ==2) {
                check = 3;
                return;
            }

            check = 3;
        }

        for(CustomClass child : (up? parents : children))
            child.commonClassSet2(set,free,up);
    }

    // 3-й шаг выводит в Set, и сбрасывает пометки
    private void commonClassSet3(CustomClassSet common,Collection<CustomClass> free,boolean up) {
        if(check ==0) return;
        if(common!=null && check ==2) common.add(this);
        if(free!=null && check ==1) free.add(this);

        check = 0;

        for(CustomClass child : (up? parents : children))
            child.commonClassSet3(common,free,up);
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(Data.OBJECT);
        outStream.writeBoolean(this instanceof ConcreteCustomClass);
        outStream.writeUTF(caption);
        outStream.writeInt(ID);
        outStream.writeUTF(getSID());

        outStream.writeByte(children.size());
        for (CustomClass cls : children)
            cls.serialize(outStream);
    }

    private List<NavigatorElement> relevantElements = new ArrayList<NavigatorElement>();
    public void addRelevant(NavigatorElement element) {
        relevantElements.add(element);        
    }

    public List<NavigatorElement> getRelevantElements(BaseLogicsModule LM, SecurityPolicy securityPolicy) {
        List<CustomClass> upParents = new ArrayList<CustomClass>();
        fillParents(upParents);

        List<NavigatorElement> result = new ArrayList<NavigatorElement>();
        for(CustomClass parent : upParents)
            for (NavigatorElement element : parent.relevantElements)
                if (securityPolicy.navigator.checkPermission(element))
                    result.add(element);
        for(CustomClass parent : upParents)
            result.add(parent.getBaseClassForm(LM));
        return result;
    }

    private FormEntity baseClassForm = null;
    public FormEntity getBaseClassForm(BaseLogicsModule LM) {
        if(baseClassForm==null) {
            baseClassForm = getListForm(LM).form;
            for(CustomClass child : children) {
                FormEntity childForm = child.getBaseClassForm(LM);
                if (childForm.getParent() == null)
                    baseClassForm.add(childForm);
            }
        }
        return baseClassForm;
    }

    // проверяет находятся ли он и все верхние в OrObjectClassSet'е
    public boolean upInSet(UpClassSet upSet, ConcreteCustomClassSet set) {
        if(upSet.has(this)) return true; // по child'ам уже не идем они явно все тоже есть
        if(this instanceof ConcreteCustomClass && !set.contains((ConcreteCustomClass) this)) return false;
        for(CustomClass child : children)
            if(!child.upInSet(upSet, set)) return false;
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

    public static Set<IsClassProperty> getProperties(Set<? extends ValueClass> classes) {
        Set<IsClassProperty> result = new HashSet<IsClassProperty>();
        for(ValueClass valueClass : classes)
            result.add(valueClass.getProperty());
        return result;
    }

    public QuickSet<CalcProperty> getChildProps() {
        QuickSet<CalcProperty> result = new QuickSet<CalcProperty>();
        for(CustomClass customClass : getChilds()) {
            result.add(customClass.getProperty().getChanged(IncrementType.SET));
            result.add(customClass.getProperty().getChanged(IncrementType.DROP));
        }
        return result;
    }

    public QuickSet<CalcProperty> getChildDropProps(ConcreteObjectClass toClass) {
        QuickSet<CalcProperty> result = new QuickSet<CalcProperty>();
        for(CustomClass customClass : getChilds())
            if(!(toClass instanceof CustomClass && ((CustomClass)toClass).isChild(customClass)))
                result.add(customClass.getProperty().getChanged(IncrementType.DROP));
        return result;
    }

    public QuickSet<CalcProperty> getParentSetProps() {
        QuickSet<CalcProperty> result = new QuickSet<CalcProperty>();
        Collection<CustomClass> parents = new HashSet<CustomClass>();
        fillParents(parents);
        for(CustomClass parent : parents)
            result.add(parent.getProperty().getChanged(IncrementType.SET));
        return result;
    }

    public static Set<ChangedProperty> getChangeProperties(Set<CustomClass> addClasses, Set<CustomClass> removeClasses) {
        Set<ChangedProperty> result = new HashSet<ChangedProperty>();
        for(CustomClass addClass : addClasses)
            result.add(addClass.getProperty().getChanged(IncrementType.SET));
        for(CustomClass removeClass : removeClasses)
            result.add(removeClass.getProperty().getChanged(IncrementType.DROP));
        return result;
    }

    public static Set<IsClassProperty> getProperties(Set<CustomClass> addClasses, Set<CustomClass> removeClasses) {
        return getProperties(BaseUtils.mergeSet(addClasses, removeClasses));
    }

    public void fillChangeProps(ConcreteObjectClass cls, QuickSet<CalcProperty> fill) {
        Set<CustomClass> addClasses = new HashSet<CustomClass>();
        Set<CustomClass> removeClasses = new HashSet<CustomClass>();
        getDiffSet(cls, addClasses, removeClasses);
        fill.addAll(getChangeProperties(addClasses, removeClasses));
    }

    public static ActionProperty getChangeClassAction(ObjectClass cls) {
        return ChangeClassActionProperty.create(cls, false, cls.getBaseClass());
    }

    @IdentityLazy
    public ActionProperty getChangeClassAction() {
        return getChangeClassAction(this);
    }
}
