"""Lightweight anomaly detection for NutriSense security alerts.

Usage:
  python scripts/security_log_monitor.py \
    --alerts-url https://your-backend/security/log-alerts \
    --monitor-key your-key
"""

import argparse
import collections
import datetime as dt
import json
import statistics
import sys
from typing import Dict, List

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry


def fetch_alerts(alerts_url: str, monitor_key: str) -> List[Dict]:
    headers = {"X-Monitor-Key": monitor_key}
    retry = Retry(
        total=3,
        connect=3,
        read=3,
        backoff_factor=1,
        status_forcelist=[429, 500, 502, 503, 504],
        allowed_methods=["GET"],
        raise_on_status=False,
    )

    session = requests.Session()
    adapter = HTTPAdapter(max_retries=retry)
    session.mount("http://", adapter)
    session.mount("https://", adapter)

    response = session.get(alerts_url, headers=headers, timeout=(10, 30))
    response.raise_for_status()
    data = response.json()
    return data.get("alerts", [])


def detect_anomalies(alerts: List[Dict]) -> Dict:
    ip_counts = collections.Counter(a.get("ip", "unknown") for a in alerts)
    reason_counts = collections.Counter(a.get("reason", "unknown") for a in alerts)

    counts = list(ip_counts.values())
    mean = statistics.mean(counts) if counts else 0
    std = statistics.pstdev(counts) if len(counts) > 1 else 0

    threshold = mean + (2 * std)
    flagged_ips = []
    for ip, count in ip_counts.items():
        z_score = 0.0 if std == 0 else (count - mean) / std
        if count > max(5, threshold):
            flagged_ips.append({"ip": ip, "events": count, "z_score": round(z_score, 2)})

    risk_score = min(100, int((len(flagged_ips) * 20) + (sum(reason_counts.values()) / 5)))
    risk_level = "low"
    if risk_score >= 70:
        risk_level = "high"
    elif risk_score >= 40:
        risk_level = "medium"

    return {
        "total_alerts": len(alerts),
        "risk_score": risk_score,
        "risk_level": risk_level,
        "flagged_ips": flagged_ips,
        "reason_counts": dict(reason_counts),
        "fetch_error": None,
    }


def generate_markdown(report: Dict) -> str:
    lines = [
        "# Security Log Monitoring Report",
        "",
        f"- Generated: {dt.datetime.utcnow().isoformat()}Z",
        f"- Total alerts: {report['total_alerts']}",
        f"- Risk score: {report['risk_score']}/100",
        f"- Risk level: **{report['risk_level'].upper()}**",
        "",
        "## Alert Types",
    ]

    if report.get("fetch_error"):
        lines.insert(7, f"- Data fetch status: degraded ({report['fetch_error']})")
        lines.insert(8, "")

    if not report["reason_counts"]:
        lines.append("- No alerts detected")
    else:
        for reason, count in sorted(report["reason_counts"].items(), key=lambda x: x[1], reverse=True):
            lines.append(f"- {reason}: {count}")

    lines.append("")
    lines.append("## Flagged IPs (Anomaly Detection)")
    if not report["flagged_ips"]:
        lines.append("- No anomalous IP patterns detected")
    else:
        for item in report["flagged_ips"]:
            lines.append(f"- {item['ip']}: {item['events']} events (z-score={item['z_score']})")

    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description="Analyze NutriSense security alerts for anomalies")
    parser.add_argument("--alerts-url", required=True, help="Backend /security/log-alerts endpoint URL")
    parser.add_argument("--monitor-key", required=True, help="Value of X-Monitor-Key header")
    parser.add_argument("--output", default="security-monitoring-report.md", help="Output markdown path")
    args = parser.parse_args()

    try:
        alerts = fetch_alerts(args.alerts_url, args.monitor_key)
        report = detect_anomalies(alerts)
    except requests.RequestException as exc:
        report = {
            "total_alerts": 0,
            "risk_score": 0,
            "risk_level": "low",
            "flagged_ips": [],
            "reason_counts": {},
            "fetch_error": f"{type(exc).__name__}: {exc}",
        }
        print(
            "Warning: could not fetch security alerts after retries; "
            "publishing degraded report without failing CI.",
            file=sys.stderr,
        )

    markdown = generate_markdown(report)
    with open(args.output, "w", encoding="utf-8") as fp:
        fp.write(markdown)

    print(json.dumps(report, indent=2))

    # Fail CI only on high risk findings.
    return 1 if report["risk_level"] == "high" else 0


if __name__ == "__main__":
    sys.exit(main())
