package blob_tracker

import org.openrndr.animatable.Animatable
import org.openrndr.boofcv.binding.circles
import org.openrndr.boofcv.binding.toGrayF32
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.noise.uniformRing
import org.openrndr.internal.colorBufferLoader
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.bounds
import java.io.File


class Droplet {

    var label = ""
    var currentIds = listOf<Int>()
    var points = setOf<TrackPoint>()
        set(value) {
            currentIds = value.map { it.id }
            field = value
        }

    val c = ColorRGBa.fromVector(Vector3.uniformRing(0.0, 1.0)).mix(ColorRGBa.WHITE, 0.5)
    fun draw(drawer: Drawer) {
        if(points.isNotEmpty()) {
            drawer.stroke = c
            drawer.strokeWeight = 1.0
            drawer.fill = null
            drawer.rectangle(points.map { it.pos }.bounds.offsetEdges(10.0))


            drawer.stroke = null
            drawer.fill = c
            drawer.circles(points.map { it.pos }, 0.7)

            drawer.text(label, points.first().pos)
        }
    }

}

class Loader {
    val path = "file:///home/marco/PycharmProjects/resnet-50-test/dataset/"

    val proxies = mutableListOf<ColorBufferProxy>()
    val labels = File("data/labels-only.csv").readLines().map {
        val s = it.split(",")
        val name = s[0]
        val label = s[1].replace("^\"|\"$", "")

        proxies.add(colorBufferLoader.loadFromUrl("file://${path + name.dropLast(7) + "/" + name}"))
        label
    }.toMutableList()

    fun next(): String {
        val copy = labels
        return if(labels.isNotEmpty()) {
            val r = copy.random()
            labels.remove(r)
            r
        } else {
            println("finished!")
            ""
        }
    }
}

class Plate(val frame: Rectangle): Animatable() {

    val loader = Loader()
    val droplets = mutableListOf<Droplet>()

    var image = colorBuffer(frame.width.toInt(), frame.height.toInt())
        set(value) {
            field = value
            val range = 20.0..200.0
            contours = computeContours(value).filter { it.bounds.width in range && it.bounds.height in range}.map { it.close() }
            rects = contours.map { it.bounds }
        }

    val tracker = KLT()

    var trackPoints = mutableListOf<TrackPoint>()
    var rects = listOf<Rectangle>()
        set(value) {
            prune()
            tracker.process(image.toGrayF32())
            tracker.spawnTracks()

            trackPoints = tracker.getActiveTracks(null).map { TrackPoint(Vector2(it.pixel.x, it.pixel.y), it.featureId.toInt()) }.toMutableList()
            val pointsToRects = value.map { rect ->
                trackPoints.filter { p -> rect.offsetEdges(-1.0).contains(p.pos) }.toSet()
            }

            val iter = pointsToRects.listIterator()
            while (iter.hasNext()) {
                val rectPoints = iter.next()
                val ids = rectPoints.map { it.id }

                val droplet = droplets.firstOrNull {
                    it.currentIds.intersect(ids).isNotEmpty()
                }

                if(droplet != null) {
                    droplet.points = rectPoints.toSet()
                } else {
                    val d = Droplet().apply {
                        label = loader.next()
                        points = rectPoints }
                    droplets.add(d)
                }

            }

            field = value
        }
    var contours = listOf<ShapeContour>()


    var timer = 0.0
    fun prune() {
        if(!hasAnimations()) {
            timer = 0.0
            cancel()
            ::timer.animate(1.0, 2000).completed.listen {
                val inactive = tracker.getInactiveTracks(null).toSet()
                tracker.dropTracks { inactive.contains(it) }
                trackPoints.removeAll(inactive)


                droplets.removeIf {
                    it.currentIds.intersect(trackPoints.map { it.id }).isEmpty()}
            }
        }
    }

    fun update(cb: ColorBuffer) {
        image = cb
    }

    fun draw(drawer: Drawer) {
        updateAnimation()
        drawer.isolated {
            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE
            drawer.strokeWeight = 0.1
            drawer.circles(trackPoints.map { it.pos }, 1.5)
        }

        droplets.forEach { it.draw(drawer) }


        drawer.isolated {
            drawer.fill = ColorRGBa.WHITE.opacify(0.5)
            drawer.stroke = null
            drawer.contours(contours)
        }


        drawer.fill = ColorRGBa.WHITE
        drawer.rectangle(0.0, 0.0, frame.width * timer, 5.0)
    }

    init {
        prune()
    }
}