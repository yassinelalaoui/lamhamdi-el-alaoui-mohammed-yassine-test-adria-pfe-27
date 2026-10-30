# ADRIA Banking Platform

Foundation for the ADIRIA PFE microservices platform. The repository currently contains the Maven reactor and service dependency declarations only; business features and executable Spring Boot applications have not been implemented.

## Requirements

- JDK 21
- Apache Maven 3.9+

## Build and verify

From this directory, list the reactor modules with:

```shell
mvn validate
```

Build and install every module with:

```shell
mvn clean install -DskipTests
```

The build requires access to Maven Central on its first run. Tests are skipped by the requested verification command; the parent configures Spring Boot's test starter for service modules.

## Structure

See [docs/structure.md](docs/structure.md) for the module inventory and directory conventions.