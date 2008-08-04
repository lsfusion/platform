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

public class RemoteNavigator<T extends BusinessLogics> {
    
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
    
    RemoteForm<T> CreateForm(NavigatorForm<T> Form) throws SQLException {
        // инстанцирует форму (нужна ), проставляет объекты, но неясно

        return Form.CreateForm(Adapter,BL);
    }
}

// создаются в бизнес-логике

abstract class NavigatorElement {
}

class NavigatorGroup extends NavigatorElement {
    
    NavigatorGroup() {
        Childs = new ArrayList();
    }
    
    void AddChild(NavigatorElement Child) {
        Childs.add(Child);
    }
    
    Collection<NavigatorElement> Childs;
}

abstract class NavigatorForm<T extends BusinessLogics> extends NavigatorElement {
    abstract RemoteForm<T> CreateForm(DataAdapter Adapter,T BL) throws SQLException;
}
