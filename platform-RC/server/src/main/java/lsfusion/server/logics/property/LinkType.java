package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;

public enum LinkType  {
    DEPEND, GOAFTERREC, EVENTACTION, USEDACTION, RECCHANGE, RECEVENT, RECUSED;
    
    public int getNum() {
        for(int i=0;i<order.length;i++)
            if(order[i].equals(this))
                return i;
        throw new RuntimeException("should not be");
    }
    
    // оставляет самую сильную связь
    private static AddValue<Object, LinkType> minLinkAdd = new SymmAddValue<Object, LinkType>() {
        @Override
        public LinkType addValue(Object key, LinkType prevValue, LinkType newValue) {
            return prevValue.min(newValue);
        }
    };
    
    public LinkType min(LinkType linkType) {
        return getNum() < linkType.getNum() ? this : linkType;  
    }
    
    public static <T> AddValue<T, LinkType> minLinkAdd() {
        return (AddValue<T, LinkType>) minLinkAdd;
    }

    public final static LinkType[] order = new LinkType[] {DEPEND, GOAFTERREC, EVENTACTION, USEDACTION, RECCHANGE, RECEVENT, RECUSED};
    public final static LinkType MAX = order[order.length - 1];    
}
