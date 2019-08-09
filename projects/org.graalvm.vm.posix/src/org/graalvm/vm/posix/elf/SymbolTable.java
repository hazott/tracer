/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.graalvm.vm.posix.elf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable extends Section {
    private List<ElfSymbol> symbols;
    private Map<String, ElfSymbol> globals;

    public SymbolTable(Section section) {
        super(section);

        if ((section.sh_size % section.sh_entsize) != 0) {
            throw new IllegalArgumentException("invalid section");
        }

        int cnt = (int) (section.sh_size / section.sh_entsize);

        symbols = new ArrayList<>();
        globals = new HashMap<>();
        for (int i = 0; i < cnt; i++) {
            ElfSymbol sym = new ElfSymbol(section, i);
            symbols.add(sym);
            if (sym.getVisibility() == Symbol.DEFAULT) {
                if (sym.getBind() == Symbol.GLOBAL) {
                    globals.put(sym.getName(), sym);
                } else if (sym.getBind() == Symbol.WEAK && !globals.containsKey(sym.getName())) {
                    globals.put(sym.getName(), sym);
                }
            }
        }
    }

    public ElfSymbol getSymbol(int i) {
        return symbols.get(i);
    }

    public ElfSymbol getSymbol(String name) {
        return globals.get(name);
    }

    public List<ElfSymbol> getSymbols() {
        return Collections.unmodifiableList(symbols);
    }

    public int size() {
        return symbols.size();
    }
}
