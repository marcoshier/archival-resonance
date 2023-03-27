package blob_tracker

import boofcv.abst.tracker.PointTrack
import classes.SlotMap
import film.itLabels
import org.openrndr.animatable.Animatable
import org.openrndr.boofcv.binding.toGrayF32
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

class TrackPoint(val pos: Vector2, val id: Int): PointTrack()

class SpaceOld: Animatable() {

    val tracker = KLT()

    val labels = itLabels.shuffled().take(100)

    var droplets = SlotMap(
        onPut = { assignToLabel(it) },
        onRemove = { unassignFromLabel(it) }
    )

    var bounds = listOf<Rectangle>()
    var trackedPoints = listOf<TrackPoint>()
        set(value) {
            field = value

            //prune()
            val pointsToRects = bounds.map { rect ->
               value.filter { p -> rect.contains(p.pos) }
            }

            if(droplets.isEmpty()) {
                println("empty")
                pointsToRects.forEachIndexed { index, points ->
                    val d = DropletOld(index)
                    d.points = points.toSet()
                    droplets[index] = d
                }
            }

            val iter = pointsToRects.listIterator()
            while (iter.hasNext()) {
                val rectPoints = iter.next().toSet()
                val index = iter.nextIndex()
                val ids = rectPoints.map { it.id }

                val droplet = droplets.entries.firstOrNull {
                    it.value.currentIds.intersect(ids).isNotEmpty()
                }?.value

                if(droplet != null) {
                    droplet.points = rectPoints.toSet()
                } else {
                    droplets.getOrPut(index) {
                        val d = DropletOld(index)
                        d.points = rectPoints
                        d
                    }
                }

            }

        }


    fun updateTrackPoints(cb: ColorBuffer) {
        tracker.process(cb.toGrayF32())
        tracker.spawnTracks()

        val active = tracker.getActiveTracks(null)
        trackedPoints = active.map {
            TrackPoint(Vector2(it.pixel.x, it.pixel.y), it.featureId.toInt())
        }

    }

    var dropTimer = 0.0
    fun prune() {
        if(!hasAnimations()) {
            //::dropTimer.cancel()
            ::dropTimer.animate(1.0, 2000).completed.listen {
                println("dropping")
                val inactive = tracker.getInactiveTracks(null)
                tracker.dropTracks { inactive.contains(it) }

                val keysToRemove = droplets.filter {
                    it.value.currentIds.intersect(trackedPoints.map { it.id }).isEmpty() || !it.value.isTracked }.keys

                for(key in keysToRemove) {
                    droplets.remove(key)
                }
            }
        }

    }

    fun updatePoints(cb: ColorBuffer) {
        val contours = computeContours(cb)
        bounds = contours.map { it.bounds }

        updateAnimation()
        updateTrackPoints(cb)
    }

    fun assignToLabel(i: Int) {
        droplets[i]!!.startTimer().completed.listen {
            println(labels[i])
        }
    }

    fun unassignFromLabel(i: Int) {
        println(labels[i])
    }

/*    fun updateSynths() {
        for((i, droplet) in droplets.entries.take(synths.size - 1)) {
            val synth = synths[i]
            if(synth.isPlaying) {
                synth.update(droplet.bounds.center)
            }
        }
    }*/

    fun draw(cb: ColorBuffer, drawer: Drawer) {

        //updateSynths()
        updatePoints(cb)

        // droplets
        drawer.isolated {
            drawer.stroke = null
            drawer.circles {
                trackedPoints.map {
                    this.fill = ColorRGBa.WHITE.opacify(0.6)
                    this.circle(it.pos, 0.5)
                }
            }

            droplets.forEach {
                it.value.draw(drawer)
            }
        }

        drawer.stroke = ColorRGBa.RED
        drawer.fill = null
        drawer.rectangles(bounds)
    }


}