// open: 모달을 보여줄지 말지
// 모달 컴포넌트를 만들고, 열릴 때만 보이게 하기

type RunningGuideModalProps = {
  open: boolean;
};

export function RunningGuideModal({ open }: RunningGuideModalProps) {
  if (!open) {
    return null;
  }

  return <div className></div>;
}
