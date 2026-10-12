import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, ArrowLeftRight, AlertTriangle, User } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';

const Sidebar = () => {
  const { user } = useAuth();

  const navItems = [
    { path: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { path: '/transactions', label: 'Transactions', icon: ArrowLeftRight },
    { path: '/alerts', label: 'Fraud Alerts', icon: AlertTriangle, role: ['ANALYST', 'ADMIN'] },
    { path: '/profile', label: 'Profile', icon: User },
  ];

  return (
    <aside style={{
      width: '240px',
      backgroundColor: 'var(--bg-secondary)',
      borderRight: '1px solid var(--border-color)',
      padding: '1.5rem 1rem',
      display: 'flex',
      flexDirection: 'column',
      gap: '0.5rem',
      shrink: 0
    }}>
      <div style={{ padding: '0 0.75rem 1rem 0.75rem', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', letterSpacing: '0.05em', textTransform: 'uppercase' }}>
        Navigation
      </div>
      {navItems.map((item) => {
        if (item.role && !item.role.includes(user?.role)) return null;
        const Icon = item.icon;
        return (
          <NavLink
            key={item.path}
            to={item.path}
            style={({ isActive }) => ({
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem',
              padding: '0.75rem 1rem',
              borderRadius: '8px',
              fontWeight: 500,
              fontSize: '0.875rem',
              color: isActive ? '#fff' : 'var(--text-secondary)',
              backgroundColor: isActive ? 'var(--bg-card-hover)' : 'transparent',
              borderLeft: isActive ? '3px solid var(--accent-indigo)' : '3px solid transparent',
              transition: 'all 0.15s ease'
            })}
          >
            <Icon size={18} />
            <span>{item.label}</span>
          </NavLink>
        );
      })}
    </aside>
  );
};

export default Sidebar;
