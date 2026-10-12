import React from 'react';
import { formatDate, getStatusBadgeClass } from '../utils/formatters';
import { AlertTriangle, ShieldAlert, CheckCircle, Clock } from 'lucide-react';

const FraudAlertCard = ({ alert }) => {
  const { transactionId, alertType, severity, reason, fraudScore, status, createdAt } = alert;

  const getSeverityIcon = (sev) => {
    switch (sev) {
      case 'CRITICAL':
      case 'HIGH':
        return <AlertTriangle size={18} color="var(--status-fraud-text)" />;
      case 'MEDIUM':
        return <ShieldAlert size={18} color="var(--status-suspicious-text)" />;
      default:
        return <Clock size={18} color="var(--accent-blue)" />;
    }
  };

  return (
    <div className="glass-card" style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', borderLeft: `4px solid ${severity === 'CRITICAL' ? 'var(--status-fraud-text)' : severity === 'HIGH' ? '#fb923c' : 'var(--status-suspicious-text)'}` }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          {getSeverityIcon(severity)}
          <span style={{ fontWeight: 700, fontSize: '0.9375rem', color: 'var(--text-primary)' }}>
            {alertType}
          </span>
          <span className={`badge badge-${severity?.toLowerCase()}`}>
            {severity}
          </span>
        </div>
        <span className={`badge ${getStatusBadgeClass(status)}`}>
          {status}
        </span>
      </div>

      <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.4 }}>
        {reason}
      </p>

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--text-muted)', paddingTop: '0.5rem', borderTop: '1px solid rgba(255,255,255,0.05)' }}>
        <span>Txn: <strong className="font-mono" style={{ color: 'var(--accent-indigo)' }}>{transactionId}</strong></span>
        <span>Score: <strong style={{ color: 'var(--text-primary)' }}>{fraudScore}</strong></span>
        <span>{formatDate(createdAt)}</span>
      </div>
    </div>
  );
};

export default FraudAlertCard;
