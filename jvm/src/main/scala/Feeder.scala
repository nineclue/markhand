import os.Path
import scala.collection.mutable.{ArrayBuffer => AB}

case class TrainCSV(id: Int, age: Int, male: Boolean)

/**
  * Feeder
  *  B: 자료 목록 type
  *  A: B를 구분하는 단위 (key)
  *  C: 실제 저장 type, bone age에서는 png 파일 목록, listed2Actual: B => C
  */
trait Feeder[A, B, C](using Ordering[A]):
    val items: Seq[B]
    val stored: Seq[C]
    def listed2Actual(listed: B): C
    val partitions: Map[A, Seq[B]]
    // val marks: Marks
    // def getPoints(itemKey: A): Option[Seq[Point]]
    // def setPoint(itemKey: A, i: Int, point: Point): Unit 
    def serve: Option[C] 

    // key와 served items의 map
    protected lazy val served = collection.mutable.Map.empty[A, AB[C]].withDefaultValue(AB.empty[C])
    // item들의 serve된 순서대로 
    protected lazy val totalServed = AB.empty[C]
    // 남아있는 key들
    private lazy val remainingSet = collection.mutable.Set.from(partitions.keys)
    def listedSize: Int = items.length
    def actualSize: Int = stored.length

    protected var index: Option[Int] = None
    protected var lastServed: Option[A] = Option.empty

    def serve(partition: A): Option[C] = 
        val s = served.getOrElseUpdate(partition, AB.empty)
        val unserved = partitions(partition).map(listed2Actual).filterNot(s.contains)
        unserved.length match
            case 0 => 
                remainingSet -= partition
                None
            case _ =>
                val picked = unserved(scala.util.Random().nextInt(unserved.length))
                lastServed = Some(partition)
                served(partition).append(picked)
                totalServed.append(picked)
                index = Some(totalServed.length - 1)
                Some(picked)        

    /* partition 별로 한 partition내의 것들 끝내고 다음 partition으로 */
    def firstServe: Option[C] = 
        var result: Option[C] = Option.empty
        remainingSet.find: k =>
            result = serve(k)
            result.nonEmpty
        match
            case Some(_) => result
            case None => None

    /* partition 순서대로 증가하면서 하나씩 serve */
    def partitionalServe: Option[C] = 
        val o = summon[Ordering[A]]
        val sortedKeys = remainingSet.toSeq.sorted
        val nextK = sortedKeys.find(k => lastServed.isEmpty || o.gt(k, lastServed.get)) match
            case Some(k) => k
            case _ => sortedKeys.head
        serve(nextK)

    /* percent 낮은 partition 부터 순서대로 */
    def proportionalServe: Option[C] = 
        val proportions = partitions.map: (k, items) =>
            (k, served.getOrElse(k, AB.empty).length.toDouble / items.length)
        val minKey = proportions.minBy(_._2)._1
        serve(minKey)

    def backward: Option[C] = 
        index match
            case Some(i) if i == 0 =>
                None
            case Some(i) =>
                index = Some(i-1)
                Some(totalServed(i))
            case None => 
                None

    def forward: Option[C] =
        index match
            case Some(i) if i < totalServed.length =>
                index = Some(i+1)
                Some(totalServed(i))
            case Some(i) =>
                // partitionalServe
                serve
            case None => 
                None        

    def neighbors(width: Int = 5) = 
        index match
            case Some(i) =>
                val min = 0 max (i - width)
                val max = totalServed.length min (i + width)
                Some(totalServed.slice(min, max))
            case _ => 
                None

    def getCurrent = index.map(totalServed.apply)

case class BoneAgePngs(path: String) extends Feeder[Int, TrainCSV, Int]:
    private val baseDir = os.Path(path) 
    private val trainDir = baseDir / "boneage-training-dataset"
    val items =
        os.read.lines(baseDir / "train.csv").tail.map: l =>
            val ws = l.split(",")
            TrainCSV(ws(0).toInt, ws(1).toInt, ws(2) == "True")
    val partitions = items.groupBy(_.age / 12)
    val stored = os.list(trainDir).withFilter(p => p.baseName.head != '.' && p.ext == "png").map(_.baseName.toInt)
    def listed2Actual(listed: TrainCSV): Int = listed.id
    def serve = proportionalServe

    val populations = partitions.mapValues(_.length)
    def completed = populations.map: (k, v) =>
            (k, (served.getOrElse(k, AB.empty).length, v))
        .toMap

    // point 관련 함수들
    val marks = HandMarks
    private val pointsMap = scala.collection.mutable.Map.empty[Int, (Int, Int, AB[(Double, Double)])]
    def getPoints(itemKey: Int): Option[Seq[(Double, Double)]] = 
        pointsMap.get(itemKey).map(_._3.toSeq.map(((_, _))))

    def setPoint(itemKey: Int, i: Int, size: (Int, Int), point: (Double, Double)): Seq[(Double, Double)] = 
        println(s"SETTING POINT of ${itemKey}(${i}) to $point")
        val (_, _, points) = pointsMap.getOrElse(itemKey, (0, 0, AB.fill(marks.markNames.length)((-1.0, -1.0))))
        points(i) = point
        pointsMap.update(itemKey, (size._1, size._2, points))
        points.toSeq

    private val newline = sys.props("line.separator")
    private val file = os.pwd / "points.txt"
    def loadPoints = 
        if os.exists(file) then
            println("loading points...")
            val contents = os.read(file).split(newline)
            println(contents.head)
            contents.tail.foreach: l =>
                val comps = l.split(",").map(_.strip)
                val (base, ps) = comps.splitAt(3)
                val bInt = base.map(_.toInt)
                val pDouble = ps.sliding(2, 2).map(pp => (pp(0).toDouble, pp(1).toDouble))
                pointsMap += ((bInt(0), (bInt(1), bInt(2), AB.from(pDouble))))
                push(bInt(0))
            println(pointsMap)
            println(s"$lastServed, $totalServed, $index")
        else
            println("no points.txt file, skipping loading.")

    def savePoints = 
        println("save points")
        val files = totalServed.mkString("[", ",", "]")
        val subs = 
            totalServed.map: i =>
                val (w, h, ps) = pointsMap.getOrElse(i, (0, 0, AB.fill(marks.markNames.length)((-1.0, -1.0))))
                s"$i,$w,$h,${ps.map(t => s"${t._1},${t._2}").mkString(",")}"
            .mkString(newline)
        os.write.over(file, files ++ newline ++ subs)

    def push(fno: Int) = 
        items.find(_.id == fno) match
            case Some(t) =>
                val k = t.age / 12
                lastServed = Some(k)
                served(k).append(fno)
                totalServed.append(fno)
                index = Some(totalServed.length - 1)
            case _ => throw Exception(s"파일 번호 ${fno}에 관한 자료를 찾을 수 없습니다.")