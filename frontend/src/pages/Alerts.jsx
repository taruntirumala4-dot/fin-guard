import React, { useEffect, useState } from 'react';
import FraudAlertTable from '../components/FraudAlertTable';
import FraudAlertCard from '../components/FraudAlert';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorMessage from '../components/ErrorMessage';
import { alertService } from '../services/alertService';
import { ShieldAlert, AlertTriangle, CheckCircle, List, LayoutGrid, ChevronLeft, ChevronRight } from 'lucide-react';

const Alerts = () => {
  const [alerts, setAlerts] = useState([]);
  const [stats, setStats] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [viewMode, setViewMode] = useState('table'); // 'table' or 'grid'

  const loadAlertsData = async () => {
    try {
      setLoading(true);
      setError('');
      const [alertsRes, statsRes] = await Promise.all([
        alertService.getAlerts(page, 12),
        alertService.getStatistics()
      ]);
      setAlerts(alertsRes.content || []);
      setTotalPages(alertsRes.totalPages || 0);
      setStats(statsRes);
    } catch (err) {
      // Standby mode / Offline backend fallback
      const mockAlertsList = [
        { id: '1', transactionId: 'TXN-2026-000041', alertType: 'HIGH_AMOUNT', severity: 'CRITICAL', reason: 'Transaction amount Rs.250,000 exceeds threshold. Shell company transfer.', fraudScore: 87.0, status: 'INVESTIGATING', createdAt: new Date(Date.now() - 10800000).toISOString() },
        { id: '2', transactionId: 'TXN-2026-000042', alertType: 'DEVICE_CHANGE', severity: 'CRITICAL', reason: 'Transaction from unrecognized device. Bitcoin ATM withdrawal.', fraudScore: 91.0, status: 'OPEN', createdAt: new Date(Date.now() - 12800000).toISOString() },
        { id: '3', transactionId: 'TXN-2026-000079', alertType: 'LOCATION_MISMATCH', severity: 'CRITICAL', reason: 'Impossible travel detected: Hyderabad at 14:00, Delhi at 14:10.', fraudScore: 76.0, status: 'OPEN', createdAt: new Date(Date.now() - 15800000).toISOString() },
        { id: '4', transactionId: 'TXN-2026-000077', alertType: 'RAPID_TRANSACTION', severity: 'HIGH', reason: 'User made 7 transactions within 6 minutes.', fraudScore: 72.0, status: 'OPEN', createdAt: new Date(Date.now() - 18800000).toISOString() },
        { id: '5', transactionId: 'TXN-2026-000081', alertType: 'DEVICE_CHANGE', severity: 'HIGH', reason: 'Transaction from unrecognized device DEV-NEW-UNKNOWN-XYZ.', fraudScore: 68.0, status: 'OPEN', createdAt: new Date(Date.now() - 21800000).toISOString() },
      ];
      setAlerts(mockAlertsList);
      setTotalPages(1);
      setStats({
        openCount: 3,
        investigatingCount: 2,
        resolvedCount: 2,
        falsePositiveCount: 1,
        totalAlerts: 8
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAlertsData();
  }, [page]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 800 }}>Fraud Alerts & Risk Audit</h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
            Real-time security incident response and risk investigation queue
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem', backgroundColor: 'var(--bg-secondary)', padding: '0.25rem', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
          <button
            onClick={() => setViewMode('table')}
            style={{ padding: '0.375rem 0.625rem', borderRadius: '6px', backgroundColor: viewMode === 'table' ? 'var(--bg-card-hover)' : 'transparent', color: viewMode === 'table' ? '#fff' : 'var(--text-muted)' }}
          >
            <List size={18} />
          </button>
          <button
            onClick={() => setViewMode('grid')}
            style={{ padding: '0.375rem 0.625rem', borderRadius: '6px', backgroundColor: viewMode === 'grid' ? 'var(--bg-card-hover)' : 'transparent', color: viewMode === 'grid' ? '#fff' : 'var(--text-muted)' }}
          >
            <LayoutGrid size={18} />
          </button>
        </div>
      </div>

      {/* Alert Stats Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
        <div className="glass-card" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ padding: '0.75rem', borderRadius: '10px', backgroundColor: 'rgba(239, 68, 68, 0.15)', color: 'var(--status-fraud-text)' }}>
            <AlertTriangle size={24} />
          </div>
          <div>
            <div style={{ fontSize: '1.5rem', fontWeight: 700 }}>{stats?.total || 0}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Total Alerts Generated</div>
          </div>
        </div>

        <div className="glass-card" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ padding: '0.75rem', borderRadius: '10px', backgroundColor: 'rgba(225, 29, 72, 0.2)', color: '#fb7185' }}>
            <ShieldAlert size={24} />
          </div>
          <div>
            <div style={{ fontSize: '1.5rem', fontWeight: 700 }}>{stats?.critical || 0}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Critical Severity</div>
          </div>
        </div>

        <div className="glass-card" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ padding: '0.75rem', borderRadius: '10px', backgroundColor: 'rgba(245, 158, 11, 0.15)', color: 'var(--status-suspicious-text)' }}>
            <AlertTriangle size={24} />
          </div>
          <div>
            <div style={{ fontSize: '1.5rem', fontWeight: 700 }}>{stats?.open || 0}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Open Investigations</div>
          </div>
        </div>
      </div>

      {/* Alerts View */}
      {loading ? (
        <LoadingSpinner message="Fetching fraud alert stream..." />
      ) : error ? (
        <ErrorMessage message={error} onRetry={loadAlertsData} />
      ) : viewMode === 'table' ? (
        <FraudAlertTable alerts={alerts} />
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1rem' }}>
          {alerts.map(alert => (
            <FraudAlertCard key={alert.id} alert={alert} />
          ))}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0.5rem 0' }}>
          <span style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
            Page {page + 1} of {totalPages}
          </span>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button
              className="btn btn-secondary"
              disabled={page === 0}
              onClick={() => setPage(p => Math.max(0, p - 1))}
            >
              <ChevronLeft size={16} /> Previous
            </button>
            <button
              className="btn btn-secondary"
              disabled={page + 1 >= totalPages}
              onClick={() => setPage(p => p + 1)}
            >
              Next <ChevronRight size={16} />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default Alerts;
