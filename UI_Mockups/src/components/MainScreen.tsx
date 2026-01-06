import { Play, FileText, Palette, Clock, Archive, Settings } from 'lucide-react';
import { ImageWithFallback } from './figma/ImageWithFallback';

interface MainScreenProps {
  onNavigate: (screen: 'investigation' | 'design-guide') => void;
}

const recentCases = [
  {
    id: '1923-AH-047',
    title: 'The Miskatonic Disappearance',
    date: 'March 15, 1923',
    status: 'Active',
    progress: 35,
  },
  {
    id: '1923-AH-042',
    title: 'Innsmouth Harbor Incident',
    date: 'February 28, 1923',
    status: 'Under Review',
    progress: 78,
  },
  {
    id: '1923-AH-038',
    title: 'The Whisperer in Darkness',
    date: 'January 12, 1923',
    status: 'Closed',
    progress: 100,
  },
];

export function MainScreen({ onNavigate }: MainScreenProps) {
  return (
    <div className="h-full w-full flex flex-col">
      {/* Header */}
      <header className="flex-shrink-0 bg-gradient-to-b from-black to-[#0a0a0a] border-b-2 border-[#8B0000] px-6 py-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-[#8B0000] tracking-[0.25em] mb-1">ARKHAM LOG</h1>
            <p className="text-[#666666] text-xs tracking-widest">INVESTIGATIVE ARCHIVE SYSTEM</p>
          </div>
          <button className="p-3 hover:bg-white/5 rounded-full transition-colors">
            <Settings className="w-6 h-6 text-[#8B0000]" />
          </button>
        </div>
      </header>

      {/* Hero Section */}
      <div className="flex-shrink-0 relative h-64 overflow-hidden">
        <ImageWithFallback
          src="https://images.unsplash.com/photo-1758730010177-1711515b7552?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxkYXJrJTIwbXlzdGVyeSUyMGxpYnJhcnl8ZW58MXx8fHwxNzY3MTU0NDYxfDA&ixlib=rb-4.1.0&q=80&w=1080&utm_source=figma&utm_medium=referral"
          alt="Dark library"
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-[#0a0a0a] via-black/60 to-transparent" />
        
        <div className="absolute bottom-6 left-6 right-6">
          <div className="text-[#8B0000] text-xs tracking-[0.3em] mb-2">FEATURED CASE</div>
          <h2 className="text-white mb-2">The Miskatonic Disappearance</h2>
          <p className="text-[#999999] text-sm mb-4">
            Three researchers vanished during an expedition to the Marsh estate...
          </p>
          <button
            onClick={() => onNavigate('investigation')}
            className="bg-[#8B0000] hover:bg-[#A00000] text-white px-6 py-3 rounded-xl flex items-center gap-2 transition-colors"
          >
            <Play className="w-5 h-5" fill="white" />
            Continue Investigation
          </button>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 overflow-y-auto px-6 py-6">
        {/* Quick Actions */}
        <div className="mb-8">
          <h3 className="text-white mb-4 tracking-wide">Quick Actions</h3>
          <div className="grid grid-cols-2 gap-3">
            <button
              onClick={() => onNavigate('investigation')}
              className="bg-gradient-to-br from-[#1a1a1a] to-[#0f0f0f] border border-[#2a2a2a] hover:border-[#8B0000] rounded-2xl p-4 text-left transition-all duration-300 group"
            >
              <div className="bg-[#8B0000]/10 group-hover:bg-[#8B0000]/20 w-12 h-12 rounded-xl flex items-center justify-center mb-3 transition-colors">
                <FileText className="w-6 h-6 text-[#8B0000]" />
              </div>
              <div className="text-white text-sm mb-1">View Cases</div>
              <div className="text-[#666666] text-xs">Browse archives</div>
            </button>

            <button
              onClick={() => onNavigate('design-guide')}
              className="bg-gradient-to-br from-[#1a1a1a] to-[#0f0f0f] border border-[#2a2a2a] hover:border-[#8B0000] rounded-2xl p-4 text-left transition-all duration-300 group"
            >
              <div className="bg-[#8B0000]/10 group-hover:bg-[#8B0000]/20 w-12 h-12 rounded-xl flex items-center justify-center mb-3 transition-colors">
                <Palette className="w-6 h-6 text-[#8B0000]" />
              </div>
              <div className="text-white text-sm mb-1">Design Guide</div>
              <div className="text-[#666666] text-xs">UI System</div>
            </button>
          </div>
        </div>

        {/* Recent Cases */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-white tracking-wide">Recent Cases</h3>
            <button className="text-[#8B0000] text-sm tracking-wide hover:underline">
              View All
            </button>
          </div>

          <div className="space-y-3">
            {recentCases.map((case_) => (
              <button
                key={case_.id}
                onClick={() => onNavigate('investigation')}
                className="w-full bg-gradient-to-br from-[#1a1a1a] to-[#0f0f0f] border border-[#2a2a2a] hover:border-[#8B0000] rounded-2xl p-4 text-left transition-all duration-300"
              >
                <div className="flex items-start justify-between mb-3">
                  <div className="flex-1">
                    <div className="text-[#8B0000] text-xs tracking-wider mb-1">
                      CASE {case_.id}
                    </div>
                    <div className="text-white text-sm mb-1">{case_.title}</div>
                    <div className="flex items-center gap-2 text-xs text-[#666666]">
                      <Clock className="w-3 h-3" />
                      {case_.date}
                    </div>
                  </div>
                  <div className={`text-xs px-3 py-1 rounded-full ${
                    case_.status === 'Active' 
                      ? 'bg-[#8B0000]/20 text-[#8B0000]'
                      : case_.status === 'Under Review'
                      ? 'bg-yellow-500/20 text-yellow-500'
                      : 'bg-green-500/20 text-green-500'
                  }`}>
                    {case_.status}
                  </div>
                </div>

                {/* Progress Bar */}
                <div className="relative h-1.5 bg-[#2a2a2a] rounded-full overflow-hidden">
                  <div
                    className="absolute inset-y-0 left-0 bg-[#8B0000] rounded-full transition-all"
                    style={{ width: `${case_.progress}%` }}
                  />
                </div>
              </button>
            ))}
          </div>
        </div>

        {/* Bottom Spacing */}
        <div className="h-6" />
      </div>

      {/* Bottom Navigation */}
      <div className="flex-shrink-0 bg-gradient-to-t from-black to-[#0a0a0a] border-t border-[#2a2a2a] px-6 py-4">
        <div className="flex items-center justify-around">
          <button className="flex flex-col items-center gap-1 text-[#8B0000]">
            <Archive className="w-6 h-6" />
            <span className="text-xs tracking-wide">Cases</span>
          </button>
          <button className="flex flex-col items-center gap-1 text-[#666666] hover:text-[#8B0000] transition-colors">
            <FileText className="w-6 h-6" />
            <span className="text-xs tracking-wide">Reports</span>
          </button>
          <button className="flex flex-col items-center gap-1 text-[#666666] hover:text-[#8B0000] transition-colors">
            <Clock className="w-6 h-6" />
            <span className="text-xs tracking-wide">Timeline</span>
          </button>
          <button
            onClick={() => onNavigate('design-guide')}
            className="flex flex-col items-center gap-1 text-[#666666] hover:text-[#8B0000] transition-colors"
          >
            <Palette className="w-6 h-6" />
            <span className="text-xs tracking-wide">Guide</span>
          </button>
        </div>
      </div>
    </div>
  );
}
