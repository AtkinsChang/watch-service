package edu.nccu.plsm.watchservice

import java.nio.file.WatchService

trait WatchServiceFixture {

  def withWatcher[T](testCode: (WatchService) => T) {
    val watcher = MacOSXWatchService.newInstance()
    try {
      testCode(watcher)
    } finally {
      watcher.close()
    }
  }

  def withTwoWatcher[T](testCode: (WatchService, WatchService) => T) {
    val watcher1 = MacOSXWatchService.newInstance()
    val watcher2 = MacOSXWatchService.newInstance()
    try {
      testCode(watcher1, watcher2)
    } finally {
      var throwed: Throwable = null
      try {
        watcher1.close()
      } catch {
        case t: Throwable => throwed = t
      }
      if (throwed ne null) {
        try {
          watcher2.close()
        } catch {
          case t: Throwable =>
            throwed.addSuppressed(t)
        }
        throw throwed
      } else {
        watcher2.close()
      }
    }
  }

}
