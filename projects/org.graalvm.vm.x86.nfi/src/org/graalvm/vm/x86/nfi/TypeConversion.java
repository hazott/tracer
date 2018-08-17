package org.graalvm.vm.x86.nfi;

import org.graalvm.vm.x86.nfi.TypeConversionFactory.AsI64NodeGen;
import org.graalvm.vm.x86.nfi.TypeConversionFactory.AsPointerNodeGen;
import org.graalvm.vm.x86.nfi.TypeConversionFactory.AsStringNodeGen;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;

public class TypeConversion extends Node {
    protected static Node createIsNull() {
        return Message.IS_NULL.createNode();
    }

    protected static boolean checkNull(Node isNull, TruffleObject object) {
        return ForeignAccess.sendIsNull(isNull, object);
    }

    abstract static class IsNullNode extends TypeConversion {
        abstract boolean execute(Object arg);

        @Specialization
        protected boolean executeTruffleObject(TruffleObject obj, @Cached("createIsNull()") Node isNull) {
            return checkNull(isNull, obj);
        }
    }

    abstract static class AsI64Node extends TypeConversion {
        abstract long execute(Object arg);

        @Specialization
        protected long executeI8(byte arg) {
            return arg;
        }

        @Specialization
        protected long executeI16(short arg) {
            return arg;
        }

        @Specialization
        protected long executeI32(int arg) {
            return arg;
        }

        @Specialization
        protected long executeI64(long arg) {
            return arg;
        }

        @Specialization(guards = "checkIsPointer(isPointer, arg)")
        @SuppressWarnings("unused")
        protected long serializePointer(TruffleObject arg,
                        @Cached("createIsPointer()") Node isPointer,
                        @Cached("createAsPointer()") Node asPointer) {
            try {
                long pointer = ForeignAccess.sendAsPointer(asPointer, arg);
                return pointer;
            } catch (UnsupportedMessageException ex) {
                CompilerDirectives.transferToInterpreter();
                throw ex.raise();
            }
        }

        @Specialization(guards = "checkNull(isNull, arg)")
        @SuppressWarnings("unused")
        long nullAsPointer(TruffleObject arg, @Cached("createIsNull()") Node isNull) {
            return 0;
        }

        protected static boolean checkIsPointer(Node isPointer, TruffleObject object) {
            return ForeignAccess.sendIsPointer(isPointer, object);
        }

        static Node createIsPointer() {
            return Message.IS_POINTER.createNode();
        }

        static Node createAsPointer() {
            return Message.AS_POINTER.createNode();
        }

        static Node createToNative() {
            return Message.TO_NATIVE.createNode();
        }

        static AsI64Node createRecursive() {
            return AsI64NodeGen.create();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "checkIsBoxed(isBoxed, arg)")
        protected long unbox(TruffleObject arg,
                        @Cached("createIsBoxed()") Node isBoxed,
                        @Cached("createUnbox()") Node unbox,
                        @Cached("createRecursive()") AsI64Node recursive) {
            try {
                Object value = ForeignAccess.sendUnbox(unbox, arg);
                return recursive.execute(value);
            } catch (UnsupportedMessageException ex) {
                CompilerDirectives.transferToInterpreter();
                throw ex.raise();
            }
        }

        protected static boolean checkIsBoxed(Node isBoxed, TruffleObject object) {
            return ForeignAccess.sendIsBoxed(isBoxed, object);
        }

        static Node createIsBoxed() {
            return Message.IS_BOXED.createNode();
        }

        static Node createUnbox() {
            return Message.UNBOX.createNode();
        }
    }

    abstract static class AsPointerNode extends TypeConversion {
        abstract NativePointer execute(TruffleObject arg);

        @Specialization(guards = "checkIsPointer(isPointer, arg)")
        @SuppressWarnings("unused")
        protected NativePointer serializePointer(TruffleObject arg,
                        @Cached("createIsPointer()") Node isPointer,
                        @Cached("createAsPointer()") Node asPointer) {
            try {
                long pointer = ForeignAccess.sendAsPointer(asPointer, arg);
                return new NativePointer(pointer);
            } catch (UnsupportedMessageException ex) {
                CompilerDirectives.transferToInterpreter();
                throw ex.raise();
            }
        }

        @Specialization(guards = "checkNull(isNull, arg)")
        @SuppressWarnings("unused")
        NativePointer nullAsPointer(TruffleObject arg, @Cached("createIsNull()") Node isNull) {
            return new NativePointer(0);
        }

        @Specialization(guards = "!checkNull(isNull, arg)", replaces = "serializePointer")
        @SuppressWarnings("unused")
        protected NativePointer transitionToNative(TruffleObject arg,
                        @Cached("createIsNull()") Node isNull,
                        @Cached("createToNative()") Node toNative,
                        @Cached("createRecursive()") AsPointerNode recursive) {
            try {
                Object nativeObj = ForeignAccess.sendToNative(toNative, arg);
                return recursive.execute((TruffleObject) nativeObj);
            } catch (UnsupportedMessageException ex) {
                CompilerDirectives.transferToInterpreter();
                throw UnsupportedTypeException.raise(ex, new Object[]{arg});
            }
        }

        protected static boolean checkIsPointer(Node isPointer, TruffleObject object) {
            return ForeignAccess.sendIsPointer(isPointer, object);
        }

        static Node createIsPointer() {
            return Message.IS_POINTER.createNode();
        }

        static Node createAsPointer() {
            return Message.AS_POINTER.createNode();
        }

        static Node createToNative() {
            return Message.TO_NATIVE.createNode();
        }

        static AsPointerNode createRecursive() {
            return AsPointerNodeGen.create();
        }
    }

    abstract static class AsStringNode extends TypeConversion {
        final boolean acceptAnything;

        AsStringNode(boolean acceptAnything) {
            this.acceptAnything = acceptAnything;
        }

        abstract String execute(Object arg);

        @Specialization
        String stringAsString(String str) {
            return str;
        }

        @Specialization(guards = "checkNull(isNull, arg)")
        @SuppressWarnings("unused")
        String nullAsString(TruffleObject arg,
                        @Cached("createIsNull()") Node isNull) {
            return null;
        }

        @Specialization(guards = "checkIsBoxed(isBoxed, arg)")
        @SuppressWarnings("unused")
        String boxedAsString(TruffleObject arg,
                        @Cached("createIsBoxed()") Node isBoxed,
                        @Cached("createUnbox()") Node unbox,
                        @Cached("createRecursive()") AsStringNode asString) {
            try {
                Object unboxed = ForeignAccess.sendUnbox(unbox, arg);
                return asString.execute(unboxed);
            } catch (UnsupportedMessageException ex) {
                CompilerDirectives.transferToInterpreter();
                throw UnsupportedTypeException.raise(ex, new Object[]{arg});
            }
        }

        @Specialization(guards = {"acceptAnything", "isOther(arg)"})
        @TruffleBoundary
        String otherAsString(Object arg) {
            return arg.toString();
        }

        protected static Node createIsBoxed() {
            return Message.IS_BOXED.createNode();
        }

        protected static Node createUnbox() {
            return Message.UNBOX.createNode();
        }

        protected static boolean checkIsBoxed(Node isBoxed, TruffleObject arg) {
            return ForeignAccess.sendIsBoxed(isBoxed, arg);
        }

        protected static boolean isOther(Object obj) {
            return !(obj instanceof String) && !(obj instanceof TruffleObject);
        }

        protected AsStringNode createRecursive() {
            return AsStringNodeGen.create(acceptAnything);
        }
    }
}