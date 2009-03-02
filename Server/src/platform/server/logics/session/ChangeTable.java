package platform.server.logics.session;

import platform.server.data.SessionTable;

abstract class ChangeTable extends SessionTable {

//    KeyField Session;

    ChangeTable(String iName) {
        super(iName);

//        Session = new KeyField("session","integer");
//        Keys.add(Session);
    }
}
