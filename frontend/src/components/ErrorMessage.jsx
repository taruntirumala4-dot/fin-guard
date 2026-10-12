import React from 'react';
import { AlertCircle } from 'lucide-react';

const ErrorMessage = ({ message = 'An unexpected error occurred.', onRetry }) => {
  return (
    <div className="glass-card" style={{ borderLeft: '4px solid var(--status-fraud-text)', backgroundColor: 'rgba(239, 68, 68, 0.1)', padding: '1.25rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        <AlertCircle size={20} color="var(--status-fraud-text)" />
        <span style={{ fontSize: '0.875rem', fontWeight: 500, color: 'var(--text-primary)' }}>
          {message}
        </span>
      </div>
      {onRetry && (
        <button onClick={onRetry} className="btn btn-secondary" style={{ padding: '0.375rem 0.75rem', fontSize: '0.75rem' }}>
          Retry
        </button>
      )}
    </div>
  );
};

export default ErrorMessage;
