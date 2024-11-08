import scala.scalajs.js
import js.annotation.*
import org.scalajs.dom
import dom.{MouseEvent, Element, Image, HTMLElement}
import scala.compiletime.uninitialized

@JSExportTopLevel("JS")
object JSApp:
    @JSExport
    def hello = println("Hello, from JS")

    private var svgElm: Element = uninitialized

    @JSExport
    def setupHandler(iElmId: String, sElmId: String) = 
        val iDiv = dom.document.getElementById(iElmId)
        svgElm = dom.document.getElementById(sElmId)
        // println(s"I'm setup handler ${iDiv.clientWidth}")
        println(s"I'm setup handler ${iDiv}")
        iDiv.addEventListener("mousemove", e => mouseHandler(iDiv)(e))
        iDiv.addEventListener("mousedown", e => mouseHandler(iDiv)(e))

    private val borderPxRx = raw"\s*(\d+)\s*px".r
    private def mouseHandler(iTag: Element)(e: MouseEvent) =
        // val (x,y) = (iTag.clientLeft, iTag.clientTop)
        val rect = iTag.getBoundingClientRect()
        e.`type` match
            case "mousemove" =>
                // println(s"(${e.clientX - rect.left} , ${e.clientY - rect.top})")
            case "mousedown" =>
                // object-fit : contain인 경우 img 태그의 위치는 변하지 않음
                val img = dom.document.getElementById("wristImage").asInstanceOf[Image]
                val iratio = img.naturalWidth.toDouble / img.naturalHeight  // 원 image의 비율
                val borderWidth = iTag.asInstanceOf[HTMLElement].style.borderWidth match
                    case borderPxRx(bw) => bw.toInt
                    case _ => 0
                // actual img tag width, height
                val (width, height) = (rect.width - borderWidth * 2, rect.height - borderWidth * 2)
                // rescaled image size
                val (iw, ih) = if iratio >= 1.0 then (width, height / iratio) else (width * iratio, height)
                //      div position - img tag position - padding for image
                val iLeft = e.clientX - rect.left - (width - iw) / 2
                val iTop = e.clientY - rect.top - (height - ih) / 2
                val (xratio, yratio) = (iLeft / iw, iTop / ih)
                println(s"Mouse Click! (${iLeft}, ${iTop}) [$iw, $ih] => <$xratio, $yratio>")
                mark((e.clientX - rect.left).toInt, (e.clientY - rect.top).toInt)
                  // (${e.clientX - rect.left} , ${e.clientY - rect.top}), [${img.naturalWidth}, ${img.naturalHeight}] => {${iw}, ${ih}}")
    @JSExport
    def mark(px: Int, py: Int, current: Boolean = true) = 
        import scalatags.JsDom.svgTags.*
        import scalatags.JsDom.svgAttrs.*
        import scalatags.JsDom.implicits.{intAttr, stringAttr}
        println("mark!")
        val fillColor = if current then "tomato" else "yellow"
        svgElm.appendChild(
            circle(cx := px, cy := py, r := 3, fill := fillColor, stroke := "white").render
        )