/**
 * Copyright 2012,2013 - SFR (http://www.sfr.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.sfr.tv.jms.client;

/**
 * Because it had to be run over Java6...
 * 
 * @author matthieu.chaplin@sfr.com.chaplin@sfr.com
 */
public enum CliArgs {

    DESTINATION("-d"),
    CLIENTID("-c"),
    SUBSCRIPTIONNAME("-s"),
    QUEUE("-q"),
    DURABLE("-p"),
    FILTER("-f"),
    UNSUBSCRIBE("-u"),
    CONNECTION_FACTORY("-cf");
    
    private String value;

    private CliArgs(String value) {
        this.value = value;
    }

    private String getValue() {
        return this.value;
    }

    public static CliArgs fromString(String string) {
        for (CliArgs value : values()) {
            if (value.getValue().equals(string)) {
                return value;
            }
        }

        return null;
    }
}