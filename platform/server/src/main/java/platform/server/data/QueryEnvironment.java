package platform.server.data;

import platform.server.data.type.ParseInterface;

public interface QueryEnvironment {

    ParseInterface getSQLUser();

    ParseInterface getSQLComputer();
    ParseInterface getIsServerRestarting();

    public final static QueryEnvironment empty = new QueryEnvironment() {
        public ParseInterface getSQLUser() {
            return ParseInterface.empty;
        }

        public ParseInterface getSQLComputer() {
            return ParseInterface.empty;
        }

        public ParseInterface getIsServerRestarting() {
            return ParseInterface.empty;
        }
    };
}
