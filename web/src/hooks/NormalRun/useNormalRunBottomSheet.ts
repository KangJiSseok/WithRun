import { useEffect, useRef, useState } from 'react';
import { useDragControls, type PanInfo } from 'motion/react';

export const useNormalRunBottomSheet = () => {
  const sheetRef = useRef<HTMLDivElement>(null);
  const headerRef = useRef<HTMLDivElement>(null);
  const [sheetHeight, setSheetHeight] = useState(0);
  const [headerHeight, setHeaderHeight] = useState(80);
  const [isSheetExpanded, setIsSheetExpanded] = useState(false);
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

  // 바텀시트 헤더 사이즈 계산용 ref observer
  useEffect(() => {
    if (!headerRef.current) return;
    const observer = new ResizeObserver(([entry]) => {
      setHeaderHeight(entry.contentRect.height);
    });
    observer.observe(headerRef.current);
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
    const mid = (sheetHeight - headerHeight) / 2;
    if (info.offset.y > mid || info.velocity.y > 500) {
      setIsSheetExpanded(false);
    } else {
      setIsSheetExpanded(true);
    }
  }

  return {
    sheetRef,
    headerRef,
    sheetHeight,
    headerHeight,
    isSheetExpanded,
    isDragging,
    dragControls,
    collapse,
    expand,
    startDrag,
    handleDragEnd,
  };
};
