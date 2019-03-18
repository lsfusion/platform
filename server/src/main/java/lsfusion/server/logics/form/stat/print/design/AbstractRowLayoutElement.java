package lsfusion.server.logics.form.stat.print.design;

interface AbstractRowLayoutElement {

    int getMinimumWidth();
    int getPreferredWidth();

    void setLeft(int left);
    void setWidth(int width);
    void setRow(int row);
}
