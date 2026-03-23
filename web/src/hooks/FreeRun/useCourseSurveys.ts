import { useEffect, useState } from 'react';
import { getCourseSurveysApi } from '@/apis';
import type { GetCourseSurveysResponse } from '@/apis';

export function useCourseSurveys(isOpen: boolean) {
  const [surveyData, setSurveyData] = useState<GetCourseSurveysResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  // Drawer가 열릴 때마다 설문 선택지 조회
  useEffect(() => {
    if (!isOpen) return;
    async function load() {
      setIsLoading(true);
      try {
        const res = await getCourseSurveysApi();
        setSurveyData(res.data);
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
