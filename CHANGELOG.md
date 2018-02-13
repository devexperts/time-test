Changelog
=========

1.3 - 2018-02-13
----------------
* Change a strategy to define if we are executing in the testing code or not. A new strategy uses entry points (see 'timetest.testingCode' property) to understand that we are executing in the testing code and traces Thread.start() invocations to mark new threads as 'in testing code' too.
* Use Dl-Check in testing phase
* Move default time provider to separate class for better logging
* ove dependency JARs to time-test's folder in order not to conflict with other agents

1.2 - 2017-04-24
----------------
* Fix deadlock in TestTimeProvider
* Support "unpark before park" executions

1.1 - 2016-10-27
----------------
* Support class caching
* Fix bug with frames processing in ASM
* Change license to LGPLv3
* shade dxlib into agent

1.0 - 2016-06-27
----------------
* Migrate to JAgent with OWNER configuration.

0.9 - 2015-10-13
----------------
* Initial public version
