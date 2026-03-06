import { CommandPanel } from './components/command-panel'
import { HealthDashboard } from './components/health-dashboard'
import { useWatchBridge } from './hooks/use-watch-bridge'
import './App.css'

function App() {
  const { lastPayload, lastAck, connectionStatus, isConnected, sendCommand } = useWatchBridge()

  return (
    <div className="app">
      <header className="app-header">
        <h1>Running Health</h1>
        <div className={`connection-badge ${isConnected ? 'connected' : 'disconnected'}`}>
          {isConnected ? `Connected: ${connectionStatus?.nodeName ?? ''}` : 'Disconnected'}
        </div>
      </header>
      <main className="app-main">
        <HealthDashboard payload={lastPayload} />
        <CommandPanel onCommand={sendCommand} lastAck={lastAck} isConnected={isConnected} />
      </main>
    </div>
  )
}

export default App
