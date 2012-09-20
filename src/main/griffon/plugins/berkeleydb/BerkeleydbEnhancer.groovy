/*
 * Copyright 2012 the original author or authors.
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

package griffon.plugins.berkeleydb

import griffon.util.CallableWithArgs
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Andres Almiray
 */
final class BerkeleydbEnhancer {
    private static final Logger LOG = LoggerFactory.getLogger(BerkeleydbEnhancer)

    private BerkeleydbEnhancer() {}

    static void enhance(MetaClass mc, BerkeleydbProvider provider = EnvironmentHolder.instance) {
        if(LOG.debugEnabled) LOG.debug("Enhancing $mc with $provider")
        mc.withBerkeleyEnv = {Closure closure ->
            provider.withBerkeleyEnv(closure)
        }
        mc.withBerkeleyEnv << {CallableWithArgs callable ->
            provider.withBerkeleyEnv(callable)
        }
        mc.withEntityStore = {Map params = [:], Closure closure ->
            provider.withEntityStore(params, closure)
        }
        mc.withEntityStore << {Map params = [:], CallableWithArgs callable ->
            provider.withEntityStore(params, callable)
        }  
        mc.withBerkeleyDb = {Map params = [:], Closure closure ->
            provider.withBerkeleyDb(params, closure)
        }
        mc.withBerkeleyDb << {Map params = [:], CallableWithArgs callable ->
            provider.withBerkeleyDb(params, callable)
        }
        mc.withBerkeleyCursor = {Map params = [:], Closure closure ->
            provider.withBerkeleyCursor(params, closure)
        }
        mc.withBerkeleyCursor << {Map params = [:], CallableWithArgs callable ->
            provider.withBerkeleyCursor(params, callable)
        }
    }
}
