package screens

import blob_tracker.Loader
import blob_tracker.Plate
import blob_tracker.loadDropletsVideo
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated

fun main() = application {
    configure {
        width = 1280
        height = 720
    }
    program {

        val source = loadDropletsVideo(drawer.bounds)
        val space = Plate(source.result.bounds)

        extend {

            source.update()
            space.update(source.result)

            space.updateAnimation()
            space.run {

                drawer.isolated {
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.WHITE
                    drawer.strokeWeight = 0.1
                    drawer.circles(trackPoints.map { it.pos }, 1.5)
                }

                space.droplets.forEach { it.draw(drawer) }

                drawer.isolated {
                    drawer.fill = ColorRGBa.WHITE.opacify(0.5)
                    drawer.stroke = null
                    drawer.contours(contours)
                }

            }


        }
    }
}