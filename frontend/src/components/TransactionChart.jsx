import React from 'react';
import { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';

const mockData = [
  { time: '00:00', volume: 12, value: 45000 },
  { time: '04:00', volume: 8, value: 28000 },
  { time: '08:00', volume: 45, value: 180000 },
  { time: '12:00', volume: 82, value: 340000 },
  { time: '16:00', volume: 65, value: 290000 },
  { time: '20:00', volume: 38, value: 150000 },
];

const TransactionChart = ({ data = mockData }) => {
  return (
    <div className="glass-card" style={{ height: '320px', display: 'flex', flexDirection: 'column' }}>
      <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '1rem', color: 'var(--text-primary)' }}>
        Transaction Volume & Value Over Time
      </h3>
      <div style={{ flex: 1, width: '100%', minHeight: 0 }}>
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
            <defs>
              <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="var(--accent-indigo)" stopOpacity={0.4}/>
                <stop offset="95%" stopColor="var(--accent-indigo)" stopOpacity={0}/>
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.5} />
            <XAxis dataKey="time" stroke="#9ca3af" fontSize={12} />
            <YAxis stroke="#9ca3af" fontSize={12} />
            <Tooltip
              contentStyle={{ backgroundColor: '#111827', borderColor: '#374151', borderRadius: '8px', color: '#fff' }}
            />
            <Area type="monotone" dataKey="value" stroke="var(--accent-indigo)" strokeWidth={2} fillOpacity={1} fill="url(#colorValue)" />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};

export default TransactionChart;
