package edu.nccu.plsm.watchservice

import java.nio.file.StandardWatchEventKinds._
import java.nio.file.WatchEvent.Kind
import java.nio.file._
import java.util.concurrent.TimeUnit

import com.sun.nio.file.ExtendedWatchEventModifier._

// from: jdk/test/java/nio/file/WatchService/FileTreeModifier.java
class FileTreeModifierSpec extends TestingBase {

  "MacOSXWatchService" should "watch subtree with modifier FILE_TREE" in withWatcher { watcher =>
    // create directories
    val subdir = Files.createDirectories(dir.resolve("a").resolve("b").resolve("c"))

    // Test ENTRY_CREATE with FILE_TREE modifier.
    val key1 = MacOSXWatchService.wrap(dir)
      .register(watcher, Array[Kind[_]](ENTRY_CREATE), FILE_TREE)
    val file = Files.createFile(subdir.resolve("foo"))
    val event1 = watcher.take.pollEvents.iterator.next
    note(f"got event: type=${event1.kind}%s, count=${event1.count}%d, context=${event1.context}%s")
    event1.kind shouldEqual ENTRY_CREATE
    event1.context shouldEqual dir.relativize(file)
    key1.reset

    // Test ENTRY_DELETE with FILE_TREE modifier.
    val key2 = MacOSXWatchService.wrap(dir)
      .register(watcher, Array[Kind[_]](ENTRY_DELETE), FILE_TREE)
    key2 should be theSameInstanceAs key1
    Files.delete(file)
    val event2 = watcher.take.pollEvents.iterator.next
    note(f"got event: type=${event2.kind}%s, count=${event2.count}%d, context=${event2.context}%s")
    event2.kind shouldEqual ENTRY_DELETE
    event2.context shouldEqual dir.relativize(file)
    key2.reset

    // Test changing registration to ENTRY_CREATE without modifier
    val key3 = MacOSXWatchService.wrap(dir)
      .register(watcher, Array[Kind[_]](ENTRY_CREATE))
    key3 should be theSameInstanceAs key1
    Files.createFile(file)
    watcher.poll(3, TimeUnit.SECONDS) shouldBe null
    val file2 = Files.createFile(dir.resolve("bar"))
    val event3 = watcher.take.pollEvents.iterator.next
    note(f"got event: type=${event3.kind}%s, count=${event3.count}%d, context=${event3.context}%s")
    event3.kind shouldEqual ENTRY_CREATE
    event3.context shouldEqual dir.relativize(file2)
    key3.reset

    // Test changing registration to <all> with FILE_TREE modifier
    val key4 = MacOSXWatchService.wrap(dir)
      .register(watcher, Array[Kind[_]](ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY), FILE_TREE)
    key4 should be theSameInstanceAs key1

    val out = Files.newOutputStream(file2)
    try {
      out.write("Double shot expresso please".getBytes("UTF-8"))
    } finally {
      if (out != null) {
        out.close()
      }
    }
    val event4 = watcher.take.pollEvents.iterator.next
    note(f"got event: type=${event4.kind}%s, count=${event4.count}%d, context=${event4.context}%s")
    event4.kind shouldEqual ENTRY_MODIFY
    event4.context shouldEqual dir.relativize(file2)
    key4.reset
  }

}
