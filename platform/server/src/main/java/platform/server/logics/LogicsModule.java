package platform.server.logics;

import net.sf.jasperreports.engine.JRException;

import java.io.FileNotFoundException;

/**
 * User: DAle
 * Date: 16.05.11
 * Time: 17:37
 */

public abstract class LogicsModule {
    public abstract void initClasses();
    public abstract void initTables();
    public abstract void initGroups();
    public abstract void initProperties();
    public abstract void initIndexes();
    public abstract void initNavigators() throws JRException, FileNotFoundException;
}
