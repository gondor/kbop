# KBOP 1.0.0
====

*Keyed Based Object Pooling (Thread Safe, Simple and Light)

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

Single Key to Single Object Based Pool
```java
IKeyedObjectPool.Single<String,MyObject>> pool = Pools.createPool(factory));
````

### TODO Finish this Readme
