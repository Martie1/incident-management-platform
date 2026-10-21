import { NavLink, Outlet } from 'react-router'

export function Layout() {
  return (
    <>
      <header className="container">
        <nav>
          <ul>
            <li>
              <strong>Incident Management</strong>
            </li>
          </ul>
          <ul>
            <li>
              <NavLink to="/incidents">Incidents</NavLink>
            </li>
            <li>
              <NavLink to="/users">Users</NavLink>
            </li>
          </ul>
        </nav>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </>
  )
}
