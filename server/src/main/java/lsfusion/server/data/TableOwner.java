package lsfusion.server.data;

public interface TableOwner {
    
    TableOwner global = new TableOwner() {
        public String toString() {
            return "global";
        }
    };

    TableOwner none = new TableOwner() {
        public String toString() {
            return "none";
        }
    };

    TableOwner debug = new TableOwner() {
        public String toString() {
            return "debug";
        }
    };

}
