package blob_tracker

import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.fx.Post
import org.openrndr.extra.fx.color.ColorCorrection
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.viewbox.ViewBox
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.ffmpeg.PlayMode
import org.openrndr.ffmpeg.loadVideo
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

fun main() = application {
    configure {
        width = 2120
        height = 720
        windowAlwaysOnTop = true
    }
    program {

        val gui = GUI()

        val sourceFrame = Rectangle(0.0, 0.0, 960.0, 540.0)
        val source = loadDropletsVideo(sourceFrame)

        val target = viewBox(sourceFrame.movedBy(Vector2.UNIT_X * 960.0)) {
            val loader = Loader()
            val space = Plate(source.result.bounds)

            extend {
                space.update(source.result)
                space.draw(drawer)
            }
        }


        extend(gui)
        mouse.apply {
            buttonDown.listeners.reverse()
            buttonUp.listeners.reverse()
            dragged.listeners.reverse()
        }
        extend {
            drawer.translate(200.0, 0.0)
            source.draw()
            target.draw()

        }
    }
}

fun Program.loadDropletsVideo(sourceFrame: Rectangle): ViewBox {
    return viewBox(sourceFrame) {
        val cb = colorBuffer(this.width, this.height)
        val video = loadVideo("data/oil_on_water.mp4", PlayMode.VIDEO).apply {
            play()
            seek(duration / 2.0)
            ended.listen {
                restart()
                seek(duration / 2.0)
            }
            newFrame.listen {
                it.frame.copyTo(cb,
                    sourceRectangle = it.frame.bounds.flippedVertically().toInt(),
                    targetRectangle = sourceFrame.toInt()
                )
            }
        }

        extend(Post()) {
            val threshold = ColorMoreThan().apply { foreground = rgb(0.0921, 0.6333, 0.0) }
            val colorcorr = ColorCorrection().apply {
                brightness = 0.18
                contrast = 1.0
                saturation = 1.0
                hueShift = -112.91
                gamma = 0.776
            }
            post { input, output ->
                val int = intermediate[0]
                colorcorr.apply(input, int)
                threshold.apply(int, output)
            }
        }
        extend {
            drawer.clear(ColorRGBa.WHITE)
            video.draw(drawer, blind = true)

            drawer.translate(drawer.bounds.center)
            drawer.scale(0.99)
            drawer.translate(-drawer.bounds.center)
            drawer.image(cb)

        }

    }

}