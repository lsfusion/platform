package platform.server.view.form.report;

/**
 * Created by IntelliJ IDEA.
 * User: ME2
 * Date: 21.02.2009
 * Time: 9:46:10
 * To change this template use File | Settings | File Templates.
 */
interface AbstractRowLayoutElement {

    int getMinimumWidth();
    int getPreferredWidth();

    void setLeft(int left);
    void setWidth(int width);
    void setRow(int row);
}
