import React from 'react';
import TransactionRow from './TransactionRow';

const TransactionTable = ({ transactions = [], onAnalyze }) => {
  if (!transactions || transactions.length === 0) {
    return (
      <div className="glass-card" style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
        No transactions found matching criteria.
      </div>
    );
  }

  return (
    <div className="glass-card" style={{ padding: 0, overflow: 'hidden' }}>
      <div style={{ overflowX: 'auto' }}>
        <table className="custom-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Amount</th>
              <th>Merchant</th>
              <th>Location</th>
              <th>Type</th>
              <th>Timestamp</th>
              <th>Risk Score</th>
              <th>Fraud Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {transactions.map((tx) => (
              <TransactionRow key={tx.id || tx.transactionId} transaction={tx} onAnalyze={onAnalyze} />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default TransactionTable;
