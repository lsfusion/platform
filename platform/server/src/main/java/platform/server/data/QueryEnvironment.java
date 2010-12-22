package platform.server.data;

import platform.server.data.type.TypeObject;
import platform.server.data.type.ParseInterface;

public interface QueryEnvironment {

    ParseInterface getSQLUser();
    ParseInterface getID();
    ParseInterface getSQLComputer();

    public final static QueryEnvironment empty = new QueryEnvironment() {
        public ParseInterface getSQLUser() {
            return ParseInterface.empty;
        }

        public ParseInterface getID() {
            return ParseInterface.empty;
        }

        public ParseInterface getSQLComputer() {
            return ParseInterface.empty;
        }
    };
}
