package lsfusion.server.logics.property;

public enum LinkType  {
    DEPEND, EVENTACTION, USEDACTION, RECCHANGE, RECUSED;
    
    public int getNum() {
        for(int i=0;i<order.length;i++)
            if(order[i].equals(this))
                return i;
        throw new RuntimeException("should not be");
    }

    public final static LinkType[] order = new LinkType[] {DEPEND, EVENTACTION, USEDACTION, RECCHANGE, RECUSED};
}
