# time-test

[ ![Download](https://api.bintray.com/packages/devexperts/Maven/time-test/images/download.svg) ](https://bintray.com/devexperts/Maven/time-test/_latestVersion)

**time-test** is a framework for testing time-based functionality.

Sometimes you have code with similar logic:
"wait for 30 seconds and then do something if no changes have been detected".

If you want to test such logic you can write something like this:

```
Thread.sleep(30_000 + 200);
check();
```

However, such tests are undesirable to be in the project. Also what if you want to test logic which should be executed once per month? Or what if this "something doing" hasn't done before the check starts?

**time-test** provides an easy way to test such logic. For example, the code above can be written more clear:

```
TestTimeProvider.increaseTime(30_000);
TestTimeProvider.waitUntilThreadsAreFrozen(200 /*ms*/);
check();
```

And this test doesn't do superfluous work.
 
**time-test** instruments byte-code and change time-based methods invocations (such as System.currentTimeMillis, Object.wait, Unsafe.park) to our own implementation.


# TimeProvider
This interface provides an implementation of time-based methods. 
The default implementation forwards all calls to standard system methods.

Use `XXXTimeProvider.start()` to start using it.
Don't forget to reset TimeProvider to default. 
Use `XXXTimeProvider.reset()` for this.

## DummyTimeProvider
**DummyTimeProvider** throws `UnsupportedOperationException` on all method calls. 
It should be used for testing functionality which does not depend on time.

## TestTimeProvider
**TestTimeProvider** provides full access on time. 
Use `TestTimeProvider.setTime(millis)` and `TestTimeProvider.inscreaseTime(millis)` 
to change current time.
Use `TestTimeProvider.waitUntilThreadsAreFrozen` to wait until all threads complete their work.


# Configuration
You can pass your own configuration in `timetest.properties` properties file or set these properties as system parameters (*-Dparam.name=value*).
The `timetest.properties` file should be in the application classpath. 

* **timetest.tests** - defines test entrance classes in glob format. Usually, test classes. Default value: *\*Test* (all classes with suffix *Test*)
* ***timetest.log.level*** defines internal logging level. Possible values: *DEBUG*, *INFO* (default value), *WARN*, *ERROR*.
* ***timetest.log.file*** defines path of file to be used for logging. By default logs are printed to the standard output.
* ***timetest.cache.dir*** [experimental] defines directory to be used for transformed classes caching. This feature is unstable, use it on your own risk.
* ***timetest.include*** defines the transformation scope using globs. For example, setting the value to ```package.to.transform.*,another.package.to.transform.*``` informs **time-test** to transform classes from these packages only. By default all classes are included.
* ***timetest.exclude*** defines the classes which should be excluded from transformation. The syntax is similar to **timetest.include** option.


# Maven
**Time-test** is implemented as java agent and you should copy it to build directory 
and configure *maven-surefire-plugin* to use it for tests.

```xml
...
<plugins>
    <!-- maven-dependency-plugin is used to copy "timetest" agent into target directory -->
    <plugin>
        <artifactId>maven-dependency-plugin</artifactId>
        <executions>
            <execution>
                <id>copy-timetest-agent</id>
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

# Usage example
```java
@Before
public void setUp() {
    TestTimeProvider.start();
}

@After
public void tearDown() {
    TestTimeProvider.reset();
}

@Test(timeout = 100)
public void testSleepWithTestTimeProvider() {
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


# Contacts
If you need help, you have a question, or you need further details on how to use **time-test**, you can refer to the following resources:

* [dxLab](https://code.devexperts.com/) research group at Devexperts
* [GitHub issues](https://github.com/Devexperts/lin-check/issues)

You can use the following e-mail to contact us directly:

![](dxlab-mail.png)