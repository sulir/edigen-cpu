/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edigen.cpu.impl;

import emulib.plugins.cpu.ICPUContext;
import emulib.plugins.device.IDeviceContext;

/**
 *
 * @author Matúš Sulír
 */
public class EdigenCPUContext implements ICPUContext {

    public boolean isInterruptSupported() {
        return false;
    }

    public void setInterrupt(IDeviceContext device, int mask) {
        // nothing
    }

    public void clearInterrupt(IDeviceContext device, int mask) {
        // nothing
    }

    public String getID() {
        return "edigen-cpu-context";
    }
    
}
