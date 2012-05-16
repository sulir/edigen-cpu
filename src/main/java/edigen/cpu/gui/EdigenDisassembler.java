/* Auto-generated file. Do not modify. */
package edigen.cpu.gui;

import static edigen.cpu.impl.EdigenDecoder.*;
import emulib.plugins.cpu.*;
import emulib.plugins.memory.IMemoryContext;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The disassembler.
 */
public class EdigenDisassembler extends SimpleDisassembler {
    /**
    * An instruction mnemonic format string with associated values.
    */
    private static class MnemonicFormat {
        private String format;
        private int[] values;

        public MnemonicFormat(String format, int[] values) {
            this.format = format;
            this.values = values;
        }

        public String getFormat() {
            return format;
        }

        public int[] getValues() {
            return values;
        }
    }

    private static final Map<Set<Integer>, MnemonicFormat> formatMap;

    private IMemoryContext memory;
    private IDecoder decoder;

    static {
        String[] formats = {
          "%s %X, %d", 
          "%s %X, [%X]"

        };
        
        int[][] values = {
            {INSTRUCTION, ADDRESS, VALUE}, 
            {INSTRUCTION, ADDRESS, COMPARED}
        };
        
        formatMap = new HashMap<Set<Integer>, MnemonicFormat>();
        
        for (int i = 0; i < formats.length; i++) {
            Set<Integer> formatValues = new HashSet<Integer>();
            
            for (int value : values[i]) {
                formatValues.add(value);
            }
            
            formatMap.put(formatValues, new MnemonicFormat(formats[i], values[i]));
        }
    }

    /**
     * The constructor.
     * @param memory the memory context which will be used to read instructions
     * @param decoder the decoder to use to decode instructions
     */
    public EdigenDisassembler(IMemoryContext memory, IDecoder decoder) {
        this.memory = memory;
        this.decoder = decoder;
    }
    
    /**
     * Disassembles an instruction.
     * @param memoryLocation the starting address of the instruction
     * @return the disassembled instruction
     */
    @Override
    public CPUInstruction disassemble(int memoryLocation) {
        String mnemonic;
        String code;
        
        try {
            DecodedInstruction instruction = decoder.decode(memoryLocation);
            MnemonicFormat format = formatMap.get(instruction.getKeys());

            if (format == null)
                mnemonic = "undisassemblable";
            else
                mnemonic = createMnemonic(instruction, format);
            
            StringBuilder codeBuilder = new StringBuilder();
            
            for (byte number : instruction.getImage()) {
                codeBuilder.append(String.format("%02X ", number));
            }
            
            code = codeBuilder.toString();
        } catch (InvalidInstructionException ex) {
            mnemonic = "unknown";
            code = String.format("%02X", (Byte) memory.read(memoryLocation));
        }
        
        return new CPUInstruction(memoryLocation, mnemonic, code);
    }
    
    /**
     * Returns an address of the instruction located right after the current
     * instruction.
     * @param memoryLocation the starting address of the current instruction
     * @return the starting address of the next instruction
     */
    @Override
    public int getNextInstructionLocation(int memoryLocation) {
        try {
            return memoryLocation + decoder.decode(memoryLocation).getLength();
        } catch (InvalidInstructionException ex) {
            return memoryLocation + 1;
        }
    }

    /**
     * Returns the instruction mnemonic.
     * @param instruction the decoded instruction
     * @param format the formatting string + rule codes
     * @return the instruction mnemonic
     */
    private String createMnemonic(DecodedInstruction instruction, MnemonicFormat format) {
        StringBuilder mnemonic = new StringBuilder(format.getFormat());
        int position = 0;
        
        for (int ruleCode : format.getValues()) {
            position = mnemonic.indexOf("%", position);
            if (position == -1 || position == mnemonic.length())
                break;
            
            byte[] value = instruction.getBits(ruleCode, false);
            if (value == null)
                value = instruction.getString(ruleCode).getBytes();
            
            String replaced = format(mnemonic.charAt(position + 1), value); 
            mnemonic.replace(position, position += 2, replaced);
        }
        
        return mnemonic.toString();
    }
    
    /**
     * Transforms the bytes into a meaningful string using the formatting
     * character.
     * @param format the formatting character ('s' for a string, etc.)
     * @param value the array of bytes
     * @return the resulting string
     */
    private String format(char format, byte[] value) {
        switch (format) {
            case 'c':
                String string = new String(value);
                return (string.length() != 0) ? string.substring(0, 1) : "?";
            case 'd':
                return new BigInteger(value).toString();
            case 'f':
                switch (value.length) {
                    case 4:
                        return Float.toString(ByteBuffer.wrap(value).getFloat());
                    case 8:
                        return Double.toString(ByteBuffer.wrap(value).getDouble());
                    default:
                        return "NaN";
                }
            case 's':
                return new String(value);
            case 'x':
                String formatString = "%0" + 2 * value.length + "x";
                return String.format(formatString, new BigInteger(value));
            case 'X':
                return format('x', value).toUpperCase();
            case '%':
                return "%";
            default:
                return Character.toString(format);
        }
    }
}
