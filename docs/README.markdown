# AMA Message Architecture - Demo

This is a simple Spring Boot application that serves to demonstrate our
[messaging](https://albinoloverats.net/projects/messaging) framework. It
exposes two REST endpoints, one for publishing a number of events and
one for querying the events. It is partially set up to allow for
benchmarking against Kafka and Axon.

There is a Python test script: docs/test.py that will hammer the event
endpoint, creating as many events as possible in a given timeframe.
