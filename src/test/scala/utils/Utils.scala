package utils

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.IteratorHasAsScala

object Utils {
  implicit class TestEitherOps[A, B](either: Either[A, B]) {
    def getUnsafe: B = either.toOption.get
  }

  def deleteFiles(dirPath: Path): List[Unit] =
    Files.list(dirPath).iterator().asScala.map(Files.delete).toList
}
