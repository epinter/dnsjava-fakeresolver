/*
   Copyright 2025 Emerson Pinter

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package dev.pinter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;
import org.xbill.DNS.lookup.LookupSession;

import java.io.IOException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

public class FakeResolverTest {
    private static final Logger logger = LoggerFactory.getLogger(FakeResolverTest.class);

    @BeforeEach
    public void setup() {
        Lookup.getDefaultCache(DClass.IN).clearCache();
    }

    @Test
    public void shouldResolveA() throws IOException {
        String domain = "fakeresolver.zone";
        FakeResolver fakeResolver = new FakeResolver();
        fakeResolver.fromZoneFile(domain, "src/test/resources/FakeResolverTest.zone");

        Lookup.setDefaultResolver(fakeResolver);
        Record[] found = new Lookup(Name.fromString("shouldResolveA", Name.fromString(domain)), Type.A).run();
        assertNotNull(found);
        assertEquals(1, found.length);
    }

    @Test
    public void shouldRejectInvalidDomain() throws IOException {
        FakeResolver fakeResolver = new FakeResolver();
        assertThrowsExactly(IllegalArgumentException.class, () -> fakeResolver.fromZoneFile(null, ""));
    }

    @Test
    public void shouldReturnList() throws IOException {
        String domain = "fakeresolver.zone";
        FakeResolver fakeResolver = new FakeResolver();
        fakeResolver.fromZoneFile(domain, "src/test/resources/FakeResolverTest.zone");
        logger.info("Size: {}", fakeResolver.getRecords().size());
        assertFalse(fakeResolver.getRecords().isEmpty());
    }

    @Test
    public void shouldResolveTXT() throws IOException {
        String domain = "fakeresolver.zone";
        FakeResolver fakeResolver = new FakeResolver();
        fakeResolver.fromZoneFile(domain, "src/test/resources/FakeResolverTest.zone");

        List<Record> found = lookup(Name.fromString("shouldResolveTXT", Name.fromString(domain)), Type.TXT, fakeResolver);
        logger.info("Found: {}", found);
        assertEquals(2, found.size());
    }

    @Test
    public void shouldResolveMX() throws IOException {
        String domain = "fakeresolver.zone";
        FakeResolver fakeResolver = new FakeResolver();
        fakeResolver.fromZoneFile(domain, "src/test/resources/FakeResolverTest.zone");

        List<Record> found = lookup(Name.fromString("shouldResolveMX", Name.fromString(domain)), Type.MX, fakeResolver);
        logger.info("Found: {}", found);
        int mxPrio = ((MXRecord) found.getFirst()).getPriority();
        logger.info("MX Priority: {}", mxPrio);
        assertEquals(1, found.size());
        assertEquals(10, mxPrio);
    }

    @Test
    public void shouldResolveExternallyCNAME() throws IOException {
        String domain = "fakeresolver.zone";
        FakeResolver fakeResolver = new FakeResolver(new SimpleResolver("8.8.4.4"));
        fakeResolver.fromZoneFile(domain, "src/test/resources/FakeResolverTest.zone");

        List<Record> found = lookup(Name.fromString("shouldResolveExternallyCNAME", Name.fromString(domain)), Type.A, fakeResolver);
        logger.info("Found: {}", found);
        assertEquals(1, found.size());
    }

    @Test
    public void shouldResolveExternallyLegacy() throws IOException {
        String domain = "fakeresolver.zone";
        FakeResolver fakeResolver = new FakeResolver(new SimpleResolver("8.8.4.4"));
        fakeResolver.fromZoneFile(domain, "src/test/resources/FakeResolverTest.zone");

        Record[] found = new Lookup(Name.fromString("www.google.com."), Type.A).run();
        assertNotNull(found);
        logger.info("Found: {}", found[0]);
        assertEquals(1, found.length);
    }

    @Test
    public void shouldReturnNotImplementedSetPort() {
        assertThrowsExactly(UnsupportedOperationException.class, () -> new FakeResolver().setPort(0));
    }

    @Test
    public void shouldReturnNotImplementedSetTCP() {
        assertThrowsExactly(UnsupportedOperationException.class, () -> new FakeResolver().setTCP(false));
    }

    @Test
    public void shouldReturnNotImplementedSetIgnoreTruncation() {
        assertThrowsExactly(UnsupportedOperationException.class, () -> new FakeResolver().setIgnoreTruncation(false));
    }

    @Test
    public void shouldReturnNotImplementedSetEDNS() {
        assertThrowsExactly(UnsupportedOperationException.class, () -> new FakeResolver().setEDNS(0));
    }

    @Test
    public void shouldReturnNotImplementedSetTSIGKey() {
        assertThrowsExactly(UnsupportedOperationException.class, () -> new FakeResolver().setTSIGKey(null));
    }

    @Test
    public void shouldReturnNotImplementedSetTimeout() {
        assertThrowsExactly(UnsupportedOperationException.class, () -> new FakeResolver().setTimeout(Duration.ZERO));
    }

    @Test
    public void shouldGenerateNStrings() {
        assertEquals(11, FakeResolver.genNRandomTXTRecords("test.", 11).size());
    }

    @Test
    public void shouldNotGenerateNStrings() {
        assertThrowsExactly(IllegalArgumentException.class, () -> FakeResolver.genNRandomTXTRecords("", 11).size());
    }

    @Test
    public void shouldReturnOneRecord() throws UnknownHostException, TextParseException {
        FakeResolver fakeResolver = new FakeResolver();
        fakeResolver.setRecord(new TXTRecord(Name.fromString("a."), DClass.IN, 60L, "a"));
        assertEquals(1, fakeResolver.getRecords().size());
    }

    @Test
    public void shouldReturnTwoRecords() throws UnknownHostException, TextParseException {
        FakeResolver fakeResolver = new FakeResolver();
        fakeResolver.setRecords(Arrays.asList(new Record[]{
                        new TXTRecord(Name.fromString("a."), DClass.IN, 60L, "a"),
                        new TXTRecord(Name.fromString("b."), DClass.IN, 30L, "b")
                })
        );
        assertEquals(2, fakeResolver.getRecords().size());
    }

    @Test
    public void shouldReturnThreeRecords() throws UnknownHostException, TextParseException {
        FakeResolver fakeResolver = new FakeResolver();
        fakeResolver.setRecords(Arrays.asList(new Record[]{
                        new TXTRecord(Name.fromString("a."), DClass.IN, 60L, "a"),
                        new TXTRecord(Name.fromString("b."), DClass.IN, 30L, "b")
                })
        );
        fakeResolver.addRecord(new TXTRecord(Name.fromString("c."), DClass.IN, 30L, "c"));
        assertEquals(3, fakeResolver.getRecords().size());
    }

    @Test
    public void shouldReturnEmpty() throws UnknownHostException, TextParseException {
        FakeResolver fakeResolver = new FakeResolver();
        fakeResolver.setRecord(new TXTRecord(Name.fromString("a."), DClass.IN, 60L, "a"));
        fakeResolver.clearRecords();
        assertEquals(0, fakeResolver.getRecords().size());
    }


    private List<Record> lookup(Name name, int type, Resolver resolver) {
        List<Record> found = new ArrayList<>();
        try {
            LookupSession lookup = LookupSession.defaultBuilder().resolver(resolver).build();
            lookup.lookupAsync(name, type)
                    .whenComplete((a, ex) -> {
                        found.addAll(a.getRecords());
                        if (ex != null) {
                            logger.error("Error", ex);
                        }
                    }).toCompletableFuture().get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return found;
    }

}
