import { ChevronDown } from 'lucide-react';
import { useState } from 'react';
import { ImageWithFallback } from './figma/ImageWithFallback';
import { NewspaperView } from './NewspaperView';

const evidenceItems = [
  {
    id: 1,
    image: 'https://images.unsplash.com/photo-1766325765472-5c37ccc1d91e?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHx2aW50YWdlJTIwc2VwaWElMjBwaG90b2dyYXBofGVufDF8fHx8MTc2NzE1MzkxNHww&ixlib=rb-4.1.0&q=80&w=1080&utm_source=figma&utm_medium=referral',
    title: 'THE MISKATONIC DISAPPEARANCE',
    date: 'March 15, 1923',
    description: 'Three researchers from Miskatonic University vanished during an expedition to the abandoned Marsh estate. Local authorities discovered their journal entries describing impossible geometric patterns and voices emanating from beneath the mansion\'s foundation.',
  },
  {
    id: 2,
    image: 'https://images.unsplash.com/photo-1759215501954-8c1a18f97b11?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxvbGQlMjBteXN0ZXJpb3VzJTIwa2V5fGVufDF8fHx8MTc2NzE1MzkxNXww&ixlib=rb-4.1.0&q=80&w=1080&utm_source=figma&utm_medium=referral',
    title: 'EVIDENCE RECOVERED',
    date: 'March 18, 1923',
    description: 'A peculiar brass key was found clutched in the hand of Professor Edmund Blackwood. The key bears inscriptions in no known language. Laboratory analysis reveals the metal composition is unlike any terrestrial alloy.',
  },
  {
    id: 3,
    image: 'https://images.unsplash.com/photo-1759910546893-3e897df4105c?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHx2aW50YWdlJTIwcG9ja2V0JTIwd2F0Y2h8ZW58MXx8fHwxNzY3MTEyODg3fDA&ixlib=rb-4.1.0&q=80&w=1080&utm_source=figma&utm_medium=referral',
    title: 'TEMPORAL ANOMALIES',
    date: 'March 20, 1923',
    description: 'Dr. Catherine Ward\'s pocket watch was discovered still running, yet its hands move in reverse. The timepiece emits a faint humming that increases in intensity during the witching hour. Several officers refuse to handle the artifact.',
  },
  {
    id: 4,
    image: 'https://images.unsplash.com/photo-1766325765472-5c37ccc1d91e?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHx2aW50YWdlJTIwc2VwaWElMjBwaG90b2dyYXBofGVufDF8fHx8MTc2NzE1MzkxNHww&ixlib=rb-4.1.0&q=80&w=1080&utm_source=figma&utm_medium=referral',
    title: 'WITNESS TESTIMONY',
    date: 'March 22, 1923',
    description: 'Groundskeeper Silas Crane reports seeing "shadows that moved against the light" and hearing chanting in languages that caused physical pain to comprehend. He has since been committed to Arkham Sanitarium.',
  },
];

export function InvestigationReport() {
  const [selectedItem, setSelectedItem] = useState<typeof evidenceItems[0] | null>(null);

  return (
    <>
      <div className="h-full bg-[#1a1a1a] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="flex-shrink-0 border-b-2 border-[#8B0000] bg-gradient-to-b from-[#0a0a0a] to-[#1a1a1a] px-4 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-[#8B0000] tracking-[0.15em] mb-1">INVESTIGATION REPORT</h2>
              <p className="text-[#666666] text-sm tracking-wider">CASE NO. 1923-AH-047</p>
            </div>
            <ChevronDown className="w-6 h-6 text-[#8B0000]" />
          </div>
        </div>

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto px-4 py-6 space-y-6">
          {/* Newspaper Style Header */}
          <div className="text-center pb-4 border-b border-[#333333]">
            <div className="text-[#8B0000] text-sm tracking-[0.3em] mb-2">ARKHAM CHRONICLE</div>
            <div className="text-[#666666] text-xs tracking-widest">EST. 1872 • VOLUME XLVII</div>
          </div>

          {/* Evidence Items */}
          {evidenceItems.map((item) => (
            <article
              key={item.id}
              className="bg-gradient-to-br from-[#1a1a1a] to-[#0f0f0f] rounded-2xl p-4 border border-[#2a2a2a] hover:border-[#8B0000]/30 transition-all duration-300"
            >
              <div className="flex gap-4">
                {/* Evidence Photo */}
                <div className="flex-shrink-0">
                  <button
                    onClick={() => setSelectedItem(item)}
                    className="w-24 h-24 rounded-lg overflow-hidden border-2 border-[#333333] hover:border-[#8B0000] relative transition-all cursor-pointer"
                  >
                    <ImageWithFallback
                      src={item.image}
                      alt={item.title}
                      className="w-full h-full object-cover opacity-80 sepia"
                    />
                    <div className="absolute inset-0 bg-[#8B0000]/10" />
                    <div className="absolute inset-0 bg-black/0 hover:bg-black/20 transition-all flex items-center justify-center">
                      <span className="text-white text-xs opacity-0 hover:opacity-100 transition-opacity">
                        View
                      </span>
                    </div>
                  </button>
                </div>

                {/* Content */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <h3 className="text-white tracking-wider leading-tight">{item.title}</h3>
                    <span className="text-[#8B0000] text-xs whitespace-nowrap">{item.date}</span>
                  </div>
                  <p className="text-[#999999] text-sm leading-relaxed">{item.description}</p>
                </div>
              </div>

              {/* Decorative divider */}
              <div className="mt-4 pt-3 border-t border-[#2a2a2a] flex items-center justify-between">
                <div className="flex gap-2">
                  <span className="text-[#666666] text-xs tracking-widest">CLASSIFIED</span>
                  <span className="text-[#8B0000] text-xs">●</span>
                  <span className="text-[#666666] text-xs tracking-widest">CONFIDENTIAL</span>
                </div>
                <div className="text-[#8B0000] text-xs tracking-[0.2em]">§ {item.id}</div>
              </div>
            </article>
          ))}

          {/* Bottom Notice */}
          <div className="text-center py-6 border-t border-[#333333]">
            <p className="text-[#666666] text-xs tracking-widest">
              END OF CURRENT DOCUMENTATION
            </p>
            <p className="text-[#8B0000] text-xs tracking-wider mt-2">
              ⚠ FURTHER INVESTIGATION REQUIRED
            </p>
          </div>
        </div>
      </div>

      {/* Newspaper Modal */}
      {selectedItem && (
        <NewspaperView
          caseData={{
            id: '1923-AH-047',
            title: selectedItem.title,
            date: selectedItem.date,
            image: selectedItem.image,
            description: selectedItem.description,
          }}
          onClose={() => setSelectedItem(null)}
        />
      )}
    </>
  );
}