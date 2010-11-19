package platform.client.form;

import platform.client.logics.ClientContainer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

// класс контейнеров отображения всех объектов на ClientFormController
// ведет подсчет количество child'ов и прячет/показывает себя, если их становится 0
class ClientFormContainer extends JPanel{

    private final ClientContainer key;

    public ClientFormContainer(ClientContainer key) {

        setOpaque(false);
        
        this.key = key;

        String title = this.key.getTitle();
        if (title != null) {
            TitledBorder border = BorderFactory.createTitledBorder(title);
            setBorder(border);
        }

        this.key.design.designComponent(this);

//        this.setBackground(new Color((int)(Math.random()*255), (int)(Math.random()*255), (int)(Math.random()*255)));

//        setPreferredSize(new Dimension(10000, 10000));

        setVisible(false);

//      для тестирования расположения контейнеров
//        setBackground(new Color((int)(Math.random() * 255), (int)(Math.random() * 255), (int)(Math.random() * 255)));

    }

    public void addComponent(Component comp, Object constraints) {

        incrementComponentCount();
        add(comp, constraints);
    }

    public void removeComponent(Component comp) {

        remove(comp);
        decrementComponentCount();
    }

    private int compCount = 0;
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
