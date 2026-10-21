# Frontend

React + Vite + TypeScript UI for the incident management platform (incidents and users CRUD).

## Requirements

- Node 24 (`nvm use` picks it up from `.nvmrc`)

## Development

The dev server proxies `/api` to the backend on `http://localhost:8080`, so start the backend first:

```sh
cd app/platform && ./gradlew bootRun
```

Then, in `app/frontend`:

```sh
npm install
npm run dev
```

## Scripts

| Command              | Description                                                                |
|----------------------|----------------------------------------------------------------------------|
| `npm run dev`        | Dev server with hot reload                                                 |
| `npm run build`      | Type-check and build to `dist/`                                            |
| `npm run lint`       | Lint with oxlint                                                           |
| `npm test`           | Run tests once (Vitest + Testing Library + MSW), JUnit report in `reports/` |
| `npm run test:watch` | Run tests in watch mode                                                    |
| `npm run preview`    | Serve the production build locally                                         |
