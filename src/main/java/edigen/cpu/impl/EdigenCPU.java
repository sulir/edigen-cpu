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
import emulib.plugins.ISettingsHandler;
import emulib.plugins.cpu.*;
import emulib.plugins.memory.IMemoryContext;
import emulib.runtime.Context;
import emulib.runtime.StaticDialogs;
import java.nio.ByteBuffer;
import javax.swing.JPanel;

/**
 * The main CPU plugin class.
 * @author Matúš Sulír
 */
public class EdigenCPU extends SimpleCPU {

    private EdigenCPUContext cpu;
    private IMemoryContext memory;
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
        cpu = new EdigenCPUContext();
        
        if (!Context.getInstance().register(pluginID, cpu, ICPUContext.class))
            StaticDialogs.showErrorMessage("Could not register the CPU.");
    }

    /**
     * Associates the plugin with a main memory and creates a decoder, status
     * panel and disassembler.
     * @param settings the settings handler
     * @return true on success, false otherwise
     */
    @Override
    public boolean initialize(ISettingsHandler settings) {
        super.initialize(settings);
        
        memory = Context.getInstance().getMemoryContext(pluginID, IMemoryContext.class);
        
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
        
        fireCpuRun(run_state);
        fireCpuState();
    }
    
    /**
     * Executes one instruction.
     */
    @Override
    public void step() {
        if (run_state == RunState.STATE_STOPPED_BREAK) {
            run_state = RunState.STATE_RUNNING;
            emulateInstruction();
            
            try {
                if (run_state == RunState.STATE_RUNNING)
                    run_state = RunState.STATE_STOPPED_BREAK;
            } catch (IndexOutOfBoundsException ex) {
                run_state = RunState.STATE_STOPPED_ADDR_FALLOUT;
            }
            
            fireCpuRun(run_state);
            fireCpuState();
        }
    }

    /**
     * Starts executing instructions until a breakpoint is reached or an address
     * fallout occurs.
     */
    @Override
    public void run() {
        run_state = RunState.STATE_RUNNING;
        fireCpuRun(run_state);
        
        while (run_state == RunState.STATE_RUNNING) {
            if (getBreakpoint(PC)) {
                run_state = RunState.STATE_STOPPED_BREAK;
                break;
            }
            
            try {
                emulateInstruction();
            } catch (IndexOutOfBoundsException ex) {
                run_state = RunState.STATE_STOPPED_ADDR_FALLOUT;
            }
        }
        
        fireCpuState();
        fireCpuRun(run_state);
    }
    
    /**
     * Pauses the emulation.
     */
    @Override
    public void pause() {
        run_state = RunState.STATE_STOPPED_BREAK;
        fireCpuRun(run_state);
    }

    /**
     * Stops the emulation.
     */
    @Override
    public void stop() {
        run_state = RunState.STATE_STOPPED_NORMAL;
        fireCpuRun(run_state);
    }

    /**
     * Returns the currently executed instruction position (stored in the PC
     * register).
     * @return content of the PC register
     */
    @Override
    public int getInstrPosition() {
        return PC;
    }

    /**
     * Sets the program counter.
     * @param position the position in memory
     * @return true on success, false on failure
     */
    @Override
    public boolean setInstrPosition(int position) {
        if (position < 0) {
            return false;
        } else {
            PC = position;
            return true;
        }
    }

    /**
     * Returns the status GUI.
     * @return the status panel
     */
    @Override
    public JPanel getStatusGUI() {
        return statusPanel;
    }
    
    /**
     * Returns the disassembler.
     * @return the disassembler
     */
    @Override
    public IDisassembler getDisassembler() {
        return disassembler;
    }

    /**
     * Returns the plugin name.
     * @return the title
     */
    @Override
    public String getTitle() {
        return "Edigen CPU";
    }

    /**
     * Returns the copyright string.
     * @return the copyright string
     */
    @Override
    public String getCopyright() {
        return "Copyright \u00A9 2012, Matúš Sulír";
    }

    /**
     * Returns the plugin description.
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Very simple CPU to test Edigen functionality";
    }

    /**
     * Returns the plugin version.
     * @return the version
     */
    @Override
    public String getVersion() {
        return "1.0";
    }

    /**
     * Called after the emulator is closed.
     */
    @Override
    public void destroy() {
        run_state = RunState.STATE_STOPPED_NORMAL;
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
                    
                    memory.write(address, oldValue + addend);
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
            run_state = RunState.STATE_STOPPED_BAD_INSTR;
        }
    }
}
