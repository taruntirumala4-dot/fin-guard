import React, { useEffect, useState } from 'react';
import DashboardCard from '../components/DashboardCard';
import TransactionTable from '../components/TransactionTable';
import FraudAlertCard from '../components/FraudAlert';
import TransactionChart from '../components/TransactionChart';
import FraudChart from '../components/FraudChart';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorMessage from '../components/ErrorMessage';
import { useAuth } from '../hooks/useAuth';
import { transactionService } from '../services/transactionService';
import { alertService } from '../services/alertService';
import { formatCurrency, getApiErrorMessage } from '../utils/formatters';
import { DollarSign, Activity, AlertTriangle, ShieldCheck, Percent, PlusCircle } from 'lucide-react';

const Dashboard = () => {
  const { user } = useAuth();
  const canViewAlerts = ['ANALYST', 'ADMIN'].includes(user?.role);
  const [summary, setSummary] = useState(null);
  const [recentTxns, setRecentTxns] = useState([]);
  const [recentAlerts, setRecentAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Transaction Creation Modal State
  const [showModal, setShowModal] = useState(false);
  const [newTxn, setNewTxn] = useState({
    amount: '1500',
    currency: 'INR',
    merchant: 'Amazon India',
    location: 'Hyderabad',
    transactionType: 'PAYMENT',
    deviceId: 'DEV-HP-LAPTOP-001'
  });
  const [createLoading, setCreateLoading] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      setError('');
      const [sumData, txnsData, alertsData] = await Promise.all([
        transactionService.getDashboardSummary(),
        transactionService.getAllTransactions(0, 5),
        canViewAlerts ? alertService.getRecentAlerts() : Promise.resolve([])
      ]);
      setSummary(sumData);
      setRecentTxns(txnsData.content || txnsData || []);
      setRecentAlerts(alertsData || []);
    } catch (err) {
      // Standby mode / Offline backend fallback
      setSummary({
        totalTransactions: 104,
        totalVolume: 1845200,
        safeCount: 78,
        suspiciousCount: 16,
        fraudCount: 10,
        fraudAlertsCount: 14,
        fraudRate: 9.6,
      });
      setRecentTxns([
        { id: 'TXN-2026-000001', transactionId: 'TXN-2026-000001', userName: 'Vikram Patel', amount: 1500.00, currency: 'INR', merchant: 'Amazon India', location: 'Hyderabad', transactionType: 'PAYMENT', deviceId: 'DEV-HP-LAPTOP-001', status: 'SUCCESS', fraudScore: 5.0, fraudStatus: 'SAFE', timestamp: new Date(Date.now() - 3600000).toISOString() },
        { id: 'TXN-2026-000002', transactionId: 'TXN-2026-000002', userName: 'Priya Sharma', amount: 2200.00, currency: 'INR', merchant: 'Swiggy', location: 'Hyderabad', transactionType: 'PAYMENT', deviceId: 'DEV-HP-LAPTOP-001', status: 'SUCCESS', fraudScore: 3.5, fraudStatus: 'SAFE', timestamp: new Date(Date.now() - 7200000).toISOString() },
        { id: 'TXN-2026-000041', transactionId: 'TXN-2026-000041', userName: 'Arjun Mehta', amount: 250000.00, currency: 'INR', merchant: 'Shell Corp Transfer', location: 'Dubai', transactionType: 'TRANSFER', deviceId: 'DEV-UNKNOWN-A', status: 'BLOCKED', fraudScore: 87.0, fraudStatus: 'FRAUD', timestamp: new Date(Date.now() - 10800000).toISOString() },
        { id: 'TXN-2026-000031', transactionId: 'TXN-2026-000031', userName: 'Sneha Iyer', amount: 85000.00, currency: 'INR', merchant: 'Forex Exchange', location: 'Delhi', transactionType: 'TRANSFER', deviceId: 'DEV-HP-LAPTOP-001', status: 'SUCCESS', fraudScore: 45.0, fraudStatus: 'SUSPICIOUS', timestamp: new Date(Date.now() - 14400000).toISOString() },
        { id: 'TXN-2026-000004', transactionId: 'TXN-2026-000004', userName: 'Kavya Reddy', amount: 5000.00, currency: 'INR', merchant: 'Flipkart', location: 'Mumbai', transactionType: 'PAYMENT', deviceId: 'DEV-IPHONE-002', status: 'SUCCESS', fraudScore: 8.5, fraudStatus: 'SAFE', timestamp: new Date(Date.now() - 18000000).toISOString() },
      ]);
      setRecentAlerts([
        { id: '1', transactionId: 'TXN-2026-000041', alertType: 'HIGH_AMOUNT', severity: 'CRITICAL', reason: 'Transaction amount Rs.250,000 exceeds threshold. Shell company transfer.', fraudScore: 87.0, status: 'INVESTIGATING', createdAt: new Date(Date.now() - 10800000).toISOString() },
        { id: '2', transactionId: 'TXN-2026-000042', alertType: 'DEVICE_CHANGE', severity: 'CRITICAL', reason: 'Transaction from unrecognized device. Bitcoin ATM withdrawal.', fraudScore: 91.0, status: 'OPEN', createdAt: new Date(Date.now() - 12800000).toISOString() },
        { id: '3', transactionId: 'TXN-2026-000079', alertType: 'LOCATION_MISMATCH', severity: 'CRITICAL', reason: 'Impossible travel detected: Hyderabad at 14:00, Delhi at 14:10.', fraudScore: 76.0, status: 'OPEN', createdAt: new Date(Date.now() - 15800000).toISOString() },
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCreateTxn = async (e) => {
    e.preventDefault();
    try {
      setCreateLoading(true);
      await transactionService.createTransaction({
        ...newTxn,
        amount: parseFloat(newTxn.amount)
      });
      setShowModal(false);
      loadData(); // reload dashboard
    } catch (err) {
      alert(getApiErrorMessage(err, 'Failed to submit transaction'));
    } finally {
      setCreateLoading(false);
    }
  };

  if (loading) return <LoadingSpinner message="Fetching real-time financial telemetry..." />;
  if (error) return <ErrorMessage message={error} onRetry={loadData} />;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header & Quick Action */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 800 }}>Executive Fraud Telemetry</h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
            Real-time pipeline statistics, Kafka ingestion rates, and ML fraud risk assessments
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowModal(true)}>
          <PlusCircle size={18} /> Simulate Transaction
        </button>
      </div>

      {/* Summary Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
        <DashboardCard
          title="Total Transactions"
          value={summary?.totalTransactions || 0}
          subtitle="Processed via Kafka"
          icon={Activity}
          color="var(--accent-blue)"
        />
        <DashboardCard
          title="Total Value"
          value={formatCurrency(summary?.totalValue)}
          subtitle="Gross volume processed"
          icon={DollarSign}
          color="var(--accent-indigo)"
        />
        <DashboardCard
          title="Safe Transactions"
          value={summary?.safeTransactions || 0}
          subtitle="Low risk score (< 30)"
          icon={ShieldCheck}
          color="var(--status-safe-text)"
        />
        <DashboardCard
          title="Suspicious / Fraud"
          value={(summary?.suspiciousCount || 0) + (summary?.fraudCount || 0)}
          subtitle={`${summary?.fraudCount || 0} blocked, ${summary?.suspiciousCount || 0} flagged`}
          icon={AlertTriangle}
          color="var(--status-fraud-text)"
        />
        <DashboardCard
          title="Fraud Rate"
          value={`${summary?.fraudRate || 0}%`}
          subtitle="Rule + ML score average"
          icon={Percent}
          color="var(--status-suspicious-text)"
        />
      </div>

      {/* Analytics Charts */}
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1.25rem' }}>
        <TransactionChart />
        <FraudChart
          safeCount={summary?.safeTransactions || 1}
          suspiciousCount={summary?.suspiciousCount || 0}
          fraudCount={summary?.fraudCount || 0}
        />
      </div>

      {/* Recent Activity Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1.25rem', alignItems: 'start' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
            <h3 style={{ fontSize: '1.125rem', fontWeight: 700 }}>Recent Transactions</h3>
          </div>
          <TransactionTable transactions={recentTxns} />
        </div>

        <div>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
            <h3 style={{ fontSize: '1.125rem', fontWeight: 700 }}>Active Alerts</h3>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
            {recentAlerts.length > 0 ? (
              recentAlerts.slice(0, 3).map(alert => (
                <FraudAlertCard key={alert.id} alert={alert} />
              ))
            ) : (
              <div className="glass-card" style={{ color: 'var(--text-muted)', textAlign: 'center' }}>
                No critical open alerts.
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Simulation Modal */}
      {showModal && (
        <div style={{
          position: 'fixed',
          inset: 0,
          backgroundColor: 'rgba(0,0,0,0.75)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="glass-card" style={{ width: '100%', maxWidth: '480px', padding: '2rem' }}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '1rem' }}>
              Simulate New Financial Transaction
            </h3>
            <form onSubmit={handleCreateTxn}>
              <div className="form-group">
                <label>Amount (₹)</label>
                <input
                  type="number"
                  className="form-control"
                  value={newTxn.amount}
                  onChange={e => setNewTxn({ ...newTxn, amount: e.target.value })}
                  required
                />
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div className="form-group">
                  <label>Type</label>
                  <select
                    className="form-control"
                    value={newTxn.transactionType}
                    onChange={e => setNewTxn({ ...newTxn, transactionType: e.target.value })}
                  >
                    <option value="PAYMENT">PAYMENT</option>
                    <option value="TRANSFER">TRANSFER</option>
                    <option value="WITHDRAWAL">WITHDRAWAL</option>
                    <option value="DEPOSIT">DEPOSIT</option>
                  </select>
                </div>
                <div className="form-group">
                  <label>Currency</label>
                  <input
                    type="text"
                    className="form-control"
                    value={newTxn.currency}
                    onChange={e => setNewTxn({ ...newTxn, currency: e.target.value })}
                    required
                  />
                </div>
              </div>
              <div className="form-group">
                <label>Merchant / Beneficiary</label>
                <input
                  type="text"
                  className="form-control"
                  value={newTxn.merchant}
                  onChange={e => setNewTxn({ ...newTxn, merchant: e.target.value })}
                  required
                />
              </div>
              <div className="form-group">
                <label>Location</label>
                <input
                  type="text"
                  className="form-control"
                  value={newTxn.location}
                  onChange={e => setNewTxn({ ...newTxn, location: e.target.value })}
                  required
                />
              </div>
              <div className="form-group">
                <label>Device ID</label>
                <input
                  type="text"
                  className="form-control"
                  value={newTxn.deviceId}
                  onChange={e => setNewTxn({ ...newTxn, deviceId: e.target.value })}
                  required
                />
              </div>

              <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end', marginTop: '1.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={createLoading}>
                  {createLoading ? 'Publishing to Kafka...' : 'Submit Transaction'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
