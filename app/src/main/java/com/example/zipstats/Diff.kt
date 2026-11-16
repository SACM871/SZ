package com.example.zipstats

class DiffResult {
    val added = ArrayList<Pair<String, Long>>()
    val removed = ArrayList<Pair<String, Long>>()
    val changed = ArrayList<Triple<String, Long, Long>>()
    var addedSize = 0L
    var removedSize = 0L
    val perAcc = HashMap<String, IntArray>()   // [مضاف، محذوف، مغيّر]
}

object Diff {
    fun snapshot(root: Node): HashMap<String, Long> {
        val m = HashMap<String, Long>()
        fun walk(n: Node, p: String) {
            for (k in n.kids) {
                val kp = p + "/" + k.name
                if (k.isDir) walk(k, kp) else m[kp] = k.size
            }
        }
        walk(root, "")
        return m
    }

    private fun accOf(path: String): String {
        for (s in path.split("/")) if (s.isNotEmpty() && Acc.kind(s) != 0) return s
        return "—"
    }

    fun compare(base: Map<String, Long>, cur: Map<String, Long>): DiffResult {
        val r = DiffResult()
        for ((p, s) in cur) {
            val b = base[p]
            if (b == null) {
                r.added.add(Pair(p, s)); r.addedSize += s
                val arr = r.perAcc.getOrPut(accOf(p)) { IntArray(3) }
                arr[0]++
            } else if (b != s) {
                r.changed.add(Triple(p, b, s))
                val arr = r.perAcc.getOrPut(accOf(p)) { IntArray(3) }
                arr[2]++
            }
        }
        for ((p, s) in base) {
            if (!cur.containsKey(p)) {
                r.removed.add(Pair(p, s)); r.removedSize += s
                val arr = r.perAcc.getOrPut(accOf(p)) { IntArray(3) }
                arr[1]++
            }
        }
        r.added.sortByDescending { it.second }
        r.removed.sortByDescending { it.second }
        r.changed.sortByDescending { it.third }
        return r
    }
}
