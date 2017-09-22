# FileSystem Watch Service
Native `WatchService` implementation for Mac OS X.

 `WatchEvent.Modifier` supported:
* com.sun.nio.file.ExtendedWatchEventModifier
* NoDeferWatchEventModifier
* LatencyWatchEventModifier

## Example
```scala
object Example {
  def main(args: Array[String]): Unit = {
    val watcher: java.nio.file.WatchService = MacOSXWatchService.newInstance
    val path = Paths.get("foo")
    // by pass path's filesystem check
    val watchable = MacOSXWatchService.wrap(path)
    
    val events: Array[WatchEvent.Kind[_]] =
      Array(StandardWatchEventKinds.ENTRY_CREATE)
    
    val key = watchable.register(watcher, events, 
      new LatencyWatchEventModifier(0.5D),
      NoDeferWatchEventModifier.INSTANCE,
      ExtendedWatchEventModifier.FILE_TREE
    )
    
    watcher.take()
    // ...
  }
}
```