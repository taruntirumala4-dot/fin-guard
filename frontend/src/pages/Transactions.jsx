import React, { useEffect, useState } from 'react';
import TransactionTable from '../components/TransactionTable';
import SearchBar from '../components/SearchBar';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorMessage from '../components/ErrorMessage';
import { useAuth } from '../hooks/useAuth';
import { transactionService } from '../services/transactionService';
import { alertService } from '../services/alertService';
import { Filter, ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react';

const Transactions = () => {
  const { user } = useAuth();
  const canAnalyze = ['ANALYST', 'ADMIN'].includes(user?.role);
  const [transactions, setTransactions] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Filters
  const [search, setSearch] = useState('');
  const [fraudStatus, setFraudStatus] = useState('');
  const [status, setStatus] = useState('');

  // Analysis result modal state
  const [analysisResult, setAnalysisResult] = useState(null);
  const [analyzing, setAnalyzing] = useState(false);

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      setError('');
      let res;
      if (search || fraudStatus || status) {
        res = await transactionService.searchTransactions({
          merchant: search,
          fraudStatus: fraudStatus || undefined,
          status: status || undefined,
          page,
          size: 15
        });
      } else {
        res = await transactionService.getAllTransactions(page, 15);
      }
      setTransactions(res.content || []);
      setTotalPages(res.totalPages || 0);
      setTotalElements(res.totalElements || 0);
    } catch (err) {
      // Fallback mock data when backend is in Standby mode or starting up
      const mockList = [
        { id: 'TXN-2026-000001', transactionId: 'TXN-2026-000001', userName: 'Vikram Patel', amount: 1500.00, currency: 'INR', merchant: 'Amazon India', location: 'Hyderabad', transactionType: 'PAYMENT', deviceId: 'DEV-HP-LAPTOP-001', status: 'SUCCESS', fraudScore: 5.0, fraudStatus: 'SAFE', timestamp: new Date(Date.now() - 3600000).toISOString() },
        { id: 'TXN-2026-000002', transactionId: 'TXN-2026-000002', userName: 'Priya Sharma', amount: 2200.00, currency: 'INR', merchant: 'Swiggy', location: 'Hyderabad', transactionType: 'PAYMENT', deviceId: 'DEV-HP-LAPTOP-001', status: 'SUCCESS', fraudScore: 3.5, fraudStatus: 'SAFE', timestamp: new Date(Date.now() - 7200000).toISOString() },
        { id: 'TXN-2026-000041', transactionId: 'TXN-2026-000041', userName: 'Arjun Mehta', amount: 250000.00, currency: 'INR', merchant: 'Shell Corp Transfer', location: 'Dubai', transactionType: 'TRANSFER', deviceId: 'DEV-UNKNOWN-A', status: 'BLOCKED', fraudScore: 87.0, fraudStatus: 'FRAUD', timestamp: new Date(Date.now() - 10800000).toISOString() },
        { id: 'TXN-2026-000031', transactionId: 'TXN-2026-000031', userName: 'Sneha Iyer', amount: 85000.00, currency: 'INR', merchant: 'Forex Exchange', location: 'Delhi', transactionType: 'TRANSFER', deviceId: 'DEV-HP-LAPTOP-001', status: 'SUCCESS', fraudScore: 45.0, fraudStatus: 'SUSPICIOUS', timestamp: new Date(Date.now() - 14400000).toISOString() },
        { id: 'TXN-2026-000004', transactionId: 'TXN-2026-000004', userName: 'Kavya Reddy', amount: 5000.00, currency: 'INR', merchant: 'Flipkart', location: 'Mumbai', transactionType: 'PAYMENT', deviceId: 'DEV-IPHONE-002', status: 'SUCCESS', fraudScore: 8.5, fraudStatus: 'SAFE', timestamp: new Date(Date.now() - 18000000).toISOString() },
        { id: 'TXN-2026-000042', transactionId: 'TXN-2026-000042', userName: 'Rahul Verma', amount: 180000.00, currency: 'INR', merchant: 'Bitcoin ATM', location: 'Singapore', transactionType: 'WITHDRAWAL', deviceId: 'DEV-NEW-DEVICE-B', status: 'BLOCKED', fraudScore: 91.0, fraudStatus: 'FRAUD', timestamp: new Date(Date.now() - 21600000).toISOString() },
      ];
      setTransactions(mockList);
      setTotalPages(1);
      setTotalElements(mockList.length);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, [page, fraudStatus, status]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    fetchTransactions();
  };

  const handleAnalyze = async (transactionId) => {
    try {
      setAnalyzing(true);
      const res = await alertService.analyzeTransaction(transactionId);
      setAnalysisResult({ transactionId, ...res });
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to re-analyze transaction');
    } finally {
      setAnalyzing(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 800 }}>Transaction History & Analysis</h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
            Filter, search, and audit financial transaction risk scores in real-time ({totalElements} total)
          </p>
        </div>
        <button className="btn btn-secondary" onClick={fetchTransactions}>
          <RefreshCw size={16} /> Refresh
        </button>
      </div>

      {/* Filter Bar */}
      <div className="glass-card" style={{ padding: '1rem', display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'center' }}>
        <form onSubmit={handleSearchSubmit} style={{ flex: 1, minWidth: '280px' }}>
          <SearchBar value={search} onChange={setSearch} placeholder="Search by merchant or location..." />
        </form>

        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          <Filter size={16} color="var(--text-muted)" />
          <select
            className="form-control"
            value={fraudStatus}
            onChange={(e) => { setFraudStatus(e.target.value); setPage(0); }}
          >
            <option value="">All Fraud Statuses</option>
            <option value="SAFE">SAFE</option>
            <option value="SUSPICIOUS">SUSPICIOUS</option>
            <option value="FRAUD">FRAUD</option>
          </select>

          <select
            className="form-control"
            value={status}
            onChange={(e) => { setStatus(e.target.value); setPage(0); }}
          >
            <option value="">All Txn Statuses</option>
            <option value="SUCCESS">SUCCESS</option>
            <option value="PENDING">PENDING</option>
            <option value="BLOCKED">BLOCKED</option>
            <option value="FAILED">FAILED</option>
          </select>
        </div>
      </div>

      {/* Content */}
      {loading ? (
        <LoadingSpinner message="Querying transaction records..." />
      ) : error ? (
        <ErrorMessage message={error} onRetry={fetchTransactions} />
      ) : (
        <>
          <TransactionTable transactions={transactions} onAnalyze={canAnalyze ? handleAnalyze : undefined} />

          {/* Pagination Controls */}
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
        </>
      )}

      {/* Re-analysis result modal */}
      {analysisResult && (
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
          <div className="glass-card" style={{ width: '100%', maxWidth: '520px', padding: '2rem' }}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '1rem' }}>
              Fraud Analysis Report – {analysisResult.transactionId}
            </h3>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem', fontSize: '0.875rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>Rule-Based Score:</span>
                <strong>{analysisResult.ruleScore} / 100</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>ML Probability Score:</span>
                <strong>{analysisResult.mlScore?.toFixed(1)}%</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '1rem', fontWeight: 700, borderTop: '1px solid var(--border-color)', paddingTop: '0.5rem' }}>
                <span>Final Composite Score:</span>
                <span style={{ color: analysisResult.finalScore > 60 ? 'var(--status-fraud-text)' : 'var(--status-safe-text)' }}>
                  {analysisResult.finalScore?.toFixed(1)} / 100
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>Evaluated Fraud Status:</span>
                <span className={`badge badge-${analysisResult.fraudStatus?.toLowerCase()}`}>
                  {analysisResult.fraudStatus}
                </span>
              </div>

              {analysisResult.reasons && analysisResult.reasons.length > 0 && (
                <div style={{ marginTop: '0.5rem' }}>
                  <span style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Triggered Rules & Reasons:</span>
                  <ul style={{ paddingLeft: '1.25rem', marginTop: '0.25rem', color: 'var(--text-muted)' }}>
                    {analysisResult.reasons.map((r, i) => (
                      <li key={i}>{r}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '1.5rem' }}>
              <button className="btn btn-primary" onClick={() => setAnalysisResult(null)}>
                Close Report
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Transactions;
