/*
 * Copyright 2010-2012 the original author or authors.
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

import griffon.core.GriffonApplication
import griffon.util.Metadata
import griffon.util.CallableWithArgs
import griffon.util.Environment as GE
import com.sleepycat.je.*
import com.sleepycat.persist.*

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Andres Almiray
 */
@Singleton
final class BerkeleydbConnector implements BerkeleydbProvider {
    private static final Logger LOG = LoggerFactory.getLogger(BerkeleydbConnector)
    private final Object[] lock = new Object[0]
    private boolean connected = false
    private bootstrap
    private GriffonApplication app
    private final ConfigSlurper configSlurper = new ConfigSlurper(GE.current.name)

    final EnvironmentConfig envConfig = new EnvironmentConfig()

    Object withBerkeleyEnv(Closure closure) {
        EnvironmentHolder.instance.withBerkeleyEnv(closure)
    }
    
    public <T> T withBerkeleyEnv(CallableWithArgs<T> callable) {
        return EnvironmentHolder.instance.withBerkeleyEnv(callable)
    }

    Object withEntityStore(Map params = [:], Closure closure) {
        return EnvironmentHolder.instance.withEntityStore(callable)
    }

    public <T> T withEntityStore(Map params = [:], CallableWithArgs<T> callable) {
        return EnvironmentHolder.instance.withEntityStore(callable)
    }
    
    Object withBerkeleyDb(Map params = [:], Closure closure) {
        return EnvironmentHolder.instance.withBerkeleyDb(callable)
    }

    public <T> T withBerkeleyDb(Map params = [:], CallableWithArgs<T> callable) {
        return EnvironmentHolder.instance.withBerkeleyDb(callable)
    }

    Object withBerkeleyCursor(Map params = [:], Closure closure) {
        return EnvironmentHolder.instance.withBerkeleyCursor(callable)
    }

    public <T> T withBerkeleyCursor(Map params = [:], CallableWithArgs<T> callable) {
        return EnvironmentHolder.instance.withBerkeleyCursor(callable)
    }

    // ======================================================

    ConfigObject createConfig(GriffonApplication app) {
        def configClass = app.class.classLoader.loadClass('BerkeleydbConfig')
        return configSlurper.parse(configClass)
    }

    void connect(GriffonApplication app, ConfigObject config) {
        synchronized(lock) {
            if(connected) return
            connected = true
        }

        this.app = app
        app.event('BerkeleydbConnectStart', [config])
        BerkeleydbConnector.instance.startEnvironment(config)
        bootstrap = app.class.classLoader.loadClass('BootstrapBerkeleydb').newInstance()
        bootstrap.metaClass.app = app
        bootstrap.init(EnvironmentHolder.instance.environment)
        app.event('BerkeleydbConnectEnd', [config, EnvironmentHolder.instance.environment])
    }

    void disconnect(GriffonApplication app, ConfigObject config) {
        synchronized(lock) {
            if(!connected) return
            connected = false
        }

        app.event('BerkeleydbDisconnectStart', [config, EnvironmentHolder.instance.environment])
        bootstrap.destroy(EnvironmentHolder.instance.environment)
        stopEnvironment(config)
        app.event('BerkeleydbDisconnectEnd')
    }

    private void startEnvironment(config) {
        String envhome = config.environment?.home ?: 'bdb_home'
        File envhomeDir = new File(envhome)
        if(!envhomeDir.absolute) envhomeDir = new File(Metadata.current.getGriffonWorkingDir(), envhome)
        envhomeDir.mkdirs()
        config.environment.each { key, value ->
            if(key == 'home') return

            // 1 - attempt direct property setter
            try {
                envConfig[key] = value
                return
            } catch(MissingPropertyException mpe) {
                // ignore
            }

            // 2 - attempt field resolution
            try {
                String rkey = EnvironmentConfig.@"${key.toUpperCase().replaceAll(' ','_')}"
                envConfig.setConfigParam(rkey, value?.toString())
                return
            } catch(Exception mpe) {
                // ignore
            }

            // 3 - assume key is a valid config param
            envConfig.setConfigParam(key, value?.toString())
        }
        BerkeleydbEnhancer.enhance(Environment.metaClass)
        EnvironmentHolder.instance.environment = new Environment(envhomeDir, envConfig)
    }

    private void stopEnvironment(config) {
        EnvironmentHolder.instance.stopAll()
    }

    Map createStoreBucket(String storeId) {
        def config = createConfig(app)
        StoreConfig storeConfig = new StoreConfig()
        TransactionConfig txnConfig = new TransactionConfig()

        config.entityStores?.get(storeId)?.each { skey, svalue ->
            if(skey == 'transactionConfig') {
                svalue.each { tkey, tvalue ->
                    txnConfig[tkey] = tvalue
                }
            } else {
                storeConfig[skey] = svalue
            }
        }

        [store: new EntityStore(EnvironmentHolder.instance.environment, storeId, storeConfig),
         txnConfig: txnConfig]
    }

    Map createDatabaseBucket(String dbId) {
        def config = createConfig(app)
        DatabaseConfig dbConfig = new DatabaseConfig()
        TransactionConfig txnConfig = new TransactionConfig()
        CursorConfig cursorConfig = new CursorConfig()

        config.databases?.get(dbId)?.each { skey, svalue ->
            if(skey == 'transactionConfig') {
                svalue.each { tkey, tvalue ->
                    txnConfig[tkey] = tvalue
                }
            } else if(skey == 'cursorConfig') {
                svalue.each { ckey, cvalue ->
                    cursorConfig[ckey] = cvalue
                }
            } else {
                dbConfig[skey] = svalue
            }
        }

        [db: new Database(EnvironmentHolder.instance.environment, dbId, dbConfig),
         txnConfig: txnConfig, cursorConfig: cursorConfig]
    }
}
