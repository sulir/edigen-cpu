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
package edigen.cpu.impl;

import emulib.plugins.cpu.ICPUContext;
import emulib.plugins.device.IDeviceContext;

/**
 * The CPU context.
 * @author Matúš Sulír
 */
public class EdigenCPUContext implements ICPUContext {

    /**
     * Interrupts are not supported.
     * @return false
     */
    public boolean isInterruptSupported() {
        return false;
    }

    /**
     * Not supported.
     * @param device the device
     * @param mask the mask
     */
    public void setInterrupt(IDeviceContext device, int mask) {
        // nothing
    }

    /**
     * Not supported.
     * @param device the device
     * @param mask the mask
     */
    public void clearInterrupt(IDeviceContext device, int mask) {
        // nothing
    }

    /**
     * Returns the CPU context ID.
     * @return the context ID
     */
    public String getID() {
        return "edigen-cpu-context";
    }
    
}
