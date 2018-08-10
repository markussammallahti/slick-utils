package mrks.slick

import java.sql.SQLException

import play.api.db.slick.HasDatabaseConfig
import slick.jdbc.JdbcProfile
import slick.lifted.CanBeQueryCondition

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

trait Extensions[P <: JdbcProfile] extends HasDatabaseConfig[P] {
  import profile.api._

  implicit class QueryEnrichment[E,U,C[_]](q: Query[E,U,C]) {
    def filterIf[T <: Rep[_]](condition: => Boolean, criteria: E => T)(implicit wt: CanBeQueryCondition[T]): Query[E,U,C] = {
      if (condition) q.filter(criteria)(wt) else q
    }

    def takeIf(condition: => Boolean, num: => Long): Query[E,U,C] = {
      if (condition) q.take(num) else q
    }

    def dropIf(condition: => Boolean, num: => Long): Query[E,U,C] = {
      if (condition) q.drop(num) else q
    }
  }

  implicit class ActionEnhancement[R](action: DBIOAction[R,NoStream,Effect.Write]) {
    def liftConstraintViolation(throwable: Throwable)(implicit ec: ExecutionContext): DBIOAction[R,NoStream,Effect.Write] = {
      action.cleanUp(convertConstraintViolation(throwable), keepFailure = false)
    }
  }

  private def convertConstraintViolation(throwable: Throwable)(result: Option[Throwable]) = result match {
    case Some(e: SQLException) if e.getSQLState.startsWith("23") =>
      DBIO.failed(throwable)

    case _ =>
      DBIO.successful(())
  }
}
