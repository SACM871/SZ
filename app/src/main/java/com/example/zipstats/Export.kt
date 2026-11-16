package com.example.zipstats

import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Export {
    private fun q(s: String): String = "\"" + s.replace("\"", "\"\"") + "\""

    private fun kindName(k: Int): String = when (k) { 1 -> "Full"; 2 -> "Temporary"; 3 -> "Unclassified"; else -> "" }

    fun filesCsv(w: Writer, root: Node) {
        w.write("\uFEFF")
        w.write("path,name,extension,size_bytes,account,account_type,modified\n")
        val fm = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        fun walk(n: Node) {
            for (k in n.kids) {
                if (k.isDir) walk(k)
                else {
                    val a = k.account()
                    val kind = if (a == null) "" else kindName(Acc.kind(a.name))
                    val tm = if (k.time > 0) fm.format(Date(k.time)) else ""
                    w.write(q(k.path()) + "," + q(k.name) + "," + q(k.type) + "," + k.size + "," +
                        q(a?.name ?: "") + "," + kind + "," + tm + "\n")
                }
            }
        }
        walk(root)
        w.flush()
    }

    fun accountsCsv(w: Writer, a: Analysis) {
        w.write("\uFEFF")
        w.write("account,type,files,folders,size_bytes,largest_file,largest_file_bytes,duplicate_items,suggested_delete_items,suggested_delete_bytes\n")
        for (s in a.accs) {
            val bf = s.bigFile
            w.write(q(s.node.name) + "," + kindName(s.kind) + "," + s.node.files + "," + s.node.dirs + "," +
                s.node.size + "," + q(bf?.name ?: "") + "," + (bf?.size ?: 0L) + "," + s.dupCount + "," +
                s.sugCount + "," + s.sugSize + "\n")
        }
        w.flush()
    }
}
