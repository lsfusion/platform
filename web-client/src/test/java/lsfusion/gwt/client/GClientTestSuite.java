package lsfusion.gwt.client;

import com.google.gwt.junit.tools.GWTTestSuite;
import junit.framework.Test;
import lsfusion.gwt.client.form.design.view.GReactFormDataTest;
import lsfusion.gwt.client.form.object.table.tree.view.GTreeTableTreeTest;

// the client's GWT tests, run as one suite: GWT compiles a module once per JVM, with the test classes it knows of at
// that moment, so test classes of one module run one by one would leave all but the first out of it (surefire runs
// this suite instead of the classes - web-client/pom.xml). A new GWT test class of the client goes here too
public class GClientTestSuite {
    public static Test suite() {
        GWTTestSuite suite = new GWTTestSuite("the client's GWT tests");
        suite.addTestSuite(GReactFormDataTest.class);
        suite.addTestSuite(GTreeTableTreeTest.class);
        return suite;
    }
}
