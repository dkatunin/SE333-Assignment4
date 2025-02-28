/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.io.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class BoundedReaderTest {
    private static final String STRING_END_NO_EOL = "0\n1\n2";

    private static final String STRING_END_EOL = "0\n1\n2\n";

    private final Reader sr = new BufferedReader(new StringReader("01234567890"));

    private final Reader shortReader = new BufferedReader(new StringReader("01"));

    @Test
    public void readTillEnd() throws IOException {
        try (final BoundedReader mr = new BoundedReader(sr, 3)) {
            mr.read();
            mr.read();
            mr.read();
            assertEquals(-1, mr.read());
        }
    }

    @Test
    public void shortReader() throws IOException {
        try (final BoundedReader mr = new BoundedReader(shortReader, 3)) {
            mr.read();
            mr.read();
            assertEquals(-1, mr.read());
        }
    }

    @Test
    public void readMulti() throws IOException {
        try (final BoundedReader mr = new BoundedReader(sr, 3)) {
            final char[] cbuf = new char[4];
            for (int i = 0; i < cbuf.length; i++) {
                cbuf[i] = 'X';
            }
            final int read = mr.read(cbuf, 0, 4);
            assertEquals(3, read);
            assertEquals('0', cbuf[0]);
            assertEquals('1', cbuf[1]);
            assertEquals('2', cbuf[2]);
            assertEquals('X', cbuf[3]);
        }
    }

    @Test
    public void readMultiWithOffset() throws IOException {
        try (final BoundedReader mr = new BoundedReader(sr, 3)) {
            final char[] cbuf = new char[4];
            for (int i = 0; i < cbuf.length; i++) {
                cbuf[i] = 'X';
            }
            final int read = mr.read(cbuf, 1, 2);
            assertEquals(2, read);
            assertEquals('X', cbuf[0]);
            assertEquals('0', cbuf[1]);
            assertEquals('1', cbuf[2]);
            assertEquals('X', cbuf[3]);
        }
    }

    @Test
    public void markReset() throws IOException {
        try (final BoundedReader mr = new BoundedReader(sr, 3)) {
            mr.mark(3);
            mr.read();
            mr.read();
            mr.read();
            mr.reset();
            mr.read();
            mr.read();
            mr.read();
            assertEquals(-1, mr.read());
        }
    }

    @Test
    public void markResetWithMarkOutsideBoundedReaderMax() throws IOException {
        try (final BoundedReader mr = new BoundedReader(sr, 3)) {
            mr.mark(4);
            mr.read();
            mr.read();
            mr.read();
            assertEquals(-1, mr.read());
        }
    }

    @Test
    public void markResetWithMarkOutsideBoundedReaderMaxAndInitialOffset() throws IOException {
        try (final BoundedReader mr = new BoundedReader(sr, 3)) {
            mr.read();
            mr.mark(3);
            mr.read();
            mr.read();
            assertEquals(-1, mr.read());
        }
    }

    @Test
    public void markResetFromOffset1() throws IOException {
        try (final BoundedReader mr = new BoundedReader(sr, 3)) {
            mr.mark(3);
            mr.read();
            mr.read();
            mr.read();
            assertEquals(-1, mr.read());
            mr.reset();
            mr.mark(1);
            mr.read();
            assertEquals(-1, mr.read());
        }
    }

    @Test
    public void markResetMarkMore() throws IOException {
        try (final BoundedReader mr = new BoundedReader(sr, 3)) {
            mr.mark(4);
            mr.read();
            mr.read();
            mr.read();
            mr.reset();
            mr.read();
            mr.read();
            mr.read();
            assertEquals(-1, mr.read());
        }
    }

    @Test
    public void skipTest() throws IOException {
        try (final BoundedReader mr = new BoundedReader(sr, 3)) {
            mr.skip(2);
            mr.read();
            assertEquals(-1, mr.read());
        }
    }

    @Test
    public void closeTest() throws IOException {
        final AtomicBoolean closed = new AtomicBoolean(false);
        try (final Reader sr = new BufferedReader(new StringReader("01234567890")) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        }) {

            try (final BoundedReader mr = new BoundedReader(sr, 3)) {
                // nothing
            }
        }
        assertTrue(closed.get());
    }

    private void testLineNumberReader(final Reader source) throws IOException {
        try (LineNumberReader reader = new LineNumberReader(new BoundedReader(source, 10_000_000))) {
            while (reader.readLine() != null) {
                // noop
            }
        }
    }

    @Test(timeout = 5000)
    public void testLineNumberReaderAndStringReaderLastLineEolNo() throws IOException {
        testLineNumberReader(new StringReader(STRING_END_NO_EOL));
    }

    @Test(timeout = 5000)
    public void testLineNumberReaderAndStringReaderLastLineEolYes() throws IOException {
        testLineNumberReader(new StringReader(STRING_END_EOL));
    }

    @Test(timeout = 5000)
    public void testLineNumberReaderAndFileReaderLastLineEolNo() throws IOException {
        testLineNumberReaderAndFileReaderLastLine(STRING_END_NO_EOL);
    }

    @Test(timeout = 5000)
    public void testLineNumberReaderAndFileReaderLastLineEolYes() throws IOException {
        testLineNumberReaderAndFileReaderLastLine(STRING_END_EOL);
    }

    public void testLineNumberReaderAndFileReaderLastLine(final String data) throws IOException {
        final Path path = Files.createTempFile(getClass().getSimpleName(), ".txt");
        try {
            final File file = path.toFile();
            FileUtils.write(file, data, StandardCharsets.ISO_8859_1);
            try (FileReader source = new FileReader(file)) {
                testLineNumberReader(source);
            }
        } finally {
            Files.delete(path);
        }
    }

    @Test(timeout = 5000)
    public void testReadBytesEOF() throws IOException {
        final BoundedReader mr = new BoundedReader(sr, 3);
        try (BufferedReader br = new BufferedReader(mr)) {
            br.readLine();
            br.readLine();
        }
    }

    @Test
    public void testMarkedReadOnEOF() throws IOException {
        try (final BoundedReader mr = new BoundedReader(new StringReader("abcd"), 10)) {
            mr.read();
            mr.mark(2);
            mr.read();
            mr.read();
            assertEquals(-1, mr.read());
        }
    }
}
