import { useState } from 'react';
import { MainScreen } from './components/MainScreen';
import { VideoPlayer } from './components/VideoPlayer';
import { InvestigationReport } from './components/InvestigationReport';
import { DesignGuide } from './components/DesignGuide';

type Screen = 'main' | 'investigation' | 'design-guide';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState<Screen>('main');

  return (
    <div className="h-screen w-full bg-[#0a0a0a] overflow-hidden">
      {currentScreen === 'main' && (
        <MainScreen 
          onNavigate={(screen) => setCurrentScreen(screen)}
        />
      )}
      
      {currentScreen === 'investigation' && (
        <div className="h-full flex flex-col">
          {/* Top Section - Video Player */}
          <div className="flex-shrink-0">
            <VideoPlayer onBack={() => setCurrentScreen('main')} />
          </div>

          {/* Bottom Section - Investigation Report */}
          <div className="flex-1 overflow-hidden">
            <InvestigationReport />
          </div>
        </div>
      )}

      {currentScreen === 'design-guide' && (
        <DesignGuide onBack={() => setCurrentScreen('main')} />
      )}
    </div>
  );
}
