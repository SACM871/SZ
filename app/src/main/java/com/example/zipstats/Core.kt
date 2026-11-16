package com.example.zipstats

import android.net.Uri
import java.util.Locale

// 1 = كامل "SAMA SA 04" | 2 = مؤقت "SAMASA28" | 3 = غير محدد
object Acc {
    private val full = Regex("^SAMA\\s+SA\\s+\\d+$", RegexOption.IGNORE_CASE)
    private val temp = Regex("^SAMASA\\d+$", RegexOption.IGNORE_CASE)
    private val loose = Regex("^SAMA\\s*SA\\s*\\d+$", RegexOption.IGNORE_CASE)
    fun kind(name: String): Int {
        val t = name.trim()
        return if (full.matches(t)) 1 else if (temp.matches(t)) 2 else if (loose.matches(t)) 3 else 0
    }
}

class Node(n0: String, val isDir: Boolean, var size: Long = 0, val type: String = "") {
    var name: String = n0.replace('/', '∕')
    var parent: Node? = null
    val kids = ArrayList<Node>()
    var files = 0
    var dirs = 0
    var time = 0L
    var doc: Uri? = null

    fun add(k: Node) { k.parent = this; kids.add(k) }

    fun path(): String {
        if (parent == null) return "/"
        val l = ArrayList<String>()
        var n: Node? = this
        while (n != null && n.parent != null) { l.add(n.name); n = n.parent }
        l.reverse()
        return "/" + l.joinToString("/")
    }

    fun account(): Node? {
        var n: Node? = this
        while (n != null) {
            if (n.isDir && Acc.kind(n.name) != 0) return n
            n = n.parent
        }
        return null
    }
}

class Dup(val isDir: Boolean, val items: List<Node>) {
    val size: Long = items[0].size
    val wasted: Long = size * (items.size - 1)
    val sugg = ArrayList<Node>()
}

fun recalc(n: Node) {
    if (!n.isDir) return
    var s = 0L; var f = 0; var d = 0; var tm = 0L
    for (k in n.kids) {
        if (k.isDir) { recalc(k); s += k.size; f += k.files; d += k.dirs + 1 }
        else { s += k.size; f++ }
        if (k.time > tm) tm = k.time
    }
    n.size = s; n.files = f; n.dirs = d; n.time = tm
}

fun extOf(n: String): String {
    val i = n.lastIndexOf('.')
    return if (i > 0 && i < n.length - 1) n.substring(i + 1).lowercase() else ""
}

// أرقام إنجليزية دائماً (حتى في الواجهة العربية)
fun fmtSize(b: Long): String {
    if (b < 1024) return "$b B"
    val u = arrayOf("KB", "MB", "GB", "TB", "PB")
    var v = b.toDouble()
    var i = -1
    while (v >= 1024 && i < u.size - 1) { v /= 1024; i++ }
    return String.format(Locale.US, "%.2f %s", v, u[i])
}

fun num(n: Long): String = String.format(Locale.US, "%,d", n)
fun num(n: Int): String = num(n.toLong())

// ================= النصوص: عربي / English =================
object L {
    var lang = "ar"

    private val m: Map<String, List<String>> = mapOf(
        "app_sub" to listOf("مدير الحسابات والملفات", "Accounts & Files Manager"),
        "lang_btn" to listOf("EN", "عربي"),
        "sec_home" to listOf("🏠 الرئيسية", "🏠 Home"),
        "sec_files" to listOf("📂 الملفات", "📂 Files"),
        "sec_accounts" to listOf("🏷️ الحسابات", "🏷️ Accounts"),
        "sec_dups" to listOf("♊ المكرر", "♊ Duplicates"),
        "sec_stats" to listOf("📊 الإحصائيات", "📊 Statistics"),
        "sec_favs" to listOf("⭐ المفضلة", "⭐ Favorites"),
        "sec_search" to listOf("🔍 بحث", "🔍 Search"),
        "sec_tools" to listOf("⚙️ الأدوات", "⚙️ Tools"),
        "sub_0" to listOf("نظرة عامة سريعة على ملفك وحساباتك", "Quick overview of your file and accounts"),
        "sub_1" to listOf("", ""),
        "sub_2" to listOf("تفاصيل كل حساب: كامل أو مؤقت", "Details of every account: full or temporary"),
        "sub_3" to listOf("فحص الملفات والمجلدات المكررة واقتراح الحذف", "Find duplicate files/folders and get cleanup suggestions"),
        "sub_4" to listOf("الأكبر حجماً وتوزيع الأنواع", "Largest items and type breakdown"),
        "sub_5" to listOf("وصول سريع لمجلداتك المهمة", "Quick access to your important folders"),
        "sub_6" to listOf("ابحث بالاسم في كل الهيكل", "Search by name across the whole structure"),
        "sub_7" to listOf("فتح، حفظ، تقارير، اللغة", "Open, save, reports, language"),
        "open" to listOf("فتح", "Open"),
        "close" to listOf("إغلاق", "Close"),
        "cancel" to listOf("إلغاء", "Cancel"),
        "save" to listOf("حفظ", "Save"),
        "back" to listOf("رجوع", "Back"),
        "empty" to listOf("(فارغ)", "(empty)"),
        "busy" to listOf("انتظر انتهاء العملية الحالية", "Please wait for the current task"),
        "error" to listOf("خطأ", "Error"),
        "reading" to listOf("جاري القراءة والتحليل...", "Reading and analyzing..."),
        "analyzing" to listOf("جاري التحليل...", "Analyzing..."),
        "saving" to listOf("جاري حفظ الهيكل...", "Saving structure..."),
        "saving_report" to listOf("جاري إعداد التقرير...", "Preparing report..."),
        "deleting" to listOf("جاري الحذف...", "Deleting..."),
        "pasting" to listOf("جاري اللصق...", "Pasting..."),
        "hero_title" to listOf("افتح ملف للبدء", "Open a file to start"),
        "hero_sub" to listOf("terabox_structure.json أو ملف ZIP", "terabox_structure.json or a ZIP file"),
        "tip_title" to listOf("نصيحة", "Tip"),
        "tip_sub" to listOf("الملفات والمجلدات تُحلَّل بالكامل وتُعرض في الأقسام أعلاه.", "Everything is analyzed and shown in the sections above."),
        "loaded" to listOf("الملف المحمّل", "Loaded file"),
        "switch_file" to listOf("تبديل الملف", "Switch File"),
        "open_folder" to listOf("فتح مجلد", "Open Folder"),
        "storage_overview" to listOf("نظرة عامة على التخزين", "Storage Overview"),
        "storage_overview_sub" to listOf("ملخص فعلي للحجم والملفات والحسابات", "Live summary of size, files and accounts"),
        "reclaim_alert" to listOf("تنبيه استرجاع المساحة", "Reclaim Alert"),
        "reclaim_sub" to listOf("مساحة قابلة للاسترجاع من العناصر المقترحة للتنظيف", "Recoverable space from suggested cleanup items"),
        "review_clean" to listOf("مراجعة وتنظيف", "Review & Clean"),
        "quick_scan" to listOf("فحص سريع لمجلد حقيقي", "Quick Scan Real Device Folder"),
        "total_files" to listOf("إجمالي الملفات", "Total files"),
        "total_dirs" to listOf("إجمالي المجلدات", "Total folders"),
        "total_size" to listOf("الحجم الكلي", "Total size"),
        "overview_metrics" to listOf("ملخص التخزين", "Storage overview"),
        "terabox_title" to listOf("TeraBox", "TeraBox"),
        "terabox_sub" to listOf("مدخل سحابي جاهز للتكامل — بدون اتصال فعلي حالياً", "Cloud entry point ready for integration — no live connection yet"),
        "terabox_msg" to listOf(
            "TeraBox موجود كواجهة تمهيدية فقط. لا يوجد تسجيل دخول أو اتصال أو إدارة ملفات سحابية مفعّلة في هذه النسخة.",
            "TeraBox is a visual entry point only. Login, live connection, and cloud file management are not enabled in this build."
        ),
        "kind_1" to listOf("✅ كامل", "✅ Full"),
        "kind_2" to listOf("🕒 مؤقت", "🕒 Temporary"),
        "kind_3" to listOf("❓ غير محدد", "❓ Unclassified"),
        "tag_1" to listOf("كامل", "Full"),
        "tag_2" to listOf("مؤقت", "Temp"),
        "tag_3" to listOf("؟", "?"),
        "accounts_word" to listOf("حسابات", "accounts"),
        "cleanup_sub" to listOf("يمكن تحريرها بحذف {0} عنصر مكرر من الحسابات المؤقتة", "Can be freed by removing {0} duplicate items from temporary accounts"),
        "cleanup_title" to listOf("مساحة قابلة للتوفير", "Recoverable space"),
        "review" to listOf("مراجعة", "Review"),
        "rep_title" to listOf("تصدير تقرير الحسابات", "Export accounts report"),
        "rep_sub" to listOf("ملف نصي فيه كل حساب والمقترح حذفه", "Text file with every account and suggested deletions"),
        "json_title" to listOf("حفظ الهيكل المعدّل (JSON)", "Save edited structure (JSON)"),
        "json_sub" to listOf("يحفظ تعديلاتك في ملف جديد", "Saves your edits to a new file"),
        "sort_row" to listOf("الترتيب", "Sort"),
        "sort_0" to listOf("بالاسم", "By name"),
        "sort_1" to listOf("بالحجم (الأكبر أولاً)", "By size (largest first)"),
        "sort_2" to listOf("بعدد الملفات", "By file count"),
        "dir_sub" to listOf("{0} ملف • {1}", "{0} files • {1}"),
        "no_acc" to listOf("ما لقيت حسابات", "No accounts found"),
        "no_acc_sub" to listOf("أسماء المجلدات المطلوبة: SAMA SA 04 أو SAMASA28", "Expected folder names: SAMA SA 04 or SAMASA28"),
        "acc_summary" to listOf("ملخص الحسابات", "Accounts summary"),
        "acc_l1" to listOf("📄 {0} ملف • 📁 {1} مجلد • 💾 {2}", "📄 {0} files • 📁 {1} folders • 💾 {2}"),
        "acc_big_file" to listOf("🔝 أكبر ملف: {0} ({1})", "🔝 Largest file: {0} ({1})"),
        "acc_big_dir" to listOf("🔝 أكبر مجلد: {0} ({1})", "🔝 Largest folder: {0} ({1})"),
        "acc_dups" to listOf("♊ عناصر مكررة: {0}", "♊ Duplicate items: {0}"),
        "acc_sug" to listOf("🗑️ مقترح حذفه: {0} عنصر ({1}) ← يتبقى {2}", "🗑️ Suggested removal: {0} items ({1}) → remaining {2}"),
        "sum_lines" to listOf("♊ مجموعات مكررة: {0}\n🗑️ مقترح حذفه من المؤقتة: {1} عنصر ({2})", "♊ Duplicate groups: {0}\n🗑️ Suggested removal from temp: {1} items ({2})"),
        "dup_header" to listOf("♊ مجموعات مكررة: {0} (📁 {1} | 📄 {2})", "♊ Duplicate groups: {0} (📁 {1} | 📄 {2})"),
        "dup_header_sub" to listOf("🗑️ مقترح حذفه من المؤقتة: {0} عنصر ({1})\n⚠️ المقارنة بالاسم + الحجم. الحسابات الكاملة لا يُقترح الحذف منها أبداً.", "🗑️ Suggested removal from temp: {0} items ({1})\n⚠️ Matching is by name + size. Full accounts are never suggested for deletion."),
        "apply_all" to listOf("طبّق كل الحذف المقترح", "Apply all suggested deletions"),
        "apply_all_sub" to listOf("{0} عنصر • {1}", "{0} items • {1}"),
        "per_copy" to listOf("{0} للنسخة • هدر {1}", "{0} per copy • wasted {1}"),
        "dup_more" to listOf("يظهر أكبر 400 مجموعة فقط (التقرير فيه كل المقترح)", "Showing top 400 groups only (the report has everything)"),
        "no_dups" to listOf("لا يوجد مكرر 🎉", "No duplicates 🎉"),
        "types_title" to listOf("توزيع الأنواع حسب الحجم", "Type breakdown by size"),
        "top_accs" to listOf("أكبر الحسابات", "Largest accounts"),
        "top_files" to listOf("أكبر {0} ملف", "Top {0} files"),
        "top_dirs" to listOf("أكبر {0} مجلد", "Top {0} folders"),
        "favs_title" to listOf("المفضلة ({0})", "Favorites ({0})"),
        "favs_hint" to listOf("اضغط مطولاً على أي عنصر في «الملفات» لإضافته", "Long-press any item in Files to add it"),
        "fav_missing" to listOf("غير موجود بالهيكل الحالي (اضغط لإزالته)", "Not in the current structure (tap to remove)"),
        "fav_remove_q" to listOf("إزالة من المفضلة؟", "Remove from favorites?"),
        "fav_add" to listOf("⭐ إضافة للمفضلة", "⭐ Add to favorites"),
        "fav_rem" to listOf("☆ إزالة من المفضلة", "☆ Remove from favorites"),
        "fav_added" to listOf("أُضيف للمفضلة ⭐", "Added to favorites ⭐"),
        "fav_removed" to listOf("أُزيل من المفضلة", "Removed from favorites"),
        "remove" to listOf("إزالة", "Remove"),
        "search_hint" to listOf("اكتب اسم ملف أو مجلد...", "Type a file or folder name..."),
        "search_btn" to listOf("بحث", "Search"),
        "search_empty" to listOf("اكتب كلمة واضغط بحث", "Type a word and press Search"),
        "search_res" to listOf("{0} نتيجة لـ «{1}»", "{0} results for “{1}”"),
        "search_none" to listOf("لا نتائج", "No results"),
        "m_details" to listOf("ℹ️ تفاصيل", "ℹ️ Details"),
        "m_rename" to listOf("✏️ إعادة تسمية", "✏️ Rename"),
        "m_copy" to listOf("📋 نسخ", "📋 Copy"),
        "m_cut" to listOf("✂️ نقل", "✂️ Move"),
        "m_delete" to listOf("🗑️ حذف", "🗑️ Delete"),
        "d_path" to listOf("المسار", "Path"),
        "d_type" to listOf("النوع", "Type"),
        "d_folder" to listOf("مجلد", "Folder"),
        "d_file" to listOf("ملف", "File"),
        "d_size" to listOf("الحجم", "Size"),
        "d_counts" to listOf("الملفات: {0} • المجلدات: {1}", "Files: {0} • Folders: {1}"),
        "d_ext" to listOf("الامتداد", "Extension"),
        "d_acc" to listOf("الحساب", "Account"),
        "rename_title" to listOf("إعادة تسمية", "Rename"),
        "invalid_name" to listOf("اسم غير صالح", "Invalid name"),
        "exists" to listOf("الاسم موجود مسبقاً", "Name already exists"),
        "del_title" to listOf("حذف", "Delete"),
        "del_q" to listOf("حذف «{0}» ({1}) من الهيكل؟", "Delete “{0}” ({1}) from the structure?"),
        "delete" to listOf("حذف", "Delete"),
        "apply_title" to listOf("تطبيق الحذف المقترح", "Apply suggested deletions"),
        "apply_msg" to listOf("سيُحذف {0} عنصر ({1}) من الحسابات المؤقتة فقط.\nالملف الأصلي لا يتغير إلا إذا حفظت JSON جديد.", "{0} items ({1}) will be removed from temporary accounts only.\nThe original file is unchanged unless you save a new JSON."),
        "apply" to listOf("طبّق", "Apply"),
        "copied" to listOf("تم النسخ — افتح المجلد الهدف في «الملفات» واضغط لصق", "Copied — open the target folder in Files and tap Paste"),
        "cut" to listOf("تم القص — افتح المجلد الهدف في «الملفات» واضغط لصق", "Cut — open the target folder in Files and tap Paste"),
        "paste_bar" to listOf("📋 الصق هنا ({0}): {1}", "📋 Paste here ({0}): {1}"),
        "w_copy" to listOf("نسخ", "copy"),
        "w_move" to listOf("نقل", "move"),
        "into_self" to listOf("لا يمكن نقل المجلد إلى داخل نفسه", "Cannot move a folder into itself"),
        "copy_w" to listOf("نسخة", "copy"),
        "saved_json" to listOf("تم حفظ الهيكل المعدّل ✅", "Edited structure saved ✅"),
        "saved_report" to listOf("تم حفظ التقرير ✅", "Report saved ✅"),
        "deleted" to listOf("تم الحذف من الهيكل ✅", "Deleted from the structure ✅"),
        "gone" to listOf("العنصر لم يعد موجوداً", "Item no longer exists"),
        "keep_mark" to listOf("✅ يُبقى: ", "✅ Keep: "),
        "del_mark" to listOf("🗑️ يُحذف: ", "🗑️ Delete: "),
        "and_more" to listOf("... و{0} أخرى", "... and {0} more"),
        "del_sugg_btn" to listOf("🗑️ احذف المقترح ({0})", "🗑️ Delete suggested ({0})"),
        "copies_n" to listOf("{0} للنسخة • {1} نسخ", "{0} per copy • {1} copies"),
        "tool_lang" to listOf("اللغة / Language", "Language / اللغة"),
        "tool_lang_sub" to listOf("العربية ⇄ English (الأرقام إنجليزية دائماً)", "English ⇄ العربية"),
        "tool_open_sub" to listOf("اختر JSON أو ZIP", "Pick a JSON or ZIP"),
        "tool_about" to listOf("حول التطبيق وملاحظات", "About & notes"),
        "tool_about_sub" to listOf("كيف يعمل وكيف تُحسب الأشياء", "How it works"),
        "about_msg" to listOf(
            "• التعديل والنقل والنسخ والحذف تتم على الهيكل المحمّل فقط، ولا تلمس السحابة ولا الملف الأصلي.\n• احفظ تعديلاتك بـ «حفظ JSON».\n• الحسابات الكاملة (SAMA SA 04) لا يُقترح الحذف منها أبداً.\n• المكرر = نفس الاسم + نفس الحجم (للمجلدات: + عدد الملفات).\n• الأرقام إنجليزية في كل الشاشات.",
            "• Edit/move/copy/delete work on the loaded structure only; the cloud and the original file are untouched.\n• Save your edits with “Save JSON”.\n• Full accounts (SAMA SA 04) are never suggested for deletion.\n• Duplicate = same name + same size (folders: + file count).\n• Numbers are always shown in English digits."
        )
    )

    fun t(k: String, vararg a: Any): String {
        val v = m[k] ?: L2.m[k] ?: return k
        var s = if (lang == "ar") v[0] else v[1]
        for ((i, x) in a.withIndex()) s = s.replace("{$i}", x.toString())
        return s
    }
}
