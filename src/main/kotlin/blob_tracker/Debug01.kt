package blob_tracker

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.Drawer
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.createEquivalent
import org.openrndr.extra.fx.Post
import org.openrndr.extra.fx.color.ColorCorrection
import org.openrndr.extra.fx.color.ColorLookup
import org.openrndr.extra.fx.color.LumaThreshold
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.ffmpeg.PlayMode
import org.openrndr.ffmpeg.loadVideo
import org.openrndr.math.Vector2
import org.openrndr.shape.IntRectangle
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
        val source = viewBox(sourceFrame) {
            val cb = colorBuffer(this.width, this.height)
            val video = loadVideo("data/oil_on_water.mp4", PlayMode.VIDEO).apply {
                play()
                seek(duration / 2.0)
                newFrame.listen {
                    it.frame.copyTo(cb,
                        sourceRectangle = it.frame.bounds.flippedVertically().toInt(),
                        targetRectangle = sourceFrame.toInt()
                    )
                }
            }

            extend(Post()) {
                val threshold = ColorMoreThan().addTo(gui)
                val colorcorr = ColorCorrection().addTo(gui)
                post { input, output ->
                    val int = intermediate[0]
                    colorcorr.apply(input, int)
                    threshold.apply(int, output)
                }
            }
            extend {
                video.draw(drawer, blind = true)
                drawer.image(cb)

            }

        }

        val target = viewBox(sourceFrame.movedBy(Vector2.UNIT_X * 960.0)) {
            val space = SpaceOld()

            extend {
                space.draw(source.result, drawer)
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
            //target.draw()

        }
    }
}
