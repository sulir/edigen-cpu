/*
 * Copyright (C) 2012 Matúš Sulír
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package edigen.cpu.gui;

import edigen.cpu.impl.EdigenCPU;
import emulib.plugins.cpu.ICPU.ICPUListener;
import emulib.plugins.cpu.ICPU.RunState;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.EventObject;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * CPU status panel.
 * @author Matúš Sulír
 */
public class EdigenStatusPanel extends JPanel {
    private JLabel pcValue = new JLabel();
    private JLabel statusValue = new JLabel();
    
    /**
     * Constructs the status panel.
     * @param cpu the CPU
     */
    public EdigenStatusPanel(final EdigenCPU cpu) {
        initComponents();
        
        cpu.addCPUListener(new ICPUListener() {
            @Override
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

            @Override
            public void stateUpdated(EventObject evt) {
                pcValue.setText(String.format("0x%04X", cpu.getInstrPosition()));
            }
        });
    }
    
    /**
     * Initializes the GUI components.
     */
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
