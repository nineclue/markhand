import scala.scalajs.js
import js.annotation.*
import scala.scalajs.js.JSON
import org.scalajs.dom
import dom.{MouseEvent, Element, Image, HTMLElement, XMLHttpRequest, Event}
import scala.compiletime.uninitialized
import io.circe.generic.auto.*, io.circe.syntax.*
import org.scalajs.dom.SVGElement
import scala.scalajs.js.Promise

@JSExportTopLevel("JS")
object JSApp:

    private val htmx = js.Dynamic.global.htmx
    private val xhttp = XMLHttpRequest()
    private def setAjaxHandler[A](f: Event => A = (_ => println(xhttp.responseText))) = 
      xhttp.onreadystatechange = e => 
        if xhttp.status == 200 && xhttp.readyState == 4 then 
          f(e)

    @JSExport
    def hello = println("Hello, from JS")

    private var conElmId: String = uninitialized
    private var imgElmId: String = uninitialized
    private var svgElmId: String = uninitialized
    private var currentPointIndex: Int = 0

    @JSExport
    def setupHandler(containerId: String, iElmId: String, sElmId: String) = 
        conElmId = containerId
        imgElmId = iElmId
        svgElmId = sElmId
        val conElm = dom.document.getElementById(containerId)
        println(s"Setup handler...")
        conElm.addEventListener("mousedown", e => mouseHandler(e))

    private val borderPxRx = raw"\s*(\d+)\s*px".r
    private val doubleFormat = "%.4f"
    private def imageInformation =
        val imgElm = dom.document.getElementById(imgElmId).asInstanceOf[Image]
        val (ow, oh) = (imgElm.naturalWidth, imgElm.naturalHeight)
        val rect = imgElm.getBoundingClientRect()
        val iratio = ow.toDouble / oh  // 원 image의 비율
        val borderWidth = imgElm.style.borderWidth match
            case borderPxRx(bw) => bw.toInt
            case _ => 0
        // actual img area width, height
        val (iw, ih) = (rect.width - borderWidth * 2, rect.height - borderWidth * 2)
        // rescaled image size
        val (width, height) = if iratio >= 1.0 then (iw, ih / iratio) else (iw * iratio, ih)
        // println(s"($ow, $oh) => ($width, $height)")
        ((ow, oh), (width, height))

    // 좌표 x, y => 원래 이미지 크기 tup2, ratio tup2 (서버로 보낼 값들)
    private def p2r(x: Double, y: Double): ((Int, Int), (Double, Double)) = 
        val ((ow, oh), (iw, ih)) = imageInformation
        val conElm = dom.document.getElementById(conElmId)
        val rect = conElm.getBoundingClientRect()
        val ix = x - rect.left - (rect.width - iw) / 2.0
        val iy = y - rect.top - (rect.height - ih) / 2.0
        // println(s"[$x, $y]<${rect.left}, ${rect.top}> => ($ix, $iy) => (${ix / iw}, ${iy / ih})")
        ((ow, oh), (ix / iw, iy / ih))

    // ratio x, y => x, y padding 값 tup2, 변환 point tup2 (svg 좌표 표시에 필요한 값들)
    private def r2p(rx: Double, ry: Double): ((Double, Double), (Double, Double)) = 
        val ((ow, oh), (iw, ih)) = imageInformation
        val conElm = dom.document.getElementById(conElmId)
        val rect = conElm.getBoundingClientRect()
        (((rect.width - iw) / 2.0, (rect.height - ih) / 2.0), (rx * iw, ry * ih))

    private def mouseHandler(e: MouseEvent) =
        val conElm = dom.document.getElementById(conElmId)
        val rect = conElm.getBoundingClientRect()
        e.`type` match
            case "mousemove" =>
                // println(s"(${e.clientX - rect.left} , ${e.clientY - rect.top})")
            case "mousedown" =>
                val ((ow, oh), (rx, ry)) = p2r(e.clientX, e.clientY)
                setAjaxHandler(clickCB)
                xhttp.open("GET", s"/click/$currentPointIndex/$ow/$oh/${String.format(doubleFormat, rx)}/${String.format(doubleFormat, ry)}", true)
                xhttp.send()
    
    private def clickCB(e: Event) = 
        import io.circe.parser.*
        val r = 
            for 
                j   <-  parse(xhttp.responseText) 
                pi  <-  j.as[Shared.IPoints]
            yield pi
        r match
            case Right(pi) => 
                // val ((xpad, ypad), (px, py)) = r2p.tupled(pi.ps(pi.pi))
                // markPoint((px + xpad).toInt, (py + ypad).toInt)
                // markDiv(pi.pi + 1)
                // if currentPointIndex == pi.ps.length then ???
                markAllPoints(pi)
                markDiv(pi.pi + 1)
                println(s"${pi.pi} : ${pi.ps}")
            case _ => 
                ()

    @JSExport
    def markPoint(px: Int, py: Int, current: Boolean = true) = 
        import scalatags.JsDom.svgTags.*
        import scalatags.JsDom.svgAttrs.*
        import scalatags.JsDom.implicits.{intAttr, stringAttr}
        val fillColor = if current then "tomato" else "yellow"
        val svgElm = dom.document.getElementById(svgElmId).asInstanceOf[SVGElement]
        svgElm.appendChild(
            circle(cx := px, cy := py, r := 3, fill := fillColor, stroke := "white").render
        )

    private def markAllPoints(ip: Shared.IPoints) = 
        val svgElm = dom.document.getElementById(svgElmId).asInstanceOf[SVGElement]
        svgElm.innerHTML = ""
        ip.ps.zipWithIndex.foreach: (p, pi) =>
            val ((xpad, ypad), (px, py)) = r2p.tupled(p)
            markPoint((px + xpad).toInt, (py + ypad).toInt, pi == ip.pi)
        
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

    @JSExport
    def resetImg = 
        val svgElm = dom.document.getElementById(svgElmId).asInstanceOf[SVGElement]
        svgElm.innerHTML = ""
        markDiv(0)

    @JSExport
    def forward = 
        htmx.ajax("GET", "/next", "#imgDiv")