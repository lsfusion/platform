package platform.server.view.form.report;

interface AbstractRowLayoutElement {

    int getMinimumWidth();
    int getPreferredWidth();

    void setLeft(int left);
    void setWidth(int width);
    void setRow(int row);
}
