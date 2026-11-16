---
name: Technical Precision
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e5eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e4fe'
  on-surface: '#0b1c30'
  on-surface-variant: '#45464d'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#76777d'
  outline-variant: '#c6c6cd'
  surface-tint: '#565e74'
  primary: '#000000'
  on-primary: '#ffffff'
  primary-container: '#131b2e'
  on-primary-container: '#7c839b'
  inverse-primary: '#bec6e0'
  secondary: '#0051d5'
  on-secondary: '#ffffff'
  secondary-container: '#316bf3'
  on-secondary-container: '#fefcff'
  tertiary: '#000000'
  on-tertiary: '#ffffff'
  tertiary-container: '#002113'
  on-tertiary-container: '#009668'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dae2fd'
  primary-fixed-dim: '#bec6e0'
  on-primary-fixed: '#131b2e'
  on-primary-fixed-variant: '#3f465c'
  secondary-fixed: '#dbe1ff'
  secondary-fixed-dim: '#b4c5ff'
  on-secondary-fixed: '#00174b'
  on-secondary-fixed-variant: '#003ea8'
  tertiary-fixed: '#6ffbbe'
  tertiary-fixed-dim: '#4edea3'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  headline-sm:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  title-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 18px
  label-md:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
  numeric-stat:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '700'
    lineHeight: 28px
    letterSpacing: -0.01em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 0.75rem
  gutter-sm: 0.5rem
  margin: 1rem
  margin-compact: 0.75rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1rem
  space-xl: 1.5rem
---

## Brand & Style

This design system serves a modern, technical, high-density mobile utility operating natively within Android portrait ecosystems. It synthesizes Google’s Material 3 structural foundations with the rigor of an industrial-grade diagnostic terminal. Designed for power users, sysadmins, and storage-conscious operators, the aesthetic strikes an intentional balance: data-dense information architecture counterbalanced by generous micro-whitespace and tactile, card-anchored rhythm.

The design movement combines **Corporate Modern** with **Technical Minimalism**. Visual clarity takes absolute precedence over ornamental flair. Surfaces are crisp, elevation is conveyed primarily through subtle tonal contrast and razor-thin borders, and interactions mirror native Android tactile states—relying on precise ripples, dynamic pill indicators, and predictable vertical stacking.

## Colors

The palette establishes an authoritative, deep slate foundation with an expansive system of semantic category accents to differentiate file systems, account layers, and disk analysis categories.

### Foundation & Surfaces
- **Top App Bar / Dominant Shell:** `#0F172A` (Deep Slate Navy). Serves as the anchor for both light and dark modes to enforce unified brand presence.
- **Light Theme:**
  - Base Canvas Background: `#F1F5F9` (Slate Tint)
  - Card & Container Surface: `#FFFFFF`
  - Subtle Border / Divider: `#E2E8F0`
  - High-Emphasis Text: `#0F172A`
  - Medium-Emphasis Text: `#64748B`
- **Dark Theme:**
  - Base Canvas Background: `#0B1220` (Midnight Obsidian)
  - Card & Container Surface: `#162033` (Elevated Deep Navy)
  - Subtle Border / Divider: `#1E293B`
  - High-Emphasis Text: `#F1F5F9`
  - Medium-Emphasis Text: `#94A3B8`

### Functional & Semantic Accents
- **Destructive / Error:** `#EF4444` (Critical Red)
- **Account Status Tiers:**
  - Full Account: `#10B981` (Emerald Green)
  - Temporary Account: `#F43F5E` (Rose)
  - Unclassified / Guest: `#64748B` (Slate Gray)

### Section Identification Palette
Every core storage vertical utilizes an isolated chromatic accent to maintain persistent mental mapping across lists, chips, and donut chart arcs:
- **Home:** `#2563EB` (Royal Blue)
- **Files:** `#0EA5E9` (Sky Blue)
- **Accounts:** `#10B981` (Emerald Green)
- **Duplicates:** `#F59E0B` (Amber)
- **Cleanup:** `#F97316` (Orange)
- **Compare:** `#14B8A6` (Teal)
- **Statistics:** `#8B5CF6` (Purple)
- **Favorites:** `#EC4899` (Pink)
- **Search:** `#06B6D4` (Cyan)
- **Tools:** `#475569` (Slate)

## Typography

The type system is driven entirely by **Inter**, selected for its mechanical neutrality, optical balance at micro sizes, and extensive OpenType feature support.

### Numerical Data Execution
Storage analytics, file manifests, and system thresholds require rigorous visual stability. All numeric values (byte sizes, item counts, durations, and percentages) must render with tabular figures:
- Enable OpenType feature settings: `font-feature-settings: "tnum" 1, "cv05" 1, "cv11" 1`.
- Standardize all disk space notations (e.g., `128.4 GB`, `42 MB`) with a non-breaking space between the value and the unit label.

### Mobile Scaling Rules
- Mobile portrait canvases are constrained horizontally: avoid using font sizes above 32px to eliminate line-wrap truncation on critical metadata.
- For compact dashboard cards, secondary stats scale using `numeric-stat` (22px bold) rather than traditional responsive display headings.

## Layout & Spacing

The layout model is tailored for Android mobile portrait usage across standard 360dp to 430dp viewport widths. It employs a **4-column fluid mobile grid** anchored by fixed outer canvas margins (`1rem` / 16dp) and compact gutters (`0.75rem` / 12dp).

### Rhythmic Discipline
- The layout relies on an absolute **4dp base grid** with an **8dp primary cadence**.
- Internal card padding defaults to `space-lg` (16dp) for critical overview components and contracts to `space-md` (12dp) for high-density diagnostic lists.
- Vertical space between distinct structural sections is maintained at `space-xl` (24dp), avoiding unnecessary content separation.

### Edge Insets & Safe Areas
- Account for native Android system bars (`status bar` and gesture navigation inset). Top bars pin to the hardware boundary, integrating `padding-top: env(safe-area-inset-top)`.
- Bottom navigation and floating action bars incorporate elevated margins plus `env(safe-area-inset-bottom)` to clear standard touch-gesture bars.

## Elevation & Depth

This design system avoids high-blur, dramatic drop shadows in favor of **structural containment through hairline boundaries and soft, tactile ambient diffusion**.

### Layering Hierarchy
- **Canvas Base (Level 0):** `#F1F5F9` in light mode; `#0B1220` in dark mode. No elevation properties.
- **Content Cards (Level 1):**
  - Light Mode: 1px border colored `#E2E8F0` coupled with an ambient shadow: `0 1px 3px rgba(15, 23, 42, 0.04), 0 4px 8px rgba(15, 23, 42, 0.02)`.
  - Dark Mode: 1px border colored `#1E293B` on `#162033` surface. Zero shadow; separation is achieved purely through chromatic contrast against the `#0B1220` canvas.
- **Sticky App Bar & Bottom Navigation (Level 2):** Elevated via a hairline edge border (`#1E293B` on dark, `#E2E8F0` on light) and a directional ambient shadow: `0 4px 12px rgba(15, 23, 42, 0.08)`.
- **Modals, Floating Toolsheets & Context Menus (Level 3):** Standard Material elevation using an explicit tinted ambient spread: `0 8px 24px rgba(15, 23, 42, 0.20)` with a 1px border highlight.

## Shapes

The geometry adheres to modern Material 3 radii while retaining the structured posture expected of a precision file system.

- **Content & Stat Cards:** `1rem` (16px) corner radius. Produces a cohesive, modern container without encroaching on edge-anchored data grids.
- **Action Buttons, Filter Pills & Badges:** `9999px` (Full Pill) corner radius. Directly communicates tap affordance and sets interactive components apart from content cards.
- **Text Inputs, Progress Tracks & Modals:** `0.75rem` (12px) corner radius.
- **Avatar & File Type Glyphs:** `0.5rem` (8px) for square iconography; full circle for user profile avatars.

## Components

### Buttons & Interactive Controls
- **Primary Pill Action:** Height of 44dp, fully rounded (`rounded-full`), filled with Section Accent or Primary Blue (`#2563EB`). Text rendered in bold white `label-lg`.
- **Secondary / Outlined Pill:** Height of 40dp, 1px border using surface dividers, transparent background, text matching category context.
- **Destructive Pill:** Accent fill using `#EF4444` with white text; used exclusively for file purge and account unlink actions.

### Horizontal Chip Scrollbar (Filter Rows)
- Displayed horizontally beneath top toolbars with edge-to-edge content overflow.
- **Unselected Chip:** Background matches container surface (`#FFFFFF` light / `#162033` dark), 1px border, `label-md` neutral text.
- **Selected Chip:** Solid background derived from the active category accent (e.g., Duplicates Amber `#F59E0B`, Files Sky Blue `#0EA5E9`), with high-contrast text. Height 32dp, `rounded-full`, left icon at 16dp.

### Stat Cards & Metric Containers
- Surface styled per Elevation Level 1 with 16px corner radius (`rounded-lg`).
- Internal layout features a three-tier vertical lockup:
  1. Top metadata header: Micro category icon + section badge + trailing contextual action icon.
  2. Metric display: Tabular digit readout (`numeric-stat`) showing primary storage consumption or record counts.
  3. Supporting footer: Micro progress track or delta tag (`+2.4% vs last week`).

### Progress Indicators & Donut Charts
- **Linear Progress Bars:** Track height fixed at 6dp or 8dp with full-radius caps (`rounded-full`). Background uses a 10% opacity shade of the category accent; active bar uses 100% solid category accent.
- **Storage Donut Charts:** Thick-stroke SVG rings (stroke width: 14dp to 18dp). Ring divisions represent storage tiers using their corresponding Section Accents. Active segments produce an expanded stroke cap with smooth transitions.

### Account Status Tags
- Micro pill badges (height 20dp, padding 0 8dp, `label-md` font size 11px):
  - **Full Account:** Emerald `#10B981` background at 12% opacity, `#059669` text (light) or `#34D399` text (dark).
  - **Temporary Account:** Rose `#F43F5E` background at 12% opacity, `#E11D48` text (light) or `#FB7185` text (dark).
  - **Unclassified:** Slate `#64748B` background at 12% opacity, `#475569` text (light) or `#94A3B8` text (dark).

### Lists & File Rows
- Row items fixed at 56dp height for standard density; 64dp for rich media previews.
- Left-to-right hierarchy: 40dp rounded square leading icon (tinted section accent) -> Title (`headline-sm`) stacked above path/date (`body-sm`) -> Right-aligned file size in tabular numerals (`title-md`) -> Context menu trigger (three dots).
- Dividers are indented 68dp from the left edge to align precisely with text content, avoiding icon collision.

### Checkboxes & Selection Mechanics
- 20dp squircle controls with a 4dp radius.
- Unchecked: 1.5px stroke `#64748B`.
- Checked: Solid category accent fill with a 1.5px white checkmark icon. Supports multi-selection toolbars floating along the bottom margin during bulk duplicate cleanup.