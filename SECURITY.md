# Security Policy

## Supported Versions

To see which versions of Apache Camel are supported please refer to this [page](https://camel.apache.org/download/).

## Reporting a Vulnerability

For information on how to report a new security problem please see [here](https://camel.apache.org/security/).

Do not file a public JIRA ticket, open a public pull request, post on a mailing list, social
media, or any other public channel for an unpublished vulnerability. Report only through the
[Apache Software Foundation Security team](https://apache.org/security/) and follow their
instructions.

## Security Model

This repository shares the same security model as Apache Camel core. Before submitting a report,
please read the project's
[Security Model](https://github.com/apache/camel/blob/main/docs/user-manual/modules/ROOT/pages/security-model.adoc).
It documents who is trusted, where the trust boundaries sit, which vulnerability classes the
Camel PMC accepts, and which categories are out of scope (route-author or operator responsibility,
explicit opt-ins, DoS through unthrottled routes, third-party transitive CVEs not reachable
through Camel code, etc.).

camel-spring-boot is an auto-configuration layer that wraps Camel components with Spring Boot
`@ConfigurationProperties` and lifecycle integration. It does not define its own trust boundaries,
consumers, or deserialization paths. The Camel core security model applies directly.

Reports outside the documented scope will be closed with a reference to that document.
