# Filing Resource Handler Java

## Overview

This service handles updating the transaction resource with a list of filings after the transaction has been closed and starts the processing of the transaction.

The diagram below shows how the filing-resource-handler-java is used in the journey.
![image info](./docs/design/filing-resource-handler-java.png)

## Requirements

In order to run the service locally you will need the following:

- [Java 21](https://www.oracle.com/java/technologies/downloads/#java21)
- [Maven](https://maven.apache.org/download.cgi)
- [Git](https://git-scm.com/downloads)

## Getting started

To checkout and build the service:

1. Clone [Docker CHS Development](https://github.com/companieshouse/docker-chs-development) and follow the steps in the
   README.

These instructions are for a local docker environment.

2. Edit `services/modules/filing-resource-handler/filing-resource-handler-java.docker-compose.yaml`
   a. Add the value for `CHS_INTERNAL_API_KEY` which can be found in MongoDB
2. Run `./bin/chs-dev services enable filing-resource-handler-java`
3. Run `./bin/chs-dev development enable filing-resource-handler-java` if you wish to see changes in the code

## Design

[design](docs/design/DESIGN.md)

## Testing

[Testing](./docs/testing/readme.md)

