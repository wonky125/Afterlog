import { Play, Pause, Maximize2, Volume2, MoreVertical, ArrowLeft } from 'lucide-react';
import { useState } from 'react';
import { ImageWithFallback } from './figma/ImageWithFallback';

export function VideoPlayer({ onBack }: { onBack?: () => void }) {
  const [isPlaying, setIsPlaying] = useState(false);

  return (
    <div className="relative bg-black">
      {/* Header Bar */}
      <div className="absolute top-0 left-0 right-0 z-20 bg-gradient-to-b from-black/80 to-transparent p-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          {onBack && (
            <button
              onClick={onBack}
              className="p-2 hover:bg-white/10 rounded-full transition-colors"
            >
              <ArrowLeft className="w-6 h-6 text-white" />
            </button>
          )}
          <h1 className="text-[#8B0000] tracking-[0.2em]">ARKHAM LOG</h1>
        </div>
        <button className="p-2 hover:bg-white/10 rounded-full transition-colors">
          <MoreVertical className="w-6 h-6 text-white" />
        </button>
      </div>

      {/* Video Content */}
      <div className="relative aspect-video w-full">
        <ImageWithFallback
          src="https://images.unsplash.com/photo-1678572474919-7b2121a95bae?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxkYXJrJTIwaG9ycm9yJTIwYm9hcmQlMjBnYW1lfGVufDF8fHx8MTc2NzE1MzkxNHww&ixlib=rb-4.1.0&q=80&w=1080&utm_source=figma&utm_medium=referral"
          alt="Lovecraftian board game scene"
          className="w-full h-full object-cover"
        />
        
        {/* Dark overlay for atmosphere */}
        <div className="absolute inset-0 bg-gradient-to-t from-[#0a0a0a] via-black/40 to-transparent" />

        {/* Play/Pause Button */}
        <button
          onClick={() => setIsPlaying(!isPlaying)}
          className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 z-10 bg-[#8B0000]/90 hover:bg-[#8B0000] rounded-full p-6 transition-all duration-300 hover:scale-110"
        >
          {isPlaying ? (
            <Pause className="w-8 h-8 text-white" fill="white" />
          ) : (
            <Play className="w-8 h-8 text-white ml-1" fill="white" />
          )}
        </button>

        {/* Video Controls */}
        <div className="absolute bottom-0 left-0 right-0 z-10 bg-gradient-to-t from-black/90 to-transparent p-4">
          {/* Progress Bar */}
          <div className="mb-3">
            <div className="h-1 bg-white/20 rounded-full overflow-hidden">
              <div className="h-full w-[35%] bg-[#8B0000] rounded-full" />
            </div>
          </div>

          {/* Control Buttons */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={() => setIsPlaying(!isPlaying)}
                className="p-2 hover:bg-white/10 rounded-full transition-colors"
              >
                {isPlaying ? (
                  <Pause className="w-5 h-5 text-white" />
                ) : (
                  <Play className="w-5 h-5 text-white ml-0.5" />
                )}
              </button>
              <span className="text-white text-sm">02:15 / 06:42</span>
            </div>

            <div className="flex items-center gap-2">
              <button className="p-2 hover:bg-white/10 rounded-full transition-colors">
                <Volume2 className="w-5 h-5 text-white" />
              </button>
              <button className="p-2 hover:bg-white/10 rounded-full transition-colors">
                <Maximize2 className="w-5 h-5 text-white" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}