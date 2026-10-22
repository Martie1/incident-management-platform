import os
import time
import logging
import psycopg2
from prometheus_client import start_http_server, Gauge

logging.basicConfig(level=logging.INFO, format='%(asctime)s [%(levelname)s] %(message)s')

DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "incidents")
DB_USER = os.getenv("DB_USER", "platform")
DB_PASSWORD = os.getenv("DB_PASSWORD", "platformSecretPassword123")
EXPORTER_PORT = int(os.getenv("EXPORTER_PORT", "8000"))
SCRAPE_INTERVAL = int(os.getenv("SCRAPE_INTERVAL", "15"))

# prometheus metrics
DB_UP = Gauge("incident_db_connected", "Database connection status (1 = connected, 0 = disconnected)")
INCIDENTS_BY_STATUS = Gauge("incident_platform_incidents_by_status", "Number of incidents by status", ["status"])
INCIDENTS_BY_SEVERITY = Gauge("incident_platform_incidents_by_severity", "Number of incidents by severity", ["severity"])
USERS_TOTAL = Gauge("incident_platform_users_total", "Total registered users")

def collect_metrics():
    try:
        conn = psycopg2.connect(
            host=DB_HOST,
            port=DB_PORT,
            dbname=DB_NAME,
            user=DB_USER,
            password=DB_PASSWORD,
            connect_timeout=5
        )
        cursor = conn.cursor()
        DB_UP.set(1)

        # count incidents by status
        cursor.execute("SELECT status, count(*) FROM incidents GROUP BY status;")
        status_counts = dict(cursor.fetchall())
        for status in ["OPEN", "IN_PROGRESS", "RESOLVED"]:
            INCIDENTS_BY_STATUS.labels(status=status).set(status_counts.get(status, 0))

        # count incidents by severity
        cursor.execute("SELECT severity, count(*) FROM incidents GROUP BY severity;")
        severity_counts = dict(cursor.fetchall())
        for severity in ["LOW", "MEDIUM", "HIGH", "CRITICAL"]:
            INCIDENTS_BY_SEVERITY.labels(severity=severity).set(severity_counts.get(severity, 0))

        # total users
        cursor.execute("SELECT count(*) FROM users;")
        user_count = cursor.fetchone()[0]
        USERS_TOTAL.set(user_count)

        cursor.close()
        conn.close()
        logging.info("Metrics successfully collected from PostgreSQL database.")
    except Exception as e:
        DB_UP.set(0)
        logging.warning("Could not collect metrics from PostgreSQL: %s", e)

def main():
    logging.info("Starting Python incident metrics exporter on port %s...", EXPORTER_PORT)
    start_http_server(EXPORTER_PORT)
    while True:
        collect_metrics()
        time.sleep(SCRAPE_INTERVAL)

if __name__ == "__main__":
    main()
