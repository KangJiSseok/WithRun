import { useEffect, useState } from 'react';
import { fetchCourseSurveys, type CourseSurveyData } from '@/shared/apis/FreeRun/courseApi';

export function useCourseSurveys(isOpen: boolean) {
  const [surveyData, setSurveyData] = useState<CourseSurveyData | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  // Drawer가 열릴 때마다 설문 선택지 조회
  useEffect(() => {
    if (!isOpen) return;
    async function load() {
      setIsLoading(true);
      try {
        const data = await fetchCourseSurveys();
        setSurveyData(data);
      } catch {
        setSurveyData(null);
      } finally {
        setIsLoading(false);
      }
    }
    load();
  }, [isOpen]);

  return { surveyData, isLoading };
}
