import { ArrowLeft, Type, Palette, Layout, Zap } from 'lucide-react';

interface DesignGuideProps {
  onBack: () => void;
}

const colors = [
  { name: 'Blood Wine Red', hex: '#8B0000', usage: 'Primary accent, CTAs, active states' },
  { name: 'Deep Black', hex: '#0a0a0a', usage: 'Main background' },
  { name: 'Charcoal Grey', hex: '#1a1a1a', usage: 'Card backgrounds, secondary surfaces' },
  { name: 'Dark Grey', hex: '#2a2a2a', usage: 'Borders, dividers' },
  { name: 'Medium Grey', hex: '#666666', usage: 'Secondary text, labels' },
  { name: 'Light Grey', hex: '#999999', usage: 'Body text, descriptions' },
  { name: 'Pure White', hex: '#ffffff', usage: 'Headings, primary text' },
];

const typographyExamples = [
  { label: 'H1 Heading', class: 'text-white tracking-[0.25em]', text: 'ARKHAM LOG' },
  { label: 'H2 Heading', class: 'text-white tracking-wide', text: 'The Miskatonic Disappearance' },
  { label: 'H3 Heading', class: 'text-white tracking-wide', text: 'Investigation Report' },
  { label: 'Body Text', class: 'text-[#999999] text-sm', text: 'Three researchers from Miskatonic University vanished during an expedition.' },
  { label: 'Label', class: 'text-[#8B0000] text-xs tracking-[0.3em]', text: 'CASE NO. 1923-AH-047' },
  { label: 'Small Text', class: 'text-[#666666] text-xs tracking-wide', text: 'March 15, 1923' },
];

const componentExamples = [
  {
    name: 'Primary Button',
    description: 'Main actions, CTAs',
    class: 'bg-[#8B0000] hover:bg-[#A00000] text-white px-6 py-3 rounded-xl transition-colors',
  },
  {
    name: 'Card',
    description: 'Content containers',
    class: 'bg-gradient-to-br from-[#1a1a1a] to-[#0f0f0f] border border-[#2a2a2a] rounded-2xl p-4',
  },
  {
    name: 'Icon Button',
    description: 'Minimal actions',
    class: 'p-3 hover:bg-white/5 rounded-full transition-colors',
  },
];

export function DesignGuide({ onBack }: DesignGuideProps) {
  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <header className="flex-shrink-0 bg-gradient-to-b from-black to-[#0a0a0a] border-b-2 border-[#8B0000] px-6 py-6">
        <div className="flex items-center gap-4 mb-4">
          <button
            onClick={onBack}
            className="p-2 hover:bg-white/5 rounded-full transition-colors"
          >
            <ArrowLeft className="w-6 h-6 text-[#8B0000]" />
          </button>
          <div className="flex-1">
            <h1 className="text-[#8B0000] tracking-[0.25em] mb-1">DESIGN GUIDE</h1>
            <p className="text-[#666666] text-xs tracking-widest">ARKHAM LOG UI SYSTEM</p>
          </div>
        </div>
      </header>

      {/* Content */}
      <div className="flex-1 overflow-y-auto px-6 py-6">
        {/* Introduction */}
        <section className="mb-8">
          <div className="bg-gradient-to-br from-[#1a1a1a] to-[#0f0f0f] border border-[#2a2a2a] rounded-2xl p-6">
            <h2 className="text-white mb-3 tracking-wide">Design Philosophy</h2>
            <p className="text-[#999999] text-sm leading-relaxed mb-4">
              The Arkham Log design system combines Material 3 principles with a dark, 
              immersive Lovecraftian horror aesthetic. The interface balances modern 
              usability with atmospheric 1920s occult styling.
            </p>
            <div className="flex flex-wrap gap-2">
              <span className="bg-[#8B0000]/20 text-[#8B0000] text-xs px-3 py-1 rounded-full">
                Material 3
              </span>
              <span className="bg-[#8B0000]/20 text-[#8B0000] text-xs px-3 py-1 rounded-full">
                Dark Theme
              </span>
              <span className="bg-[#8B0000]/20 text-[#8B0000] text-xs px-3 py-1 rounded-full">
                Horror Aesthetic
              </span>
            </div>
          </div>
        </section>

        {/* Color Palette */}
        <section className="mb-8">
          <div className="flex items-center gap-3 mb-4">
            <div className="bg-[#8B0000]/10 w-10 h-10 rounded-xl flex items-center justify-center">
              <Palette className="w-5 h-5 text-[#8B0000]" />
            </div>
            <h2 className="text-white tracking-wide">Color Palette</h2>
          </div>

          <div className="space-y-3">
            {colors.map((color) => (
              <div
                key={color.hex}
                className="bg-gradient-to-br from-[#1a1a1a] to-[#0f0f0f] border border-[#2a2a2a] rounded-2xl p-4 flex items-center gap-4"
              >
                <div
                  className="w-16 h-16 rounded-xl border-2 border-[#2a2a2a] flex-shrink-0"
                  style={{ backgroundColor: color.hex }}
                />
                <div className="flex-1 min-w-0">
                  <div className="text-white text-sm mb-1">{color.name}</div>
                  <div className="text-[#8B0000] text-xs tracking-wider mb-1">{color.hex}</div>
                  <div className="text-[#666666] text-xs">{color.usage}</div>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Typography */}
        <section className="mb-8">
          <div className="flex items-center gap-3 mb-4">
            <div className="bg-[#8B0000]/10 w-10 h-10 rounded-xl flex items-center justify-center">
              <Type className="w-5 h-5 text-[#8B0000]" />
            </div>
            <h2 className="text-white tracking-wide">Typography</h2>
          </div>

          <div className="space-y-4">
            {typographyExamples.map((example, index) => (
              <div
                key={index}
                className="bg-gradient-to-br from-[#1a1a1a] to-[#0f0f0f] border border-[#2a2a2a] rounded-2xl p-4"
              >
                <div className="text-[#666666] text-xs tracking-wider mb-2">{example.label}</div>
                <div className={example.class}>{example.text}</div>
                <div className="mt-3 pt-3 border-t border-[#2a2a2a]">
                  <code className="text-[#8B0000] text-xs break-all">
                    className="{example.class}"
                  </code>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Components */}
        <section className="mb-8">
          <div className="flex items-center gap-3 mb-4">
            <div className="bg-[#8B0000]/10 w-10 h-10 rounded-xl flex items-center justify-center">
              <Layout className="w-5 h-5 text-[#8B0000]" />
            </div>
            <h2 className="text-white tracking-wide">Components</h2>
          </div>

          <div className="space-y-4">
            {componentExamples.map((component, index) => (
              <div
                key={index}
                className="bg-gradient-to-br from-[#1a1a1a] to-[#0f0f0f] border border-[#2a2a2a] rounded-2xl p-4"
              >
                <div className="text-white text-sm mb-1">{component.name}</div>
                <div className="text-[#666666] text-xs mb-4">{component.description}</div>
                
                {/* Demo */}
                <div className="bg-[#0a0a0a] rounded-xl p-4 mb-3 flex items-center justify-center">
                  {component.name === 'Primary Button' && (
                    <button className={component.class}>
                      Action Button
                    </button>
                  )}
                  {component.name === 'Card' && (
                    <div className={component.class + ' w-full'}>
                      <div className="text-white text-sm mb-2">Card Title</div>
                      <div className="text-[#999999] text-xs">Card content goes here</div>
                    </div>
                  )}
                  {component.name === 'Icon Button' && (
                    <button className={component.class}>
                      <Zap className="w-5 h-5 text-[#8B0000]" />
                    </button>
                  )}
                </div>

                {/* Code */}
                <div className="bg-[#0a0a0a] rounded-xl p-3 overflow-x-auto">
                  <code className="text-[#8B0000] text-xs whitespace-pre-wrap break-all">
                    className="{component.class}"
                  </code>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Spacing & Layout */}
        <section className="mb-8">
          <div className="flex items-center gap-3 mb-4">
            <div className="bg-[#8B0000]/10 w-10 h-10 rounded-xl flex items-center justify-center">
              <Layout className="w-5 h-5 text-[#8B0000]" />
            </div>
            <h2 className="text-white tracking-wide">Spacing & Layout</h2>
          </div>

          <div className="bg-gradient-to-br from-[#1a1a1a] to-[#0f0f0f] border border-[#2a2a2a] rounded-2xl p-6">
            <div className="space-y-3 text-sm">
              <div className="flex items-center justify-between">
                <span className="text-[#999999]">Border Radius</span>
                <span className="text-white">12px (rounded-xl), 16px (rounded-2xl)</span>
              </div>
              <div className="h-px bg-[#2a2a2a]" />
              <div className="flex items-center justify-between">
                <span className="text-[#999999]">Spacing Scale</span>
                <span className="text-white">4px base unit (Tailwind default)</span>
              </div>
              <div className="h-px bg-[#2a2a2a]" />
              <div className="flex items-center justify-between">
                <span className="text-[#999999]">Container Padding</span>
                <span className="text-white">24px (px-6)</span>
              </div>
              <div className="h-px bg-[#2a2a2a]" />
              <div className="flex items-center justify-between">
                <span className="text-[#999999]">Gap Between Cards</span>
                <span className="text-white">12px (gap-3)</span>
              </div>
            </div>
          </div>
        </section>

        {/* Effects */}
        <section className="mb-8">
          <div className="flex items-center gap-3 mb-4">
            <div className="bg-[#8B0000]/10 w-10 h-10 rounded-xl flex items-center justify-center">
              <Zap className="w-5 h-5 text-[#8B0000]" />
            </div>
            <h2 className="text-white tracking-wide">Effects & States</h2>
          </div>

          <div className="bg-gradient-to-br from-[#1a1a1a] to-[#0f0f0f] border border-[#2a2a2a] rounded-2xl p-6">
            <div className="space-y-4">
              <div>
                <div className="text-white text-sm mb-2">Hover Effects</div>
                <div className="text-[#999999] text-xs mb-3">
                  Subtle background changes, border color shifts to #8B0000
                </div>
                <button className="bg-[#1a1a1a] border border-[#2a2a2a] hover:border-[#8B0000] px-4 py-2 rounded-xl text-white text-sm transition-all">
                  Hover Me
                </button>
              </div>

              <div className="h-px bg-[#2a2a2a]" />

              <div>
                <div className="text-white text-sm mb-2">Gradients</div>
                <div className="text-[#999999] text-xs mb-3">
                  bg-gradient-to-br from-[#1a1a1a] to-[#0f0f0f]
                </div>
                <div className="bg-gradient-to-br from-[#1a1a1a] to-[#0f0f0f] h-16 rounded-xl border border-[#2a2a2a]" />
              </div>

              <div className="h-px bg-[#2a2a2a]" />

              <div>
                <div className="text-white text-sm mb-2">Transitions</div>
                <div className="text-[#999999] text-xs">
                  transition-colors, transition-all duration-300
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Bottom Spacing */}
        <div className="h-6" />
      </div>
    </div>
  );
}
