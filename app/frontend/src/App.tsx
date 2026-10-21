import { Navigate, Route, Routes } from 'react-router'
import { Layout } from './components/Layout'
import { IncidentsPage } from './pages/IncidentsPage'
import { UsersPage } from './pages/UsersPage'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="incidents" element={<IncidentsPage />} />
        <Route path="users" element={<UsersPage />} />
        <Route path="*" element={<Navigate to="/incidents" replace />} />
      </Route>
    </Routes>
  )
}
