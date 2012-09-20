/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Andres Almiray
 */
class BerkeleydbGriffonPlugin {
    // the plugin version
    String version = '0.7'
    // the version or versions of Griffon the plugin is designed for
    String griffonVersion = '1.1.0 > *'
    // the other plugins this plugin depends on
    Map dependsOn = [:]
    // resources that are included in plugin packaging
    List pluginIncludes = []
    // the plugin license
    String license = 'Apache Software License 2.0'
    // Toolkit compatibility. No value means compatible with all
    // Valid values are: swing, javafx, swt, pivot, gtk
    List toolkits = []
    // Platform compatibility. No value means compatible with all
    // Valid values are:
    // linux, linux64, windows, windows64, macosx, macosx64, solaris
    List platforms = []
    // URL where documentation can be found
    String documentation = ''
    // URL where source can be found
    String source = 'https://github.com/griffon/griffon-berkeleydb-plugin'

    List authors = [
        [
            name: 'Andres Almiray',
            email: 'aalmiray@yahoo.com'
        ]
    ]
    String title = 'Berkeleydb support'
    String description = '''
The Berkeleydb plugin enables lightweight access to [Berkeleydb][1] datastores.
This plugin does NOT provide domain classes nor dynamic finders like GORM does.

Usage
-----
Upon installation the plugin will generate the following artifacts in `$appdir/griffon-app/conf`:

 * BerkeleydbConfig.groovy - contains the datastore definitions.
 * BootstrapBerkeleydb.groovy - defines init/destroy hooks for data to be manipulated during app startup/shutdown.

Remember to make all datastore calls off the EDT
otherwise your application may appear unresponsive when doing long computations
inside the EDT.

The following dynamic methods will be added

 * **withBerkeleyEnv { Environment env -> ... }** - direct access to the current Berkeley DB environment.
 * **withBerkeleyDb(Map args) { Database db, Transaction txn -> ... }** - access to a named Database object.
 * **withBerkeleyCursor(Map args) { Cursor cursor, Transaction txn -> ... }** - access to a named Cursor object.
 * **withEntityStore(Map args) { EntityStore store, Transaction txn -> ... }** - access to a named EntityStore object.

The last 3 methods accept an optional Map argument that may contain the following keys:

 * **id** - name of a database or entityStore. Will pull configuration from `BerlekeydbConfig` if the id matches a configured database or entityStore.
 * **txnConfig** - a `TransactionConfig` instance that overrides any transaction settings available BerlekeydbConfig for a matching id.
 * **cursorConfig** - a `CursorConfig` instance that overrides any cursor settings available BerlekeydbConfig for a matching id.

These methods are also accessible to any component through the singleton `griffon.plugins.berkeleydb.BerkeleydbConnector`.
You can inject these methods to non-artifacts via metaclasses. Simply grab hold of a particular metaclass and call
`BerkeleydbEnhancer.enhance(metaClassInstance, berkeleydbProviderInstance)`.

Configuration
-------------
### Dynamic method injection

Dynamic methods will be added to controllers by default. You can
change this setting by adding a configuration flag in `griffon-app/conf/Config.groovy`

    griffon.berkeleydb.injectInto = ['controller', 'service']

### Events

The following events will be triggered by this addon

 * BerkeleydbConnectStart[config] - triggered before connecting to the datastore
 * BerkeleydbConnectEnd[config, environment] - triggered after connecting to the datastore
 * BerkeleydbDisconnectStart[config, environment] - triggered before disconnecting from the datastore
 * BerkeleydbDisconnectEnd[config] - triggered after disconnecting from the datastore

### Multiple Stores

The config file `BerkeleydbConfig.groovy` defines an entityStores block that can be used to configure multiple EntityStores
available to the application. For example connecting to an EntityStore whose name is 'internal'
can be done in this way

    entityStores {
        internal {
            transactional = true
            allowCreate = true
        }
    }

This block can be used inside the `environments()` block in the same way as the other blocks found in this config file.

### Multiple Databases

The config file `BerkeleydbConfig.groovy` defines an databases block that can be used to configure multiple Database
available to the application. For example connecting to a Database whose name is 'internal'
can be done in this way

    databases {
        internal {
            transactional = true
            allowCreate = true
        }
    }

This block can be used inside the `environments()` block in the same way as the other blocks found in this config file.

### Example

A trivial sample application can be found at [https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/berkeleydb][2]

Testing
-------
Dynamic methods will not be automatically injected during unit testing, because addons are simply not initialized
for this kind of tests. However you can use `BerkeleydbEnhancer.enhance(metaClassInstance, berkeleydbProviderInstance)` where 
`berkeleydbProviderInstance` is of type `griffon.plugins.berkeleydb.BerkeleydbProvider`. The contract for this interface looks like this

    public interface BerkeleydbProvider {
        Object withBerkeleyEnv(Closure closure);
        Object withBerkeleyEnv(String serverName, Closure closure);
        <T> T withBerkeleyEnv(CallableWithArgs<T> callable);
        <T> T withBerkeleyEnv(String serverName, CallableWithArgs<T> callable);
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

It's up to you define how these methods need to be implemented for your tests. For example, here's an implementation that never
fails regardless of the arguments it receives

    class MyBerkeleydbProvider implements BerkeleydbProvider {
        Object withBerkeleyEnv(Closure closure) { null }
        public <T> T withBerkeleyEnv(CallableWithArgs<T> callable) { null }  
        Object withEntityStore(Map params = [:], Closure closure) { null }
        public <T> T withEntityStore(Map params = [:], CallableWithArgs<T> callable) { null }
        Object withBerkeleydb(Map params = [:], Closure closure) { null }
        public <T> T withBerkeleydb(Map params = [:], CallableWithArgs<T> callable) { null }
        Object withBerkeleyCursor(Map params = [:], Closure closure) { null }
        public <T> T withBerkeleyCursor(Map params = [:], CallableWithArgs<T> callable) { null }
    }

This implementation may be used in the following way

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            BerkeleydbEnhancer.enhance(service.metaClass, new MyBerkeleydbProvider())
            // exercise service methods
        }
    }


[1]: http://www.oracle.com/technology/software/products/berkeley-db/index.html
[2]: https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/berkeleydb
'''
}
