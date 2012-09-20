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

package griffon.plugins.berkeleydb;

import java.util.Map;
import groovy.lang.Closure;
import griffon.util.CallableWithArgs;

/**
 * @author Andres Almiray
 */
public interface BerkeleydbProvider {
    Object withBerkeleyEnv(Closure closure);

    <T> T withBerkeleyEnv(CallableWithArgs<T> callable);

    Object withEntityStore(Closure closure);

    Object withEntityStore(Map params, Closure closure);

    <T> T withEntityStore(CallableWithArgs<T> callable);

    <T> T withEntityStore(Map params, CallableWithArgs<T> callable);

    Object withBerkeleyDb(Closure closure);

    Object withBerkeleyDb(Map params, Closure closure);

    <T> T withBerkeleyDb(CallableWithArgs<T> callable);

    <T> T withBerkeleyDb(Map params, CallableWithArgs<T> callable);

    Object withBerkeleyCursor(Closure closure);

    Object withBerkeleyCursor(Map params, Closure closure);

    <T> T withBerkeleyCursor(CallableWithArgs<T> callable);

    <T> T withBerkeleyCursor(Map params, CallableWithArgs<T> callable);
}
