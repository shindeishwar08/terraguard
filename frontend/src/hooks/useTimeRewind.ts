import { useState, useEffect, useCallback } from 'react';

const NOW = Date.now();
const TWENTY_FOUR_HOURS = 24 * 60 * 60 * 1000;

export const useTimeRewind = () => {
  const [sliderValue, setSliderValue] = useState(NOW);
  const [isPlaying, setIsPlaying] = useState(false);

  const min = NOW - TWENTY_FOUR_HOURS;
  const max = NOW;

  useEffect(() => {
    if (!isPlaying) return;

    const interval = setInterval(() => {
      setSliderValue(prev => {
        if (prev >= max) {
          setIsPlaying(false);
          return max;
        }
        return prev + 60 * 1000 * 10; // advance 10 minutes per tick
      });
    }, 100);

    return () => clearInterval(interval);
  }, [isPlaying]);

  const togglePlay = useCallback(() => {
    if (sliderValue >= max) setSliderValue(min); // reset if at end
    setIsPlaying(prev => !prev);
  }, [sliderValue]);

  const getLabel = () => {
    if (sliderValue >= max - 60000) return 'Showing: Now';
    const diffMs = max - sliderValue;
    const diffHrs = Math.floor(diffMs / (1000 * 60 * 60));
    const diffMins = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
    return `Showing: ${diffHrs}h ${diffMins}m ago`;
  };

  return { sliderValue, setSliderValue, isPlaying, togglePlay, min, max, getLabel };
};
