import { useEffect, useState } from "react";
import { bridge } from "./bridge";
import UserLocationMap from "@/components/UserLocationMap";
import useWatchMessage from "@/hooks/useWatchMessage";

function App() {
  // 레이아웃 조정하는거니깐 useLayoutEffect를 쓰는게 라이브사이클 흐름상 더 좋지 않을까
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const insetTop = params.get("insetTop") ?? "0";
    const insetBottom = params.get("insetBottom") ?? "0";

    document.documentElement.style.setProperty("--inset-top", `${insetTop}px`);
    document.documentElement.style.setProperty(
      "--inset-bottom",
      `${insetBottom}px`,
    );
  }, []);

  const handleButtonClick = async () => {
    const whoRU = await bridge.whoRU();
    alert(whoRU);
  };

  const { lastMessage } = useWatchMessage();

  const [a, setA] = useState(0);
  const [b, setB] = useState(0);
  const [sumRes, setSumRes] = useState(0);

  return (
    <div
      className="mx-5 my-3 "
      style={{
        paddingTop: "var(--inset-top)",
        paddingBottom: "var(--inset-bottom)",
      }}
    >
      <span className="text-3xl font-black">웹뷰 화면</span>
      <p className="text-2xl font-semibold">그래그래 웹뷰야</p>
      <div className="border border-blue-500 p-2 my-2 rounded">
        <p className="font-bold">워치 메시지</p>
        <p>{lastMessage ?? "아직 수신 없음"}</p>
      </div>
      <button className="border border-black p-1" onClick={handleButtonClick}>
        RN한테 안부 묻기 버튼
      </button>
      <div>
        <input
          type="string"
          value={a}
          onChange={(e) => setA(Number(e.currentTarget.value))}
          className="border border-black"
        />
        +
        <input
          type="string"
          value={b}
          onChange={(e) => setB(Number(e.currentTarget.value))}
          className="border border-black"
        />
        =<span>{sumRes}</span>
      </div>
      <button onClick={() => setSumRes(a + b)} className="border border-black">
        계산
      </button>
      <UserLocationMap />
    </div>
  );
}

export default App;
