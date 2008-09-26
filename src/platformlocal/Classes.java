/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.SQLException;
import java.util.*;

/**
 *
 * @author ME
 */
abstract class Class {
    Collection<Class> Parents;
    List<Class> Childs;
    
    Integer ID;
    String caption;
    Class(Integer iID, String icaption) {
        ID=iID;
        caption = icaption;
        Parents = new ArrayList<Class>();
        Childs = new ArrayList<Class>();
    }

    void AddParent(Class ParentClass) {
        // проверим что в Parent'ах нету этого класса
        for(Class Parent:Parents) 
            if(Parent.IsParent(ParentClass)) return;
        
        Iterator<Class> i = Parents.iterator();
        while(i.hasNext())
            if(ParentClass.IsParent(i.next())) i.remove();
            
        Parents.add(ParentClass);
        ParentClass.Childs.add(this);        
    }

    boolean IsParent(Class Class) {
        if(Class==this) return true;

        for(Class Parent : Parents)
            if (Parent.IsParent(Class)) return true;
        
        return false;
    }
    
    Class FindClassID(Integer idClass) {
        if(ID.equals(idClass)) return this;

        for(Class Child : Childs) {
            Class FindClass = Child.FindClassID(idClass);
            if(FindClass!=null) return FindClass;
        }
        
        return null;
    }
    
    Class CommonParent(Class ToCommon) {
        CommonParent1(1);
        Class Result = CommonParent2();
        CommonParent1(0);
        
        return Result;
    }
    
    // 1-й шаг пометки
    private void CommonParent1(int SetCheck) {
        if(Check==SetCheck) return;
        Check = SetCheck;
        for(Class Parent : Parents) Parent.CommonParent1(SetCheck);
    }

    // 2-й шаг выводит в Set, и сбрасывает пометки
    private Class CommonParent2() {
        if(Check==1) return this;
        
        for(Class Parent : Parents) {
            Class ParentClass = Parent.CommonParent2();
            if(ParentClass!=null) return ParentClass;
        }
        
        return null;
    }

    void FillSetID(Collection<Integer> SetID) {
        if (SetID.contains(ID))
            return;
        
        SetID.add(ID);
        
        for(Class Child : Childs)
            Child.FillSetID(SetID);
    }

    // заполняет список классов
    void FillClassList(List<Class> ClassList) {
        if (ClassList.contains(ID))
            return;
        
        ClassList.add(this);
        
        for(Class Child : Childs)
            Child.FillClassList(ClassList);
    }

    // заполняет список классов
    void fillParents(Collection<Class> ParentSet) {
        if (ParentSet.contains(ID))
            return;

        ParentSet.add(this);

        for(Class Parent : Parents)
            Parent.fillParents(ParentSet);
    }

    // получает классы у которого есть оба интерфейса
    Collection<Class> CommonClassSet(Class ToCommon) {
        CommonClassSet1(false);
        ToCommon.CommonClassSet2(false,null,false);

        Collection<Class> Result = new ArrayList<Class>();
        CommonClassSet3(Result,null,false);
        return Result;
    }

    int Check = 0;
    // 1-й шаг расставляем пометки 1
    private void CommonClassSet1(boolean Up) {
        if(Check==1) return;
        Check = 1;
        for(Class Child : (Up?Parents:Childs))
            Child.CommonClassSet1(Up);
    }
    
    // 2-й шаг пометки 
    // 2 - верхний общий класс
    // 3 - просто общий класс
    private void CommonClassSet2(boolean Set,Collection<Class> Free,boolean Up) {
        if(!Set) {
            if(Check>0) {
                if(Check!=1) return;
                Check = 2;
                Set = true;
            } else
                if(Free!=null) Free.add(this);
        } else {
            if(Check==3 || Check==2) {
                Check = 3;
                return;
            }
            
            Check = 3;
        }
            
        for(Class Child : (Up?Parents:Childs))
            Child.CommonClassSet2(Set,Free,Up);
    }
    
    // 3-й шаг выводит в Set, и сбрасывает пометки
    private void CommonClassSet3(Collection<Class> Common,Collection<Class> Free,boolean Up) {
        if(Check==0) return;
        if(Common!=null && Check==2) Common.add(this);
        if(Free!=null && Check==1) Free.add(this);
               
        Check = 0;

        for(Class Child : (Up?Parents:Childs))
            Child.CommonClassSet3(Common,Free,Up);
    }
    
    void GetDiffSet(Class DiffClass,Collection<Class> AddClasses,Collection<Class> RemoveClasses) {
        CommonClassSet1(true);
        if(DiffClass!=null) DiffClass.CommonClassSet2(false,RemoveClasses,true);

        CommonClassSet3(null,AddClasses,true);
    }
    
    String GetDBType() {
        return "integer";
    }
    
    // получает рандомный объект
    abstract Object GetRandomObject(DataSession Session,TableFactory TableFactory,Random Randomizer,Integer Diap) throws SQLException;
    abstract Object getRandomObject(Map<Class, List<Integer>> Objects,Random Randomizer,Integer Diap) throws SQLException;

    ArrayList<NavigatorElement> relevantElements = new ArrayList();
    void addRelevantElement(NavigatorElement relevantElement) {
        relevantElements.add(relevantElement);
    }

}

// класс который можно сравнивать
class IntegralClass extends Class {
    
    IntegralClass(Integer iID, String caption) {super(iID, caption);}
    
    Object GetRandomObject(DataSession Session,TableFactory TableFactory,Random Randomizer,Integer Diap) throws SQLException {
        return Randomizer.nextInt(Diap*Diap+1);
    }

    Object getRandomObject(Map<Class, List<Integer>> Objects, Random Randomizer, Integer Diap) throws SQLException {
        return Randomizer.nextInt(Diap);
    }
}

// класс который можно суммировать
class QuantityClass extends IntegralClass {    
    QuantityClass(Integer iID, String caption) {super(iID, caption);}
}

class DateClass extends IntegralClass {
    DateClass(Integer iID, String caption) {super(iID, caption);}
}

class BitClass extends IntegralClass {
    BitClass(Integer iID, String caption) {super(iID, caption);}
}

class StringClass extends Class {

    StringClass(Integer iID, String caption) {super(iID, caption);}

    @Override
    String GetDBType() {
        return "char(50)";
    }
    
    Object GetRandomObject(DataSession Session,TableFactory TableFactory,Random Randomizer,Integer Diap) throws SQLException {
        return "NAME "+Randomizer.nextInt(50);
    }

    Object getRandomObject(Map<Class, List<Integer>> Objects, Random Randomizer, Integer Diap) throws SQLException {
        return "NAME "+Randomizer.nextInt(Diap);
    }

}

class ObjectClass extends Class {    
    ObjectClass(Integer iID, String caption) {super(iID, caption);}
    
    Object GetRandomObject(DataSession Session,TableFactory TableFactory,Random Randomizer,Integer Diap) throws SQLException {
        ArrayList<Map<KeyField,Integer>> Result = new ArrayList<Map<KeyField,Integer>>(TableFactory.ObjectTable.getClassJoin(this).executeSelect(Session).keySet());
        return Result.get(Randomizer.nextInt(Result.size())).get(TableFactory.ObjectTable.Key);
    }

    Object getRandomObject(Map<Class, List<Integer>> Objects, Random Randomizer, Integer Diap) throws SQLException {
        List<Integer> ClassObjects = Objects.get(this);
        return ClassObjects.get(Randomizer.nextInt(ClassObjects.size()));
    }
}