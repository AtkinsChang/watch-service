package edu.nccu.plsm.watchservice

import java.io.IOException
import java.nio.file.WatchEvent.Kind
import java.nio.file._
import java.util.concurrent.TimeUnit

import scala.concurrent.duration._

// from: jdk/test/java/nio/file/WatchService/Basic.java
class BasicWatchServiceSpec extends TestingBase {

  def shouldProcessEvent(watcher: WatchService, watchedEvent: Kind[Path])(action: => Path): Unit = {
    note(watchedEvent.name())

    // register for event
    note(s"register $dir for ${watchedEvent.name()}")
    val key = MacOSXWatchService.wrap(dir).register(watcher, watchedEvent)
    key.isValid shouldBe true
    key.watchable should be theSameInstanceAs dir

    val path = action

    // remove key and check that we got the ENTRY_CREATE event
    note("take events...")
    watcher.take should be theSameInstanceAs key
    val event = key.pollEvents.iterator().next()
    note(f"got event: type=${event.kind}%s, count=${event.count}%d, context=${event.context}%s")
    event.kind shouldEqual watchedEvent
    event.context shouldEqual path

    note("reset key")
    key.reset() shouldBe true
  }

  "MacOSXWatchService" should "process standard events" in withWatcher { watcher =>
    val file = dir.resolve("foo")

    shouldProcessEvent(watcher, StandardWatchEventKinds.ENTRY_CREATE) {
      note(f"create $file%s")
      Files.createFile(file)
      file.getFileName
    }

    shouldProcessEvent(watcher, StandardWatchEventKinds.ENTRY_DELETE) {
      note(f"delete $file%s")
      Files.delete(file)
      file.getFileName
    }

    // create the file for the next test
    Files.createFile(file)

    shouldProcessEvent(watcher, StandardWatchEventKinds.ENTRY_MODIFY) {
      note(f"update $file%s")
      val out = Files.newOutputStream(file, StandardOpenOption.APPEND)
      try {
        out.write("I am a small file".getBytes("UTF-8"))
      } finally {
        if (out != null) {
          out.close()
        }
      }
      file.getFileName
    }

    Files.delete(file)
  }

  it should "not queue a cancelled key" in withWatcher { watcher =>
    note(s"register $dir for events")
    val key = MacOSXWatchService.wrap(dir).register(watcher, StandardWatchEventKinds.ENTRY_CREATE)
    key.isValid shouldBe true
    key.watchable should be theSameInstanceAs dir

    note("cancel key")
    key.cancel()

    // create a file in the directory
    val file = dir.resolve("mars")
    note(s"create: $file")
    Files.createFile(file)

    // poll for keys - there will be none
    note("poll...")
    val pollKey = watcher.poll(3000, TimeUnit.MILLISECONDS)
    pollKey shouldBe null

    // done
    Files.delete(file)
  }

  it should "cancel and queue the key when deleting a registered directory" in withWatcher { watcher =>
    val subdir = Files.createDirectory(dir.resolve("bar"))
    note(s"register $subdir for events")

    val key = MacOSXWatchService.wrap(subdir).register(
      watcher,
      StandardWatchEventKinds.ENTRY_CREATE,
      StandardWatchEventKinds.ENTRY_DELETE,
      StandardWatchEventKinds.ENTRY_MODIFY
    )

    note(s"delete: $subdir")
    Files.delete(subdir)

    note("take events...")
    watcher.take should be theSameInstanceAs key

    key.reset() shouldBe false
    key.isValid shouldBe false
  }

  it should "wakeup blocked threads when asynchronous close of watcher" in withWatcher { watcher =>
    // start thread to close watch service after delay
    new Thread(() => {
      try {
        Thread.sleep(5000)
        note("close WatchService...")
        watcher.close()
      } catch {
        case x: InterruptedException =>
          x.printStackTrace()
        case x: IOException =>
          x.printStackTrace()
      }
    }).start()

    assertThrows[ClosedWatchServiceException] {
      note("take...")
      watcher.take()
    }
  }

  it should "get null when polling without any register" in withWatcher { watcher =>
    note("poll...")
    watcher.poll() shouldBe null

    note("poll with timeout...")
    val start = System.nanoTime()
    watcher.poll(3000, TimeUnit.MILLISECONDS) shouldBe null
    (System.nanoTime() - start).nanos.toMillis shouldEqual 3000L +- 100
  }

  it should "throw IllegalArgumentException if register with empty event" in withWatcher { watcher =>
    assertThrows[IllegalArgumentException] {
      MacOSXWatchService.wrap(dir).register(watcher /*empty event list*/)
    }
    // OVERFLOW is ignored so this is equivalent to the empty set
    assertThrows[IllegalArgumentException] {
      MacOSXWatchService.wrap(dir).register(watcher, StandardWatchEventKinds.OVERFLOW)
    }
    // OVERFLOW is ignored even if specified multiple times
    assertThrows[IllegalArgumentException] {
      MacOSXWatchService.wrap(dir).register(watcher, StandardWatchEventKinds.OVERFLOW, StandardWatchEventKinds.OVERFLOW)
    }
  }

  it should "throw UnsupportedOperationException if custom event" in withWatcher { watcher =>
    assertThrows[UnsupportedOperationException] {
      MacOSXWatchService.wrap(dir).register(watcher, new Kind[Any] {
        override def name(): String = "custom"
        override def `type`(): Class[Any] = classOf[Any]
      })
    }
  }

  it should "throw UnsupportedOperationException if custom modifier" in withWatcher { watcher =>
    assertThrows[UnsupportedOperationException] {
      MacOSXWatchService.wrap(dir).register(watcher, Array[Kind[_]](StandardWatchEventKinds.ENTRY_CREATE), () => "custom")
    }
  }

  it should "throw NullPointerException if parameter has null" in withWatcher { watcher =>
    assertThrows[NullPointerException] {
      MacOSXWatchService.wrap(dir).register(null, StandardWatchEventKinds.ENTRY_CREATE)
    }
    assertThrows[NullPointerException] {
      MacOSXWatchService.wrap(dir).register(watcher, Array[Kind[_]](null): _*)
    }
    assertThrows[NullPointerException] {
      MacOSXWatchService.wrap(dir).register(watcher, Array[Kind[_]](StandardWatchEventKinds.ENTRY_CREATE), null)
    }
  }

  it should "throw ClosedWatchServiceException if watcher service is close" in withWatcher { watcher =>
    watcher.close()
    assertThrows[ClosedWatchServiceException] {
      watcher.poll()
    }

    // assume that poll throws exception immediately
    val start = System.nanoTime()
    assertThrows[ClosedWatchServiceException] {
      watcher.poll(10000, TimeUnit.MILLISECONDS)
    }
    (System.nanoTime() - start).nanos.toMillis should be < 5000L

    assertThrows[ClosedWatchServiceException] {
      watcher.take()
    }

    assertThrows[ClosedWatchServiceException] {
      MacOSXWatchService.wrap(dir).register(watcher, StandardWatchEventKinds.ENTRY_CREATE)
    }
  }

  it should "preserve thread interruped statu upon a call to register() " in withWatcher { watcher =>
    val curr = Thread.currentThread()
    note("interrupting current thread")
    try {
      curr.interrupt()
      MacOSXWatchService.wrap(dir).register(watcher, StandardWatchEventKinds.ENTRY_CREATE)
      curr shouldBe 'interrupted
    } finally {
      Thread.interrupted()
    }
  }

  "Two MacOSXWatchService" should "be able to registered to same directory and that events don't interfere with each other" in
    withTwoWatcher { (watcher1, watcher2) =>
      val name1 = Paths.get("gus1")
      val name2 = Paths.get("gus2")

      // create gus1
      val file1 = dir.resolve(name1)
      note(s"create $file1")
      Files.createFile(file1)
      Files.newBufferedReader(file1).close()
      // FileSystem issue? sleep a bit to avoid seeing create event above
      Thread.sleep(100)

      // register with both watch services (different events)
      note("register for different events")
      val key1 = MacOSXWatchService.wrap(dir).register(watcher1, StandardWatchEventKinds.ENTRY_CREATE)
      val key2 = MacOSXWatchService.wrap(dir).register(watcher2, StandardWatchEventKinds.ENTRY_DELETE)
      key1 shouldNot be theSameInstanceAs key2

      // create gus2
      val file2 = dir.resolve(name2)
      note(s"create $file2")
      Files.createFile(file2)

      // check that key1 got ENTRY_CREATE
      note("take events...")
      watcher1.take should be theSameInstanceAs key1
      val createEvent = key1.pollEvents.iterator().next()
      note(f"got event: type=${createEvent.kind}%s, count=${createEvent.count}%d, context=${createEvent.context}%s")
      createEvent.kind shouldEqual StandardWatchEventKinds.ENTRY_CREATE
      createEvent.context shouldEqual name2

      // check that key2 got zero events
      watcher2.poll shouldBe null

      // delete gus1
      Files.delete(file1)

      // check that key2 got ENTRY_DELETE
      note("take events...")
      watcher2.take should be theSameInstanceAs key2
      val deleteEvent = key2.pollEvents.iterator().next()
      note(f"got event: type=${deleteEvent.kind}%s, count=${deleteEvent.count}%d, context=${deleteEvent.context}%s")
      deleteEvent.kind shouldEqual StandardWatchEventKinds.ENTRY_DELETE
      deleteEvent.context shouldEqual name1

      // check that key1 got zero events
      watcher1.poll shouldBe null

      // reset for next test
      key1.reset()
      key2.reset()

      // change registration with watcher2 so that they are both
      // registered for the same event
      note("register for same event")
      MacOSXWatchService.wrap(dir).register(watcher2, StandardWatchEventKinds.ENTRY_CREATE) should be theSameInstanceAs key2

      // create file and key2 should be queued
      note(s"create $file1")
      Files.createFile(file1)

      note("take events...")
      watcher1.take should be theSameInstanceAs key1
      val createEventKey1 = key1.pollEvents.iterator().next()
      note(f"got event: type=${createEventKey1.kind}%s, count=${createEventKey1.count}%d, context=${createEventKey1.context}%s")
      createEventKey1.kind shouldEqual StandardWatchEventKinds.ENTRY_CREATE
      createEventKey1.context shouldEqual name1

      watcher2.take should be theSameInstanceAs key2
      val createEventKey2 = key2.pollEvents.iterator().next()
      note(f"got event: type=${createEventKey2.kind}%s, count=${createEventKey2.count}%d, context=${createEventKey2.context}%s")
      createEventKey2.kind shouldEqual StandardWatchEventKinds.ENTRY_CREATE
      createEventKey2.context shouldEqual name1
    }

}