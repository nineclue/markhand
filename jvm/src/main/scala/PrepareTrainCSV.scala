//> using scala 3.5.0
//> using dep co.fs2::fs2-core:3.11.0
//> using dep co.fs2::fs2-io:3.11.0

import cats.effect.{IO, IOApp, ExitCode}
import fs2.io.file.{Files, Path}


object PrepareTrainCSV extends IOApp:
    // def main(as: Array[String]): Unit = println("준비 완료!")

    def run(as: List[String]): IO[ExitCode] =
        opentrain.flatMap(IO.println) *> IO(ExitCode.Success)

    case class TrainCSV(id: Int, age: Int, mail: Boolean)
    case class Point(x: Double, y: Double)
    case class HandMarks(points: Seq[Point])

    val opentrain =
        Files[IO].readUtf8Lines(Path("/Users/nineclue/lab/boneage/train.csv"))
            .tail.take(30).map: l =>
                val ws = l.split(",")
                TrainCSV(ws(0).toInt, ws(1).toInt, ws(2) == "True")
            .compile.toList.map(_.groupBy(_.age / 12))
