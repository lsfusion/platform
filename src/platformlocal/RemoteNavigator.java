/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

// навигатор работает с абстрактной BL

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

// приходится везде BusinessLogics Generics'ом гонять потому как при инстанцировании формы нужен конкретный класс

public class RemoteNavigator<T extends BusinessLogics<T>> {
    
    DataAdapter Adapter;
    T BL;
    Map<Class,Integer> MapObjects;
    // в настройку надо будет вынести : по группам, способ релевантности групп, какую релевантность отсекать
    
    RemoteNavigator(DataAdapter iAdapter,T iBL,Map<Class,Integer> iMapObjects) {
        Adapter = iAdapter;
        BL = iBL;
        MapObjects = iMapObjects;
    }

    List<NavigatorElement> GetElements(NavigatorGroup Group) {
        // пока без релевантностей
        if(Group==null) Group = BL.BaseGroup;
 
        return new ArrayList(Group.Childs);
    }
    
    RemoteForm<T> CreateForm(int FormID) throws SQLException {

        // инстанцирует форму
        return BL.BaseGroup.CreateFormID(FormID,Adapter,BL);
    }
}

// создаются в бизнес-логике

abstract class NavigatorElement<T extends BusinessLogics<T>> {

    // пока так потом может через Map
    abstract RemoteForm<T> CreateFormID(int FormID,DataAdapter Adapter,T BL) throws SQLException;
}

class NavigatorGroup<T extends BusinessLogics<T>> extends NavigatorElement<T> {
    
    NavigatorGroup() {
        Childs = new ArrayList();
    }
    
    void AddChild(NavigatorElement<T> Child) {
        Childs.add(Child);
    }
    
    Collection<NavigatorElement<T>> Childs;

    RemoteForm<T> CreateFormID(int FormID,DataAdapter Adapter,T BL) throws SQLException {
        for(NavigatorElement<T> Child : Childs) {
            RemoteForm<T> Form = Child.CreateFormID(FormID,Adapter,BL);
            if(Form!=null) return Form;
        }

        return null;
    }
}

abstract class NavigatorForm<T extends BusinessLogics<T>> extends NavigatorElement<T> {

    int ID;
    NavigatorForm(int iID) {ID=iID;}

    RemoteForm<T> CreateFormID(int FormID,DataAdapter Adapter,T BL) throws SQLException {
        if(FormID==ID)
            return CreateForm(Adapter,BL);
        else
            return null;
    }

    abstract RemoteForm<T> CreateForm(DataAdapter Adapter,T BL) throws SQLException;
}
