/* Auto-generated file. Do not modify. */
package edigen.cpu.impl;

import emulib.plugins.cpu.DecodedInstruction;
import emulib.plugins.cpu.IDecoder;
import emulib.plugins.cpu.InvalidInstructionException;
import emulib.plugins.memory.IMemoryContext;
import java.util.Arrays;

/**
 * The instruction decoder.
 */
public class EdigenDecoder implements IDecoder {
    private IMemoryContext memory;
    private int memoryPosition;
    private byte unit;
    private byte[] instructionBytes = new byte[1024];
    private int bytesRead;
    private DecodedInstruction instruction;
    
    public static final int ADD = 1;
    public static final int JZ = 2;
    public static final int INSTRUCTION = 3;
    public static final int ADDRESS = 4;
    public static final int COMPARED = 5;
    public static final int TARGET = 6;
    public static final int VALUE = 7;

    
    /**
     * The constructor.
     * @param memory the memory context which will be used to read instructions
     */
    public EdigenDecoder(IMemoryContext memory) {
        this.memory = memory;
    }
    
    /**
     * Decodes an instruction.
     * @param memoryPosition the address of the start of the instruction
     * @return the decoded instruction object
     * @throws InvalidInstructionException when decoding is not successful
     */
    @Override
    public DecodedInstruction decode(int memoryPosition) throws InvalidInstructionException {
        this.memoryPosition = memoryPosition;
        bytesRead = 0;

        instruction = new DecodedInstruction();
        instruction(0);
        instruction.setImage(Arrays.copyOfRange(instructionBytes, 0, bytesRead));
        return instruction;
    }

    /**
     * Reads at most one unit (byte) of the current instruction.
     * @param start the number of bits from the start of the current instruction
     * @param length the number of bits to read
     * @return the bits read
     */
    private byte read(int start, int length) {
        if (start + length > 8 * bytesRead) {
            instructionBytes[bytesRead++] = ((Short) memory.read(memoryPosition++)).byteValue();
        }
        
        int startByte = start / 8;
        int startOffset = start % 8;
        byte startMask = (byte) (0xFF >>> startOffset);

        int endByte = (start + length) / 8;
        int endOffset = (startOffset + length) % 8;
        byte endMask = (byte) (0xFF << (8 - endOffset));
        
        byte result = (byte) ((instructionBytes[startByte] & startMask) << endOffset);
        return (byte) (result | (instructionBytes[endByte] & endMask) >>> (8 - endOffset));
    }

    /**
     * Reads an arbitrary number of bits of the current instruction.
     * @param start the number of bits from the start of the current instruction
     * @param length the number of bits to read
     * @return the bytes read
     */
    private byte[] getValue(int start, int length) {
        int byteCount = (length - 1) / 8 + 1;
        byte[] result = new byte[byteCount];
        
        for (int i = 0; i < byteCount; i++) {
            int bits = (i != byteCount - 1) ? 8 : length % 8;
            result[i] = read(start, bits);
            start += 8;
        }

        return result;
    }
    
    private void instruction(int start) throws InvalidInstructionException {
        unit = read(start + 0, 8);
        
        switch (unit & 0xff) {
        case 0x01:
            unit = read(start + 8, 8);
            
            unit = read(start + 16, 8);
            
            instruction.add(INSTRUCTION, "add", ADD);
            address(start + 8, ADDRESS);
            value(start + 24);
            break;
        case 0x02:
            unit = read(start + 8, 8);
            
            unit = read(start + 16, 8);
            
            instruction.add(INSTRUCTION, "jz", JZ);
            address(start + 8, COMPARED);
            address(start + 24, TARGET);
            break;
        default:
            throw new InvalidInstructionException();
        }
    }
    
    private void address(int start, int rule) throws InvalidInstructionException {
        unit = read(start + 0, 8);
        
        unit = read(start + 8, 8);
        
        instruction.add(rule, getValue(start + 0, 16));
    }
    
    private void value(int start) throws InvalidInstructionException {
        unit = read(start + 0, 8);
        
        instruction.add(VALUE, getValue(start + 0, 8));
    }
    

}
