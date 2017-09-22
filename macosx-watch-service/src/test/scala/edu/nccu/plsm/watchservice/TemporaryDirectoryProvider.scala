package edu.nccu.plsm.watchservice

import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, FileVisitor, Files, Path}

import org.scalatest.{Alerting, BeforeAndAfterAll}

trait TemporaryDirectoryProvider { this: BeforeAndAfterAll with Alerting =>

  protected var dir: Path = _

  override protected def beforeAll(): Unit = {
    dir = Files.createTempDirectory(getClass.getSimpleName)
    //
    Thread.sleep(100)
  }

  override protected def afterAll(): Unit = {
    Files.walkFileTree(dir, new FileVisitor[Path] {
      override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
        FileVisitResult.CONTINUE
      }
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        try {
          Files.delete(file)
        } catch {
          case x: IOException =>
            alert(f"Unable to delete $file%s: $x%s")
        }
        FileVisitResult.CONTINUE
      }
      override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
        try {
          Files.delete(dir)
        } catch {
          case x: IOException =>
            alert(f"Unable to delete $dir%s: $x%s")
        }
        FileVisitResult.CONTINUE
      }
      override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = {
        alert(f"Unable to visit $file%s: $exc%s")
        FileVisitResult.CONTINUE
      }
    })
  }

}
