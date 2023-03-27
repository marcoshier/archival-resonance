package blob_tracker

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.PropertyAnimationKey
import org.openrndr.color.ColorHSLa
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds

class DropletOld(var id: Int): Animatable() {

    init {
        println("$id is created")
    }

    var isTracked = false
    var currentIds = listOf<Int>()
    var previousIds = listOf<Int>()
    var bounds = Rectangle(0.0, 0.0, 1.0, 1.0)

    var points = setOf<TrackPoint>()
        set(value) {
            previousIds = (field.ifEmpty { value }).map { it.id }
            currentIds = value.map { it.id }
            positions = value.map { it.pos }

            field = value
        }

    var positions = listOf<Vector2>()

    var timer = 0.0

    fun startTimer(): PropertyAnimationKey<Double> {
        ::timer.cancel()
        return ::timer.animate(1.0, 2000)
    }

    fun draw(drawer: Drawer) {

        updateAnimation()
        isTracked = currentIds.intersect(previousIds).isNotEmpty()

        val c = ColorHSLa(0.5, 0.8, 0.8).shiftHue(id * 10.0).toRGBa()
        bounds = positions.bounds.offsetEdges(5.0)

        drawer.stroke = c
        drawer.strokeWeight = 0.5
        drawer.fill = null
        drawer.rectangle(bounds.offsetEdges(3.0))

        drawer.fill = c
        drawer.circles(positions, 1.3)

        drawer.fill = ColorRGBa.BLACK
        drawer.text(id.toString(), positions.bounds.center)

    }
}