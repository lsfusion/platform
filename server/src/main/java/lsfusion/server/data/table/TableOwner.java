package lsfusion.server.data.table;

public interface TableOwner {
    
    String getDebugInfo();
    
    TableOwner global = new TableOwner() {
        public String toString() {
            return "global";
        }

        @Override
        public String getDebugInfo() {
            return toString();
        }
    };

    TableOwner none = new TableOwner() {
        public String toString() {
            return "none";
        }

        @Override
        public String getDebugInfo() {
            return toString();
        }
    };

    TableOwner debug = new TableOwner() {
        public String toString() {
            return "debug";
        }

        @Override
        public String getDebugInfo() {
            return toString();
        }
    };

}
