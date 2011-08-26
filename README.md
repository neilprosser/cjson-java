#CJSON in Java

CJSON is a compressed JSON format described in Steve Hanov's [blog post](http://stevehanov.ca/blog/index.php?id=104).

The implementation is a Java port of the [Javascript version](http://stevehanov.ca/blog/cjson.js) Steve provides.

To use this library:

```java
CJSON.pack(someJSON);

CJSON.unpack(someCJSON);
```
