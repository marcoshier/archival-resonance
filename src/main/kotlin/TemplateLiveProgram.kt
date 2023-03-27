import org.openrndr.application
import org.openrndr.color.ColorHSLa
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

/**
 *  This is a template for a live program.
 *
 *  It uses oliveProgram {} instead of program {}. All code inside the
 *  oliveProgram {} can be changed while the program is running.
 */

fun main() = application {
    configure {
        width = 800
        height = 800
    }
    oliveProgram {

        val list = drawer.bounds.scatter(20.0)
        val circles =
        extend {

            Random.resetState()
            drawer.clear(ColorRGBa.PINK)

            drawer.rectangles {
                for (pear in list) {
                    val h = Double.uniform(0.0, 360.0, Random.rnd)
                    val hsla = ColorHSLa(h, 0.5, 0.5)

                    this.fill = hsla.toRGBa()
                    val r = Rectangle.fromCenter(pear, 25.0, 25.0)
                    this.rectangle(r)
                }
            }


        }
    }
}