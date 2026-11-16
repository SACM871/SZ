package com.example.zipstats

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.JsonWriter
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import java.io.OutputStreamWriter
import java.util.Locale

// حالة تبقى حية عند تغيير اللغة أو الثيم
object State {
    var root = Node("/", true)
    val stack = ArrayList<Node>()
    var mode = 0
    var an: Analysis? = null
    var fileName = ""
    val clips = ArrayList<Node>()
    var clipMove = false
    var sortMode = 0
    var query = ""
    var dark = false
    var selMode = false
    val sel = HashSet<Node>()
    var tree: Uri? = null
    var baseSnap: HashMap<String, Long>? = null
    var baseName = ""
    var diff: DiffResult? = null
}

// type: 0 عادي | 1 رجوع | 2 معلومات | 4 مفضلة مفقودة | 5 إجراء
class Row(
    val title: String,
    val sub: String = "",
    val icon: String = "",
    val color: Int = 0,
    val node: Node? = null,
    val type: Int = 0,
    val dup: Dup? = null,
    val fav: String? = null,
    val act: Int = 0,
    val bar: Float = -1f,
    val tag: String = "",
    val pie: List<Pair<String, Long>>? = null,
    val pieColors: List<Int>? = null,
    val selected: Boolean = false,
    val metrics: List<Pair<String, String>>? = null,
    val iconRes: Int = 0
)

class MergeStats {
    var moved = 0
    var skipped = 0
    var renamed = 0
}

class MainActivity : Activity() {
    companion object {
        const val HOME = 0
        const val FILES = 1
        const val ACCS = 2
        const val DUPS = 3
        const val RULES = 4
        const val CMP = 5
        const val STATS = 6
        const val FAVS = 7
        const val SEARCH = 8
        const val TOOLS = 9
    }

    private val secColors = listOf("#2563EB", "#0EA5E9", "#10B981", "#F59E0B", "#EF4444",
        "#8B5CF6", "#EC4899", "#F43F5E", "#14B8A6", "#6366F1").map { Color.parseColor(it) }
    private val secKeys = listOf("sec_home", "sec_files", "sec_accounts", "sec_dups", "sec_rules",
        "sec_cmp", "sec_stats", "sec_favs", "sec_search", "sec_tools")
    private val secIcons = intArrayOf(
        R.drawable.ic_home, R.drawable.ic_folder, R.drawable.ic_account,
        R.drawable.ic_duplicates, R.drawable.ic_rules, R.drawable.ic_compare,
        R.drawable.ic_stats, R.drawable.ic_favorite, R.drawable.ic_search, R.drawable.ic_tools
    )
    private val navModes = listOf(HOME, FILES, ACCS, DUPS, RULES)
    private val palette = listOf("#2563EB", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6", "#06B6D4", "#EC4899", "#94A3B8")
        .map { Color.parseColor(it) }
    private val cGreen = Color.parseColor("#10B981")
    private val cRose = Color.parseColor("#EF4444")
    private val cGray = Color.parseColor("#94A3B8")

    // Design tokens (surface / surface-bright / on-surface / outline / outline-variant / surface-container)
    private fun cBg(): Int = Color.parseColor(if (dark) "#0B1220" else "#F8F9FF")
    private fun cCard(): Int = Color.parseColor(if (dark) "#1E293B" else "#FFFFFF")
    private fun cInk(): Int = Color.parseColor(if (dark) "#F1F5F9" else "#0F172A")
    private fun cMuted(): Int = Color.parseColor(if (dark) "#94A3B8" else "#64748B")
    private fun cTop(): Int = Color.parseColor(if (dark) "#090D16" else "#0F172A")
    private fun cStroke(): Int = Color.parseColor(if (dark) "#334155" else "#CBD5E1")
    private fun cTrack(): Int = Color.parseColor(if (dark) "#1B273D" else "#E2E8F0")

    private lateinit var bannerTitle: TextView
    private lateinit var bannerSub: TextView
    private lateinit var banner: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var searchBar: LinearLayout
    private lateinit var searchEt: EditText
    private lateinit var selBar: LinearLayout
    private lateinit var selCount: TextView
    private lateinit var pasteBar: LinearLayout
    private lateinit var pasteText: TextView
    private lateinit var list: ListView
    private val chips = ArrayList<TextView>()
    private val navItems = ArrayList<LinearLayout>()

    private var busy = false
    private var shownRows: List<Row> = emptyList()
    private val favs = HashSet<String>()

    private var root: Node
        get() = State.root
        set(v) { State.root = v }
    private val stack: ArrayList<Node>
        get() = State.stack
    private var mode: Int
        get() = State.mode
        set(v) { State.mode = v }
    private var an: Analysis?
        get() = State.an
        set(v) { State.an = v }
    private var fileName: String
        get() = State.fileName
        set(v) { State.fileName = v }
    private val clips: ArrayList<Node>
        get() = State.clips
    private var clipMove: Boolean
        get() = State.clipMove
        set(v) { State.clipMove = v }
    private var sortMode: Int
        get() = State.sortMode
        set(v) { State.sortMode = v }
    private var query: String
        get() = State.query
        set(v) { State.query = v }
    private var dark: Boolean
        get() = State.dark
        set(v) { State.dark = v }
    private var selMode: Boolean
        get() = State.selMode
        set(v) { State.selMode = v }
    private val sel: HashSet<Node>
        get() = State.sel
    private var diff: DiffResult?
        get() = State.diff
        set(v) { State.diff = v }

    private fun t(k: String, vararg a: Any): String = L.t(k, *a)
    private fun toast(s: String) { Toast.makeText(this, s, Toast.LENGTH_LONG).show() }
    private fun dp(x: Int): Int = (x * resources.displayMetrics.density).toInt()
    private fun tint(c: Int, a: Int): Int = (c and 0x00FFFFFF) or (a shl 24)
    private fun shade(c: Int): Int =
        Color.rgb((Color.red(c) * 0.72f).toInt(), (Color.green(c) * 0.72f).toInt(), (Color.blue(c) * 0.72f).toInt())

    private fun dlg(): AlertDialog.Builder =
        AlertDialog.Builder(this, if (dark) AlertDialog.THEME_DEVICE_DEFAULT_DARK else AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)

    // حوار بزوايا 24 مثل التصميم
    private fun AlertDialog.Builder.display() {
        val d = create()
        d.window?.setBackgroundDrawable(InsetDrawable(rounded(cCard(), 24, cStroke()), dp(12)))
        d.show()
    }

    // Bottom sheet بزوايا علوية 24
    private fun AlertDialog.Builder.sheet() {
        val d = create()
        val r = dp(24).toFloat()
        val bg = GradientDrawable().apply {
            setColor(cCard())
            cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        }
        d.window?.setBackgroundDrawable(bg)
        d.window?.setGravity(Gravity.BOTTOM)
        d.show()
        d.window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun rounded(color: Int, r: Int, stroke: Int = 0, sw: Int = 1): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(r).toFloat()
            if (stroke != 0) setStroke(dp(sw), stroke)
        }

    private fun tv(s: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = s
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun pill(s: String, bg: Int, fg: Int, rad: Int = 20, f: () -> Unit): TextView =
        TextView(this).apply {
            text = s
            setTextColor(fg)
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = rounded(bg, rad)
            setOnClickListener { f() }
        }

    // ================= اللغة (أرقام إنجليزية دائماً) =================
    override fun attachBaseContext(base: Context) {
        val l = base.getSharedPreferences("p", 0).getString("lang", "ar") ?: "ar"
        L.lang = l
        val loc = Locale.Builder().setLanguage(l).setUnicodeLocaleKeyword("nu", "latn").build()
        val cfg = Configuration(base.resources.configuration)
        cfg.setLocale(loc)
        cfg.setLayoutDirection(loc)
        super.attachBaseContext(base.createConfigurationContext(cfg))
    }

    private fun switchLang() {
        if (busy) { toast(t("busy")); return }
        val nl = if (L.lang == "ar") "en" else "ar"
        getSharedPreferences("p", 0).edit().putString("lang", nl).apply()
        recreate()
    }

    private fun toggleDark() {
        if (busy) { toast(t("busy")); return }
        getSharedPreferences("p", 0).edit().putBoolean("dark", !dark).apply()
        recreate()
    }

    private fun iconButton(resId: Int, tintColor: Int = Color.WHITE, description: String = "", action: () -> Unit): FrameLayout {
        val box = FrameLayout(this).apply {
            background = rounded(Color.parseColor("#162033"), 20, Color.parseColor("#263247"))
            setOnClickListener { action() }
            contentDescription = description.ifEmpty { t("app_sub") }
            foreground = getDrawable(android.R.attr.selectableItemBackgroundBorderless)
        }
        val iv = ImageView(this).apply {
            setImageResource(resId)
            imageTintList = android.content.res.ColorStateList.valueOf(tintColor)
            scaleType = ImageView.ScaleType.CENTER
        }
        box.addView(iv, FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER))
        return box
    }

    private fun applySystemBars() {
        window.statusBarColor = cTop()
        window.navigationBarColor = cTop()
        var flags = 0
        if (!dark && android.os.Build.VERSION.SDK_INT >= 23) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (!dark && android.os.Build.VERSION.SDK_INT >= 26) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        window.decorView.systemUiVisibility = flags
    }

    // ================= بناء الواجهة =================
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val sp = getSharedPreferences("p", 0)
        dark = sp.getBoolean("dark", false)
        applySystemBars()
        favs.addAll(sp.getStringSet("favs", emptySet()) ?: emptySet())
        if (stack.isEmpty()) stack.add(root)
        buildUi()
        render()
    }

    private fun buildUi() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cBg())
        }

        // Top bar — compact Material-style app header
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(cTop())
            setPadding(dp(16), dp(12), dp(16), dp(8))
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.ic_app_logo)
            imageTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        top.addView(logo, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(10) })

        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(tv("FASILHD", 20f, Color.WHITE, true))
        titles.addView(tv(t("app_sub"), 12f, cGray))
        top.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))

        top.addView(iconButton(R.drawable.ic_search, Color.WHITE, t("cd_search")) { changeMode(SEARCH) },
            LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginStart = dp(6) })
        top.addView(iconButton(R.drawable.ic_settings, Color.WHITE, t("cd_settings")) { changeMode(TOOLS) },
            LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginStart = dp(6) })
        top.addView(iconButton(if (dark) R.drawable.ic_light else R.drawable.ic_dark,
            Color.WHITE, t(if (dark) "cd_light" else "cd_dark")) { toggleDark() },
            LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginStart = dp(6) })
        top.addView(iconButton(R.drawable.ic_language, Color.WHITE, t("cd_language")) { switchLang() },
            LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginStart = dp(6) })
        page.addView(top)

        // Section chips
        val chipRow = LinearLayout(this).apply { setPadding(dp(16), dp(8), dp(16), dp(12)) }
        for (i in secKeys.indices) {
            val c = TextView(this).apply {
                text = t(secKeys[i]).replace(Regex("^\\p{So}\\s*"), "").trim()
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(dp(14), dp(7), dp(14), dp(7))
                setOnClickListener { changeMode(i) }
            }
            chips.add(c)
            chipRow.addView(c, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(8) })
        }
        val hs = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(cTop())
        }
        hs.addView(chipRow)
        page.addView(hs)

        // Banner (gradient 135°، زوايا 16، padding 24)
        banner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(18), dp(24), dp(18))
        }
        bannerTitle = tv("", 18f, Color.WHITE, true)
        bannerSub = tv("", 13f, Color.parseColor("#E2E8F0"))
        banner.addView(bannerTitle)
        banner.addView(bannerSub)
        page.addView(banner, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(16), dp(16), dp(16), dp(8)) })

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            visibility = View.GONE
        }
        page.addView(progress, LinearLayout.LayoutParams(-1, dp(4)))

        // شريط التحديد المتعدد (بطاقة)
        selBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = rounded(cCard(), 16, cStroke())
            visibility = View.GONE
        }
        selCount = tv("", 13f, cInk(), true)
        selBar.addView(selCount, LinearLayout.LayoutParams(0, -2, 1f))
        val actions = listOf<Pair<String, () -> Unit>>(
            "📋" to { selCopy(false) },
            "✂️" to { selCopy(true) },
            "🗑️" to { selDelete() },
            "⭐" to { selFav() },
            "☑" to { selAll() },
            "✖" to { exitSel(); render() }
        )
        for ((lbl, f) in actions)
            selBar.addView(pill(lbl, secColors[1], Color.WHITE, 10) { f() },
                LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(6) })
        page.addView(selBar, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(16), 0, dp(16), dp(8)) })

        // البحث
        searchBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
        }
        searchEt = EditText(this).apply {
            hint = t("search_hint")
            setSingleLine()
            setText(query)
            setTextColor(cInk())
            setHintTextColor(cMuted())
            background = rounded(cCard(), 12, cStroke())
            setPadding(dp(14), dp(12), dp(14), dp(12))
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setOnEditorActionListener { _, id, _ ->
                if (id == EditorInfo.IME_ACTION_SEARCH) { doSearch(); true } else false
            }
        }
        searchBar.addView(searchEt, LinearLayout.LayoutParams(0, -2, 1f))
        searchBar.addView(pill("🔍 " + t("search_btn"), secColors[SEARCH], Color.WHITE, 12) { doSearch() },
            LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        page.addView(searchBar, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(16), 0, dp(16), dp(8)) })

        // بطاقة الحافظة (Clipboard Active + Paste Here)
        pasteBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = rounded(tint(cGreen, 0x1A), 16, cGreen)
            visibility = View.GONE
            setOnLongClickListener { clips.clear(); updatePasteBar(); true }
        }
        pasteText = tv("", 13f, cInk(), true)
        pasteBar.addView(pasteText, LinearLayout.LayoutParams(0, -2, 1f))
        pasteBar.addView(pill(t("paste_btn"), cGreen, Color.WHITE, 8) { paste() })
        page.addView(pasteBar, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(16), 0, dp(16), dp(8)) })

        list = ListView(this).apply {
            divider = null
            dividerHeight = 0
            setSelector(android.R.color.transparent)
            setOnItemClickListener { _, _, pos, _ ->
                val r = shownRows.getOrNull(pos)
                if (r != null) onRow(r)
            }
            setOnItemLongClickListener { _, _, pos, _ ->
                val n = shownRows.getOrNull(pos)?.node
                if (n == null) false
                else if (selMode) { if (n.isDir && mode == FILES) { stack.add(n); render() }; true }
                else { menu(n); true }
            }
        }
        page.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))

        // Bottom navigation — five primary sections
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(cTop())
            setPadding(dp(4), dp(7), dp(4), dp(8))
        }
        for (m in navModes) {
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(4), dp(4), dp(4))
                contentDescription = t(secKeys[m])
                foreground = getDrawable(android.R.attr.selectableItemBackgroundBorderless)
                setOnClickListener { changeMode(m) }
            }
            val iv = ImageView(this).apply {
                setImageResource(secIcons[m])
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                imageTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            }
            item.addView(iv, LinearLayout.LayoutParams(dp(24), dp(24)))
            val label = tv(t(secKeys[m]).replace(Regex("^\\p{So}\\s*"), "").trim(), 10f, Color.WHITE, false)
            label.gravity = Gravity.CENTER
            item.addView(label, LinearLayout.LayoutParams(-2, -2))
            navItems.add(item)
            bottom.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
        }
        page.addView(bottom)
        setContentView(page)
    }

    inner class RowAdapter(private val rows: List<Row>) : BaseAdapter() {
        override fun getCount(): Int = rows.size
        override fun getItem(position: Int): Any = rows[position]
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup?): View = rowView(rows[position])
    }

    private fun legendLine(color: Int, text: String): View {
        val l = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        l.addView(tv("●", 14f, color), LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(6) })
        l.addView(tv(text, 12f, cInk()))
        return l
    }

    private fun rowView(r: Row): View {
        val accent = if (r.color != 0) r.color else secColors[mode]
        val wrap = FrameLayout(this).apply { setPadding(dp(16), dp(8), dp(16), dp(8)) }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        if (r.type == 2) {
            card.background = rounded(tint(accent, 0x1A), 16, accent)
        } else {
            card.background = rounded(cCard(), 16,
                if (r.selected) secColors[FILES] else cStroke(), if (r.selected) 2 else 1)
            card.elevation = dp(1).toFloat()
        }
        card.clipToOutline = true
        // شريط جانبي 4dp للبطاقات الإجرائية (مثل بطاقة التنظيف)
        if (r.type == 5) card.addView(View(this).apply { setBackgroundColor(accent) }, LinearLayout.LayoutParams(dp(4), -1))
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        card.addView(content, LinearLayout.LayoutParams(0, -2, 1f))

        val pie = r.pie
        if (r.metrics != null) {
            content.orientation = LinearLayout.HORIZONTAL
            content.gravity = Gravity.CENTER_VERTICAL
            content.setPadding(dp(10), dp(10), dp(10), dp(10))
            for ((index, metric) in r.metrics.withIndex()) {
                val box = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                    background = rounded(cCard(), 14, cStroke())
                }
                box.addView(tv(metric.first, 11f, cMuted(), true))
                box.addView(tv(metric.second, 18f, cInk(), true), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
                content.addView(box, LinearLayout.LayoutParams(0, -2, 1f).apply {
                    if (index > 0) marginStart = dp(6)
                })
            }
            wrap.addView(card, FrameLayout.LayoutParams(-1, -2))
            return wrap
        }

        if (pie != null) {
            // Donut 140dp وفتحة 100dp، والنص في المنتصف، والمفتاح تحته
            content.orientation = LinearLayout.VERTICAL
            content.gravity = Gravity.CENTER_HORIZONTAL
            val total = pie.sumOf { it.second }
            val cols = r.pieColors ?: palette
            content.addView(tv(r.title, 14f, cInk(), true))
            content.addView(
                PieView(this, pie.map { it.second.toFloat() }, cols, cCard(), fmtSize(total), t("indexed"), cInk(), cMuted()),
                LinearLayout.LayoutParams(dp(140), dp(140)).apply { topMargin = dp(12); bottomMargin = dp(12) })
            val leg = LinearLayout(this).apply {
                orientation = if (pie.size <= 3) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            }
            for ((i, p) in pie.withIndex()) {
                val pct = if (total > 0L) p.second.toFloat() * 100f / total.toFloat() else 0f
                val line = legendLine(cols[i % cols.size], p.first + " (" + String.format(Locale.US, "%.1f%%", pct) + ")")
                if (pie.size <= 3) leg.addView(line, LinearLayout.LayoutParams(0, -2, 1f))
                else leg.addView(line)
            }
            content.addView(leg, LinearLayout.LayoutParams(-1, -2))
            wrap.addView(card, FrameLayout.LayoutParams(-1, -2))
            return wrap
        }

        if (r.iconRes != 0 || r.icon.isNotEmpty()) {
            val badge = FrameLayout(this).apply {
                background = rounded(tint(accent, 0x26), 10)
            }
            if (r.iconRes != 0) {
                val iv = ImageView(this).apply {
                    setImageResource(r.iconRes)
                    imageTintList = android.content.res.ColorStateList.valueOf(accent)
                    scaleType = ImageView.ScaleType.CENTER
                }
                badge.addView(iv, FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER))
            } else {
                badge.addView(TextView(this).apply {
                    text = r.icon
                    textSize = 18f
                    gravity = Gravity.CENTER
                }, FrameLayout.LayoutParams(dp(40), dp(40)))
            }
            content.addView(badge, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(12) })
        }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val titleColor = if (r.type == 2 && r.color != 0) accent else cInk()
        col.addView(tv(r.title, 16f, titleColor, true))
        if (r.sub.isNotEmpty()) col.addView(tv(r.sub, 13f, cMuted()))
        if (r.bar >= 0f) {
            val f = r.bar.coerceIn(0.03f, 1f)
            val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; background = rounded(cTrack(), 4) }
            bar.addView(View(this).apply { background = rounded(accent, 4) }, LinearLayout.LayoutParams(0, dp(8), f))
            bar.addView(View(this), LinearLayout.LayoutParams(0, dp(8), 1f - f))
            col.addView(bar, LinearLayout.LayoutParams(-1, dp(8)).apply { topMargin = dp(8) })
        }
        content.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        if (r.tag.isNotEmpty()) {
            val tg = TextView(this).apply {
                text = r.tag
                textSize = 10f
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(8), dp(2), dp(8), dp(2))
                background = rounded(accent, 4)          // tag: خلفية صلبة + نص أبيض
            }
            content.addView(tg, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        }
        wrap.addView(card, FrameLayout.LayoutParams(-1, -2))
        return wrap
    }

    // ================= العرض =================
    private fun changeMode(m: Int) {
        mode = m
        render()
    }

    private fun renderKeep() {
        val idx = list.firstVisiblePosition
        val top = list.getChildAt(0)?.top ?: 0
        render()
        list.setSelectionFromTop(idx, top)
    }

    private fun render() {
        val rows = ArrayList<Row>()
        when (mode) {
            HOME -> homeRows(rows)
            FILES -> fileRows(rows)
            ACCS -> accountRows(rows)
            DUPS -> dupRows(rows)
            RULES -> ruleRows(rows)
            CMP -> cmpRows(rows)
            STATS -> statRows(rows)
            FAVS -> favRows(rows)
            SEARCH -> searchRows(rows)
            else -> toolRows(rows)
        }
        shownRows = rows
        list.adapter = RowAdapter(rows)
        for ((i, c) in chips.withIndex()) {
            if (i == mode) {
                c.background = rounded(secColors[i], 24)
                c.setTextColor(Color.WHITE)
                c.setTypeface(null, Typeface.BOLD)
            } else {
                c.background = rounded(cTrack(), 24)
                c.setTextColor(cInk())
                c.setTypeface(null, Typeface.NORMAL)
            }
        }
        for ((i, item) in navItems.withIndex()) {
            val on = navModes[i] == mode
            val c = if (on) secColors[navModes[i]] else Color.parseColor("#94A3B8")
            item.background = if (on) rounded(tint(c, 0x22), 14) else rounded(Color.TRANSPARENT, 14)
            val iv = item.getChildAt(0) as? ImageView
            iv?.imageTintList = android.content.res.ColorStateList.valueOf(c)
            val label = item.getChildAt(1) as? TextView
            label?.setTextColor(c)
            label?.setTypeface(label.typeface, if (on) Typeface.BOLD else Typeface.NORMAL)
        }
        bannerTitle.text = if (mode == HOME && root.kids.isNotEmpty()) t("storage_overview") else t(secKeys[mode])
        bannerSub.text = if (mode == FILES) {
            val d = stack.last()
            d.path() + "\n" + t("dir_sub", num(d.files), fmtSize(d.size))
        } else t(secKeys[mode].replace("sec_", "sb_"))
        banner.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR, intArrayOf(secColors[mode], shade(secColors[mode]))
        ).apply { cornerRadius = dp(16).toFloat() }
        searchBar.visibility = if (mode == SEARCH) View.VISIBLE else View.GONE
        selBar.visibility = if (selMode && mode == FILES) View.VISIBLE else View.GONE
        selCount.text = t("sel_count", num(sel.size))
        updatePasteBar()
        if (mode == CMP && State.baseSnap != null && diff == null && an != null && !busy) startDiff()
    }

    private fun updatePasteBar() {
        if (clips.isNotEmpty() && mode == FILES) {
            pasteBar.visibility = View.VISIBLE
            val w = if (clipMove) t("w_move") else t("w_copy")
            pasteText.text = t("clip_active", num(clips.size)) + " • " + w +
                (if (clips.size == 1) "\n" + clips[0].name else "")
        } else pasteBar.visibility = View.GONE
    }

    private fun kindColor(k: Int): Int = when (k) { 1 -> cGreen; 2 -> cRose; 3 -> cGray; else -> 0 }

    private fun fileIcon(ext: String): String = when (ext) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic" -> "🖼️"
        "mp4", "mkv", "avi", "mov", "webm" -> "🎬"
        "mp3", "wav", "flac", "m4a", "ogg" -> "🎵"
        "pdf" -> "📕"
        "doc", "docx", "txt", "rtf", "odt" -> "📝"
        "xls", "xlsx", "csv" -> "📈"
        "zip", "rar", "7z", "tar", "gz" -> "🗜️"
        "apk" -> "📦"
        "json" -> "🧾"
        else -> "📄"
    }

    private fun pieData(src: List<Pair<String, Long>>): List<Pair<String, Long>> {
        val s = src.filter { it.second > 0 }
        if (s.size <= 7) return s
        val top = s.take(6)
        val rest = s.drop(6).sumOf { it.second }
        return top + Pair(t("other"), rest)
    }

    private fun accSub(s: AccStat): String {
        val a = s.node
        val l = ArrayList<String>()
        l.add(t("acc_l1", num(a.files), num(a.dirs), fmtSize(a.size)))
        val bf = s.bigFile
        if (bf != null) l.add(t("acc_big_file", bf.name, fmtSize(bf.size)))
        val bd = s.bigDir
        if (bd != null) l.add(t("acc_big_dir", bd.name, fmtSize(bd.size)))
        l.add(t("acc_dups", num(s.dupCount)))
        if (s.kind == 2) l.add(t("acc_sug", num(s.sugCount), fmtSize(s.sugSize), fmtSize(a.size - s.sugSize)))
        if (s.exts.isNotEmpty()) l.add("🏷️ " + s.exts.joinToString("، ") { it.first + ": " + num(it.second) })
        return l.joinToString("\n")
    }

    private fun sortedAccs(a: Analysis): List<AccStat> =
        a.accs.sortedWith(compareBy<AccStat>({
            when (it.kind) { 1 -> 0; 3 -> 1; else -> 2 }
        }, { it.node.name.lowercase() }))

    private fun analyzingRow(rows: ArrayList<Row>) {
        rows.add(Row(t("analyzing"), "", "⏳", type = 2))
    }

    private fun realRow(rows: ArrayList<Row>) {
        if (root.doc != null) rows.add(Row(t("real_mode"), "", "💽", cRose, type = 2))
    }

    // ---------- 🏠 الرئيسية ----------
    private fun homeRows(rows: ArrayList<Row>) {
        if (root.kids.isEmpty()) {
            rows.add(Row(t("hero_title"), t("hero_sub"), "📂", type = 5, act = 1, tag = t("open")))
            rows.add(Row(t("terabox_title"), t("terabox_sub"), "", cGreen, type = 5, act = 19,
                iconRes = R.drawable.ic_cloud))
            rows.add(Row(t("tool_real"), t("tool_real_sub"), "💽", secColors[2], type = 5, act = 15))
            rows.add(Row(t("tip_title"), t("tip_sub"), "💡", type = 2))
            return
        }
        realRow(rows)
        rows.add(Row(
            if (fileName.isEmpty()) t("loaded") else fileName,
            t("loaded"), "🗂️", type = 2
        ))
        // Dashboard actions: real operations, no mock data.
        rows.add(Row(t("switch_file"), t("hero_sub"), "⇄", secColors[HOME], type = 5, act = 1))
        rows.add(Row(t("open_folder"), t("tool_real_sub"), "📂", secColors[FILES], type = 5, act = 15))
        rows.add(Row(
            t("terabox_title"), t("terabox_sub"), "", cGreen, type = 5, act = 19,
            iconRes = R.drawable.ic_cloud
        ))
        rows.add(Row(
            title = t("storage_overview"),
            sub = t("storage_overview_sub"),
            metrics = listOf(
                Pair(t("total_files"), num(root.files)),
                Pair(t("total_dirs"), num(root.dirs)),
                Pair(t("total_size"), fmtSize(root.size))
            )
        ))
        val a = an
        if (a == null) { analyzingRow(rows); return }
        val total = maxOf(1L, a.sz[1] + a.sz[2] + a.sz[3])
        for (k in 1..3) {
            if (a.cnt[k] == 0) continue
            rows.add(Row(t("kind_$k") + " — " + num(a.cnt[k]) + " " + t("accounts_word"), fmtSize(a.sz[k]),
                "🏷️", color = kindColor(k), type = 5, act = 8, bar = a.sz[k].toFloat() / total))
        }
        val kp = (1..3).filter { a.sz[it] > 0 }
        if (kp.size >= 2)
            rows.add(Row(t("kinds_pie"), pie = kp.map { Pair(t("kind_$it"), a.sz[it]) },
                pieColors = kp.map { kindColor(it) }))
        if (a.sug.isNotEmpty()) {
            rows.add(Row(
                t("reclaim_alert"),
                fmtSize(a.sugSize) + " • " + t("reclaim_sub") + "\n" + t("cleanup_sub", num(a.sug.size)),
                "🗑️", cRose, type = 5, act = 7, tag = t("review_clean")
            ))
        }
        rows.add(Row(t("rep_title"), t("rep_sub"), "📝", color = secColors[9], type = 5, act = 3))
        rows.add(Row(t("quick_scan"), t("tool_real_sub"), "⌕", cGreen, type = 5, act = 15))
    }

    // ---------- 📂 الملفات ----------
    private fun fileRows(rows: ArrayList<Row>) {
        val d = stack.last()
        realRow(rows)
        if (stack.size > 1) rows.add(Row(t("back"), "", "⬆️", type = 1))
        if (!selMode) rows.add(Row(t("select_multi"), t("select_multi_sub"), "☑️", secColors[7], type = 5, act = 10))
        rows.add(Row(t("sort_row"), t("sort_$sortMode"), "↕️", color = secColors[9], type = 5, act = 5))
        if (d.kids.isEmpty()) rows.add(Row(t("empty"), "", "📭", type = 2))
        val base = d.path()
        val kids = when (sortMode) {
            1 -> d.kids.sortedWith(compareBy<Node>({ !it.isDir }, { -it.size }))
            2 -> d.kids.sortedWith(compareBy<Node>({ !it.isDir }, { -it.files }))
            else -> d.kids.sortedWith(compareBy<Node>({ !it.isDir }, { it.name.lowercase() }))
        }
        val mx = kids.maxOfOrNull { it.size } ?: 0L
        for (n in kids) {
            val p = if (base == "/") "/" + n.name else base + "/" + n.name
            val star = if (favs.contains(p)) "⭐ " else ""
            val k = if (n.isDir) Acc.kind(n.name) else 0
            val icon = if (!n.isDir) fileIcon(n.type) else if (k != 0) "🗂️" else "📁"
            val on = selMode && sel.contains(n)
            var title = star + n.name
            if (selMode) title = (if (on) "☑ " else "☐ ") + title
            val sub0 = if (n.isDir) t("dir_sub", num(n.files), fmtSize(n.size))
            else fmtSize(n.size) + (if (n.type.isNotEmpty()) " • " + n.type.uppercase() else "")
            val sub = if (on) sub0 + "\n" + t("sel_sub") else sub0
            rows.add(Row(title, sub, icon, kindColor(k), n,
                bar = if (mx > 0) n.size.toFloat() / mx else -1f,
                tag = if (k != 0) t("tag_$k") else "", selected = on))
        }
    }

    // ---------- 🏷️ الحسابات ----------
    private fun accountRows(rows: ArrayList<Row>) {
        val a = an
        if (a == null) { analyzingRow(rows); return }
        if (a.accs.isEmpty()) { rows.add(Row(t("no_acc"), t("no_acc_sub"), "🔎", type = 2)); return }
        val summary = (1..3).filter { a.cnt[it] > 0 }
            .joinToString("\n") { t("kind_$it") + ": " + num(a.cnt[it]) + " (" + fmtSize(a.sz[it]) + ")" } +
            "\n" + t("sum_lines", num(a.dups.size), num(a.sug.size), fmtSize(a.sugSize))
        rows.add(Row(t("acc_summary"), summary, "📋", type = 2))
        if (a.accs.size >= 2)
            rows.add(Row(t("merge_row"), t("merge_row_sub"), "🔗", secColors[ACCS], type = 5, act = 11))
        val mx = a.accs.maxOf { it.node.size }
        for (s in sortedAccs(a)) {
            rows.add(Row(s.node.name, accSub(s), "🗂️", kindColor(s.kind), s.node,
                bar = if (mx > 0) s.node.size.toFloat() / mx else -1f, tag = t("tag_${s.kind}")))
        }
    }

    // ---------- ♊ المكرر ----------
    private fun dupRows(rows: ArrayList<Row>) {
        val a = an
        if (a == null) { analyzingRow(rows); return }
        if (a.dups.isEmpty()) { rows.add(Row(t("no_dups"), "", "✨", type = 2)); return }
        if (root.doc != null) rows.add(Row(t("sha_note"), "", "🔐", secColors[ACCS], type = 2))
        rows.add(Row(
            t("dup_header", num(a.dups.size), num(a.dups.count { it.isDir }), num(a.dups.count { !it.isDir })),
            t("dup_header_sub", num(a.sug.size), fmtSize(a.sugSize)), "♊", type = 2))
        if (a.sug.isNotEmpty())
            rows.add(Row(t("apply_all"), t("apply_all_sub", num(a.sug.size), fmtSize(a.sugSize)), "🗑️",
                cRose, type = 5, act = 6, tag = t("apply")))
        val shown = a.dups.take(400)
        val mx = shown.maxOfOrNull { it.wasted } ?: 0L
        for (g in shown) {
            val accs = g.items.map { it.account()?.name ?: "—" }.distinct().take(4).joinToString("، ")
            rows.add(Row(g.items[0].name + "  ×" + num(g.items.size),
                t("per_copy", fmtSize(g.size), fmtSize(g.wasted)) + "\n" + accs,
                if (g.isDir) "📁" else "📄", if (g.sugg.isNotEmpty()) cRose else 0, null,
                dup = g, bar = if (mx > 0) g.wasted.toFloat() / mx else -1f,
                tag = if (g.sugg.isNotEmpty()) "🗑️ " + num(g.sugg.size) else ""))
        }
        if (a.dups.size > 400) rows.add(Row(t("dup_more"), "", "…", type = 2))
    }

    // ---------- 🧹 قواعد التنظيف ----------
    private fun ruleRows(rows: ArrayList<Row>) {
        val a = an
        if (a == null) { analyzingRow(rows); return }
        realRow(rows)
        rows.add(Row(t("rules_info"), "", "🛡️", type = 2))
        val icons = listOf("📅", "🗓️", "🐘", "🦣", "📭", "🧽")
        for (i in 0..5) {
            val l = a.rules[i]
            val dated = i == 0 || i == 1
            val sub = if (dated && !a.tempHasDates) t("rule_nodate")
            else t("rule_sub", num(l.size), fmtSize(l.sumOf { it.size }))
            rows.add(Row(t("rule_$i"), sub, icons[i], secColors[RULES], type = 5, act = 20 + i,
                tag = if (l.isNotEmpty()) t("review") else ""))
        }
        rows.add(Row(t("apply_all"), t("apply_all_sub", num(a.sug.size), fmtSize(a.sugSize)), "♊", cRose,
            type = 5, act = 6))
    }

    private fun ruleDialog(i: Int) {
        val a = an ?: return
        val l = a.rules[i]
        if (l.isEmpty()) { toast(t("rule_none")); return }
        val sb = StringBuilder()
        sb.append(t("rule_head", num(l.size), fmtSize(l.sumOf { it.size }))).append("\n\n")
        for (n in l.take(25)) sb.append("• ").append(n.path()).append("\n")
        if (l.size > 25) sb.append(t("and_more", num(l.size - 25)))
        if (l.any { it.doc != null }) sb.append("\n\n").append(t("real_warn"))
        dlg().setTitle(t("rule_$i")).setMessage(sb.toString())
            .setPositiveButton(t("rule_apply", num(l.size))) { _, _ -> deleteMany(ArrayList(l)) }
            .setNegativeButton(t("cancel"), null).display()
    }

    // ---------- 🔀 المقارنة ----------
    private fun startDiff() {
        val b = State.baseSnap ?: return
        bg(t("analyzing"), { diff = Diff.compare(b, Diff.snapshot(root)) }, { render() })
    }

    private fun cmpRows(rows: ArrayList<Row>) {
        val b = State.baseSnap
        if (b == null) {
            rows.add(Row(t("cmp_hero"), t("cmp_hero_sub"), "🔀", type = 2))
            rows.add(Row(t("cmp_use_cur"), t("cmp_use_cur_sub"), "📸", secColors[CMP], type = 5, act = 12))
            rows.add(Row(t("cmp_load"), t("cmp_load_sub"), "📂", secColors[CMP], type = 5, act = 13))
            return
        }
        rows.add(Row(t("cmp_base", State.baseName), t("cmp_base_sub", num(b.size)), "📸", type = 2))
        rows.add(Row(t("cmp_use_cur"), t("cmp_use_cur_sub"), "📸", secColors[CMP], type = 5, act = 12))
        rows.add(Row(t("cmp_load"), t("cmp_load_sub"), "📂", secColors[CMP], type = 5, act = 13))
        val d = diff
        if (d == null) { analyzingRow(rows); return }
        if (d.added.isEmpty() && d.removed.isEmpty() && d.changed.isEmpty()) {
            rows.add(Row(t("cmp_none"), "", "✅", cGreen, type = 2)); return
        }
        val mx = maxOf(1, maxOf(d.added.size, maxOf(d.removed.size, d.changed.size)))
        rows.add(Row(t("cmp_added") + ": " + num(d.added.size), fmtSize(d.addedSize), "➕", cGreen,
            bar = d.added.size.toFloat() / mx))
        rows.add(Row(t("cmp_removed") + ": " + num(d.removed.size), fmtSize(d.removedSize), "➖", cRose,
            bar = d.removed.size.toFloat() / mx))
        rows.add(Row(t("cmp_changed") + ": " + num(d.changed.size), "", "✏️", secColors[DUPS],
            bar = d.changed.size.toFloat() / mx))
        rows.add(Row(t("cmp_per_acc"), "", "🗂️", type = 2))
        for ((name, v) in d.perAcc.entries.sortedBy { it.key })
            rows.add(Row(name, t("cmp_acc_sub", num(v[0]), num(v[1]), num(v[2])), "🗂️", kindColor(Acc.kind(name))))
        rows.add(Row(t("cmp_top_added"), "", "➕", cGreen, type = 2))
        for (p in d.added.take(100)) rows.add(Row(p.first.substringAfterLast('/'), fmtSize(p.second) + "\n" + p.first, "➕", cGreen, find(p.first)))
        rows.add(Row(t("cmp_top_removed"), "", "➖", cRose, type = 2))
        for (p in d.removed.take(100)) rows.add(Row(p.first.substringAfterLast('/'), fmtSize(p.second) + "\n" + p.first, "➖", cRose))
        rows.add(Row(t("cmp_top_changed"), "", "✏️", secColors[DUPS], type = 2))
        for (p in d.changed.take(100))
            rows.add(Row(p.first.substringAfterLast('/'), t("cmp_changed_sub", fmtSize(p.second), fmtSize(p.third)) + "\n" + p.first,
                "✏️", secColors[DUPS], find(p.first)))
    }

    // ---------- 📊 الإحصائيات ----------
    private fun statRows(rows: ArrayList<Row>) {
        val a = an
        if (a == null) { analyzingRow(rows); return }
        rows.add(Row(t("types_title"), pie = pieData(a.typeSizes)))
        val mxT = a.typeSizes.maxOfOrNull { it.second } ?: 0L
        for (p in a.typeSizes)
            rows.add(Row(p.first.uppercase(), fmtSize(p.second), fileIcon(p.first),
                bar = if (mxT > 0) p.second.toFloat() / mxT else -1f))
        if (a.accs.isNotEmpty()) {
            rows.add(Row(t("top_accs"), "", "🗂️", type = 2))
            val top = a.accs.sortedByDescending { it.node.size }.take(10)
            val mx = top.first().node.size
            for (s in top)
                rows.add(Row(s.node.name, fmtSize(s.node.size), "🗂️", kindColor(s.kind), s.node,
                    bar = if (mx > 0) s.node.size.toFloat() / mx else -1f, tag = t("tag_${s.kind}")))
        }
        rows.add(Row(t("top_files", num(a.topFiles.size)), "", "📄", type = 2))
        val mf = a.topFiles.firstOrNull()?.size ?: 0L
        for (n in a.topFiles)
            rows.add(Row(n.name, fmtSize(n.size) + "\n" + n.path(), fileIcon(n.type), 0, n,
                bar = if (mf > 0) n.size.toFloat() / mf else -1f))
        rows.add(Row(t("top_dirs", num(a.topDirs.size)), "", "📁", type = 2))
        val md = a.topDirs.firstOrNull()?.size ?: 0L
        for (n in a.topDirs)
            rows.add(Row(n.name, t("dir_sub", num(n.files), fmtSize(n.size)) + "\n" + n.path(), "📁", 0, n,
                bar = if (md > 0) n.size.toFloat() / md else -1f))
    }

    // ---------- ⭐ المفضلة ----------
    private fun favRows(rows: ArrayList<Row>) {
        rows.add(Row(t("favs_title", num(favs.size)), t("favs_hint"), "⭐", type = 2))
        for (p in favs.sorted()) {
            val n = find(p)
            if (n != null) {
                val sub = (if (n.isDir) t("dir_sub", num(n.files), fmtSize(n.size)) else fmtSize(n.size))
                rows.add(Row(p, sub, if (n.isDir) "📁" else fileIcon(n.type), 0, n))
            } else rows.add(Row(p, t("fav_missing"), "⚠️", cRose, null, 4, null, p))
        }
    }

    private fun find(path: String): Node? {
        var c: Node = root
        for (p in path.split("/")) {
            if (p.isEmpty()) continue
            c = c.kids.firstOrNull { it.name == p } ?: return null
        }
        return c
    }

    // ---------- 🔍 البحث ----------
    private fun doSearch() {
        query = searchEt.text.toString().trim()
        render()
    }

    private fun searchRows(rows: ArrayList<Row>) {
        if (query.isEmpty()) { rows.add(Row(t("search_empty"), "", "🔍", type = 2)); return }
        val q = query.lowercase()
        val res = ArrayList<Node>()
        val st = ArrayList<Node>()
        st.add(root)
        while (st.isNotEmpty() && res.size < 300) {
            val n = st.removeAt(st.size - 1)
            for (k in n.kids) {
                if (k.name.lowercase().contains(q)) res.add(k)
                if (k.isDir) st.add(k)
            }
        }
        if (res.isEmpty()) { rows.add(Row(t("search_none"), "", "🫗", type = 2)); return }
        rows.add(Row(t("search_res", num(minOf(res.size, 300)), query), "", "🔍", type = 2))
        for (n in res.take(300)) {
            val sub = (if (n.isDir) t("dir_sub", num(n.files), fmtSize(n.size)) else fmtSize(n.size)) + "\n" + n.path()
            rows.add(Row(n.name, sub, if (n.isDir) "📁" else fileIcon(n.type), 0, n))
        }
    }

    // ---------- ⚙️ الأدوات ----------
    private fun toolRows(rows: ArrayList<Row>) {
        rows.add(Row(t("open"), t("tool_open_sub"), "📂", secColors[0], type = 5, act = 1))
        rows.add(Row(t("tool_real"), t("tool_real_sub"), "💽", secColors[2], type = 5, act = 15))
        rows.add(Row(t("json_title"), t("json_sub"), "💾", secColors[2], type = 5, act = 2))
        rows.add(Row(t("rep_title"), t("rep_sub"), "📝", secColors[6], type = 5, act = 3))
        rows.add(Row(t("tool_csv_files"), t("tool_csv_files_sub"), "📑", secColors[5], type = 5, act = 16))
        rows.add(Row(t("tool_csv_acc"), t("tool_csv_acc_sub"), "📊", secColors[5], type = 5, act = 17))
        rows.add(Row(t("tool_dark"), if (dark) t("on") else t("off"), if (dark) "🌙" else "☀️", secColors[9],
            type = 5, act = 18))
        rows.add(Row(t("tool_lang"), t("tool_lang_sub"), "🌐", secColors[8], type = 5, act = 4, tag = t("lang_btn")))
        rows.add(Row(t("tool_about"), t("tool_about_sub"), "💡", secColors[3], type = 5, act = 9))
    }

    // ================= التفاعل =================
    private fun onRow(r: Row) {
        when (r.type) {
            1 -> if (stack.size > 1) { stack.removeAt(stack.size - 1); render() }
            2 -> {}
            5 -> onAction(r.act)
            4 -> {
                val fp = r.fav
                if (fp != null) dlg().setMessage(t("fav_remove_q") + "\n" + fp)
                    .setPositiveButton(t("remove")) { _, _ -> favs.remove(fp); saveFavs(); render() }
                    .setNegativeButton(t("cancel"), null).display()
            }
            else -> {
                val g = r.dup
                val n = r.node
                if (g != null) dupDialog(g)
                else if (n != null) {
                    if (mode == FILES) {
                        if (selMode) { toggleSel(n) }
                        else if (n.isDir) { stack.add(n); render() }
                        else details(n)
                    } else goTo(if (n.isDir) n else (n.parent ?: root))
                }
            }
        }
    }

    private fun onAction(a: Int) {
        when (a) {
            1 -> pick(1)
            2 -> saveJson()
            3 -> saveReport()
            4 -> switchLang()
            5 -> { sortMode = (sortMode + 1) % 3; render() }
            6 -> confirmApplyAll()
            7 -> changeMode(DUPS)
            8 -> changeMode(ACCS)
            9 -> dlg().setTitle(t("tool_about")).setMessage(t("about2"))
                .setPositiveButton(t("close"), null).display()
            10 -> { selMode = true; sel.clear(); render() }
            11 -> mergeStart()
            12 -> { State.baseSnap = Diff.snapshot(root); State.baseName = t("cur_snapshot"); diff = null; render() }
            13 -> pick(7)
            14 -> { diff = null; render() }
            15 -> pickTree()
            16 -> createDoc(5, "text/csv", "files.csv")
            17 -> createDoc(6, "text/csv", "accounts.csv")
            18 -> toggleDark()
            19 -> dlg().setTitle(t("terabox_title"))
                .setMessage(t("terabox_msg"))
                .setPositiveButton(t("close"), null).display()
            in 20..25 -> ruleDialog(a - 20)
        }
    }

    private fun goTo(d: Node) {
        val chain = ArrayList<Node>()
        var n: Node? = d
        while (n != null) { chain.add(n); n = n.parent }
        chain.reverse()
        if (chain.first() !== root) { toast(t("gone")); return }
        stack.clear(); stack.addAll(chain)
        mode = FILES
        render()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (selMode) { exitSel(); render() }
        else if (mode != FILES && mode != HOME) changeMode(FILES)
        else if (mode == FILES && stack.size > 1) { stack.removeAt(stack.size - 1); render() }
        else if (mode == FILES) changeMode(HOME)
        else super.onBackPressed()
    }

    // ================= التحديد المتعدد =================
    private fun toggleSel(n: Node) {
        if (sel.contains(n)) sel.remove(n) else sel.add(n)
        renderKeep()
    }

    private fun exitSel() { selMode = false; sel.clear() }

    private fun pruneNested(l: Collection<Node>): List<Node> {
        val set = HashSet<Node>(l)
        return l.filter { n ->
            var p = n.parent
            var nested = false
            while (p != null) {
                if (set.contains(p)) { nested = true; break }
                p = p.parent
            }
            !nested
        }
    }

    private fun selItems(): List<Node> = pruneNested(sel.filter { it.parent != null })

    private fun selCopy(cut: Boolean) {
        val l = selItems()
        if (l.isEmpty()) return
        clips.clear(); clips.addAll(l)
        clipMove = cut
        exitSel()
        toast(if (cut) t("cut") else t("copied"))
        render()
    }

    private fun selDelete() {
        val l = selItems()
        if (l.isEmpty()) return
        val warn = if (l.any { it.doc != null }) "\n\n" + t("real_warn") else ""
        dlg().setTitle(t("del_title"))
            .setMessage(t("del_many_q", num(l.size), fmtSize(l.sumOf { it.size })) + warn)
            .setPositiveButton(t("delete")) { _, _ -> exitSel(); deleteMany(l) }
            .setNegativeButton(t("cancel"), null).display()
    }

    private fun selFav() {
        val l = selItems()
        for (n in l) favs.add(n.path())
        saveFavs()
        toast(t("fav_added"))
        exitSel()
        render()
    }

    private fun selAll() {
        sel.addAll(stack.last().kids)
        renderKeep()
    }

    // ================= الملفات والعمليات الثقيلة =================
    private fun pick(code: Int) {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
        }, code)
    }

    private fun pickTree() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }, 8)
    }

    private fun createDoc(code: Int, mime: String, title: String) {
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = mime
            putExtra(Intent.EXTRA_TITLE, title)
        }, code)
    }

    private fun saveJson() = createDoc(2, "application/json", "terabox_structure_edited.json")
    private fun saveReport() = createDoc(3, "text/plain", "sama_report.txt")

    private fun newAnalysis(r: Node): Analysis = Analysis(r, contentResolver).also { it.run() }

    private fun displayName(uri: Uri): String {
        var nm = ""
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) nm = c.getString(i) ?: ""
            }
        }
        return nm
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        val uri = data?.data ?: return
        if (res != RESULT_OK) return
        when (req) {
            1 -> bg(t("reading"), {
                val nm = displayName(uri)
                val r = Reader(contentResolver).read(uri)
                root = r
                State.tree = null
                fileName = nm
                stack.clear(); stack.add(r)
                clips.clear(); exitSel()
                diff = null
                an = newAnalysis(r)
            }, { mode = HOME; render() })
            2 -> bg(t("saving"), { writeJson(uri) }, { toast(t("saved_json")); render() })
            3 -> bg(t("saving_report"), {
                val a = an ?: newAnalysis(root).also { an = it }
                val txt = buildReport(a)
                contentResolver.openOutputStream(uri)!!.use { it.write(txt.toByteArray(Charsets.UTF_8)) }
            }, { toast(t("saved_report")); render() })
            5 -> bg(t("saving_report"), {
                contentResolver.openOutputStream(uri)!!.use { os ->
                    Export.filesCsv(OutputStreamWriter(os, "UTF-8"), root)
                }
            }, { toast(t("saved_csv")); render() })
            6 -> bg(t("saving_report"), {
                val a = an ?: newAnalysis(root).also { an = it }
                contentResolver.openOutputStream(uri)!!.use { os ->
                    Export.accountsCsv(OutputStreamWriter(os, "UTF-8"), a)
                }
            }, { toast(t("saved_csv")); render() })
            7 -> bg(t("reading"), {
                val nm = displayName(uri)
                val r = Reader(contentResolver).read(uri)
                State.baseSnap = Diff.snapshot(r)
                State.baseName = nm
                diff = null
            }, { mode = CMP; render() })
            8 -> bg(t("reading_real"), {
                try {
                    contentResolver.takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                } catch (e: Exception) { }
                val r = Real.scan(contentResolver, uri)
                root = r
                State.tree = uri
                fileName = uri.lastPathSegment ?: ""
                stack.clear(); stack.add(r)
                clips.clear(); exitSel()
                diff = null
                an = newAnalysis(r)
            }, { mode = HOME; render() })
        }
    }

    private fun bg(label: String, work: () -> Unit, done: () -> Unit) {
        if (busy) { toast(t("busy")); return }
        busy = true
        progress.visibility = View.VISIBLE
        bannerSub.text = label
        Thread(null, {
            var err: String? = null
            try { work() } catch (e: Throwable) { err = e.toString() }
            runOnUiThread {
                busy = false
                progress.visibility = View.GONE
                if (err != null) bannerSub.text = t("error") + ": " + err else done()
            }
        }, "w", 256L * 1024 * 1024).start()
    }

    private fun afterChange() {
        an = null
        diff = null
        var i = 1
        while (i < stack.size) {
            if (stack[i].parent !== stack[i - 1]) {
                while (stack.size > i) stack.removeAt(stack.size - 1)
                break
            }
            i++
        }
        clips.removeAll { it.parent == null }
        render()
        bg(t("analyzing"), { an = newAnalysis(root) }, { render() })
    }

    private fun writeJson(uri: Uri) {
        contentResolver.openOutputStream(uri)!!.use { os ->
            val w = JsonWriter(OutputStreamWriter(os, "UTF-8"))
            writeNode(w, root)
            w.flush()
        }
    }

    private fun writeNode(w: JsonWriter, n: Node) {
        w.beginObject()
        w.name("name").value(n.name)
        w.name("isDir").value(n.isDir)
        w.name("size").value(n.size)
        if (n.time > 0) w.name("mtime").value(n.time / 1000)
        if (n.isDir) {
            w.name("children").beginArray()
            for (k in n.kids) writeNode(w, k)
            w.endArray()
        }
        w.endObject()
    }

    private fun buildReport(a: Analysis): String {
        val sb = StringBuilder()
        sb.append("SAMA — ").append(fileName).append("\n").append("=".repeat(34)).append("\n")
        for (k in 1..3) if (a.cnt[k] > 0)
            sb.append(t("kind_$k")).append(": ").append(num(a.cnt[k])).append(" (").append(fmtSize(a.sz[k])).append(")\n")
        sb.append(t("sum_lines", num(a.dups.size), num(a.sug.size), fmtSize(a.sugSize))).append("\n\n")
        for (s in sortedAccs(a)) {
            sb.append(t("kind_${s.kind}")).append(" — ").append(s.node.name).append("\n")
            sb.append(accSub(s)).append("\n")
            if (s.kind == 2)
                for (g in a.dups) for (n in g.sugg)
                    if (n.account() === s.node) sb.append("   🗑️ ").append(n.path()).append("  (").append(fmtSize(n.size)).append(")\n")
            sb.append("\n")
        }
        return sb.toString()
    }

    // ================= القائمة والتفاصيل =================
    private fun menu(n: Node) {
        val fav = favs.contains(n.path())
        val items = arrayOf(t("m_details"), t("m_rename"), t("m_copy"), t("m_cut"), t("m_delete"),
            if (fav) t("fav_rem") else t("fav_add"), t("m_select"))
        dlg().setTitle(n.name).setItems(items) { _, w ->
            when (w) {
                0 -> details(n)
                1 -> rename(n)
                2 -> { clips.clear(); clips.add(n); clipMove = false; updatePasteBar(); toast(t("copied")) }
                3 -> { clips.clear(); clips.add(n); clipMove = true; updatePasteBar(); toast(t("cut")) }
                4 -> confirmDelete(n)
                5 -> toggleFav(n)
                6 -> { selMode = true; sel.clear(); sel.add(n); mode = FILES; render() }
            }
        }.sheet()
    }

    private fun details(n: Node) {
        val a = n.account()
        val k = if (a == null) "—" else t("kind_" + Acc.kind(a.name))
        val sb = StringBuilder()
        sb.append(t("d_path")).append(": ").append(n.path()).append("\n")
        sb.append(t("d_type")).append(": ").append(if (n.isDir) t("d_folder") else t("d_file")).append("\n")
        sb.append(t("d_size")).append(": ").append(fmtSize(n.size)).append("\n")
        if (n.isDir) sb.append(t("d_counts", num(n.files), num(n.dirs))).append("\n")
        else sb.append(t("d_ext")).append(": ").append(if (n.type.isEmpty()) "—" else n.type).append("\n")
        if (n.time > 0)
            sb.append(t("d_date")).append(": ")
                .append(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(java.util.Date(n.time))).append("\n")
        sb.append(t("d_acc")).append(": ").append(a?.name ?: "—").append(" (").append(k).append(")")
        if (n.doc != null) sb.append("\n💽 ").append(t("real_item"))
        dlg().setTitle(n.name).setMessage(sb.toString()).setPositiveButton(t("close"), null).display()
    }

    private fun uniqueName(dir: Node, base: String): String {
        val names = HashSet<String>()
        for (k in dir.kids) names.add(k.name.lowercase())
        if (!names.contains(base.lowercase())) return base
        val dot = base.lastIndexOf('.')
        val stem = if (dot > 0) base.substring(0, dot) else base
        val ext = if (dot > 0) base.substring(dot) else ""
        var i = 1
        while (true) {
            val c = "$stem (" + t("copy_w") + " $i)$ext"
            if (!names.contains(c.lowercase())) return c
            i++
        }
    }

    private fun rename(n: Node) {
        val et = EditText(this)
        et.setText(n.name)
        dlg().setTitle(t("rename_title")).setView(et)
            .setPositiveButton(t("save")) { _, _ ->
                val nn = et.text.toString().trim()
                val p = n.parent
                if (nn.isEmpty() || nn.contains("/")) toast(t("invalid_name"))
                else if (p != null && p.kids.any { it !== n && it.name.equals(nn, ignoreCase = true) }) toast(t("exists"))
                else {
                    val o = n.path()
                    var failed = false
                    bg(t("working"), {
                        if (n.doc != null) {
                            val nu = Real.rename(contentResolver, n, nn)
                            if (nu == null) failed = true
                            else {
                                n.name = nn
                                n.doc = nu
                                val tr = State.tree
                                if (n.isDir && tr != null) Real.scanDir(contentResolver, tr, n)
                                recalc(root)
                            }
                        } else n.name = nn
                    }, {
                        if (failed) toast(t("real_fail")) else retarget(o, n.path())
                        afterChange()
                    })
                }
            }.setNegativeButton(t("cancel"), null).display()
    }

    private fun confirmDelete(n: Node) {
        val warn = if (n.doc != null) "\n\n" + t("real_warn") else ""
        dlg().setTitle(t("del_title"))
            .setMessage(t("del_q", n.name, fmtSize(n.size)) + warn)
            .setPositiveButton(t("delete")) { _, _ -> deleteMany(listOf(n)) }
            .setNegativeButton(t("cancel"), null).display()
    }

    private fun confirmApplyAll() {
        val a = an ?: return
        val warn = if (a.sug.any { it.doc != null }) "\n\n" + t("real_warn") else ""
        dlg().setTitle(t("apply_title"))
            .setMessage(t("apply_msg", num(a.sug.size), fmtSize(a.sugSize)) + warn)
            .setPositiveButton(t("apply")) { _, _ -> deleteMany(ArrayList(a.sug)) }
            .setNegativeButton(t("cancel"), null).display()
    }

    private fun dupDialog(g: Dup) {
        val sb = StringBuilder()
        sb.append(t("copies_n", fmtSize(g.size), num(g.items.size))).append("\n\n")
        for (n in g.items.take(40)) {
            val a = n.account()
            val k = if (a == null) "" else " [" + t("tag_" + Acc.kind(a.name)) + "]"
            sb.append(if (g.sugg.contains(n)) t("del_mark") else t("keep_mark"))
                .append(n.path()).append(k).append("\n")
        }
        if (g.items.size > 40) sb.append(t("and_more", num(g.items.size - 40)))
        val b = dlg().setTitle(g.items[0].name).setMessage(sb.toString())
            .setNegativeButton(t("close"), null)
        if (g.sugg.isNotEmpty())
            b.setPositiveButton(t("del_sugg_btn", num(g.sugg.size))) { _, _ -> deleteMany(ArrayList(g.sugg)) }
        b.display()
    }

    private fun deleteMany(nodes0: List<Node>) {
        val nodes = pruneNested(nodes0)
        var failed = 0
        bg(t("deleting"), {
            val ok = ArrayList<Node>()
            for (n in nodes) {
                if (n.doc != null) { if (Real.delete(contentResolver, n)) ok.add(n) else failed++ }
                else ok.add(n)
            }
            val byParent = HashMap<Node, HashSet<Node>>()
            for (n in ok) {
                val p = n.parent
                if (p != null) byParent.getOrPut(p) { HashSet() }.add(n)
            }
            for ((p, s) in byParent) {
                p.kids.removeAll(s)
                for (x in s) x.parent = null
            }
            recalc(root)
        }, {
            toast(if (failed > 0) t("deleted_partial", num(failed)) else t("deleted"))
            afterChange()
        })
    }

    private fun clone(n: Node): Node {
        val c = Node(n.name, n.isDir, n.size, n.type)
        c.files = n.files; c.dirs = n.dirs; c.time = n.time
        for (k in n.kids) c.add(clone(k))
        return c
    }

    private fun paste() {
        if (clips.isEmpty()) return
        val dest = stack.last()
        val items = ArrayList(clips)
        val move = clipMove
        val moves = ArrayList<Pair<String, String>>()
        var err: String? = null
        bg(t("pasting"), {
            var rescan = false
            for (c in items) {
                if (move) {
                    var p: Node? = dest
                    var bad = false
                    while (p != null) { if (p === c) { bad = true; break }; p = p.parent }
                    if (bad) { err = t("into_self"); continue }
                    val from = c.parent ?: continue
                    val oldP = c.path()
                    if (c.doc != null) {
                        if (Real.move(contentResolver, c, from, dest) == null) { err = t("real_fail"); continue }
                        from.kids.remove(c)
                        c.parent = null
                        rescan = true
                        moves.add(Pair(oldP, dest.path() + "/" + c.name))
                    } else {
                        from.kids.remove(c)
                        c.name = uniqueName(dest, c.name)
                        dest.add(c)
                        moves.add(Pair(oldP, c.path()))
                    }
                } else {
                    if (c.doc != null) {
                        if (Real.copy(contentResolver, c, dest) == null) err = t("real_fail") else rescan = true
                    } else {
                        val cp = clone(c)
                        cp.name = uniqueName(dest, c.name)
                        dest.add(cp)
                    }
                }
            }
            val tr = State.tree
            if (rescan && tr != null) Real.scanDir(contentResolver, tr, dest)
            recalc(root)
        }, {
            val em = err
            if (em != null) toast(em)
            if (move) { for (m in moves) retarget(m.first, m.second); clips.clear() }
            afterChange()
        })
    }

    // ================= دمج الحسابات =================
    private fun mergeStart() {
        val a = an ?: return
        if (root.doc != null) { toast(t("merge_real")); return }
        val accs = sortedAccs(a).map { it.node }
        if (accs.size < 2) { toast(t("merge_need")); return }
        val labels = accs.map { it.name + " (" + t("tag_" + Acc.kind(it.name)) + " • " + fmtSize(it.size) + ")" }.toTypedArray()
        val checked = BooleanArray(accs.size)
        dlg().setTitle(t("merge_pick")).setMultiChoiceItems(labels, checked) { _, i, c -> checked[i] = c }
            .setPositiveButton(t("next")) { _, _ ->
                val chosen = accs.filterIndexed { i, _ -> checked[i] }
                if (chosen.size < 2) toast(t("merge_need")) else mergeTarget(chosen)
            }.setNegativeButton(t("cancel"), null).display()
    }

    private fun mergeTarget(chosen: List<Node>) {
        val names = chosen.map { it.name }.toTypedArray()
        var choice = 0
        dlg().setTitle(t("merge_into")).setSingleChoiceItems(names, 0) { _, i -> choice = i }
            .setPositiveButton(t("merge_btn")) { _, _ -> mergeRun(chosen, chosen[choice]) }
            .setNegativeButton(t("cancel"), null).display()
    }

    private fun mergeInto(target: Node, src: Node, st: MergeStats) {
        val byName = HashMap<String, Node>()
        for (k in target.kids) byName[k.name.lowercase()] = k
        for (k in ArrayList(src.kids)) {
            val ex = byName[k.name.lowercase()]
            if (ex == null) {
                target.add(k); byName[k.name.lowercase()] = k; st.moved++
            } else if (ex.isDir && k.isDir) {
                mergeInto(ex, k, st)
            } else if (!ex.isDir && !k.isDir && ex.size == k.size) {
                st.skipped++
            } else {
                k.name = uniqueName(target, k.name)
                target.add(k); byName[k.name.lowercase()] = k; st.renamed++
            }
        }
    }

    private fun mergeRun(chosen: List<Node>, target: Node) {
        val st = MergeStats()
        bg(t("merging"), {
            for (s in chosen) {
                if (s === target) continue
                mergeInto(target, s, st)
                s.parent?.let { it.kids.remove(s) }
                s.parent = null
            }
            recalc(root)
        }, {
            dlg().setTitle(t("merge_done"))
                .setMessage(t("merge_res", target.name, num(st.moved), num(st.skipped), num(st.renamed)))
                .setPositiveButton(t("close"), null).display()
            afterChange()
        })
    }

    // ================= المفضلة =================
    private fun saveFavs() {
        getSharedPreferences("p", 0).edit().putStringSet("favs", HashSet(favs)).apply()
    }

    private fun toggleFav(n: Node) {
        val p = n.path()
        if (favs.contains(p)) { favs.remove(p); toast(t("fav_removed")) }
        else { favs.add(p); toast(t("fav_added")) }
        saveFavs()
        renderKeep()
    }

    private fun retarget(o: String, nw: String) {
        val rem = ArrayList<String>()
        val upd = ArrayList<String>()
        for (f in favs) {
            if (f == o) { rem.add(f); upd.add(nw) }
            else if (f.startsWith(o + "/")) { rem.add(f); upd.add(nw + f.substring(o.length)) }
        }
        favs.removeAll(rem.toSet())
        favs.addAll(upd)
        saveFavs()
    }
}
