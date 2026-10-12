import React from 'react';
import { formatDate, getStatusBadgeClass } from '../utils/formatters';

const FraudAlertTable = ({ alerts = [] }) => {
  if (!alerts || alerts.length === 0) {
    return (
      <div className="glass-card" style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
        No fraud alerts currently reported.
      </div>
    );
  }

  return (
    <div className="glass-card" style={{ padding: 0, overflow: 'hidden' }}>
      <div style={{ overflowX: 'auto' }}>
        <table className="custom-table">
          <thead>
            <tr>
              <th>Alert ID</th>
              <th>Txn ID</th>
              <th>Alert Type</th>
              <th>Severity</th>
              <th>Reason</th>
              <th>Fraud Score</th>
              <th>Status</th>
              <th>Created At</th>
            </tr>
          </thead>
          <tbody>
            {alerts.map((alert) => (
              <tr key={alert.id}>
                <td className="font-mono">#{alert.id}</td>
                <td className="font-mono" style={{ color: 'var(--accent-indigo)', fontWeight: 600 }}>
                  {alert.transactionId}
                </td>
                <td style={{ fontWeight: 600 }}>{alert.alertType}</td>
                <td>
                  <span className={`badge badge-${alert.severity?.toLowerCase()}`}>
                    {alert.severity}
                  </span>
                </td>
                <td style={{ maxWidth: '300px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }} title={alert.reason}>
                  {alert.reason}
                </td>
                <td style={{ fontWeight: 700 }}>{alert.fraudScore}</td>
                <td>
                  <span className={`badge ${getStatusBadgeClass(alert.status)}`}>
                    {alert.status}
                  </span>
                </td>
                <td style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                  {formatDate(alert.createdAt)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default FraudAlertTable;
