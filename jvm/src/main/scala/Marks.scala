case class Point(x: Float, y: Float)

trait Marks:
    val markNames: Seq[String]

object HandMarks extends Marks:
    val markNames = Seq(
        // "1st Distal Phalanx Distal End",
        "1st Distal Phalanx Proximal End",
        "1st Proximal Phalanx Distal End",
        "1st Proximal Phalanx Proximal End",
        // "2nd Distal Phalanx Distal End",
        "2nd Distal Phalanx Proximal End",
        "2nd Middle Phalanx Distal End",
        "2nd Middle Phalanx Proximal End",
        "2nd Proximal Phalanx Distal End",
        "2nd Proximal Phalanx Proximal End",
        // "3rd Distal Phalanx Distal End",
        "3rd Distal Phalanx Proximal End",
        "3rd Middle Phalanx Distal End",
        "3rd Middle Phalanx Proximal End",
        "3rd Proximal Phalanx Distal End",
        "3rd Proximal Phalanx Proximal End",
        // "4th Distal Phalanx Distal End",
        "4th Distal Phalanx Proximal End",
        "4th Middle Phalanx Distal End",
        "4th Middle Phalanx Proximal End",
        "4th Proximal Phalanx Distal End",
        "4th Proximal Phalanx Proximal End",
        // "5th Distal Phalanx Distal End",
        "5th Distal Phalanx Proximal End",
        "5th Middle Phalanx Distal End",
        "5th Middle Phalanx Proximal End",
        "5th Proximal Phalanx Distal End",
        "5th Proximal Phalanx Proximal End",
        "1st Metacarpal Distal End",
        "1st Metacarpal Proximal End",
        "2nd Metacarpal Distal End",
        "2nd Metacarpal Proximal End",
        "3rd Metacarpal Distal End",
        "3rd Metacarpal Proximal End",
        "4th Metacarpal Distal End",
        "4th Metacarpal Proximal End",
        "5th Metacarpal Distal End",
        "5th Metacarpal Proximal End",
        "Distal Radius",
        "Distal Ulna"
    )