# Key Based Object Pool 1.0.0
====

*Key Based Object Pooling (Thread Safe, Simple and Light) aka KBOP

KBOP is a lightweight Keyed Object Pool which supports Single Key to Single Object Pooling or Single Key to Multiple Object Pooling.  KBOP provides factory lifecycles, metrics, borrowing, releasing and object invalidation.

### Setup

Maven Dependency Setup

```xml
<dependency>
	<groupId>org.pacesys</groupId>
	<artifactId>kbop</artifactId>
	<version>1.0.0</version>
</dependency>
```

## Usage

Creating : Single Key to Single Object Based Pool
```java
IKeyedObjectPool.Single<String,MyObject>> pool = Pools.createPool(factory));
````

Creating : Single Key to Multiple Object Based Pool
```java
IKeyedObjectPool.Multi<MyKey, MyObject> = Pools.createMultiPool(factory, maxItemsPerKey)
````

Borrowing Objects from the Pool - Block until available
```java
// borrow an object and block until available
IPooledObject<MyObject> obj = pool.borrow(key);
````

Borrowing Objects from the Pool - Block until max time has elapsed
```java
IPooledObject<MyObject> obj = pool.borrow(key, 1, TimeUnit.SECONDS);
````

Releasing Objects back to the Pool
```java
IPooledObject<MyObject> obj = pool.borrow(key);

// Release via borrowed Object
obj.release();

// Release via Pool
pool.release(obj);
````

Invalidating a borrowed object - The pool will release the object and destroy it.  The next time
an object is requested for the same key the pool will re-create a new instance
```java
IPooledObject<MyObject> obj = pool.borrow(key);
pool.invalidate(obj);
````

Shutting down a Pool
```java
pool.shutdown();
````

Implementing a Factory to Create Objects when needed to populate a Pool
```java
IPoolObjectFactory<String, MyObject> factory = new IPoolObjectFactory<String, MyObject>() 
{
  /**
   * Creates the defined Object V based on the current Pool Key.
   *
   * @param key the key being requested
   * @return the Object to be inserted into the Pool
   */
  public String create(PoolKey<String> key) {
     // Create the Object based on the Key
     return new MyObject();
  }

  /**
   * Reinitialize an instance to be returned to the borrower
   *
   * @param object the object being borrowed
   */
  @Override
  public void activate(MyObject object) {
    // called right
  }

  /**
   * Uninitialize an instance which has been released back to the pool
   *
   * @param object the object to which has just beel released
   */
  @Override
  public void passivate(String object) {
  }

  /**
   * Destroy/Release an instance no longer needed by the pool
   *
   * @param object the object to destroy
   */
  @Override
  public void destroy(String object) {
  }
};
````

License:
KBOP is hereby released under the MIT License.

Copyright (c) 2013 Jeremy Unruh

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
