# peppol-directory-client

<!-- ph-badge-start -->
[![Sonatype Central](https://maven-badges.sml.io/sonatype-central/com.helger.peppol.directory/peppol-directory-client-parent-pom/badge.svg)](https://maven-badges.sml.io/sonatype-central/com.helger.peppol.directory/peppol-directory-client-parent-pom/)
[![javadoc](https://javadoc.io/badge2/com.helger.peppol.directory/peppol-directory-client/javadoc.svg)](https://javadoc.io/doc/com.helger.peppol.directory/peppol-directory-client)

> If this project saved you some time or made your day a little easier, a star would mean a lot — it helps others find it too.
<!-- ph-badge-end -->

The Java client libraries for the Peppol Directory (PD; https://directory.peppol.eu).

This project is part of my Peppol solution stack. See https://github.com/phax/peppol for other components and libraries in that area.

These libraries were part of https://github.com/phax/phoss-directory up to and including v0.19.1 and were extracted
  into this repository, so that they no longer share the release cycle and the Java baseline of the Directory server.
The Maven coordinates were changed on that occasion - the group ID is now `com.helger.peppol.directory` and the
  artifact IDs were renamed from `phoss-directory-*` to `peppol-directory-*` - because these libraries talk to the
  Peppol Directory and are not bound to a specific Directory implementation.
See the [Migration](#migration-from-phoss-directory) section below.

This project is split into the following sub-projects:
* `peppol-directory-client` - a client library to be added to SMP servers to force indexing in the PD (until v0.19.1: `phoss-directory-client`)
* `peppol-directory-searchapi` - a library with the data structures and the constants of the Directory search REST API (since v0.7.2; until v0.19.1: `phoss-directory-searchapi`)
* `peppol-directory-searchclient` - a client library to query the Directory search REST API (only v0.19.1; `phoss-directory-searchclient`)

The Directory server itself - the indexer and the publisher web application - stays in https://github.com/phax/phoss-directory

* Production version is available at https://directory.peppol.eu (for Peppol)
    * It can only handle participants registered at the SML
    * For the indexing REST API, a client certificate (SMP production) is needed 
* Test version is available at https://test-directory.peppol.eu
    * It can only handle participants registered at the SMK
    * For the indexing REST API, a client certificate (SMP test) is needed

# Migration from phoss-directory

Up to and including v0.19.1 these libraries were released from https://github.com/phax/phoss-directory.
The Maven coordinates changed as shown below - for the Java code itself, nothing changed.

| up to v0.19.1 | since v1.0.0 |
|---|---|
| `com.helger:phoss-directory-client` | `com.helger.peppol.directory:peppol-directory-client` |
| `com.helger:phoss-directory-searchapi` | `com.helger.peppol.directory:peppol-directory-searchapi` |
| `com.helger:phoss-directory-searchclient` | `com.helger.peppol.directory:peppol-directory-searchclient` |
| `com.helger:phoss-directory-parent-pom` (BOM import) | `com.helger.peppol.directory:peppol-directory-client-parent-pom` |

```xml
<!-- until v0.19.1 -->
<dependency>
  <groupId>com.helger</groupId>
  <artifactId>phoss-directory-client</artifactId>
  <version>0.19.1</version>
</dependency>

<!-- since v1.0.0 -->
<dependency>
  <groupId>com.helger.peppol.directory</groupId>
  <artifactId>peppol-directory-client</artifactId>
  <version>1.0.0</version>
</dependency>
```

What did *not* change:
* The Java package names and all class names are unchanged - `com.helger.pd.client`, `com.helger.pd.searchapi`, `com.helger.pd.searchapi.v1` and `com.helger.pd.searchclient`
* The configuration property names of the PD Client (`pdclient.*`) are unchanged
* The XML Schema in `peppol-directory-searchapi` is still contained at `/schemas/directory-search-result-list-v1.xsd`

Besides the coordinates, v1.0.0 contains these backwards incompatible changes - see the News section for the details:
* The properties files `pd-client.properties` and `private-pd-client.properties` are not evaluated any more
* The configuration properties `pdclient.keystore.type` and `pdclient.truststore.type` are mandatory now, because the implicit default `JKS` was removed
* The old names of the `pdclient.*` properties - `keystore.type`, `truststore.path` etc. - are not evaluated as a fallback anymore
* `PDClientConfiguration.getKeyStoreType ()` and `getTrustStoreType ()` may return `null`, and the constant `PDClientConfiguration.DEFAULT_TRUSTSTORE_TYPE` was removed

The old artifacts stay available on Maven Central in their released versions, but they will not receive updates any more.
If you import `com.helger:phoss-directory-parent-pom` as a BOM only to resolve the version of the Directory client,
  replace that import with `com.helger.peppol.directory:peppol-directory-client-parent-pom`.

# Building requirements

To build the software you need at least Java 17 and Apache Maven 3.x.

All artifacts of this project are consumed by third parties, so they are compiled for Java 17, so that SMP servers
  running on Java 17 can keep using them.

# PD Client

The PD client is a small Java library that uses Apache HttpClient to connect to an arbitrary phoss Directory Indexer to perform all the allowed operations (get, create/update, delete).

## Client Configuration resolution

The PD client uses `ph-config` to resolve configuration items.
See https://github.com/phax/ph-commons/wiki/ph-config for the details on the resolution logic.

Note: the proprietary configuration files `pd-client.properties` and `private-pd-client.properties` are not
  evaluated anymore since v1.0.0. Place the properties in `application.properties` instead.

## Client Configuration properties

Note: the configuration properties were heavily renamed in v0.10.0. Where an old name is still evaluated as a
  fallback, it is shown in brackets. Since v1.0.0 the old names of the `pdclient.*` properties - the ones without the
  `pdclient.` prefix - are not evaluated anymore.

Note: since v1.0.0 the `http.proxy.*` properties are no longer read by the PD Client itself. They are evaluated by the
  standard mechanism of `ph-httpclient` (`HttpClientSettingsConfig`), so the PD Client supports the same proxy
  properties as all the other Peppol components. The PD Client specific names `proxy.username` and `proxy.password`
  are still evaluated as deprecated legacy, and log a warning when used.

Note: every `http.proxy.*` property may additionally be prefixed with `pdclient.` - e.g.
  `pdclient.http.proxy.host`. The prefixed name is evaluated first, so a proxy that applies to all components can be
  configured with the unprefixed name, and the PD Client can deviate from it with the prefixed name. Each property is
  resolved on its own, so e.g. `pdclient.http.proxy.host` may be combined with an unprefixed `http.proxy.port`.

The following configuration items are supported by the PD Client:
* **`pdclient.keystore.type`** (since v0.6.0) - the type of the keystore. Can be `JKS` or `PKCS12` (case insensitive). Mandatory since v1.0.0 - up to v0.19.1 it defaulted to `JKS`.
* **`pdclient.keystore.path`** - the path to the keystore where the SMP certificate is contained
* **`pdclient.keystore.password`** - the password to open the key store
* **`pdclient.keystore.key.alias`** - the alias in the key store that denotes the SMP key 
* **`pdclient.keystore.key.password`** - the password to open the key in the key store
* **`pdclient.truststore.type`** (since v0.6.0) - the type of the keystore. Can be `JKS` or `PKCS12` (case insensitive). Mandatory since v1.0.0 - up to v0.19.1 it defaulted to `JKS`.
* **`pdclient.truststore.path`** (since v0.5.1) - the path to the trust store, where the public certificates of the phoss Directory servers are contained. No default.
* **`pdclient.truststore.password`** (since v0.5.1) - the password to open the truststore store. No default.
* **`http.proxy.enabled`** (since v1.0.0) - a boolean value to explicitly disable the proxy (`false`). If it is not set, a configured proxy host and port are sufficient to enable the proxy.
* **`http.proxy.host`** (old: **`http.proxyHost`** or **`http.proxy.address`**) - the proxy host. No default. A proxy is only used if the host **and** the port are set.
* **`http.proxy.port`** (old: **`http.proxyPort`**) - the proxy port. No default.
* Removed in 0.10.0: ~**`https.proxyHost`** - the HTTP proxy host for `https` connections only. No default.~
* Removed in 0.10.0: ~**`https.proxyPort`** - the HTTP proxy port for `https` connections only. No default.~
* **`http.proxy.username`** (old: **`http.proxyUsername`**; deprecated: **`proxy.username`**) (since v0.6.0) - the proxy username. No default. Proxy credentials are only used if the username **and** the password are set.
* **`http.proxy.password`** (old: **`http.proxyPassword`**; deprecated: **`proxy.password`**) (since v0.6.0) - the proxy password. No default.
* **`http.proxy.nonProxyHosts`** (old: **`http.nonProxyHosts`** or **`http.proxy.non-proxy`**) (since v1.0.0) - a `|` separated list of host names that are contacted directly instead of through the proxy. No default.
* **`http.connect.timeout.ms`** (old: **`connect.timeout.ms`**) (since v0.6.0) - the connection timeout in milliseconds to connect to the server. The default value is `5000` (5 seconds). A value of `0` means indefinite. A value of `-1` means using the system default.
* **`http.response.timeout.ms`** (old: **`http.request.timeout.ms`** or **`request.timeout.ms`**) (since v0.10.3) - the response/request/read timeout in milliseconds to read from the server. The default value is `10000` (10 seconds). A value of `0` means indefinite. A value of `-1` means using the system default.
* **`https.hostname-verification.disabled`** (since v0.5.1) - a boolean value to indicate if https hostname verification should be disabled (`true`) or enabled (`false`). The default value is `true`.

A template with all the supported properties and dummy values is contained in
  [`example-config/application.properties`](example-config/application.properties).

Example PD Client configuration properties:

```ini
# Key store with SMP key (required)
pdclient.keystore.type         = pkcs12
pdclient.keystore.path         = smp-test.p12
pdclient.keystore.password     = password
pdclient.keystore.key.alias    = cert
pdclient.keystore.key.password = password

# Default trust store (optional)
pdclient.truststore.type     = pkcs12
# For Test:
pdclient.truststore.path     = truststore/2025/smp-test-truststore.p12
# For production:
# pdclient.truststore.path     = truststore/2025/smp-prod-truststore.p12
pdclient.truststore.password = peppol

# TLS settings
https.hostname-verification.disabled = false
```

# PD Search Client

The PD Search Client is a small Java library that uses Apache HttpClient to query the search REST API of an arbitrary
  phoss Directory Publisher.
Contrary to the PD Client, that pushes indexing requests, the search API is publicly readable, so no client certificate
  and no configuration file are needed.

```java
try (final PDSearchClient aClient = new PDSearchClient ("https://directory.peppol.eu/"))
{
  final ResultListType aResult = aClient.search (PDSearchQuery.createGeneric ("Helger"));
  System.out.println (aResult.getTotalResultCount ());
}
```

The query fields are the ones of `EPDSearchAPIField` and are combined with "AND".
A query without a single match is answered with HTTP 200 and an empty result list - it is not an error.
The server limits the number of results that can be paged through to `CPDSearchAPI.MAX_RESULTS`, and it rate limits the
  API - an exceeded rate limit results in a `PDSearchRateLimitException` that carries the number of seconds to wait.

# PD Search API

The PD Search API library contains the data structures and the constants of the Directory search REST API.
It is used by `peppol-directory-searchclient` but can also be used stand alone - e.g. to parse a search result that was
  retrieved by other means.

* The JAXB classes in package `com.helger.pd.searchapi.v1` are generated from `directory-search-result-list-v1.xsd`,
  that is also contained in the JAR at `/schemas/directory-search-result-list-v1.xsd`
* `PDResultListMarshaller` reads and writes `ResultListType` objects, with XSD validation enabled
* `CPDSearchAPI` contains the REST API constants - the relative path, the output formats and the paging query parameters
* `EPDSearchAPIField` contains the query parameter names of the search REST API

# News and noteworthy

v1.0.0 - work in progress
* Extracted `phoss-directory-client`, `phoss-directory-searchapi` and `phoss-directory-searchclient` from https://github.com/phax/phoss-directory (last common release was v0.19.1) into this repository
* Changed the Maven group ID to `com.helger.peppol.directory` and renamed the artifact IDs from `phoss-directory-*` to `peppol-directory-*`. All package and class names are unchanged - see the [Migration](#migration-from-phoss-directory) section
* The new parent POM is `com.helger.peppol.directory:peppol-directory-client-parent-pom` - SMP servers that import `com.helger:phoss-directory-parent-pom` as a BOM need to import the new one instead
* Removed the support for the proprietary configuration files `pd-client.properties` and `private-pd-client.properties`. Only the `ph-config` default resolution is used now - place the properties in `application.properties` instead. The method `PDClientConfiguration.createPDClientValueProvider ()` was removed, because it only existed for that fallback
* The HTTP proxy is configured with `HttpClientSettingsConfig.assignConfigValuesForProxy (...)` of `ph-httpclient` now, instead of with custom code in `PDHttpClientSettings`. As a result, the PD Client supports the same proxy configuration properties as the other Peppol components - including the new properties `http.proxy.enabled` and `http.proxy.nonProxyHosts` as well as the alternative names `http.proxy.address`, `http.proxyUsername` and `http.proxyPassword`
    * All proxy properties may additionally be prefixed with `pdclient.` - e.g. `pdclient.http.proxy.host` - so that the PD Client can use a different proxy than the rest of the application. The prefixed name is evaluated first, the unprefixed name second
    * The methods `PDClientConfiguration.getHttpProxyHost ()`, `getHttpProxyPort ()`, `getProxyUsername ()` and `getProxyPassword ()` are deprecated, because the proxy is not read via them anymore
    * The PD Client specific property names `proxy.username` and `proxy.password` are still evaluated as deprecated legacy - using them logs a warning. `getProxyUsername ()` and `getProxyPassword ()` only resolve these two legacy names now
    * Proxy credentials are only used if the username and the password are set - previously a username alone was sufficient
* Removed the evaluation of the old names of the `pdclient.*` configuration properties - `keystore.type`, `keystore.path`, `keystore.password`, `keystore.key.alias`, `keystore.key.password`, `truststore.type`, `truststore.path` and `truststore.password` are not used as a fallback anymore. The old names of the `http.*` properties are still evaluated
* Removed the implicit default `JKS` of the configuration properties `pdclient.keystore.type` and `pdclient.truststore.type`, so that a missing or unknown type is no longer silently interpreted as `JKS`. `PDClientConfiguration.getKeyStoreType ()` and `getTrustStoreType ()` are `@Nullable` now, and loading a key store without a configured type fails with a `NullPointerException` from `KeyStoreHelper`. The constant `PDClientConfiguration.DEFAULT_TRUSTSTORE_TYPE` was removed
* Added `PDClient.toString ()`
* Added the file `example-config/application.properties` that contains all supported configuration properties with dummy values, as a copy-paste template
* Updated the JAXB binding file of `peppol-directory-searchapi` to the Jakarta EE binding namespace `https://jakarta.ee/xml/ns/jaxb` version 3.0, so that XJC no longer warns about the JAXB 2.x customization namespace
* For the news of v0.19.1 and before see https://github.com/phax/phoss-directory
