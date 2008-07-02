/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author ME
 */
class Class {
    Collection<Class> Parents;
    Collection<Class> Childs;
    
    Integer ID;
    Class(Integer iID) {
        ID=iID;
        Parents = new ArrayList<Class>();
        Childs = new ArrayList<Class>();
    }

    void AddParent(Class ParentClass) {
        Parents.add(ParentClass);
        ParentClass.Childs.add(this);        
    }

    boolean IsParent(Class Class) {
        if(Class==this) return true;

        Iterator<Class> i = Parents.iterator();
        while (i.hasNext())
            if (i.next().IsParent(Class)) return true;
        
        return false;
    }
    
    Class FindClassID(Integer idClass) {
        if(ID.equals(idClass)) return this;

        Iterator<Class> i = Childs.iterator();
        while (i.hasNext()) {
            Class FindClass = i.next().FindClassID(idClass);
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
        Iterator<Class> i = Parents.iterator();
        while (i.hasNext()) i.next().CommonParent1(SetCheck);
    }

    // 2-й шаг выводит в Set, и сбрасывает пометки
    private Class CommonParent2() {
        if(Check==1) return this;
        
        Iterator<Class> i = Parents.iterator();
        while (i.hasNext()) {
            Class ParentClass = i.next().CommonParent2();
            if(ParentClass!=null) return ParentClass;
        }
        
        return null;
    }

    void FillSetID(Collection<Integer> SetID) {
        if (SetID.contains(ID))
            return;
        
        SetID.add(ID);
        
        Iterator<Class> i = Childs.iterator();
        while(i.hasNext())
            i.next().FillSetID(SetID);
    }

    // получает классы у которого есть оба интерфейса
    Collection<Class> CommonClassSet(Class ToCommon) {
        CommonClassSet1();
        ToCommon.CommonClassSet2(false);
        
        Collection<Class> Result = new ArrayList<Class>();
        CommonClassSet3(Result);
        return Result;
    }
    
    int Check = 0;
    // 1-й шаг расставляем пометки 1
    private void CommonClassSet1() {
        if(Check==1) return;
        Check = 1;
        Iterator<Class> i = Childs.iterator();
        while (i.hasNext()) i.next().CommonClassSet1();
    }
    
    // 2-й шаг пометки 2, 3
    private void CommonClassSet2(boolean Set) {
        if(!Set) {
            if(Check>0) {
                if(Check!=1) return;
                Check = 2;
                Set = true;
            }
        } else {
            if(Check==3 || Check==2) {
                Check = 3;
                return;
            }
            
            Check = 3;
        }
            
        Iterator<Class> i = Childs.iterator();
        while (i.hasNext()) i.next().CommonClassSet2(Set);
    }
    
    // 3-й шаг выводит в Set, и сбрасывает пометки
    private void CommonClassSet3(Collection<Class> Out) {
        if(Check==0) return;
        if(Check==2) Out.add(this);
        Check = 0;

        Iterator<Class> i = Childs.iterator();
        while (i.hasNext()) i.next().CommonClassSet3(Out);
    }
    
    String GetDBType() {
        return "integer";
    }
    
    // пока все в параметры кинем, добавляет класс 
    Integer AddObject(DataAdapter Adapter, TableFactory Factory) throws SQLException {

        Integer FreeID = Factory.IDTable.GenerateID(Adapter);
        
        Map<KeyField,Integer> InsertKeys = new HashMap<KeyField,Integer>();
        InsertKeys.put(Factory.ObjectTable.Key, FreeID);
        Map<Field,Object> InsertProps = new HashMap<Field,Object>();
        InsertProps.put(Factory.ObjectTable.Class, ID);
        try {
        Adapter.InsertRecord(Factory.ObjectTable,InsertKeys,InsertProps);
        } catch (Exception e) {            
        }
        
        return FreeID;
    }
}

// класс который можно сравнивать
class IntegralClass extends Class {
    
    IntegralClass(Integer iID) {super(iID);}
}

// класс который можно суммировать
class QuantityClass extends IntegralClass {    
    QuantityClass(Integer iID) {super(iID);}
}

class DateClass extends IntegralClass {
    DateClass(Integer iID) {super(iID);}
}

class StringClass extends Class {    

    StringClass(Integer iID) {super(iID);}

    @Override
    String GetDBType() {
        return "char(50)";
    }
}

class ObjectClass extends Class {    
    ObjectClass(Integer iID) {super(iID);}
}