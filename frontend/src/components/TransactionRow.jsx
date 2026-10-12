import React from 'react';
import { formatCurrency, formatDate, getStatusBadgeClass } from '../utils/formatters';

const TransactionRow = ({ transaction, onAnalyze }) => {
  const {
    transactionId,
    amount,
    currency,
    merchant,
    location,
    transactionType,
    timestamp,
    fraudScore,
    fraudStatus,
    status
  } = transaction;

  return (
    <tr>
      <td className="font-mono" style={{ fontWeight: 600, color: 'var(--accent-indigo)' }}>
        {transactionId}
      </td>
      <td style={{ fontWeight: 600 }}>
        {formatCurrency(amount, currency)}
      </td>
      <td>{merchant}</td>
      <td>{location}</td>
      <td>
        <span style={{ fontSize: '0.75rem', padding: '0.15rem 0.4rem', borderRadius: '4px', backgroundColor: 'var(--bg-card-hover)', color: 'var(--text-secondary)' }}>
          {transactionType}
        </span>
      </td>
      <td style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
        {formatDate(timestamp)}
      </td>
      <td>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
          <div style={{
            width: '28px',
            height: '28px',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '0.75rem',
            fontWeight: 700,
            backgroundColor: fraudScore > 60 ? 'rgba(239, 68, 68, 0.2)' : fraudScore > 30 ? 'rgba(245, 158, 11, 0.2)' : 'rgba(16, 185, 129, 0.2)',
            color: fraudScore > 60 ? 'var(--status-fraud-text)' : fraudScore > 30 ? 'var(--status-suspicious-text)' : 'var(--status-safe-text)',
            border: `1px solid ${fraudScore > 60 ? 'var(--status-fraud-text)' : fraudScore > 30 ? 'var(--status-suspicious-text)' : 'var(--status-safe-text)'}`
          }}>
            {Math.round(fraudScore)}
          </div>
        </div>
      </td>
      <td>
        <span className={`badge ${getStatusBadgeClass(fraudStatus)}`}>
          {fraudStatus}
        </span>
      </td>
      <td>
        {onAnalyze && (
          <button
            onClick={() => onAnalyze(transactionId)}
            className="btn btn-secondary"
            style={{ padding: '0.25rem 0.5rem', fontSize: '0.75rem' }}
          >
            Re-Analyze
          </button>
        )}
      </td>
    </tr>
  );
};

export default TransactionRow;
