# A 'Fake' resolver for dnsjava

To be used with unit tests. Generate DNS records at runtime, or import from zonefiles. External records are resolved by
normal dnsjava resolvers.

### Usage

```
        FakeResolver fakeResolver = new FakeResolver();
        fakeResolver.addRecord(
                new MXRecord(Name.fromString("xyz.aaa."), DClass.IN, 30L, 10, Name.fromString("mx.xyz.aaa."))
        );
        fakeResolver.fromZoneFile("xyz.aaa", "filename.zone");

        Lookup query1 = new Lookup(Name.fromString("xyz.aaa."), Type.MX);
        Lookup query2 = new Lookup(Name.fromString("www.xyz.aaa."), Type.A);
        query1.setResolver(fakeResolver);
        query2.setResolver(fakeResolver);
        System.out.println(Arrays.toString(query1.run()));
        System.out.println(Arrays.toString(query2.run()));
```

Example of zonefile

```
$TTL 30
@   30    IN    SOA    ns1.fakeresolver.demo. hostmaster.fakeresolver.demo. 1 30 30 30 30
@   30    IN    NS     ns1.fakeresolver.demo.

www   IN    A   192.0.2.200
```

Output:

```
[xyz.aaa.		30	IN	MX	10 mx.xyz.aaa.]
[www.xyz.aaa.		30	IN	A	192.0.2.200]
```