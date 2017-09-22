package edu.nccu.plsm.watchservice

import java.nio.file.StandardWatchEventKinds._
import java.nio.file.WatchEvent.Kind
import java.nio.file._
import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import com.sun.nio.file.SensitivityWatchEventModifier
import org.scalatest.time.Span

import scala.language.postfixOps
import scala.util.Random

// from: jdk/test/java/nio/file/WatchService/SensitivityModifier.java
class LatencyModifierSpec extends TestingBase {

  override val timeLimit: Span = 240 seconds

    "MacOSXWatchService" should "pass jdk's sanity test for JDK-specific sensitivity level watch event modifier" in withWatcher { watcher =>
    // create directories and files
    val nDirs = 5 + Random.nextInt(20);
    val nFiles = 50 + Random.nextInt(50);
    val dirs = Array.tabulate(nDirs) { i =>
      Files.createDirectory(dir.resolve("dir" + i))
    }
    val files = Array.tabulate(nFiles) { i =>
      val subDir = dirs(Random.nextInt(nDirs))
      Files.createFile(subDir.resolve("file" + i))
    }

    val sensitivtives = SensitivityWatchEventModifier.values()

    dirs.foreach { subDir =>
      val sensivity = sensitivtives(Random.nextInt(sensitivtives.length))
      MacOSXWatchService.wrap(subDir).register(watcher, Array[Kind[_]](ENTRY_MODIFY), sensivity)
    }
    Thread.sleep(1000)

    for (_ <- 0 to 9) {
      val file = files(Random.nextInt(nFiles))
      note("Modify: " + file)
      val out = Files.newOutputStream(file)
      try {
        out.write(new Array[Byte](100))
      } finally {
        if (out != null) {
          out.close()
        }
      }
      note("Waiting for event(s)...")
      var eventReceived = false
      var break = false
      var key = watcher.take()
      do {
        val it = key.pollEvents().iterator()
        while(it.hasNext && !break) {
          val event = it.next
          event.kind shouldBe ENTRY_MODIFY
          if (event.context == file.getFileName) {
            eventReceived = true
            break = true
          }
        }
        key.reset()
        key = watcher.poll(1, TimeUnit.SECONDS)
      } while (key != null)

      eventReceived shouldBe true

      // re-register the directories to force changing their sensitivity level
      dirs.foreach { subDir =>
        val sensivity = sensitivtives(Random.nextInt(sensitivtives.length))
        MacOSXWatchService.wrap(subDir).register(watcher, Array[Kind[_]](ENTRY_MODIFY), sensivity)
      }
    }
  }
}
