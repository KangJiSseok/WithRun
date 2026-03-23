import { useEffect, useRef, useState } from 'react';
import { useDragControls, type PanInfo } from 'motion/react';

export const HANDLE_HEIGHT = 40;

export const useNormalRunBottomSheet = () => {
  const sheetRef = useRef<HTMLDivElement>(null);
  const [sheetHeight, setSheetHeight] = useState(0);
  const [isSheetExpanded, setIsSheetExpanded] = useState(true);
  const [isDragging, setIsDragging] = useState(false);
  const dragControls = useDragControls();

  useEffect(() => {
    if (!sheetRef.current) return;
    const observer = new ResizeObserver(([entry]) => {
      setSheetHeight(entry.contentRect.height);
    });
    observer.observe(sheetRef.current);
    return () => observer.disconnect();
  }, []);

  function collapse() {
    setIsSheetExpanded(false);
  }

  function expand() {
    setIsSheetExpanded(true);
  }

  function startDrag(e: React.PointerEvent) {
    setIsDragging(true);
    dragControls.start(e);
  }

  function handleDragEnd(_: unknown, info: PanInfo) {
    setIsDragging(false);
    const mid = (sheetHeight - HANDLE_HEIGHT) / 2;
    if (info.offset.y > mid || info.velocity.y > 500) {
      setIsSheetExpanded(false);
    } else {
      setIsSheetExpanded(true);
    }
  }

  return {
    sheetRef,
    sheetHeight,
    isSheetExpanded,
    isDragging,
    dragControls,
    collapse,
    expand,
    startDrag,
    handleDragEnd,
  };
};
