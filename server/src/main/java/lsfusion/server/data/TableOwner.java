package lsfusion.server.data;

public interface TableOwner {
    
    public final static TableOwner global = new TableOwner() {
        public String toString() {
            return "global";
        }
    };

    public final static TableOwner none = new TableOwner() {
        public String toString() {
            return "none";
        }
    };

    public final static TableOwner debug = new TableOwner() {
        public String toString() {
            return "debug";
        }
    };

}
