import { Download, X, FileImage, FileText } from 'lucide-react';
import { useRef } from 'react';
import { ImageWithFallback } from './figma/ImageWithFallback';

interface NewspaperViewProps {
  onClose: () => void;
  caseData: {
    id: string;
    title: string;
    date: string;
    image: string;
    description: string;
  };
}

export function NewspaperView({ onClose, caseData }: NewspaperViewProps) {
  const newspaperRef = useRef<HTMLDivElement>(null);

  const downloadAsPNG = async () => {
    if (!newspaperRef.current) return;
    
    try {
      const html2canvas = (await import('html2canvas')).default;
      const canvas = await html2canvas(newspaperRef.current, {
        backgroundColor: '#f5e6d3',
        scale: 2,
      });
      
      const link = document.createElement('a');
      link.download = `arkham-chronicle-${caseData.id}.png`;
      link.href = canvas.toDataURL('image/png');
      link.click();
    } catch (error) {
      console.error('Download failed:', error);
    }
  };

  const downloadAsPDF = async () => {
    if (!newspaperRef.current) return;
    
    try {
      const html2canvas = (await import('html2canvas')).default;
      const jsPDF = (await import('jspdf')).default;
      
      const canvas = await html2canvas(newspaperRef.current, {
        backgroundColor: '#f5e6d3',
        scale: 2,
      });
      
      const imgData = canvas.toDataURL('image/png');
      const pdf = new jsPDF({
        orientation: 'portrait',
        unit: 'mm',
        format: 'a4',
      });
      
      const imgWidth = 210; // A4 width in mm
      const imgHeight = (canvas.height * imgWidth) / canvas.width;
      
      pdf.addImage(imgData, 'PNG', 0, 0, imgWidth, imgHeight);
      pdf.save(`arkham-chronicle-${caseData.id}.pdf`);
    } catch (error) {
      console.error('Download failed:', error);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/95 flex flex-col">
      {/* Header Controls */}
      <div className="flex-shrink-0 bg-gradient-to-b from-black to-transparent p-4 flex items-center justify-between">
        <button
          onClick={onClose}
          className="p-2 hover:bg-white/10 rounded-full transition-colors"
        >
          <X className="w-6 h-6 text-white" />
        </button>
        
        <div className="flex items-center gap-2">
          <button
            onClick={downloadAsPNG}
            className="flex items-center gap-2 bg-[#8B0000] hover:bg-[#A00000] text-white px-4 py-2 rounded-xl transition-colors text-sm"
          >
            <FileImage className="w-4 h-4" />
            PNG
          </button>
          <button
            onClick={downloadAsPDF}
            className="flex items-center gap-2 bg-[#8B0000] hover:bg-[#A00000] text-white px-4 py-2 rounded-xl transition-colors text-sm"
          >
            <FileText className="w-4 h-4" />
            PDF
          </button>
        </div>
      </div>

      {/* Newspaper Content */}
      <div className="flex-1 overflow-y-auto p-4 flex items-start justify-center">
        <div
          ref={newspaperRef}
          className="w-full max-w-2xl bg-[#f5e6d3] shadow-2xl"
          style={{
            backgroundImage: `repeating-linear-gradient(0deg, transparent, transparent 1px, rgba(139, 69, 19, 0.03) 1px, rgba(139, 69, 19, 0.03) 2px)`,
          }}
        >
          {/* Newspaper Header */}
          <div className="border-b-4 border-double border-[#3a2817] p-6 text-center bg-gradient-to-b from-[#ede0cd] to-[#f5e6d3]">
            <div className="text-xs tracking-[0.3em] text-[#8B0000] mb-2">
              SPECIAL REPORT • CLASSIFIED
            </div>
            <h1 
              className="text-5xl tracking-wider text-[#1a1a1a] mb-2"
              style={{ fontFamily: 'serif' }}
            >
              ARKHAM CHRONICLE
            </h1>
            <div className="text-xs tracking-widest text-[#5a4a3a] border-t border-b border-[#8B6f47] py-2 mt-2">
              EST. 1872 • VOLUME XLVII • {caseData.date}
            </div>
          </div>

          {/* Main Content */}
          <div className="p-6">
            {/* Headline */}
            <div className="text-center mb-6 pb-4 border-b-2 border-[#8B6f47]">
              <h2 
                className="text-3xl tracking-wide text-[#1a1a1a] mb-2"
                style={{ fontFamily: 'serif' }}
              >
                {caseData.title.toUpperCase()}
              </h2>
              <div className="text-sm text-[#8B0000] tracking-widest">
                CASE NO. {caseData.id}
              </div>
            </div>

            {/* Article Content */}
            <div className="grid grid-cols-3 gap-4 mb-6">
              {/* Image */}
              <div className="col-span-1">
                <div className="border-4 border-double border-[#5a4a3a] p-2 bg-white">
                  <ImageWithFallback
                    src={caseData.image}
                    alt={caseData.title}
                    className="w-full h-auto sepia opacity-90"
                  />
                  <p 
                    className="text-xs text-center mt-2 text-[#5a4a3a] italic"
                    style={{ fontFamily: 'serif' }}
                  >
                    Evidence recovered from scene
                  </p>
                </div>
              </div>

              {/* Text Content */}
              <div className="col-span-2">
                <p 
                  className="text-[#2a2a2a] leading-relaxed text-justify mb-4"
                  style={{ 
                    fontFamily: 'serif',
                    columnCount: 1,
                    textIndent: '2em',
                  }}
                >
                  {caseData.description}
                </p>
                <p 
                  className="text-[#2a2a2a] leading-relaxed text-justify mb-4"
                  style={{ 
                    fontFamily: 'serif',
                    textIndent: '2em',
                  }}
                >
                  Local authorities report unprecedented circumstances surrounding this investigation. 
                  The evidence collected defies conventional explanation, leading experts to question 
                  the very nature of reality itself.
                </p>
                <p 
                  className="text-[#2a2a2a] leading-relaxed text-justify"
                  style={{ 
                    fontFamily: 'serif',
                    textIndent: '2em',
                  }}
                >
                  Witnesses describe phenomena that challenge the boundaries of human understanding. 
                  The Arkham Police Department has declared this matter of utmost importance to 
                  public safety, though officials remain tight-lipped about specific details.
                </p>
              </div>
            </div>

            {/* Additional Details Box */}
            <div className="border-2 border-[#8B0000] bg-[#fff8f0] p-4 mb-6">
              <h3 
                className="text-lg tracking-wider text-[#8B0000] mb-3 text-center border-b border-[#8B0000] pb-2"
                style={{ fontFamily: 'serif' }}
              >
                OFFICIAL POLICE STATEMENT
              </h3>
              <p 
                className="text-sm text-[#2a2a2a] leading-relaxed italic text-center"
                style={{ fontFamily: 'serif' }}
              >
                "Citizens are advised to exercise extreme caution. Anyone with information 
                regarding this case should contact the Arkham Police Department immediately. 
                Do not approach any suspicious individuals or locations alone."
              </p>
              <div className="text-center mt-3 pt-3 border-t border-[#8B0000]">
                <p className="text-xs text-[#5a4a3a]">
                  — Chief Inspector Howard Phillips, Arkham P.D.
                </p>
              </div>
            </div>

            {/* Game Log Section */}
            <div className="border-t-2 border-[#8B6f47] pt-4">
              <h3 
                className="text-xl tracking-wider text-[#1a1a1a] mb-4 text-center"
                style={{ fontFamily: 'serif' }}
              >
                INVESTIGATION LOG
              </h3>
              
              <div className="space-y-3 text-sm" style={{ fontFamily: 'serif' }}>
                <div className="flex gap-3">
                  <span className="text-[#8B0000] font-bold">15:30</span>
                  <p className="text-[#2a2a2a] flex-1">
                    Investigation commenced at abandoned Marsh estate. Team consists of 
                    three researchers from Miskatonic University.
                  </p>
                </div>
                <div className="flex gap-3">
                  <span className="text-[#8B0000] font-bold">16:45</span>
                  <p className="text-[#2a2a2a] flex-1">
                    Discovered hidden chamber beneath main hall. Strange symbols adorning walls.
                  </p>
                </div>
                <div className="flex gap-3">
                  <span className="text-[#8B0000] font-bold">17:20</span>
                  <p className="text-[#2a2a2a] flex-1">
                    Team reports hearing unexplained sounds. Decision made to document findings 
                    before exiting premises.
                  </p>
                </div>
                <div className="flex gap-3">
                  <span className="text-[#8B0000] font-bold">18:00</span>
                  <p className="text-[#2a2a2a] flex-1">
                    Last radio contact with research team. Transmission interrupted by static 
                    and unintelligible voices.
                  </p>
                </div>
                <div className="flex gap-3">
                  <span className="text-[#8B0000] font-bold">20:15</span>
                  <p className="text-[#2a2a2a] flex-1">
                    Search party dispatched. No trace of researchers found. Only their equipment 
                    and journal entries remain.
                  </p>
                </div>
              </div>
            </div>

            {/* Footer */}
            <div className="mt-6 pt-4 border-t-2 border-double border-[#8B6f47] text-center">
              <p className="text-xs text-[#5a4a3a] tracking-widest">
                ⚠ CLASSIFIED DOCUMENT • ARKHAM POLICE DEPARTMENT • DO NOT DISTRIBUTE ⚠
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
