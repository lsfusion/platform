package lsfusion.gwt.client;

import com.google.gwt.junit.tools.GWTTestSuite;
import junit.framework.Test;
import lsfusion.gwt.client.base.view.ReactRootTest;
import lsfusion.gwt.client.form.design.view.GReactFormDataTest;
import lsfusion.gwt.client.form.object.table.tree.view.GTreeTableTreeTest;
import lsfusion.gwt.client.form.order.user.GGridSortableHeaderManagerTest;

// the client's GWT tests, run as one suite: GWT compiles a module once per JVM, with the test classes it knows of at
// that moment, so test classes of one module run one by one would leave all but the first out of it. Surefire runs
// this suite instead of its classes, so a new GWT test class of the client goes here AND into the excludes of
// web-client/pom.xml's surefire; never into a .gwt.xml of its own, which the GWT tooling would take for the client's
public class GClientTestSuite {
    public static Test suite() {
        GWTTestSuite suite = new GWTTestSuite("the client's GWT tests");
        suite.addTestSuite(GReactFormDataTest.class);
        suite.addTestSuite(GTreeTableTreeTest.class);
        suite.addTestSuite(GGridSortableHeaderManagerTest.class);
        suite.addTestSuite(GFormChangesTest.class);
        suite.addTestSuite(ReactRootTest.class);
        return suite;
    }
}
