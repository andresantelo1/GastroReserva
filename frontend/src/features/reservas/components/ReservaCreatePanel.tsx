import QueryState from '../../../components/common/QueryState'
import { useApiQuery } from '../../../hooks/useApiQuery'
import type { Cliente } from '../../clientes/models/Cliente'
import { reservaService } from '../services/reservaService'
import type { Reserva } from '../models/Reserva'
import ReservaForm from './ReservaForm'
const cargarOpciones = async (signal: AbortSignal) => {
  const [mesas, turnos] = await Promise.all([reservaService.mesas(signal), reservaService.turnos(signal)])
  return { mesas, turnos }
}
export default function ReservaCreatePanel({ clientes, onCreated, onBusyChange }: { clientes: Cliente[]; onCreated: (reserva: Reserva) => void; onBusyChange: (busy: boolean) => void }) {
  const { data, loading, error, reload } = useApiQuery(cargarOpciones, { mesas: [], turnos: [] })
  return <><QueryState loading={loading} error={error} onRetry={reload} label="mesas y turnos" />
    {!loading && !error && <ReservaForm {...data} clientes={clientes} onCreated={onCreated} onBusyChange={onBusyChange} />}
  </>
}

