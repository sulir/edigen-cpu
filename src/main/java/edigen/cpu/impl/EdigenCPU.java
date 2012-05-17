/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 *
 * @author Matúš Sulír
 */
public class EdigenCPU extends SimpleCPU {

    private EdigenCPUContext cpu;
    private IMemoryContext memory;
    private EdigenDecoder decoder;
    private EdigenStatusPanel statusPanel;
    private EdigenDisassembler disassembler;
    
    private int PC;
    
    public EdigenCPU(Long pluginID) {
        super(pluginID);
        cpu = new EdigenCPUContext();
        
        if (!Context.getInstance().register(pluginID, cpu, ICPUContext.class))
            StaticDialogs.showErrorMessage("Could not register the CPU.");
    }

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

    @Override
    public void reset(int address) {
        super.reset(address);
        
        PC = address;
        
        fireCpuRun(run_state);
        fireCpuState();
    }
    
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
    
    public void pause() {
        run_state = RunState.STATE_STOPPED_BREAK;
        fireCpuRun(run_state);
    }

    public void stop() {
        run_state = RunState.STATE_STOPPED_NORMAL;
        fireCpuRun(run_state);
    }

    public int getInstrPosition() {
        return PC;
    }

    public boolean setInstrPosition(int position) {
        if (position < 0) {
            return false;
        } else {
            PC = position;
            return true;
        }
    }

    public JPanel getStatusGUI() {
        return statusPanel;
    }
    
    public IDisassembler getDisassembler() {
        return disassembler;
    }

    public String getTitle() {
        return "Edigen CPU";
    }

    public String getCopyright() {
        return "Copyright \u00A9 2012, Matúš Sulír";
    }

    public String getDescription() {
        return "Very simple CPU to test Edigen functionality";
    }

    public String getVersion() {
        return "1.0";
    }

    public void destroy() {
        run_state = RunState.STATE_STOPPED_NORMAL;
    }

    public boolean isShowSettingsSupported() {
        return false;
    }
    
    public void showSettings() {
        // none
    }

    public int getPC() {
        return PC;
    }
    
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
