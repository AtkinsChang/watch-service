package edu.nccu.plsm.watchservice

import org.scalatest.concurrent.{Signaler, ThreadSignaler, TimeLimitedTests}
import org.scalatest.time.Span
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.language.postfixOps

abstract class TestingBase extends FlatSpec
  with Matchers
  with BeforeAndAfterAll
  with TimeLimitedTests
  with TemporaryDirectoryProvider
  with WatchServiceFixture {
  val timeLimit: Span = 10 seconds
  override val defaultTestSignaler: Signaler = ThreadSignaler

}
