import { useRive } from '@rive-app/react-canvas';

export default function RunCharactor() {
  const { RiveComponent } = useRive({
    src: '/rive/withrun_character.riv',
    artboard: 'main',
    animations: 'running',
    autoplay: true,
  });

  return <RiveComponent />;
}
