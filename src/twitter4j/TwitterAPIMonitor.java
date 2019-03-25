/*
 * Copyright 2007 Yusuke Yamamoto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package twitter4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Singleton instance of all Twitter API monitoring. Handles URL parsing and "wire off" logic.
 * We could avoid using a singleton here if Twitter objects were instantiated
 * from a factory.
 *
 * @author Nick Dellamaggiore (nick.dellamaggiore at gmail.com)
 * @since Twitter4J 2.2.1
 */
public class TwitterAPIMonitor {
    private static final Logger logger = Logger.getLogger(TwitterAPIMonitor.class);
    // https?:\/\/[^\/]+\/[0-9.]*\/([a-zA-Z_\.]*).*
    // finds the "method" part a Twitter REST API url, ignoring member-specific resource names
    private static final Pattern pattern =
	Pattern.compile("https?://[^/]+/[0-9.]*/([a-zA-Z_.]*).*");

    private static final TwitterAPIMonitor SINGLETON = new TwitterAPIMonitor();

	/**
     * Constructor
     */
    private TwitterAPIMonitor() {
    }

    public static TwitterAPIMonitor getInstance() {
        return SINGLETON;
    }


}
