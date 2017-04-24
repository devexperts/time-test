package com.devexperts.timetest;

/*
 * #%L
 * transformer
 * %%
 * Copyright (C) 2015 - 2016 Devexperts, LLC
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.aeonbits.owner.Config;


@Config.Sources("classpath:timetest.properties")
public interface Configuration extends Config {

    @Key("timetest.include")
    @DefaultValue("*")
    String[] include();

    @Key("timetest.exclude")
    @DefaultValue("")
    String[] exclude();

    @Key("timetest.tests")
    @DefaultValue("com.devexperts.*.test.*")
    String[] testClasses();

    @Key("timetest.log.level")
    @DefaultValue("INFO")
    String logLevel();

    @Key("timetest.log.file")
    String logFile();

    @Key("timetest.redefinition.verbose")
    @DefaultValue("false")
    boolean verboseRedifinition();

    @Key("timetest.redefinition.enabled")
    @DefaultValue("false")
    boolean redefine();

    @Key("timetest.cache.dir")
    String cacheDir();

    @Key("timetest.dump.dir")
    String dumpDir();
}
