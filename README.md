time-test
=========

[ ![Download](https://api.bintray.com/packages/devexperts/Maven/time-test/images/download.svg) ](https://bintray.com/devexperts/Maven/time-test/_latestVersion)

**time-test** -- framework for testing time-based functionality.

Sometimes you have code with similar logic:
"wait for 30 seconds and then do something if no changes are detected".

If you want to test such logic you should write something like this:

```
doWork();
Thread.sleep(30_000);
check();
```

But what if you want to test logic which should be executed once per month? Or only on specific days?

**time-test** provides easy way to test such logic. For example, the code above can be written else:

```
doWork();
TestTimeProvider.increaseTime(30_000);
check();
```

TimeProvider
------------
This interface provides implementation of time-based methods. 
The default implementation forwards all calls to standard system methods.

Use `XXXTimeProvider.start()` to start using it.
Don't forget to reset TimeProvider to default. 
Use `XXXTimeProvider.reset()` for this.

### Transformation rules
* `System.currentTimeMillis` -> `TimeProvider.timeMillis`
* `Object.nanoTime` -> `TimeProvider.nanoTime`
* `Object.wait` -> `TimeProvider.waitOn`
* `Thread.sleep` -> `TimeProvider.sleep`
* `Unsafe.park` -> `TimeProvider.park`
* `Unsafe.unpark` -> `TimeProvider.unpark`

### DummyTimeProvider
**DummyTimeProvider** throws `UnsupportedOperationException` on all method calls. 
It should be used for testing functionality which does not depend on time.

### TestTimeProvider
**TestTimeProvider** provides full access on time. 
Use `TestTimeProvider.setTime(millis)` and `TestTimeProvider.inscreaseTime(millis)` 
to change current time.
Use `TestTimeProvider.waitUntilThreadsAreFrozen` to wait until all threads complete their work.


Confguration
------------
You can pass your own configuration in `timetest.properties` properties file. 
This file should be in classpath. Also you can pass properties to command line.

Configuration has 2 properties:
* **include** - list of patterns in glob format, if any of this pattern applies 
    processed class name then this class is transformed.
* **exclude** - list of patterns in glob format, if any of this pattern not applies 
     processed class name then this class is transformed. Overrides **include** property.

### Default configuration:
```xml
exclude = java.util.logging.*,\
          org.apache.log4j.*,\
          org.apache.logging.*,\
          org.slf4j.*,\
          com.devexperts.logging.*,\
          org.junit.*,\
          org.apache.maven.*
```

Maven
-----
Timetest is implemented as java agent and you should copy it to build directory 
and configure *maven-surefire-plugin* to use it for tests.

```xml
...
<plugins>
    <!-- maven-dependency-plugin is used to copy "timetest" agent into target directory -->
    <plugin>
        <artifactId>maven-dependency-plugin</artifactId>
        <executions>
            <execution>
                <id>copy-sample-agent</id>
                <phase>process-test-classes</phase>
                <goals>
                    <goal>copy</goal>
                </goals>
                <configuration>
                    <artifactItems>
                        <artifactItem>
                            <groupId>com.devexperts.timetest</groupId>
                            <artifactId>agent</artifactId>
                            <version>${project.version}</version>
                            <outputDirectory>${project.build.directory}</outputDirectory>
                            <destFileName>timetest.jar</destFileName>
                        </artifactItem>
                    </artifactItems>
                </configuration>
            </execution>
        </executions>
    </plugin>
    <!-- Configure maven-surefire-plugin to use "timetest" agent -->
    <plugin>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
            <argLine>-javaagent:${project.build.directory}/timetest.jar</argLine>
        </configuration>
    </plugin>
</plugins>
...
```

Example
-------
```java
@After
public void tearDown() {
    TestTimeProvider.reset();
}

@Test(timeout = 100)
public void testSleepWithTestTimeProvider() {
    TestTimeProvider.start();
    Thread t = new Thread(() -> {
        // Do smth
        int sum = 0;
        for (int i = 0; i < 100_000; i++)
            sum += i;
        // Sleep
        Thread.sleep(10_000);
    });
    t.start();
    TestTimeProvider.waitUntilThreadsAreFrozen();
    TestTimeProvider.increaseTime(10_000);
    t.join();
}
```