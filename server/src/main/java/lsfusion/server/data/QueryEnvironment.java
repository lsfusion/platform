package lsfusion.server.data;

import lsfusion.server.data.type.ParseInterface;

public interface QueryEnvironment {

    ParseInterface getSQLUser();
    
    int getTransactTimeout();

    ParseInterface getIsFullClient();
    ParseInterface getSQLComputer();
    ParseInterface getIsServerRestarting();

    public final static QueryEnvironment empty = new QueryEnvironment() {
        public ParseInterface getSQLUser() {
            return ParseInterface.empty;
        }

        public ParseInterface getIsFullClient() {
            return ParseInterface.empty;
        }

        public ParseInterface getSQLComputer() {
            return ParseInterface.empty;
        }

        public ParseInterface getIsServerRestarting() {
            return ParseInterface.empty;
        }
        
        public int getTransactTimeout() {
            return 0;
        }
    };
}
