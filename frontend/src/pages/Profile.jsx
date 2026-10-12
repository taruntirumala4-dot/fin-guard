import React, { useEffect, useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { authService } from '../services/authService';
import LoadingSpinner from '../components/LoadingSpinner';
import { User, Mail, Shield, Calendar, Key, CheckCircle } from 'lucide-react';
import { formatDate } from '../utils/formatters';

const Profile = () => {
  const { user: localUser } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const getProfile = async () => {
      try {
        const data = await authService.fetchProfile();
        setProfile(data);
      } catch (err) {
        setProfile(localUser);
      } finally {
        setLoading(false);
      }
    };
    getProfile();
  }, [localUser]);

  if (loading) return <LoadingSpinner message="Fetching user security profile..." />;

  const displayUser = profile || localUser;

  return (
    <div style={{ maxWidth: '800px', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 800 }}>Account & Security Profile</h1>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
          Identity details and platform access permissions
        </p>
      </div>

      <div className="glass-card" style={{ padding: '2rem', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem', paddingBottom: '1.5rem', borderBottom: '1px solid var(--border-color)' }}>
          <div style={{
            width: '64px',
            height: '64px',
            borderRadius: '50%',
            background: 'linear-gradient(135deg, var(--accent-indigo), var(--accent-blue))',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '1.75rem',
            fontWeight: 800,
            color: '#fff',
            boxShadow: '0 8px 20px rgba(99, 102, 241, 0.4)'
          }}>
            {displayUser?.name ? displayUser.name.charAt(0).toUpperCase() : 'U'}
          </div>
          <div>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700 }}>{displayUser?.name}</h2>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.25rem' }}>
              <span className="badge badge-low" style={{ fontSize: '0.75rem' }}>
                <Shield size={12} /> {displayUser?.role} ROLE
              </span>
              <span style={{ fontSize: '0.75rem', color: 'var(--status-safe-text)', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                <CheckCircle size={12} /> Active Session
              </span>
            </div>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
          <div className="form-group">
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Mail size={16} /> Registered Email
            </label>
            <div className="form-control" style={{ backgroundColor: 'var(--bg-secondary)', fontWeight: 500 }}>
              {displayUser?.email}
            </div>
          </div>

          <div className="form-group">
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Calendar size={16} /> Account Created
            </label>
            <div className="form-control" style={{ backgroundColor: 'var(--bg-secondary)', fontWeight: 500 }}>
              {displayUser?.createdAt ? formatDate(displayUser.createdAt) : 'N/A'}
            </div>
          </div>
        </div>

        <div style={{ padding: '1.25rem', borderRadius: '8px', backgroundColor: 'rgba(99, 102, 241, 0.08)', border: '1px solid rgba(99, 102, 241, 0.2)' }}>
          <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--accent-indigo)', display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
            <Key size={16} /> Role Privileges Overview
          </h4>
          <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
            {displayUser?.role === 'ADMIN' ? (
              'System Administrator: Full system access including transaction creation, fraud alert audit, role management, and system metrics.'
            ) : displayUser?.role === 'ANALYST' ? (
              'Fraud Analyst: Privileged access to transaction search, risk evaluation breakdown, real-time alert triage, and security statistics.'
            ) : (
              'Standard User: Access to personal transactions, balance summary, and manual simulation of new financial payments.'
            )}
          </p>
        </div>
      </div>
    </div>
  );
};

export default Profile;
