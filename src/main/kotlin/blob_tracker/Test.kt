package blob_tracker

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.color.presets.ORANGE
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.ffmpeg.PlayMode
import org.openrndr.ffmpeg.loadVideo
import org.openrndr.ffmpeg.loadVideoDevice

fun main() = application {
    configure {
        width = 1920
        height = 540
    }

    program {

        val gui = GUI()
        val settings = object {

            @ColorParameter("Color 0")
            var color0 = ColorRGBa.ORANGE


            @ColorParameter("Color 1")
            var color1 = ColorRGBa.WHITE

        }.addTo(gui)

        var cb = colorBuffer(960, 540)

        val video = loadVideo("data/oil_on_water.mp4", PlayMode.VIDEO).apply {
            play()
            seek(duration / 2.0)

            val ss = shadeStyle {

                fragmentTransform = """
                  
                  vec3 c = vec3(0.4);
                  vec3 fill = texture(p_cb, c_boundsPosition.xy).xyz;
                  
                  if(any(lessThan(fill, p_c1))) {
                        c = fill.xyz;
                  } else {
                        c = vec3(0.5).xyz;
                  }
                  
                  x_fill = vec4(c, 1.0);
                           
                  """.trimIndent()
                parameter("cb", cb)
            }

            val rt = renderTarget(960, 540) {
                colorBuffer()
                depthBuffer()
            }

            newFrame.listen {

                drawer.isolatedWithTarget(rt) {
                    drawer.clear(ColorRGBa.WHITE)

                    ss.parameter("c0", settings.color0.toVector4().xyz)
                    ss.parameter("c1", settings.color1.toVector4().xyz)

                    drawer.shadeStyle = ss

                    drawer.ortho()
                    drawer.image(it.frame, 0.0, 0.0, 960.0, 540.0)
                }

                cb = rt.colorBuffer(0)
            }

        }

        val space = SpaceOld()

        extend(gui)
        extend {
            gui.visible = true

            drawer.clear(ColorRGBa.BLACK)

            video.draw(drawer, true)
            space.draw(cb, drawer)

        }
    }
}

