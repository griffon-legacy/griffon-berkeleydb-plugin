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

import com.sleepycat.je.*
import com.sleepycat.persist.*
import griffon.util.CallableWithArgs
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Andres Almiray
 */
@Singleton
class EnvironmentHolder implements BerkeleydbProvider {
    private final Map ENTITY_STORES = new ConcurrentHashMap()
    private final Map DATABASES = new ConcurrentHashMap()
    Environment environment

    Object withBerkeleyEnv(Closure closure) {
        return closure(environment)
    }

    public <T> T withBerkeleyEnv(CallableWithArgs<T> callable) {
        callable.args = [environment] as Object[]
        return callable.run()
    }

    Object withEntityStore(Map params = [:], Closure closure) {
        Map storeBucket = fetchStoreBucket(params)

        if(storeBucket.store.config.transactional) {
            TransactionConfig txnConfig = params.txnConfig ?: storeBucket.txnConfig
            Transaction txn = environment.beginTransaction(null, txnConfig)
            try {
                Object result = closure(storeBucket.store, txn)
                txn.commit()
                return result
            } catch(Exception ex) {
                txn.abort()
                throw ex
            }
        } else {
            return closure(storeBucket.store, null)
        }
    }

    public <T> T withEntityStore(Map params = [:], CallableWithArgs<T> callable) {
        Map storeBucket = fetchStoreBucket(params)

        if(storeBucket.store.config.transactional) {
            TransactionConfig txnConfig = params.txnConfig ?: storeBucket.txnConfig
            Transaction txn = environment.beginTransaction(null, txnConfig)
            try {
                callable.args = [storeBucket.store, txn] as Object[]
                T result = callable.run()
                txn.commit()
                return result
            } catch(Exception ex) {
                txn.abort()
                throw ex
            }
        } else {
            callable.args = [storeBucket.store, null] as Object[]
            return callable.run()
        }
    }
    
    private Map fetchStoreBucket(Map params) {
        if(!params.id) {
            throw new IllegalArgumentException('You must specify a value for id: when fetching a store bucket.')
        }

        Map storeBucket = ENTITY_STORES[params.id]
        if(!storeBucket) {
            storeBucket = BerkeleydbConnector.instance.createStoreBucket(params.id)
            ENTITY_STORES[params.id] = storeBucket
        }

        storeBucket
    }

    Object withBerkeleyDb(Map params = [:], Closure closure) {
        Map storeBucket = fetchDatabaseBucket(params)

        if(dbBucket.db.config.transactional) {
            TransactionConfig txnConfig = params.txnConfig ?: dbBucket.txnConfig
            Transaction txn = environment.beginTransaction(null, txnConfig)
            try {
                Object result = closure(dbBucket.db, txn)
                txn.commit()
                return result
            } catch(Exception ex) {
                txn.abort()
                throw ex
            }
        } else {
            return closure(dbBucket.db, null)
        }
    }

    public <T> T withBerkeleyDb(Map params = [:], CallableWithArgs<T> callable) {
        Map storeBucket = fetchDatabaseBucket(params)

        if(dbBucket.db.config.transactional) {
            TransactionConfig txnConfig = params.txnConfig ?: dbBucket.txnConfig
            Transaction txn = environment.beginTransaction(null, txnConfig)
            try {
                callable.args = [dbBucket.db, txn] as Object[]
                T result = callable.run()
                txn.commit()
                return result
            } catch(Exception ex) {
                txn.abort()
                throw ex
            }
        } else {
            callable.args = [dbBucket.db, null] as Object[]
            return callable.run()
        }
    }
    
    private Map fetchDatabaseBucket(Map params) {
        if(!params.id) {
            throw new IllegalArgumentException('You must specify a value for id: when fetching a database bucket.')
        }

        Map dbBucket = DATABASES[params.id]
        if(!dbBucket) {
            dbBucket = BerkeleydbConnector.instance.createDatabaseBucket(params.id)
            DATABASES[params.id] = dbBucket
        }

        dbBucket
    }

    Object withBerkeleyCursor(Map params = [:], Closure closure) {
        Map storeBucket = fetchDatabaseBucket(params)

        if(dbBucket.db.config.transactional) {
            TransactionConfig txnConfig = params.txnConfig ?: dbBucket.txnConfig
            CursorConfig cursorConfig = params.cursorConfig ?: dbBucket.cursorConfig
            Transaction txn = environment.beginTransaction(null, txnConfig)
            try {
                Object result = closure(dbBucket.db.openCursor(txn, cursorConfig), txn)
                txn.commit()
                return result
            } catch(Exception ex) {
                txn.abort()
                throw ex
            }
        } else {
            return closure(dbBucket.db.openCursor(null, cursorConfig), null)
        }
    }

    public <T> T withBerkeleyCursor(Map params = [:], CallableWithArgs<T> callable) {
        Map storeBucket = fetchDatabaseBucket(params)

        if(dbBucket.db.config.transactional) {
            TransactionConfig txnConfig = params.txnConfig ?: dbBucket.txnConfig
            CursorConfig cursorConfig = params.cursorConfig ?: dbBucket.cursorConfig
            Transaction txn = environment.beginTransaction(null, txnConfig)
            try {
                callable.args = [dbBucket.db.openCursor(txn, cursorConfig), txn] as Object[]
                T result = callable.run()
                txn.commit()
                return result
            } catch(Exception ex) {
                txn.abort()
                throw ex
            }
        } else {
            callable.args = [dbBucket.db.openCursor(null, cursorConfig), null] as Object[]
            return callable.run()
        }
    }
    
    void stopAll() {
        ENTITY_STORES.each { id, storeBucket -> storeBucket.store.close() }
        DATABASES.each { id, dbBucket ->
            dbBucket.db.with {
                sync()
                close()
            }
        }
        EnvironmentHolder.instance.environment.with {
            cleanLog()
            close()
        }
    }
}
