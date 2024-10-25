import cats.effect.*
import cats.syntax.all.*
import org.http4s.*
import org.http4s.server.{Router, AuthMiddleware}
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.scalatags.*
import org.http4s.ember.server.*
import com.comcast.ip4s.*

object MarkHand extends IOApp:
    import _root_.scalatags.Text
    import Text.all.*
    import Text.tags2.title

    val pngFeed = BoneAgePngs("/Users/nineclue/lab/boneage")

    private def replaceAfterLoad(targetUrl: String) = modifier(data.hx.get := targetUrl, data.hx.trigger := "load")

    def root =
        doctype("html")(
            html(
                head(
                    title("MarkHand"),
                    meta(charset := "UTF-8"),
                    meta(name := "viewport", content := "width=device-width, initial-scale=1.0"),
                    link(href := "/assets/markhand.css", rel := "stylesheet"),
                    script(src := "/assets/htmx.min.js"),
                    script(src := "/assets/markhand.js"),
                ),
                lang := "ko",
                body(
                    h2("MarkHand"),
                    div(s"CSV files: ${pngFeed.listedSize}, folder files: ${pngFeed.actualSize}"),
                    div(replaceAfterLoad("/echo/안녕여러분"), "HelloEveryone"),
                    div(pngFeed.populations.keys.toSeq.sorted.map(k => 
                        div(s"$k : ${pngFeed.completed(k)._1} / ${pngFeed.completed(k)._2}"))),
                    div(replaceAfterLoad("/next"), "PNG image")
                )))

    override def run(as: List[String]): IO[ExitCode] =
        IO.println(s"MARKHAND server running...") *>
        server.use(_ => IO.never).as(ExitCode.Success)

    def servePng = 
        // pngFeed.serve match
        pngFeed.partitionalServe match
            case Some(p) =>
                div(
                    div(p),
                    div(pngFeed.neighbors().map(_.mkString(",")).getOrElse("NONE")),
                    img(src := s"/img/${p}.png"))
            case _ =>
                div("모든 작업이 끝났습니다.")

    def server =
        // ln -s /Users/nineclue/lab/radserver/js/target/scala-3.3.3/rs-fastopt/main.js /Users/nineclue/lab/radserver/jvm/src/main/resources/mkhrad-fastopt.js
        // ln -s /Users/nineclue/lab/radserver/js/target/scala-3.3.3/rs-opt/main.js /Users/nineclue/lab/radserver/jvm/src/main/resources/mkhrad.js
        // mv ~/Downloads/htmx.min-2.js jvm/src/main/resources
        val simpleRoutes: HttpRoutes[IO] = HttpRoutes.of:
            case request @ GET -> Root / "assets" / "markhand.js" =>
                StaticFile.fromPath(fs2.io.file.Path("js/target/scala-3.5.0/m-fastopt/main.js"), Some(request))
                    .getOrElseF(NotFound()) // In case the file doesn't exist
            case request @ GET -> Root / "assets" / "markhand.js.map" =>
                StaticFile.fromPath(fs2.io.file.Path("js/target/scala-3.5.0/m-fastopt/main.js.map"), Some(request))
                    .getOrElseF(NotFound()) // In case the file doesn't exist
            case request@GET -> Root / "assets" / file =>
                StaticFile.fromResource(file, Some(request)).getOrElseF(NotFound())
            case request @ GET -> Root / "img" / file =>
                StaticFile.fromPath(fs2.io.file.Path(s"/Users/nineclue/lab/boneage/boneage-training-dataset/$file"), Some(request))
                    .getOrElseF(NotFound())
            case request@GET -> Root =>
                IO.println(s"Got request from ${request.remoteAddr.getOrElse("unknown")}") *>
                Ok(root)
            case GET -> Root / "next" =>
                Ok(servePng)
            case GET -> Root / "echo" / content =>
                Ok(content)

        val corsService = org.http4s.server.middleware.CORS.policy.withAllowOriginAll(simpleRoutes)
        EmberServerBuilder
            .default[IO]
            .withHost(ipv4"0.0.0.0")
            .withPort(port"8080")
            .withHttpApp(corsService.orNotFound)
            .build
