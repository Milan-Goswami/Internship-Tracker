# InternTrack — Design Principles & Design System Tokens

**Document Status:** Approved Specification Draft  
**Version:** 1.0.0  

---

## 1. Visual Design Principles

InternTrack follows a **Developer-Tool / Linear-Inspired Aesthetic**:

1. **Extreme Technical Precision:** Clean, predictable grid lines, 1px subtle borders, high contrast readability.
2. **High Information Density:** Designed for power users managing 100+ applications. Compact rows, minimal padding bloat, progressive disclosure via side drawers.
3. **Dark-First Canvas:** Crafted for long duration usage (`#0B0F17` base canvas) with strictly tuned gray/slate tokens to eliminate eye strain.
4. **Restrained Color Usage:** Color is used strictly for semantic communication (e.g., status badges, actionable focus states). Zero gratuitous AI glowing gradients or decorative hero illustrations.
5. **Keyboard & Micro-interaction First:** Ultra-crisp focus rings (`2px solid #6366F1`), subtle hover backgrounds (`#1F2937`), instant response feel.

---

## 2. Color Palette & CSS Tokens Specification

### Base Canvas & Surface Palette
```css
:root {
  /* Canvas & Surface Layers */
  --color-canvas-bg:       #0B0F17; /* Deep Obsidian Canvas */
  --color-surface-base:    #111827; /* Main Panel Background */
  --color-surface-overlay: #1F2937; /* Card & Row Background */
  --color-surface-hover:   #374151; /* Interactive Hover State */
  
  /* Borders & Hairlines */
  --color-border-subtle:   #1F2937; /* 1px Hairline Separator */
  --color-border-default:  #374151; /* Input & Card Border */
  --color-border-strong:   #4B5563; /* Active / Focus Border */

  /* Typography Text Tokens */
  --color-text-primary:    #F9FAFB; /* High Contrast Headers & Titles */
  --color-text-secondary:  #9CA3AF; /* Body Text & Labels */
  --color-text-muted:      #6B7280; /* Metadata, Dates, Subtitles */
  --color-text-inverse:    #111827; /* Dark Text on Light Badges */

  /* Brand Primary Accent */
  --color-accent-primary:  #6366F1; /* Indigo Accent */
  --color-accent-hover:    #4F46E5; /* Darker Indigo Hover */
  --color-accent-subtle:   rgba(99, 102, 241, 0.12); /* Selected Row Fill */

  /* Semantic Status Colors (Badges & Indicators) */
  --color-status-saved:      #64748B; /* Slate Gray */
  --color-status-applied:    #3B82F6; /* Crisp Blue */
  --color-status-screening:  #8B5CF6; /* Purple */
  --color-status-interview:  #F59E0B; /* Amber */
  --color-status-offer:      #10B981; /* Emerald Green */
  --color-status-rejected:   #EF4444; /* Rose Red */
  --color-status-withdrawn:  #71717A; /* Zinc Neutral */
}
```

---

## 3. Typography Scale & Fonts

- **Font Family:** `Inter`, `-apple-system`, `BlinkMacSystemFont`, `Segoe UI`, `Roboto`, `sans-serif`.
- **Letter Spacing Tracking:** Tight letter spacing (`-0.011em`) for headings to achieve a sharp, modern developer feel.

```css
:root {
  /* Font Sizes */
  --font-size-xs:   11px; /* Badges, Monospace Code, Micro-labels */
  --font-size-sm:   13px; /* Table Data, Body Copy, Secondary Buttons */
  --font-size-md:   14px; /* Standard Body Text, Form Inputs */
  --font-size-lg:   16px; /* Section Titles, Subheaders */
  --font-size-xl:   20px; /* Primary Page Headers */
  --font-size-2xl:  24px; /* Dashboard Key Metric Numbers */

  /* Font Weights */
  --font-weight-regular:  400;
  --font-weight-medium:   500;
  --font-weight-semibold: 600;
  --font-weight-bold:     700;

  /* Line Heights */
  --line-height-tight:  1.2;
  --line-height-normal: 1.5;
}
```

---

## 4. Spacing Scale & Grid System

All layouts operate on a strict **4px Base Grid**:

| Token Name | Pixel Value | Typical Usage |
| :--- | :--- | :--- |
| `--space-1` | `4px` | Micro-gap between badge icon & text |
| `--space-2` | `8px` | Compact button padding, input vertical padding |
| `--space-3` | `12px` | Table cell padding, card inner padding |
| `--space-4` | `16px` | Standard component gap, container margin |
| `--space-6` | `24px` | Section spacing, panel header padding |
| `--space-8` | `32px` | Major layout grid column gap |
| `--space-12` | `48px` | Screen section vertical spacing |

---

## 5. Core Component Tokens & Specifications

### 5.1 Buttons
- **Primary CTA:** Background `--color-accent-primary`, Text `#FFFFFF`, Border None, Radius `4px`, Font Weight `500`. Hover: `--color-accent-hover`.
- **Secondary Button:** Background `--color-surface-overlay`, Text `--color-text-primary`, Border `1px solid --color-border-default`, Radius `4px`.
- **Ghost Button:** Background Transparent, Text `--color-text-secondary`. Hover: Background `--color-surface-hover`.

### 5.2 Status Badges
- **Anatomy:** Compact inline-flex chip, height `22px`, padding `2px 8px`, border-radius `4px`, font size `11px`, font weight `600`, text-transform `uppercase`.
- **Combination:** Every badge MUST contain a status dot icon + semantic text (e.g. Amber dot + "INTERVIEWING").

### 5.3 Data Tables
- **Header Row:** Height `36px`, background `--color-surface-base`, text `--color-text-muted`, font-size `12px`, text-transform `uppercase`, letter-spacing `0.05em`.
- **Body Rows:** Height `44px`, border-bottom `1px solid --color-border-subtle`, hover background `--color-surface-overlay`.

### 5.4 Slide-Over Drawers (Detail Panel)
- **Width:** `480px` fixed right drawer.
- **Background:** `--color-surface-base`, border-left `1px solid --color-border-default`.
- **Shadow:** Left edge box shadow `0 20px 25px -5px rgba(0,0,0,0.5)`.
