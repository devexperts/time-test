package com.devexperts.timetest;

/*
 * #%L
 * test
 * %%
 * Copyright (C) 2015 - 2017 Devexperts, LLC
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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class RepeatRule implements TestRule {

    private static class RepeatStatement extends Statement {

        private final int times;
        private final Statement statement;

        private RepeatStatement( int times, Statement statement ) {
            this.times = times;
            this.statement = statement;
        }

        @Override
        public void evaluate() throws Throwable {
            for( int i = 0; i < times; i++ ) {
                statement.evaluate();
            }
        }
    }

    @Override
    public Statement apply( Statement statement, Description description ) {
        Statement result = statement;
        Repeat repeat = description.getAnnotation( Repeat.class );
        if( repeat != null ) {
            int times = repeat.value();
            result = new RepeatStatement( times, statement );
        }
        return result;
    }
}
