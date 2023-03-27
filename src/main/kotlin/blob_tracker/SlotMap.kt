package classes

import blob_tracker.DropletOld

class SlotMap(private val map: MutableMap<Int, DropletOld> = hashMapOf(),
              val onPut: (i: Int) -> Unit,
              val onRemove: (i: Int) -> Unit): MutableMap<Int, DropletOld> by map {


    override fun put(key: Int, value: DropletOld): DropletOld? {
        return map.put(key, value).also {
            println("droplet not found, creating new one at $key")
            onPut(key)
        }
    }

    override fun remove(key: Int): DropletOld? {
        return map.remove(key).also {
            println("droplet $key removed. Decoupling synth $key")
            onRemove(key)
        }
    }
}