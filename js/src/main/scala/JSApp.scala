import scala.scalajs.js
import js.annotation.*

@JSExportTopLevel("JS")
object JSApp:
    @JSExport
    def hello = println("Hello, from JS")