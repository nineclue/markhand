import scala.scalajs.js
import js.annotation.*
import scala.scalajs.js.JSON
import org.scalajs.dom
import dom.{MouseEvent, Element, Image, HTMLElement, XMLHttpRequest, Event}
import scala.compiletime.uninitialized
import io.circe.generic.auto.*, io.circe.syntax.*

@JSExportTopLevel("JS")
object JSApp:

    // private val htmx = js.Dynamic.global.htmx
    private val xhttp = XMLHttpRequest()
    private def setAjaxHandler[A](f: Event => A = (_ => println(xhttp.responseText))) = 
      xhttp.onreadystatechange = e => 
        if xhttp.status == 200 && xhttp.readyState == 4 then 
          f(e)

    @JSExport
    def hello = println("Hello, from JS")

    private var svgElm: Element = uninitialized
    private var currentPointIndex: Int = 0

    @JSExport
    def setupHandler(iElmId: String, sElmId: String) = 
        val iDiv = dom.document.getElementById(iElmId)
        svgElm = dom.document.getElementById(sElmId)
        println(s"Setup handler...")
        // iDiv.addEventListener("mousemove", e => mouseHandler(iDiv)(e))
        iDiv.addEventListener("mousedown", e => mouseHandler(iDiv)(e))

    private val borderPxRx = raw"\s*(\d+)\s*px".r
    private val doubleFormat = "%.4f"
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

                println(s"Original size : ${img.naturalWidth}, ${img.naturalHeight}")
                println(s"Image ratio : $iratio | Border width : $borderWidth")
                println(s"Image tag size : ${rect.width}, ${rect.height}")
                println(s"Mouse position : ${e.clientX}, ${e.clientY}")
                println(s"Calculated image position : $iLeft, $iTop")
                // println(s"Mouse Click! (${iLeft}, ${iTop}) [$iw, $ih] => <$xratio, $yratio>")
                // println(s"VS. ${(e.clientX - rect.left).toInt}, ${(e.clientY - rect.top).toInt}")
                // println("calling" ++ s"/click/$currentPointIndex/${String.format(doubleFormat, xratio)}/${String.format(doubleFormat, yratio)}")
                setAjaxHandler(clickCB)
                xhttp.open("GET", s"/click/$currentPointIndex/${String.format(doubleFormat, xratio)}/${String.format(doubleFormat, yratio)}", true)
                xhttp.send()
                // markPoint((e.clientX - rect.left).toInt, (e.clientY - rect.top).toInt)
                  // (${e.clientX - rect.left} , ${e.clientY - rect.top}), [${img.naturalWidth}, ${img.naturalHeight}] => {${iw}, ${ih}}")
    
    private def clickCB(e: Event) = 
        import io.circe.parser.*
        val r = 
            for 
                j   <-  parse(xhttp.responseText) 
                pi  <-  j.as[Shared.IPoints]
            yield pi
        r match
            case Right(pi) => 
                markDiv(pi.pi + 1)
                if currentPointIndex == pi.ps.length then ???
                println(s"${pi.pi} : ${pi.ps}")
            case _ => 
                ()

    @JSExport
    def markPoint(px: Int, py: Int, current: Boolean = true) = 
        import scalatags.JsDom.svgTags.*
        import scalatags.JsDom.svgAttrs.*
        import scalatags.JsDom.implicits.{intAttr, stringAttr}
        println("mark!")
        val fillColor = if current then "tomato" else "yellow"
        svgElm.appendChild(
            circle(cx := px, cy := py, r := 3, fill := fillColor, stroke := "white").render
        )

    @JSExport
    def markDiv(i: Int) =
        val parent = dom.document.getElementById("pointContainer")
        parent.children.foreach: e => 
            val s = e.asInstanceOf[HTMLElement].style
            s.color = "black"
            s.background = "white"
        if i >= 0 && i < parent.childElementCount then
            val s = parent.children(i).asInstanceOf[HTMLElement].style
            s.color = "white"
            s.background = "black"
            currentPointIndex = i
