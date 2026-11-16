package com.example.zipstats

import android.content.ContentResolver

class AccStat(val node: Node, val kind: Int) {
    var bigFile: Node? = null
    var bigDir: Node? = null
    var dupCount = 0
    var sugCount = 0
    var sugSize = 0L
    var exts: List<Pair<String, Int>> = emptyList()
}

class Analysis(private val root: Node, private val cr: ContentResolver? = null) {
    val accs = ArrayList<AccStat>()
    val dups = ArrayList<Dup>()
    val sug = ArrayList<Node>()
    val cnt = IntArray(4)
    val sz = LongArray(4)
    var sugSize = 0L
    var topFiles: List<Node> = emptyList()
    var topDirs: List<Node> = emptyList()
    var typeSizes: List<Pair<String, Long>> = emptyList()
    // قواعد التنظيف (الحسابات المؤقتة فقط): 0 أقدم من 180 يوم، 1 أقدم من سنة، 2 ≥1GB، 3 ≥10GB، 4 مجلدات فارغة، 5 ملفات مؤقتة
    val rules = Array(6) { ArrayList<Node>() }
    var tempHasDates = false

    private class Scan {
        var big: Node? = null
        val ext = HashMap<String, Int>()
    }

    private fun hasAnc(n: Node, set: Set<Node>): Boolean {
        var p = n.parent
        while (p != null) {
            if (set.contains(p)) return true
            p = p.parent
        }
        return false
    }

    private fun scan(n: Node, s: Scan) {
        for (k in n.kids) {
            if (k.isDir) scan(k, s)
            else {
                val b = s.big
                if (b == null || k.size > b.size) s.big = k
                val e = if (k.type.isEmpty()) "—" else k.type
                s.ext[e] = (s.ext[e] ?: 0) + 1
            }
        }
    }

    private val junk = setOf("tmp", "log", "bak", "cache", "temp", "part", "crdownload", "ds_store")

    private fun ruleWalk(n: Node, now: Long) {
        val day = 86400000L
        val gb = 1073741824L
        for (k in n.kids) {
            if (k.isDir) {
                if (k.kids.isEmpty()) rules[4].add(k) else ruleWalk(k, now)
            } else {
                if (k.time > 0) {
                    tempHasDates = true
                    if (now - k.time > 180L * day) rules[0].add(k)
                    if (now - k.time > 365L * day) rules[1].add(k)
                }
                if (k.size >= gb) rules[2].add(k)
                if (k.size >= 10L * gb) rules[3].add(k)
                if (junk.contains(k.type)) rules[5].add(k)
            }
        }
    }

    fun run() {
        val allFiles = ArrayList<Node>()
        val allDirs = ArrayList<Node>()
        val accNodes = ArrayList<Node>()
        fun walk(n: Node) {
            for (k in n.kids) {
                if (k.isDir) {
                    if (Acc.kind(k.name) != 0) accNodes.add(k) else allDirs.add(k)
                    walk(k)
                } else allFiles.add(k)
            }
        }
        walk(root)

        // --- المجلدات المكررة (الاسم + عدد الملفات + الحجم)
        val fg = HashMap<String, ArrayList<Node>>()
        for (d in allDirs) if (d.files > 0)
            fg.getOrPut(d.name.lowercase() + "|" + d.files + "|" + d.size) { ArrayList() }.add(d)
        val dupDirs = HashSet<Node>()
        for (v in fg.values) if (v.size > 1) dupDirs.addAll(v)
        for (v in fg.values) if (v.size > 1 && !v.all { hasAnc(it, dupDirs) }) dups.add(Dup(true, v))

        // --- الملفات المكررة: وهمية = الاسم + الحجم | حقيقية = بصمة SHA-256
        val ffg = HashMap<String, ArrayList<Node>>()
        val realBySize = HashMap<Long, ArrayList<Node>>()
        for (f in allFiles) {
            if (f.doc != null && cr != null) {
                if (f.size > 0) realBySize.getOrPut(f.size) { ArrayList() }.add(f)
            } else ffg.getOrPut(f.name.lowercase() + "|" + f.size) { ArrayList() }.add(f)
        }
        for (v in ffg.values) if (v.size > 1 && !v.all { hasAnc(it, dupDirs) }) dups.add(Dup(false, v))
        for (v in realBySize.values) {
            if (v.size < 2) continue
            val byHash = HashMap<String, ArrayList<Node>>()
            for (n in v) {
                val h = Real.sha256(cr!!, n) ?: continue
                byHash.getOrPut(h) { ArrayList() }.add(n)
            }
            for (g in byHash.values) if (g.size > 1 && !g.all { hasAnc(it, dupDirs) }) dups.add(Dup(false, g))
        }
        dups.sortByDescending { it.wasted }

        // --- المقترح حذفه: فقط من الحسابات المؤقتة
        val sset = HashSet<Node>()
        val ordered = dups.sortedWith(compareBy<Dup>({ !it.isDir }, { -it.wasted }))
        for (g in ordered) {
            val temps = g.items.filter { val a = it.account(); a != null && Acc.kind(a.name) == 2 }
            if (temps.isEmpty()) continue
            val del = if (temps.size < g.items.size) temps else temps.drop(1)
            for (n in del) if (!hasAnc(n, sset)) { sset.add(n); g.sugg.add(n) }
        }
        for (g in dups) g.sugg.removeAll { hasAnc(it, sset) }
        for (g in dups) for (n in g.sugg) sug.add(n)
        val sugAcc = HashMap<Node, ArrayList<Node>>()
        for (n in sug) {
            val a = n.account()
            if (a != null) sugAcc.getOrPut(a) { ArrayList() }.add(n)
        }
        val dupCnt = HashMap<Node, Int>()
        for (g in dups) for (n in g.items) {
            val a = n.account()
            if (a != null) dupCnt[a] = (dupCnt[a] ?: 0) + 1
        }

        // --- إحصائيات الحسابات
        val now = System.currentTimeMillis()
        for (a in accNodes) {
            val k = Acc.kind(a.name)
            val st = AccStat(a, k)
            val sc = Scan()
            scan(a, sc)
            st.bigFile = sc.big
            st.bigDir = a.kids.filter { it.isDir }.maxByOrNull { it.size }
            st.dupCount = dupCnt[a] ?: 0
            val l = sugAcc[a]
            st.sugCount = l?.size ?: 0
            st.sugSize = l?.sumOf { it.size } ?: 0L
            st.exts = sc.ext.entries.sortedByDescending { it.value }.take(3).map { Pair(it.key, it.value) }
            accs.add(st)
            cnt[k]++
            sz[k] += a.size
            if (k == 2) ruleWalk(a, now)
        }
        sugSize = sug.sumOf { it.size }

        // --- الأكبر والأنواع
        topFiles = allFiles.sortedByDescending { it.size }.take(100)
        topDirs = allDirs.sortedByDescending { it.size }.take(50)
        val ts = HashMap<String, Long>()
        for (f in allFiles) {
            val e = if (f.type.isEmpty()) "—" else f.type
            ts[e] = (ts[e] ?: 0L) + f.size
        }
        typeSizes = ts.entries.sortedByDescending { it.value }.take(12).map { Pair(it.key, it.value) }
    }
}
