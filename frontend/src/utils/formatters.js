export const formatCurrency = (amount, currency = 'INR') => {
  if (amount === null || amount === undefined) return '₹0.00';
  const val = Number(amount);
  if (currency === 'INR') {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2,
    }).format(val);
  }
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency || 'USD',
  }).format(val);
};

export const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return new Intl.DateTimeFormat('en-IN', {
    dateStyle: 'medium',
    timeStyle: 'medium',
  }).format(date);
};

export const formatRelativeTime = (dateString) => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  const now = new Date();
  const diffInSeconds = Math.floor((now - date) / 1000);

  if (diffInSeconds < 60) return `${diffInSeconds}s ago`;
  if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)}m ago`;
  if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)}h ago`;
  return `${Math.floor(diffInSeconds / 86400)}d ago`;
};

export const getApiErrorMessage = (err, fallback = 'Something went wrong. Please try again.') => {
  const data = err?.response?.data;
  if (data) {
    if (typeof data.message === 'string' && data.message.trim()) return data.message;
    if (data.errors && typeof data.errors === 'object') {
      const messages = Object.values(data.errors).filter(Boolean);
      if (messages.length > 0) return messages.join('; ');
    }
    if (typeof data.error === 'string' && data.error.trim()) return data.error;
  }
  if (!err?.response) return 'Could not reach the FinGuard server. Please check your connection and try again.';
  return fallback;
};

export const getStatusBadgeClass = (status) => {
  switch (status?.toUpperCase()) {
    case 'SAFE':
    case 'SUCCESS':
    case 'RESOLVED':
    case 'LOW':
      return 'badge-safe';
    case 'SUSPICIOUS':
    case 'PENDING':
    case 'INVESTIGATING':
    case 'MEDIUM':
      return 'badge-suspicious';
    case 'FRAUD':
    case 'BLOCKED':
    case 'OPEN':
    case 'HIGH':
    case 'CRITICAL':
      return 'badge-fraud';
    default:
      return 'badge-low';
  }
};
