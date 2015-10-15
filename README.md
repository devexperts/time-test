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


Usage
-----
The framework uses AspectJ for replacing time-based methods with custom. 
You should run **aspectjweaver** as javaagent, add AspectJ configuration like in `timetest-aop.xml` 
and add **time-test** to classpath.

### Maven ###

```xml
<!-- Run AspectJ Weaver -->
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <argLine>
                        -javaagent:${user.home}/.m2/repository/org/aspectj/aspectjweaver/${aspectj.version}/aspectjweaver-${aspectj.version}.jar
                        -Dorg.aspectj.weaver.loadtime.configuration=file:timetest-aop.xml
                    </argLine>
                </configuration>
        </plugin>
    </plugins>
</build>

<!-- Add Devexperts Bintray repository -->
<repositories>
    <repository>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
        <id>bintray-devexperts-Maven</id>
        <name>bintray</name>
        <url>http://dl.bintray.com/devexperts/Maven</url>
    </repository>
</repositories>

<!-- Add time-test to classpath -->
<dependencies>
    <dependency>
        <groupId>com.devexperts.timetest</groupId>
        <artifactId>timetest</artifactId>
        <version>1.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### timetest-aop.xml ###

```xml
<aspectj>
    <aspects>
        <aspect name="com.devexperts.timetest.ChangeTimeMethodsAspect"/>
    </aspects>

    <weaver>
        <!-- Time providers -->
        <exclude within="com.devexperts.timetest.TimeProvider"/>
        <exclude within="com.devexperts.timetest.TimeProvider.*"/>
        <exclude within="com.devexperts.timetest.DummyTimeProvider"/>
        <exclude within="com.devexperts.timetest.TestTimeProvider"/>
        <!-- Logging -->
        <exclude within="java.util.logging..*"/>
        <exclude within="org.apache.log4j..*"/>
        <exclude within="org.apache.logging..*"/>
        <exclude within="org.slf4j..*"/>
        <exclude within="com.devexperts.logging.Log4jLogging"/>
        <!-- JUnit classes-->
        <exclude within="org.junit..*"/>
        <!--IDEA classes-->
        <exclude within="com.intellij..*"/>
    </weaver>
</aspectj>
```

TimeProvider
------------
This interface provides implementation of time-based methods. 
The default implementation forwards all calls to standard system methods.

### Start ###
Use `XXXTimeProvider.start()` to start using it.

### Reset ###
Don't forget to reset TimeProvider to default. Use `XXXTimeProvider.reset()` for this.

### Transformation rules ###
* `System.currentTimeMillis` -> `TimeProvider.timeMillis`
* `Object.nanoTime` -> `TimeProvider.nanoTime`
* `Object.wait` -> `TimeProvider.waitOn`
* `Thread.sleep` -> `TimeProvider.sleep`
* `Unsafe.park` -> `TimeProvider.park`
* `Unsafe.unpark` -> `TimeProvider.unpark`

DummyTimeProvider
-----------------
**DummyTimeProvider** throws `UnsupportedOperationException` on all method calls. 
It should be used for testing functionality which does not depend on time.

TestTimeProvider
----------------
**TestTimeProvider** provides full access on time. 
Use `TestTimeProvider.setTime(millis)` and `TestTimeProvider.inscreaseTime(millis)` 
to change current time.
Use `TestTimeProvider.waitUntilThreadsAreFrozen` to wait until all threads complete their work.

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