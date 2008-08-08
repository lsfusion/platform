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
        return BL.BaseGroup.getNavigatorForm(FormID).CreateForm(Adapter,BL);
    }

    String getCaption(int FormID){

        // инстанцирует форму
        return BL.BaseGroup.getNavigatorForm(FormID).caption;
    }

}

// создаются в бизнес-логике

abstract class NavigatorElement<T extends BusinessLogics<T>> {

    String caption = "";
    public NavigatorElement(String icaption) { caption = icaption; }

    // пока так потом может через Map
    abstract NavigatorForm<T> getNavigatorForm(int FormID);

    abstract boolean allowChildren();

    @Override
    public String toString() { return caption; }
}

class NavigatorGroup<T extends BusinessLogics<T>> extends NavigatorElement<T> {
    
    NavigatorGroup(String caption) {
        super(caption);
        Childs = new ArrayList();
    }
    
    void AddChild(NavigatorElement<T> Child) {

        Childs.add(Child);
    }
    
    Collection<NavigatorElement<T>> Childs;

    NavigatorForm<T> getNavigatorForm(int FormID) {
        for(NavigatorElement<T> Child : Childs) {
            NavigatorForm<T> Form = Child.getNavigatorForm(FormID);
            if(Form!=null) return Form;
        }

        return null;
    }

    boolean allowChildren() {
        return true;
    }

}

abstract class NavigatorForm<T extends BusinessLogics<T>> extends NavigatorElement<T> {

    int ID;
    NavigatorForm(int iID, String caption) {super(caption); ID=iID;}

    NavigatorForm<T> getNavigatorForm(int FormID) {
        if(FormID==ID)
            return this;
        else
            return null;
    }

    abstract RemoteForm<T> CreateForm(DataAdapter Adapter,T BL) throws SQLException;

    boolean allowChildren() {
        return false;
    }
    
}
