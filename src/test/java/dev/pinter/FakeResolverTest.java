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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.RRset;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FakeResolverTest {
    private static final Logger logger = LoggerFactory.getLogger(FakeResolverTest.class);
    private static final String DNSZONEFILE = "src/test/resources/FakeResolverTest.zone";
    private static String externalNameA = "example.com";
    private static String externalNameAAAA = "example.com";
    private static String externalNameTXT = "example.com";
    private static String externalNameMX = "example.com";
    private static String externalNameCNAME = "www.example.com";

    @BeforeAll
    public static void setupAll() throws TextParseException {
        if (new Lookup(externalNameA, Type.A).run() == null) {
            externalNameA = "apache.org";
        }

        if (new Lookup(externalNameAAAA, Type.A).run() == null) {
            externalNameAAAA = "apache.org";
        }

        if (new Lookup(externalNameTXT, Type.TXT).run() == null) {
            externalNameTXT = "apache.org";
        }

        if (new Lookup(externalNameMX, Type.MX).run() == null) {
            externalNameMX = "apache.org";
        }

        if (new Lookup(externalNameCNAME, Type.CNAME).run() == null) {
            externalNameCNAME = "www.apache.org";
        }
    }

    @BeforeEach
    public void setup() {
        Lookup.getDefaultCache(DClass.IN).clearCache();
    }

    @Test
    public void shouldResolveA() throws IOException {
        String domain = "fakeresolver.zone";
        FakeResolver fakeResolver = new FakeResolver();
        fakeResolver.fromZoneFile(domain, DNSZONEFILE);

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
        fakeResolver.fromZoneFile(domain, DNSZONEFILE);
        logger.info("Size: {}", fakeResolver.getRecords().size());
        assertFalse(fakeResolver.getRecords().isEmpty());
    }

    @Test
    public void shouldResolveTXT() throws IOException {
        String domain = "fakeresolver.zone";
        FakeResolver fakeResolver = new FakeResolver();
        fakeResolver.fromZoneFile(domain, DNSZONEFILE);

        List<Record> found = lookup(Name.fromString("shouldResolveTXT", Name.fromString(domain)), Type.TXT, fakeResolver);
        assertNotNull(found);
        found.forEach(r -> assertEquals(Type.TXT, r.getType()));
        assertEquals(2, found.size());
    }

    @Test
    public void shouldMatchResolvedTXT() throws IOException {
        recordsMatch(Name.fromString(externalNameTXT), Type.TXT);
    }

    @Test
    public void shouldResolveMX() throws IOException {
        String domain = "fakeresolver.zone";
        FakeResolver fakeResolver = new FakeResolver();
        fakeResolver.fromZoneFile(domain, DNSZONEFILE);

        List<Record> found = lookup(Name.fromString("shouldResolveMX", Name.fromString(domain)), Type.MX, fakeResolver);
        assertNotNull(found);
        found.forEach(r -> assertEquals(Type.MX, r.getType()));
        assertEquals(1, found.size());
        assertEquals(10, ((MXRecord) found.get(0)).getPriority());
    }

    @Test
    public void shouldMatchResolvedMX() throws IOException {
        recordsMatch(Name.fromString(externalNameMX), Type.MX);
    }

    @Test
    public void shouldResolveExternallyCNAME() throws IOException {
        String domain = "fakeresolver.zone";
        FakeResolver fakeResolver = new FakeResolver(new SimpleResolver());
        fakeResolver.fromZoneFile(domain, DNSZONEFILE);

        List<Record> found = lookup(Name.fromString("shouldResolveExternallyCNAME", Name.fromString(domain)), Type.A, fakeResolver);
        assertNotNull(found);
        found.forEach(r -> assertEquals(Type.A, r.getType()));
        assertEquals(1, found.size());
    }

    @Test
    public void shouldMatchResolvedCNAME() throws IOException {
        recordsMatch(Name.fromString(externalNameCNAME), Type.CNAME);
    }

    @Test
    public void shouldMatchResolvedA() throws IOException {
        recordsMatch(Name.fromString(externalNameA), Type.A);
    }

    @Test
    public void shouldMatchResolvedAAAA() throws IOException {
        recordsMatch(Name.fromString(externalNameAAAA), Type.AAAA);
    }

    @Test
    public void shouldResolveExternallyLegacy() throws IOException {
        String domain = "fakeresolver.zone";
        FakeResolver fakeResolver = new FakeResolver(new SimpleResolver());
        fakeResolver.fromZoneFile(domain, DNSZONEFILE);

        Record[] found = new Lookup(Name.fromString(externalNameCNAME), Type.A).run();
        assertNotNull(found);
        Arrays.asList(found).forEach(r -> assertEquals(Type.A, r.getType()));
        assertTrue(found.length > 0);
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

    private void recordsMatch(Name name, int type) throws UnknownHostException {
        FakeResolver fakeResolver = new FakeResolver();
        SimpleResolver simpleResolver = new SimpleResolver();

        RRset resultFakeResolver = lookupRRset(name, type, fakeResolver);
        RRset resultSimpleResolver = lookupRRset(name, type, simpleResolver);

        assertTrue(() -> rrMatch(resultSimpleResolver, resultFakeResolver),
                String.format("Records for name '%s' type '%s' are different.\n\tResolvers: [%s, %s],\n\tRecords:\n\t\t[%s,\n\t\t%s]",
                        name, Type.string(type),
                        simpleResolver, fakeResolver,
                        resultSimpleResolver, resultFakeResolver)
        );
    }

    private boolean rrMatch(RRset rr1, RRset rr2) {
        int count = 0;
        for (Record r1 : rr1.rrs()) {
            for (Record r2 : rr2.rrs()) {
                if (r1.equals(r2)) {
                    count++;
                }
            }
        }
        return count == rr1.size() && count == rr2.size();
    }

    private RRset lookupRRset(Name name, int type, Resolver resolver) {
        RRset rr = new RRset();
        try {
            LookupSession lookup = LookupSession.defaultBuilder().resolver(resolver).build();
            lookup.lookupAsync(name, type)
                    .whenComplete((a, ex) -> {
                        a.getRecords().forEach(rr::addRR);
                        if (ex != null) {
                            throw new RuntimeException(ex);
                        }
                    })
                    .toCompletableFuture().get();
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error during dns lookup: ", e);
            return new RRset();
        }
        return rr;
    }

    private List<Record> lookup(Name name, int type, Resolver resolver) {
        return lookupRRset(name, type, resolver).rrs();
    }
}
