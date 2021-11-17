package org.graalvm.vm.trcview.data.type;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.graalvm.vm.util.BitTest;

public class VariableType {
    public static final VariableType GENERIC_POINTER = new VariableType(0, "void*");
    public static final VariableType POINTER_I8 = new VariableType(1, "I8*");
    public static final VariableType POINTER_I16 = new VariableType(2, "I16*");
    public static final VariableType POINTER_I32 = new VariableType(3, "I32*");
    public static final VariableType POINTER_I64 = new VariableType(4, "I64*");
    public static final VariableType POINTER_U8 = new VariableType(5, "U8*");
    public static final VariableType POINTER_U16 = new VariableType(6, "U16*");
    public static final VariableType POINTER_U32 = new VariableType(7, "U32*");
    public static final VariableType POINTER_U64 = new VariableType(8, "U64*");
    public static final VariableType POINTER_S8 = new VariableType(9, "S8*");
    public static final VariableType POINTER_S16 = new VariableType(10, "S16*");
    public static final VariableType POINTER_S32 = new VariableType(11, "S32*");
    public static final VariableType POINTER_S64 = new VariableType(12, "S64*");
    public static final VariableType POINTER_F32 = new VariableType(13, "F32*");
    public static final VariableType POINTER_F64 = new VariableType(14, "F64*");
    public static final VariableType POINTER_FX16 = new VariableType(15, "FX16*");
    public static final VariableType POINTER_FX32 = new VariableType(16, "FX32*");
    public static final VariableType POINTER_CODE = new VariableType(17, "CODE*");
    public static final VariableType I8 = new VariableType(18, "I8");
    public static final VariableType I16 = new VariableType(19, "I16");
    public static final VariableType I32 = new VariableType(20, "I32");
    public static final VariableType I64 = new VariableType(21, "I64");
    public static final VariableType U8 = new VariableType(22, "U8");
    public static final VariableType U16 = new VariableType(23, "U16");
    public static final VariableType U32 = new VariableType(24, "U32");
    public static final VariableType U64 = new VariableType(25, "U64");
    public static final VariableType S8 = new VariableType(26, "S8");
    public static final VariableType S16 = new VariableType(27, "S16");
    public static final VariableType S32 = new VariableType(28, "S32");
    public static final VariableType S64 = new VariableType(29, "S64");
    public static final VariableType F32 = new VariableType(30, "F32");
    public static final VariableType F64 = new VariableType(31, "F64");
    public static final VariableType FX16 = new VariableType(32, "FX16");
    public static final VariableType FX32 = new VariableType(33, "FX32");
    public static final VariableType PC = new VariableType(34, "PC");
    public static final VariableType SP = new VariableType(35, "SP");
    public static final VariableType FLAGS = new VariableType(36, "FLAGS");

    public static final VariableType CONFLICT = new VariableType(61, "CONFLICT");
    public static final VariableType UNKNOWN = new VariableType(62, "UNKNOWN");

    public static final long CHAIN_BIT = 0x80000000_00000000L;
    public static final long BREAK_BIT = 0x40000000_00000000L;

    private final long mask;
    private final String name;

    private static final List<VariableType> TYPES = Collections.unmodifiableList(
                    Arrays.asList(GENERIC_POINTER, POINTER_I8, POINTER_I16, POINTER_I32, POINTER_I32, POINTER_I64, POINTER_U8, POINTER_U16, POINTER_U32, POINTER_U64, POINTER_S8, POINTER_S16,
                                    POINTER_S32, POINTER_S64, POINTER_F32, POINTER_F64, POINTER_FX16, POINTER_FX32, POINTER_CODE, I8, I16, I32, I64, U8, U16, U32, U64, S8, S16, S32, S64, F32, F64,
                                    FX16, FX32, PC, SP, FLAGS));

    public VariableType(int bit, String name) {
        mask = 1L << bit;
        this.name = name;
    }

    public long getMask() {
        return mask;
    }

    public long combine(long bits) {
        return bits | mask;
    }

    public boolean test(long bits) {
        return BitTest.test(bits, mask);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static List<VariableType> getTypeConstraints() {
        return TYPES;
    }

    public static VariableType resolve(long bitmask, int addrsize) {
        long bits = bitmask & ~(CHAIN_BIT | UNKNOWN.mask | CONFLICT.mask | PC.mask | SP.mask | PC.mask);
        long ptrbits = GENERIC_POINTER.mask | POINTER_I8.mask | POINTER_I16.mask | POINTER_I32.mask | POINTER_I64.mask | POINTER_U8.mask | POINTER_U16.mask | POINTER_U32.mask | POINTER_U64.mask |
                        POINTER_S8.mask | POINTER_S16.mask | POINTER_S32.mask | POINTER_S64.mask | POINTER_F32.mask | POINTER_F64.mask | POINTER_FX16.mask | POINTER_FX32.mask | POINTER_CODE.mask;

        if (bits == 0) {
            return null;
        }

        long addrbits;
        switch (addrsize) {
            case 1:
                addrbits = I8.mask | U8.mask | S8.mask;
                break;
            case 2:
                addrbits = I16.mask | U16.mask | S16.mask;
                break;
            case 4:
                addrbits = I32.mask | U32.mask | S32.mask;
                break;
            case 8:
                addrbits = I64.mask | U64.mask | S64.mask;
                break;
            default:
                throw new IllegalArgumentException("invalid address size: " + addrsize);
        }

        // check conflicts
        long intmask = I8.mask | I16.mask | I32.mask | I64.mask;
        long uintmask = U8.mask | U16.mask | U32.mask | U64.mask;
        long sintmask = S8.mask | S16.mask | S32.mask | S64.mask;

        long intbits = Long.bitCount(bits & intmask);
        long uintbits = Long.bitCount(bits & uintmask);
        long sintbits = Long.bitCount(bits & sintmask);

        if (intbits > 1 || uintbits > 1 || sintbits > 1) {
            return UNKNOWN;
        }

        // S8:
        if (S8.test(bits)) {
            if (U8.test(bits)) {
                long m = U8.mask | S8.mask | I8.mask;
                if ((bits & ~m) == 0) {
                    return I8;
                } else {
                    return UNKNOWN;
                }
            } else {
                long m = S8.mask | I8.mask;
                if ((bits & ~m) == 0) {
                    return S8;
                } else {
                    return UNKNOWN;
                }
            }
        }

        // U8:
        if (U8.test(bits)) {
            long m = U8.mask | I8.mask;
            if ((bits & ~m) == 0) {
                return U8;
            } else {
                return UNKNOWN;
            }
        }

        // precise pointer types for f32/f64/fx16/fx32
        if (POINTER_F32.test(bits) && (bits & ~(addrbits | POINTER_F32.mask)) == 0) {
            return POINTER_F32;
        }

        if (POINTER_F64.test(bits) && (bits & ~(addrbits | POINTER_F64.mask)) == 0) {
            return POINTER_F64;
        }

        if (POINTER_FX16.test(bits) && (bits & ~(addrbits | POINTER_FX16.mask)) == 0) {
            return POINTER_FX16;
        }

        if (POINTER_FX32.test(bits) && (bits & ~(addrbits | POINTER_FX32.mask)) == 0) {
            return POINTER_FX32;
        }

        // precise pointer types with signed/unsigned
        if (POINTER_U8.test(bits) && (bits & ~(addrbits | POINTER_U8.mask | POINTER_I8.mask)) == 0) {
            return POINTER_U8;
        }

        if (POINTER_S8.test(bits) && (bits & ~(addrbits | POINTER_S8.mask | POINTER_I8.mask)) == 0) {
            return POINTER_S8;
        }

        if (POINTER_U16.test(bits) && (bits & ~(addrbits | POINTER_U16.mask | POINTER_I16.mask)) == 0) {
            return POINTER_U16;
        }

        if (POINTER_S16.test(bits) && (bits & ~(addrbits | POINTER_S16.mask | POINTER_I16.mask)) == 0) {
            return POINTER_S16;
        }

        if (POINTER_U32.test(bits) && (bits & ~(addrbits | POINTER_U32.mask | POINTER_I32.mask)) == 0) {
            return POINTER_U32;
        }

        if (POINTER_S32.test(bits) && (bits & ~(addrbits | POINTER_S32.mask | POINTER_I32.mask)) == 0) {
            return POINTER_S32;
        }

        // generic pointer types
        // I8*
        if (POINTER_I8.test(bits) && (bits & ~(addrbits | POINTER_I8.mask | POINTER_U8.mask | POINTER_S8.mask)) == 0) {
            return POINTER_I8;
        }

        // I16*
        if (POINTER_I16.test(bits) && (bits & ~(addrbits | POINTER_I16.mask | POINTER_U16.mask | POINTER_S16.mask)) == 0) {
            return POINTER_I16;
        }

        // I32*
        if (POINTER_I32.test(bits) && (bits & ~(addrbits | POINTER_I32.mask | POINTER_U32.mask | POINTER_S32.mask)) == 0) {
            return POINTER_I32;
        }

        // I64*
        if (POINTER_I64.test(bits) && (bits & ~(addrbits | POINTER_I64.mask | POINTER_U64.mask | POINTER_S64.mask)) == 0) {
            return POINTER_I64;
        }

        // check for generic pointers
        if ((bits & addrbits) != 0 && (bits & ~(addrbits | ptrbits)) == 0) {
            return GENERIC_POINTER;
        }

        return UNKNOWN;
    }
}