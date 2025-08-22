/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.core.util;

import de.bluecolored.bluemap.api.debug.DebugDump;

@DebugDump
public class Key {
    private static final String MINECRAFT_NAMESPACE = StringPool.intern("minecraft");

    private final String namespace;
    private final String value;
    private final String formatted;

    public Key(String formatted) {
        int separator = formatted.indexOf(':');
        if (separator > 0) {
            String namespace = formatted.substring(0, separator);
            this.namespace = StringPool.intern(namespace);
            this.value = StringPool.intern(formatted.substring(separator + 1));
        } else {
            this.namespace = MINECRAFT_NAMESPACE;
            this.value = StringPool.intern(formatted);
        }

        this.formatted = StringPool.intern(this.namespace + ":" + this.value);
    }

    public Key(String namespace, String value) {
        this.namespace = StringPool.intern(namespace);
        this.value = StringPool.intern(value);
        this.formatted = StringPool.intern(this.namespace + ":" + this.value);
    }

    public String getNamespace() {
        return namespace;
    }

    public String getValue() {
        return value;
    }

    public String getFormatted() {
        return formatted;
    }

    @SuppressWarnings("StringEquality")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Key that = (Key) o;
        return getFormatted() == that.getFormatted();
    }

    @Override
    public int hashCode() {
        return getFormatted().hashCode();
    }

    @Override
    public String toString() {
        return formatted;
    }
}
