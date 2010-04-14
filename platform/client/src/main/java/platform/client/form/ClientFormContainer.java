package platform.client.form;

import platform.client.logics.ClientContainerView;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

// класс контейнеров отображения всех объектов на ClientForm
// ведет подсчет количество child'ов и прячет/показывает себя, если их становится 0
public class ClientFormContainer extends JPanel{

    final ClientContainerView view;

    public ClientFormContainer(ClientContainerView iview) {

        view = iview;

        String title = view.getTitle();
        if (title != null) {
            TitledBorder border = BorderFactory.createTitledBorder(title);
            setBorder(border);
        }

        setPreferredSize(new Dimension(10000, 10000));

        setVisible(false);

    }

    public void addComponent(Component comp, Object constraints) {

        incrementComponentCount();
        add(comp, constraints);
    }

    public void removeComponent(Component comp) {

        remove(comp);
        decrementComponentCount();
    }

    int compCount = 0;
    private void incrementComponentCount() {

        if (compCount == 0)
            setVisible(true);

        compCount++;

        Container parent = getParent();
        if (parent instanceof ClientFormContainer)
            ((ClientFormContainer)parent).incrementComponentCount();
    }

    private void decrementComponentCount() {

        compCount--;
        if (compCount == 0)
            setVisible(false);

        Container parent = getParent();
        if (parent instanceof ClientFormContainer)
            ((ClientFormContainer)parent).decrementComponentCount();
    }
}
