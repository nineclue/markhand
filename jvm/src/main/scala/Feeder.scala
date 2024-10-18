import os.Path
import scala.collection.mutable.{ArrayBuffer => AB}

case class TrainCSV(id: Int, age: Int, male: Boolean)

/**
  * Feeder
  *  B: 자료 목록 type
  *  A: B를 구분하는 단위 (key)
  *  C: 실제 저장 type
  */
trait Feeder[A, B, C](using Ordering[A]):
    val items: Seq[B]
    val stored: Seq[C]
    def listed2Actual(listed: B): C
    val partitions: Map[A, Seq[B]]

    protected lazy val served = collection.mutable.Map.empty[A, AB[C]]
    protected lazy val totalServed = AB.empty[C]
    private lazy val remainingSet = collection.mutable.Set.from(partitions.keys)
    def listedSize: Int = items.length
    def actualSize: Int = stored.length

    private var index: Option[Int] = None
    private var lastServed: Option[A] = Option.empty

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
    def serve: Option[C] = 
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
                partitionalServe
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
    // def listed2Actual(listed: TrainCSV): os.Path = trainDir / s"${listed.id}.png"
    // def toPath(i: Int) = trainDir / s"${i}.png"

    val populations = partitions.mapValues(_.length)
    def completed = populations.map: (k, v) =>
            (k, (served.getOrElse(k, AB.empty).length, v))
        .toMap
