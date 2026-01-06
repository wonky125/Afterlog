# AfterLog - Implementation Script for AI

Use this script when providing the codebase to another AI for implementation or modification.

---

## Project Overview

**Project Name:** AfterLog  
**Type:** Android-style mobile web application  
**Theme:** 1920s Lovecraftian horror investigation archive system  
**Design System:** Material 3 principles with dark horror aesthetic

---

## Design System Summary

### Color Palette
- **Primary Accent:** `#8B0000` (Blood Wine Red) - Used for CTAs, active states, emphasis
- **Main Background:** `#0a0a0a` (Deep Black) - App background
- **Card Background:** `#1a1a1a` (Charcoal Grey) - Content containers
- **Borders:** `#2a2a2a` (Dark Grey) - Dividers, card borders
- **Secondary Text:** `#666666` (Medium Grey) - Labels, metadata
- **Body Text:** `#999999` (Light Grey) - Descriptions
- **Headings:** `#ffffff` (White) - Titles, important text

### Typography Style
- **H1 Headers:** `text-[#8B0000] tracking-[0.25em]` - Wide letter spacing for gothic feel
- **H2 Headers:** `text-white tracking-wide`
- **Body Text:** `text-[#999999] text-sm`
- **Labels:** `text-[#8B0000] text-xs tracking-[0.3em]` - Uppercase with wide spacing
- **Small Text:** `text-[#666666] text-xs tracking-wide`

### Component Patterns
- **Primary Buttons:** `bg-[#8B0000] hover:bg-[#A00000] text-white px-6 py-3 rounded-xl transition-colors`
- **Cards:** `bg-gradient-to-br from-[#1a1a1a] to-[#0f0f0f] border border-[#2a2a2a] rounded-2xl p-4`
- **Icon Buttons:** `p-3 hover:bg-white/5 rounded-full transition-colors`
- **Hover States:** `hover:border-[#8B0000]` on cards and interactive elements

### Layout Standards
- **Border Radius:** `rounded-xl` (12px), `rounded-2xl` (16px)
- **Container Padding:** `px-6` (24px)
- **Card Spacing:** `gap-3` (12px)
- **Section Margins:** `mb-8` (32px)

---

## Application Structure

### Screen Flow
```
Main Screen (MainScreen.tsx)
  ├─> Investigation Detail (VideoPlayer.tsx + InvestigationReport.tsx)
  │     └─> Newspaper View (NewspaperView.tsx) - Click evidence photos
  └─> Design Guide (DesignGuide.tsx)
```

### Main Screens

#### 1. Main Screen (`/components/MainScreen.tsx`)
**Purpose:** Home dashboard showing case overview and quick actions

**Features:**
- App header with "AfterLog" branding and settings icon
- Hero section with featured case image and "Continue Investigation" CTA
- Quick Actions grid (2 columns):
  - View Cases button
  - Design Guide button
- Recent Cases list showing 3 cases with:
  - Case ID and title
  - Date with clock icon
  - Status badge (Active/Under Review/Closed)
  - Progress bar
- Bottom navigation bar with 4 tabs

**Navigation:**
- Click "Continue Investigation", "View Cases", or any case card → Goes to Investigation screen
- Click "Design Guide" → Goes to Design Guide screen

#### 2. Investigation Screen (Split View)
**Purpose:** Display case investigation with video player and evidence report

**Top Section - Video Player (`/components/VideoPlayer.tsx`):**
- Cinematic video player with dark Lovecraftian board game scene
- Header with back arrow (if `onBack` prop provided) and AfterLog title
- Center play/pause button with red accent
- Bottom controls:
  - Play/pause, timestamp (02:15 / 06:42)
  - Volume and fullscreen buttons
  - Progress bar with red accent

**Bottom Section - Investigation Report (`/components/InvestigationReport.tsx`):**
- Header: "INVESTIGATION REPORT" with case number
- Newspaper-style header: "AfterLog • EST. 1872"
- Scrollable evidence cards (4 items):
  - Small sepia-toned photo on left (clickable - RED BOX in reference image)
  - Title, date, description on right
  - Classified/Confidential footer
  
**Interactive Feature:**
- **Click any evidence photo** → Opens NewspaperView modal

#### 3. Newspaper View (`/components/NewspaperView.tsx`)
**Purpose:** Full-screen vintage newspaper report with download functionality

**Features:**
- Full-screen modal with dark overlay
- Close button (X) top-left
- Download buttons top-right:
  - PNG button (exports as image)
  - PDF button (exports as PDF)
- Newspaper content:
  - Vintage paper texture background (#f5e6d3)
  - "AfterLog" masthead with decorative borders
  - Case headline and ID
  - Evidence photo with caption (bordered, sepia tone)
  - Multi-paragraph article text with serif font
  - Official police statement box (red border)
  - Investigation log timeline with timestamps
  - Classified document footer

**Technical Implementation:**
- Uses `html2canvas` for PNG export
- Uses `jspdf` for PDF export
- Serif font family for authentic newspaper feel
- Sepia filter on images
- Text-indent for newspaper paragraph style

#### 4. Design Guide (`/components/DesignGuide.tsx`)
**Purpose:** Reference documentation for the design system

**Sections:**
- Design Philosophy explanation
- Color Palette showcase (7 colors with hex codes and usage)
- Typography examples (6 text styles with actual demos)
- Component examples (Primary Button, Card, Icon Button with live demos)
- Spacing & Layout specifications
- Effects & States demonstrations

---

## Key Interactions

1. **Navigation Flow:**
   - Main → Investigation: Click any case or "Continue Investigation"
   - Investigation → Main: Click back arrow in video player
   - Main → Design Guide: Click "Design Guide" button or bottom nav
   - Design Guide → Main: Click back arrow

2. **Evidence Photo Click:**
   - Small evidence photos in Investigation Report are clickable
   - Shows hover effect (border changes to red)
   - Opens full-screen Newspaper View modal
   - Modal is dismissible via X button

3. **Download Functionality:**
   - PNG: Captures newspaper view as high-res image (scale: 2)
   - PDF: Generates A4 portrait PDF document
   - File naming: `afterlog-{caseId}.png/pdf`

---

## Special Effects & Styling

### Gradients
- Cards: `bg-gradient-to-br from-[#1a1a1a] to-[#0f0f0f]`
- Headers: `bg-gradient-to-b from-black to-[#0a0a0a]`
- Image overlays: `bg-gradient-to-t from-[#0a0a0a] via-black/60 to-transparent`

### Transitions
- All interactive elements: `transition-colors` or `transition-all duration-300`
- Hover states are subtle but noticeable
- Border color changes on hover: `hover:border-[#8B0000]`

### Visual Effects
- Sepia filter: `.sepia { filter: sepia(0.7) contrast(1.1); }` (in globals.css)
- Red overlay on evidence photos: `bg-[#8B0000]/10`
- Paper texture in newspaper: Repeating linear gradient for lines

### Typography Details
- **Wide letter-spacing** throughout (tracking-wide, tracking-[0.25em])
- **Uppercase labels** with extra spacing for dramatic effect
- **Serif fonts** in newspaper view only
- **Sans-serif** (default) everywhere else

---

## Implementation Notes

### Required Libraries
```javascript
import { Play, Pause, Maximize2, Volume2, MoreVertical, ArrowLeft, 
         ChevronDown, Download, X, FileImage, FileText, Type, 
         Palette, Layout, Zap, Clock, Archive, Settings } from 'lucide-react';
```

### State Management
- App.tsx manages main screen routing with `useState<'main' | 'investigation' | 'design-guide'>`
- InvestigationReport manages newspaper modal with `useState<evidenceItem | null>`
- VideoPlayer manages play state with `useState<boolean>`

### Responsive Considerations
- App is designed for mobile-first (portrait orientation)
- Max-width: 2xl on newspaper view for readability
- Scrollable content areas use `overflow-y-auto`
- Fixed headers/footers with flex layout

### Performance
- Images use `ImageWithFallback` component
- Lazy imports for html2canvas and jspdf (only loaded on download)
- Transitions are GPU-accelerated (opacity, transform)

---

## Implementation Instructions for AI

**When implementing this design:**

1. **Preserve the exact color palette** - these specific hex codes create the horror atmosphere
2. **Maintain wide letter-spacing** - essential for the 1920s occult aesthetic  
3. **Keep the gradient patterns** - they add depth to the dark UI
4. **Ensure all interactive elements have hover states** - border color changes to #8B0000
5. **Use rounded-xl/2xl consistently** - Material 3 characteristic
6. **Implement the newspaper view exactly** - it's the key feature for downloads
7. **Test the download functionality** - both PNG and PDF must work
8. **Preserve the navigation flow** - back buttons and screen transitions

**Critical Styling Rules:**
- Never use bright backgrounds - always dark (#0a0a0a, #1a1a1a)
- Always use #8B0000 for emphasis/CTAs - no other primary colors
- All cards must have the gradient: `from-[#1a1a1a] to-[#0f0f0f]`
- All borders default to #2a2a2a, hover to #8B0000
- Text hierarchy: white for titles, #999999 for body, #666666 for labels

---

## Code Provided

The complete codebase includes:
- `/App.tsx` - Main application router
- `/components/MainScreen.tsx` - Dashboard/home screen
- `/components/VideoPlayer.tsx` - Video player component
- `/components/InvestigationReport.tsx` - Evidence list with clickable photos
- `/components/NewspaperView.tsx` - Full-screen newspaper modal with downloads
- `/components/DesignGuide.tsx` - Design system documentation
- `/styles/globals.css` - Custom CSS including sepia filter
- `/STYLE_GUIDE.md` - Quick reference for styling

**Instruction to AI:**
"Please implement this AfterLog application exactly as designed. The codebase is provided. Pay special attention to the color palette (#8B0000 for accents, dark backgrounds), wide letter-spacing, and the newspaper download functionality. The evidence photos in the Investigation Report must be clickable and open the NewspaperView modal. Maintain the exact navigation flow and all interactive states."
