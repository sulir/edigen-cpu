/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edigen.cpu.gui;

import edigen.cpu.impl.EdigenCPU;
import emulib.plugins.cpu.ICPU;
import emulib.plugins.cpu.ICPU.RunState;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.EventObject;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author Matúš Sulír
 */
public class EdigenStatusPanel extends JPanel {
    private JLabel pcValue = new JLabel();
    private JLabel statusValue = new JLabel();
    
    public EdigenStatusPanel(final EdigenCPU cpu) {
        initComponents();
        
        cpu.addCPUListener(new ICPU.ICPUListener() {
            public void runChanged(EventObject evt, RunState runState) {
                switch (runState) {
                    case STATE_STOPPED_NORMAL:
                        statusValue.setText("Stopped");
                        break;
                    case STATE_STOPPED_BREAK:
                        statusValue.setText("Breakpoint");
                        break;
                    case STATE_STOPPED_BAD_INSTR:
                        statusValue.setText("Bad instruction");
                        break;
                    case STATE_STOPPED_ADDR_FALLOUT:
                        statusValue.setText("Address fallout");
                        break;
                }
            }

            public void stateUpdated(EventObject evt) {
                pcValue.setText(String.format("0x%04X", cpu.getPC()));
            }
        });
    }
    
    private void initComponents() {
        setMaximumSize(new Dimension(200, 100));
        setLayout(new GridLayout(2, 2));
        
        JLabel pcLabel = new JLabel("PC:");
        JLabel statusLabel = new JLabel("Status:");
        statusValue.setFont(statusValue.getFont().deriveFont(Font.BOLD));
        
        add(pcLabel);
        add(pcValue);
        add(statusLabel);
        add(statusValue);
    }
}
