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

import edigen.cpu.gui.EdigenDisassembler;
import edigen.cpu.gui.EdigenStatusPanel;
import static edigen.cpu.impl.EdigenDecoder.*;
import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.cpu.*;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.*;
import java.nio.ByteBuffer;
import javax.swing.JPanel;

/**
 * The main CPU plugin class.
 * @author Matúš Sulír
 */
@PluginType(title = "Edigen CPU",
        copyright = "Copyright \u00A9 2012, Matúš Sulír",
        description = "Very simple CPU to test Edigen functionality",
        type = PLUGIN_TYPE.CPU)
public class EdigenCPU extends AbstractCPU {

    private MemoryContext memory;
    private EdigenDecoder decoder;
    private EdigenStatusPanel statusPanel;
    private EdigenDisassembler disassembler;
    
    private int PC;
    
    /**
     * CPU constructor.
     * @param pluginID the plugin ID
     */
    public EdigenCPU(Long pluginID) {
        super(pluginID);
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
    
    /**
     * Associates the plugin with a main memory and creates a decoder, status
     * panel and disassembler.
     * @param settings the settings handler
     * @return true on success, false otherwise
     */
    @Override
    public boolean initialize(SettingsManager settings) {
        super.initialize(settings);
        
        try {
            memory = ContextPool.getInstance().getMemoryContext(pluginID, MemoryContext.class);
        } catch (InvalidContextException ex) {
            StaticDialogs.showErrorMessage("Could not access memory context.");
            return false;
        }
        
        if (memory == null) {
            StaticDialogs.showErrorMessage("Could not access memory.");
            return false;
        }
        
        if (memory.getDataType() != Short.class) {
            StaticDialogs.showErrorMessage("Invalid memory type.");
            return false;
        }
        
        decoder = new EdigenDecoder(memory);
        statusPanel = new EdigenStatusPanel(this);
        disassembler = new EdigenDisassembler(memory, decoder);
        
        return true;
    }

    /**
     * Resets the emulation.
     * @param address the starting address
     */
    @Override
    public void reset(int address) {
        super.reset(address);
        
        PC = address;
        notifyChange();
    }
    
    /**
     * Executes one instruction.
     */
    @Override
    public void step() {
        if (runState == RunState.STATE_STOPPED_BREAK) {
            runState = RunState.STATE_RUNNING;
            emulateInstruction();
            
            try {
                if (runState == RunState.STATE_RUNNING) {
                    runState = RunState.STATE_STOPPED_BREAK;
                }
            } catch (IndexOutOfBoundsException ex) {
                runState = RunState.STATE_STOPPED_ADDR_FALLOUT;
            }
            
            notifyChange();
        }
    }

    /**
     * Starts executing instructions until a breakpoint is reached or an address
     * fallout occurs.
     */
    @Override
    public void run() {
        runState = RunState.STATE_RUNNING;
        notifyChange();
        
        while (runState == RunState.STATE_RUNNING) {
            if (isBreakpointSet(PC)) {
                runState = RunState.STATE_STOPPED_BREAK;
                break;
            }
            
            try {
                emulateInstruction();
            } catch (IndexOutOfBoundsException ex) {
                runState = RunState.STATE_STOPPED_ADDR_FALLOUT;
            }
        }
        
        notifyChange();
    }
    
    /**
     * Pauses the emulation.
     */
    @Override
    public void pause() {
        runState = RunState.STATE_STOPPED_BREAK;
        notifyCPURunState(runState);
    }

    /**
     * Stops the emulation.
     */
    @Override
    public void stop() {
        runState = RunState.STATE_STOPPED_NORMAL;
        notifyCPURunState(runState);
    }

    /**
     * Returns the currently executed instruction position (stored in the PC
     * register).
     * @return content of the PC register
     */
    @Override
    public int getInstructionPosition() {
        return PC;
    }

    /**
     * Sets the program counter.
     * @param position the position in memory
     * @return true on success, false on failure
     */
    @Override
    public boolean setInstructionPosition(int position) {
        if (position < 0) {
            return false;
        } else {
            PC = position;
            return true;
        }
    }

    /**
     * Returns the GUI status panel.
     * @return the status panel
     */
    @Override
    public JPanel getStatusPanel() {
        return statusPanel;
    }
    
    /**
     * Returns the disassembler.
     * @return the disassembler
     */
    @Override
    public Disassembler getDisassembler() {
        return disassembler;
    }

    /**
     * Called after the emulator is closed.
     */
    @Override
    public void destroy() {
        runState = RunState.STATE_STOPPED_NORMAL;
    }

    /**
     * This plugin does not support any settings.
     * @return false
     */
    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }
    
    /**
     * Not supported.
     */
    @Override
    public void showSettings() {
        // none
    }
    
    /**
     * Emulates one instruction.
     */
    private void emulateInstruction() {
        try {
            DecodedInstruction in = decoder.decode(PC);
            
            switch (in.get(INSTRUCTION)) {
                case ADD:
                    // this mess is mainly type conversion
                    short address = ByteBuffer.wrap(in.getBits(ADDRESS, true)).getShort();
                    short oldValue = (Short) memory.read(address);
                    byte addend = in.getBits(VALUE)[0];
                    
                    memory.write(address, (short) (oldValue + addend));
                    break;
                case JZ:
                    short addressToRead = ByteBuffer.wrap(in.getBits(COMPARED, true)).getShort();
                    short comparedValue = (Short) memory.read(addressToRead);
                    
                    if (comparedValue == 0) {
                        PC = ByteBuffer.wrap(in.getBits(TARGET, true)).getShort();
                    }
                    return;
            }
            
            PC += in.getLength();
        } catch (InvalidInstructionException ex) {
            runState = RunState.STATE_STOPPED_BAD_INSTR;
        }
    }
    
    /**
     * Notifies all listeners about both the run state and the internal CPU
     * state change.
     */
    private void notifyChange() {
        notifyCPURunState(runState);
        notifyCPUState();
    }
}
