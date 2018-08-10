package mrks.slick

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import slick.basic.DatabaseConfig
import slick.jdbc.H2Profile

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits._

class ExtensionsSpec extends WordSpec
  with MustMatchers
  with ScalaFutures
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with Extensions[H2Profile] {

  import profile.api._

  val dbConfig = DatabaseConfig.forConfig[H2Profile]("slick")

  case class TestData(key: String, value: Int)

  class TestDataTable(tag: Tag) extends Table[TestData](tag: Tag, "test_data") {
    def key   = column[String]("key", O.Unique)
    def value = column[Int]("value")

    def * = (key, value) <> (TestData.tupled, TestData.unapply)
  }

  private val testDataTable = TableQuery[TestDataTable]

  private val data1 = TestData("k1", 1)
  private val data2 = TestData("k2", 2)
  private val data3 = TestData("k3", 3)

  case class TestError(message: String) extends Throwable

  override def beforeAll(): Unit = {
    super.beforeAll()
    Await.ready(db.run(testDataTable.schema.create), Duration.Inf)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    Await.ready(db.run(DBIO.seq(
      testDataTable.delete,
      testDataTable ++= Seq(data1, data2, data3)
    )), Duration.Inf)
  }

  "filter if" when {
    "condition is false" should {
      "not apply filter" in {
        whenReady(db.run(testDataTable.filterIf(condition = false, _.key === "k1").result)) {
          _ must contain only (data1, data2, data3)
        }
      }
    }
    "condition is true" should {
      "apply filter" in {
        whenReady(db.run(testDataTable.filterIf(condition = true, _.key === "k1").result)) {
          _ must contain only data1
        }
        whenReady(db.run(testDataTable.filterIf(condition = true, _.value > 1).result)) {
          _ must contain only (data2, data3)
        }
      }
    }
  }

  "take if" when {
    "condition is false" should {
      "not apply take" in {
        whenReady(db.run(testDataTable.sortBy(_.value).takeIf(condition = false, 1).result)) {
          _ must contain only (data1, data2, data3)
        }
      }
    }
    "condition is true" should {
      "apply take" in {
        whenReady(db.run(testDataTable.sortBy(_.value).takeIf(condition = true, 1).result)) {
          _ must contain only data1
        }
        whenReady(db.run(testDataTable.sortBy(_.value).takeIf(condition = true, 2).result)) {
          _ must contain only (data1, data2)
        }
      }
    }
  }

  "drop if" when {
    "condition is false" should {
      "not apply drop" in {
        whenReady(db.run(testDataTable.sortBy(_.value).dropIf(condition = false, 1).result)) {
          _ must contain only (data1, data2, data3)
        }
      }
    }
    "condition is true" should {
      "apply drop" in {
        whenReady(db.run(testDataTable.sortBy(_.value).dropIf(condition = true, 1).result)) {
          _ must contain only (data2, data3)
        }
        whenReady(db.run(testDataTable.sortBy(_.value).dropIf(condition = true, 2).result)) {
          _ must contain only data3
        }
      }
    }
  }

  "lift constraint violation" should {
    "convert constraint violation exception to given throwable" in {
      val actions = for {
        n1 <- (testDataTable += data1).liftConstraintViolation(TestError("e1"))
        n2 <- (testDataTable += data2).liftConstraintViolation(TestError("e2"))
      } yield {
        n1 + n2
      }

      whenReady(db.run(actions.transactionally).failed) {
        _ mustBe TestError("e1")
      }
    }
  }
}
