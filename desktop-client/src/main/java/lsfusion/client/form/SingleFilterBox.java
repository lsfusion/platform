package lsfusion.client.form;

import lsfusion.client.logics.ClientRegularFilter;
import lsfusion.client.logics.ClientRegularFilterGroup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

import static lsfusion.client.ClientResourceBundle.getString;

public abstract class SingleFilterBox extends JCheckBox {
    private AWTEvent latestCheckBoxEvent;
    private boolean internalChange = false;

    public SingleFilterBox(final ClientRegularFilterGroup filterGroup, final ClientRegularFilter singleFilter) {
        super(singleFilter.getFullCaption());

        addItemListener(new ItemListener() {

            public void itemStateChanged(final ItemEvent ie) {
                if (internalChange) {
                    internalChange = false;
                    return;
                }

                // штука, которую не собираются править 
                // http://bugs.java.com/view_bug.do?bug_id=6924233
                // блокировка EDT путём показывания нашего busyDialog'а во время обработки события не очень хорошая идея.
                // это может привести к разным непредвиденным последствиям. в нашем случае при показе диалога на чек-боксе 
                // срабатывает событие FOCUS_LOST, которое приводит к ещё одному нежелательному событию itemStateChanged.
                // в результате получаем нарушение синхронности событий.
                // в то же время предлагаемый вызов через invokeLater нам тоже не помогает. поэтому извращаемся таким образом
                if (latestCheckBoxEvent == null || latestCheckBoxEvent.getID() != FocusEvent.FOCUS_LOST) {
                    // убрал invokeLater(), поскольку setRegularFilter() - синхронное событие - должно сразу блокировать EDT
                    RmiQueue.runAction(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (ie.getStateChange() == ItemEvent.SELECTED) {
                                    selected();
                                }
                                if (ie.getStateChange() == ItemEvent.DESELECTED) {
                                    deselected();
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(getString("form.error.changing.regular.filter"), e);
                            }
                        }
                    });
                } else {
                    internalChange = true; // при изменении состояния на FOCUS_LOST ничего не делаем => возвращаем состояние
                    setSelected(!isSelected());
                }
            }
        });

        if (filterGroup.defaultFilterIndex >= 0) {
            setSelected(true);
        }
    }

    @Override
    protected void processEvent(AWTEvent e) {
        latestCheckBoxEvent = e;
        super.processEvent(e);
    }
    
    public abstract void selected() throws IOException;
    public abstract void deselected() throws IOException;
}