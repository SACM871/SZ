package com.example.zipstats

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.security.MessageDigest

// ملفات الجهاز الحقيقية عبر Storage Access Framework
object Real {
    fun scan(cr: ContentResolver, tree: Uri): Node {
        val id = DocumentsContract.getTreeDocumentId(tree)
        val r = Node("/", true)
        r.doc = DocumentsContract.buildDocumentUriUsingTree(tree, id)
        scanDir(cr, tree, r)
        recalc(r)
        return r
    }

    fun scanDir(cr: ContentResolver, tree: Uri, node: Node) {
        val d = node.doc ?: return
        node.kids.clear()
        val id = DocumentsContract.getDocumentId(d)
        val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, id)
        val cols = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        val subDirs = ArrayList<Node>()
        cr.query(childUri, cols, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val cid = c.getString(0) ?: continue
                val name = c.getString(1) ?: continue
                val isDir = c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR
                val sz = if (isDir || c.isNull(3)) 0L else c.getLong(3)
                val n = Node(name, isDir, sz, if (isDir) "" else extOf(name))
                if (!c.isNull(4)) n.time = c.getLong(4)
                n.doc = DocumentsContract.buildDocumentUriUsingTree(tree, cid)
                node.add(n)
                if (isDir) subDirs.add(n)
            }
        }
        for (s in subDirs) scanDir(cr, tree, s)
    }

    fun delete(cr: ContentResolver, n: Node): Boolean = try {
        val d = n.doc
        d != null && DocumentsContract.deleteDocument(cr, d)
    } catch (e: Exception) { false }

    fun rename(cr: ContentResolver, n: Node, name: String): Uri? = try {
        val d = n.doc
        if (d == null) null else DocumentsContract.renameDocument(cr, d, name)
    } catch (e: Exception) { null }

    fun copy(cr: ContentResolver, n: Node, dest: Node): Uri? = try {
        val s = n.doc
        val p = dest.doc
        if (s == null || p == null) null else DocumentsContract.copyDocument(cr, s, p)
    } catch (e: Exception) { null }

    fun move(cr: ContentResolver, n: Node, from: Node, to: Node): Uri? = try {
        val s = n.doc
        val a = from.doc
        val b = to.doc
        if (s == null || a == null || b == null) null else DocumentsContract.moveDocument(cr, s, a, b)
    } catch (e: Exception) { null }

    // بصمة المحتوى الحقيقية لكشف المكرر بدقة
    fun sha256(cr: ContentResolver, n: Node): String? {
        try {
            val d = n.doc ?: return null
            cr.openInputStream(d)?.use { ins ->
                val md = MessageDigest.getInstance("SHA-256")
                val buf = ByteArray(65536)
                var r = ins.read(buf)
                while (r > 0) { md.update(buf, 0, r); r = ins.read(buf) }
                return md.digest().joinToString("") { b -> "%02x".format(b) }
            }
        } catch (e: Exception) { }
        return null
    }
}
