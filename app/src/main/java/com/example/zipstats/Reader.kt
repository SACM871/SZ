package com.example.zipstats

import android.content.ContentResolver
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class Reader(private val cr: ContentResolver) {

    fun read(uri: Uri): Node {
        val head = ByteArray(2)
        cr.openInputStream(uri)!!.use { it.read(head) }
        return if (head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()) zipTree(uri) else jsonTree(uri)
    }

    // ---------- JSON: متداخل / قاموس / قائمة مسطحة بـ path ----------
    private val nameKeys = listOf("name", "server_filename", "filename", "file_name", "title", "folder_name", "path")
    private val kidKeys = listOf("children", "items", "contents", "files", "folders", "entries", "list")
    private val sizeKeys = listOf("size", "file_size", "bytes", "length")

    private fun nameOf(o: JSONObject): String? {
        for (k in nameKeys) {
            val v = o.opt(k)
            if (v is String && v.isNotEmpty()) return if (k == "path") v.trimEnd('/').substringAfterLast('/') else v
        }
        return null
    }

    private fun kidsOf(o: JSONObject): Any? {
        for (k in kidKeys) {
            val v = o.opt(k)
            if (v is JSONArray || v is JSONObject) return v
        }
        return null
    }

    private fun sizeOf(o: JSONObject): Long {
        for (k in sizeKeys) {
            val v = o.opt(k)
            if (v is Number) return v.toLong()
            if (v is String) { val l = v.toLongOrNull(); if (l != null) return l }
        }
        return 0
    }

    private fun isDirFlag(o: JSONObject): Boolean? {
        for (k in listOf("isdir", "is_dir", "isDir", "is_folder", "dir")) {
            val v = o.opt(k)
            if (v is Boolean) return v
            if (v is Number) return v.toInt() != 0
        }
        val t = o.optString("type", o.optString("category", "")).lowercase()
        if (t == "folder" || t == "directory" || t == "dir") return true
        if (t == "file") return false
        return null
    }


    private val timeKeys = listOf("server_mtime", "mtime", "local_mtime", "modified", "modified_time",
        "last_modified", "lastModified", "updated_at", "server_ctime", "ctime", "created_at", "time")

    private fun timeOf(o: JSONObject): Long {
        for (k in timeKeys) {
            val v = o.opt(k)
            if (v is Number) return normTime(v.toLong())
            if (v is String) {
                val l = v.toLongOrNull()
                if (l != null) return normTime(l)
                for (p in listOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd")) {
                    try {
                        val d = java.text.SimpleDateFormat(p, java.util.Locale.US).parse(v)
                        if (d != null) return d.time
                    } catch (e: Exception) { }
                }
            }
        }
        return 0L
    }

    private fun normTime(v: Long): Long = if (v in 1..99999999999L) v * 1000 else if (v > 0) v else 0L

    private fun dosTime(t: Int, d: Int): Long {
        val y = 1980 + ((d shr 9) and 0x7f)
        val mo = (d shr 5) and 0xf
        val day = d and 0x1f
        if (mo < 1 || mo > 12 || day < 1) return 0L
        val c = java.util.Calendar.getInstance()
        c.clear()
        c.set(y, mo - 1, day, (t shr 11) and 0x1f, (t shr 5) and 0x3f, (t and 0x1f) * 2)
        return c.timeInMillis
    }

    private fun isFs(o: JSONObject): Boolean =
        nameOf(o) != null && (kidsOf(o) != null || isDirFlag(o) != null || sizeKeys.any { o.has(it) })

    private fun jsonTree(uri: Uri): Node {
        val text = cr.openInputStream(uri)!!.use { String(it.readBytes(), Charsets.UTF_8) }
        val t = text.trim().removePrefix("\uFEFF")
        val v = JSONTokener(t).nextValue()
        val r = Node("/", true)
        when (v) {
            is JSONArray -> convArray(v, r)
            is JSONObject -> {
                if (isFs(v)) {
                    val kk = kidsOf(v)
                    if (v.optString("name") == "/" && kk != null) {
                        if (kk is JSONArray) convArray(kk, r) else if (kk is JSONObject) convDict(kk, r)
                    } else fsNode(v, r)
                } else convDict(v, r)
            }
        }
        recalc(r)
        return r
    }

    private fun fsNode(o: JSONObject, parent: Node) {
        val nm = nameOf(o) ?: return
        val kids = kidsOf(o)
        val isDir = isDirFlag(o) ?: (kids != null)
        if (isDir) {
            val d = Node(nm, true)
            parent.add(d)
            if (kids is JSONArray) convArray(kids, d) else if (kids is JSONObject) convDict(kids, d)
        } else { val f = Node(nm, false, sizeOf(o), extOf(nm)); f.time = timeOf(o); parent.add(f) }
    }

    private fun convDict(o: JSONObject, dir: Node) {
        val keys = o.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = o.opt(k)
            when (v) {
                is JSONObject -> {
                    if (isFs(v)) fsNode(v, dir)
                    else { val d = Node(k, true); dir.add(d); convDict(v, d) }
                }
                is JSONArray -> { val d = Node(k, true); dir.add(d); convArray(v, d) }
                is Number -> dir.add(Node(k, false, v.toLong(), extOf(k)))
                else -> dir.add(Node(k, false, 0, extOf(k)))
            }
        }
    }

    private fun convArray(a: JSONArray, dir: Node) {
        if (looksFlat(a)) { convFlat(a, dir); return }
        for (i in 0 until a.length()) {
            val v = a.opt(i)
            when (v) {
                is JSONObject -> {
                    if (isFs(v)) fsNode(v, dir)
                    else { val d = Node("[$i]", true); dir.add(d); convDict(v, d) }
                }
                is JSONArray -> { val d = Node("[$i]", true); dir.add(d); convArray(v, d) }
                is String -> dir.add(Node(v, false, 0, extOf(v)))
                else -> dir.add(Node("[$i]", false, (v as? Number)?.toLong() ?: 0L, ""))
            }
        }
    }

    private fun looksFlat(a: JSONArray): Boolean {
        val n = minOf(a.length(), 20)
        if (n == 0) return false
        for (i in 0 until n) {
            val o = a.optJSONObject(i) ?: return false
            val p = o.optString("path", "")
            if (!p.startsWith("/") || kidsOf(o) != null) return false
        }
        return true
    }

    private fun convFlat(a: JSONArray, base: Node) {
        val idx = HashMap<String, Node>()
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            val segs = o.optString("path", "").split("/").filter { it.isNotEmpty() }
            if (segs.isEmpty()) continue
            val isDir = isDirFlag(o) ?: false
            var cur = base
            var acc = ""
            for ((j, s) in segs.withIndex()) {
                if (j == segs.size - 1 && !isDir) {
                    val f = Node(s, false, sizeOf(o), extOf(s)); f.time = timeOf(o); cur.add(f)
                } else {
                    acc += "/" + s
                    var d = idx[acc]
                    if (d == null) { d = Node(s, true); idx[acc] = d; cur.add(d) }
                    cur = d
                }
            }
        }
    }

    // ---------- ZIP: يقرأ الفهرس فقط ويدعم Zip64 ----------
    private fun readAt(ch: FileChannel, pos: Long, n: Int): ByteBuffer {
        val b = ByteBuffer.allocate(n).order(ByteOrder.LITTLE_ENDIAN)
        while (b.hasRemaining()) {
            val r = ch.read(b, pos + b.position())
            if (r < 0) break
        }
        return b
    }

    private fun zipTree(uri: Uri): Node {
        val M = 0xFFFFFFFFL
        val r = Node("/", true)
        val dirMap = HashMap<String, Node>()
        cr.openFileDescriptor(uri, "r")!!.use { pfd ->
            FileInputStream(pfd.fileDescriptor).channel.use { ch ->
                val len = ch.size()
                val tailLen = minOf(len, 70000L).toInt()
                val tail = readAt(ch, len - tailLen, tailLen)
                var p = tailLen - 22
                while (p >= 0 && tail.getInt(p) != 0x06054b50) p--
                if (p < 0) throw Exception("ZIP index not found")
                var cdSize = tail.getInt(p + 12).toLong() and M
                var cdOff = tail.getInt(p + 16).toLong() and M
                if (cdSize == M || cdOff == M || (tail.getShort(p + 10).toInt() and 0xFFFF) == 0xFFFF) {
                    val eocdPos = len - tailLen + p
                    val loc = readAt(ch, eocdPos - 20, 20)
                    val z64 = readAt(ch, loc.getLong(8), 56)
                    cdSize = z64.getLong(40)
                    cdOff = z64.getLong(48)
                }
                val cd = readAt(ch, cdOff, cdSize.toInt())
                var o = 0
                while (o + 46 <= cd.capacity() && cd.getInt(o) == 0x02014b50) {
                    var usize = cd.getInt(o + 24).toLong() and M
                    val nl = cd.getShort(o + 28).toInt() and 0xFFFF
                    val el = cd.getShort(o + 30).toInt() and 0xFFFF
                    val cl = cd.getShort(o + 32).toInt() and 0xFFFF
                    val tmv = dosTime(cd.getShort(o + 12).toInt() and 0xFFFF, cd.getShort(o + 14).toInt() and 0xFFFF)
                    val nb = ByteArray(nl)
                    cd.position(o + 46)
                    cd.get(nb)
                    val name = String(nb, Charsets.UTF_8)
                    if (usize == M) {
                        var x = o + 46 + nl
                        val end = x + el
                        while (x + 4 <= end) {
                            val id = cd.getShort(x).toInt() and 0xFFFF
                            val sz = cd.getShort(x + 2).toInt() and 0xFFFF
                            if (id == 1) { usize = cd.getLong(x + 4); break }
                            x += 4 + sz
                        }
                    }
                    val isDir = name.endsWith("/")
                    val parts = name.trimEnd('/').split("/").filter { it.isNotEmpty() }
                    var cur = r
                    var acc = ""
                    for ((i, part) in parts.withIndex()) {
                        if (i == parts.size - 1 && !isDir) {
                            val f = Node(part, false, usize, extOf(part)); f.time = tmv; cur.add(f)
                        } else {
                            acc += "/" + part
                            var d = dirMap[acc]
                            if (d == null) { d = Node(part, true); dirMap[acc] = d; cur.add(d) }
                            cur = d
                        }
                    }
                    o += 46 + nl + el + cl
                }
            }
        }
        recalc(r)
        return r
    }
}
