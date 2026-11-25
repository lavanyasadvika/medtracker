import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/api';
import './DashboardPage.css';

const STATUS_COLORS = {
    red: '#dc3545',
    amber: '#ffc107',
    green: '#28a745'
};

const DashboardPage = () => {
    const { user } = useAuth();
    const navigate = useNavigate();

    const [expiryData, setExpiryData] = useState({ expired: [], expiring: [], safe: [] });
    const [lowStockData, setLowStockData] = useState([]);
    const [medicines, setMedicines] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [lastUpdated, setLastUpdated] = useState(null);

    useEffect(() => {
        fetchDashboardData();
    }, []);

    const fetchDashboardData = async () => {
        try {
            setLoading(true);
            setError(null);

            const [expiryResponse, lowStockResponse, medicinesResponse] = await Promise.all([
                api.get('/reports/expiry'),
                api.get('/reports/low-stock'),
                api.get('/medicines')
            ]);

            setExpiryData(expiryResponse.data);
            setLowStockData(lowStockResponse.data);
            setMedicines(medicinesResponse.data);
            setLastUpdated(new Date());
        } catch (err) {
            console.error('Error fetching dashboard data:', err);
            setError('Failed to load dashboard data. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const goToInventory = (drugName) => navigate(`/inventory?q=${encodeURIComponent(drugName)}`);
    const goToReport = (tab) => navigate(`/reports?tab=${tab}`);

    const expiredCount = expiryData.expired?.length || 0;
    const expiringCount = expiryData.expiring?.length || 0;
    const safeCount = expiryData.safe?.length || 0;
    const lowStockCount = lowStockData?.length || 0;
    const totalActive = expiredCount + expiringCount + safeCount;

    const { totalMedicines, totalUnits, totalValue } = useMemo(() => {
        return medicines.reduce(
            (acc, medicine) => {
                const quantity = Number(medicine.quantity) || 0;
                const price = Number(medicine.price) || 0;
                acc.totalMedicines += 1;
                acc.totalUnits += quantity;
                acc.totalValue += quantity * price;
                return acc;
            },
            { totalMedicines: 0, totalUnits: 0, totalValue: 0 }
        );
    }, [medicines]);

    const { expiredPct, expiringPct, safePct, donutGradient } = useMemo(() => {
        if (totalActive === 0) {
            return { expiredPct: 0, expiringPct: 0, safePct: 0, donutGradient: 'conic-gradient(#e9ecef 0% 100%)' };
        }
        const eP = (expiredCount / totalActive) * 100;
        const xP = (expiringCount / totalActive) * 100;
        const sP = (safeCount / totalActive) * 100;
        const gradient = `conic-gradient(${STATUS_COLORS.red} 0% ${eP}%, ${STATUS_COLORS.amber} ${eP}% ${eP + xP}%, ${STATUS_COLORS.green} ${eP + xP}% 100%)`;
        return { expiredPct: eP, expiringPct: xP, safePct: sP, donutGradient: gradient };
    }, [expiredCount, expiringCount, safeCount, totalActive]);

    const renderAlertSection = ({ icon, title, items, variant, emptyMessage, reportTab, renderDetail, showQuantity = true }) => (
        <div className="alert-section">
            <div className="alert-section-header">
                <h2>{icon} {title}</h2>
                {items?.length > 0 && (
                    <button type="button" className="btn-link" onClick={() => goToReport(reportTab)}>
                        View full report →
                    </button>
                )}
            </div>

            {!items || items.length === 0 ? (
                <div className="empty-alert">{emptyMessage}</div>
            ) : (
                <div className="alert-list">
                    {items.slice(0, 5).map((item) => (
                        <button
                            key={item.id}
                            type="button"
                            className={`alert-item ${variant}`}
                            onClick={() => goToInventory(item.drugName)}
                            title="View in Inventory"
                        >
                            <span className="alert-name">{item.drugName}</span>
                            <span className="alert-detail">{renderDetail(item)}</span>
                            {showQuantity && <span className="alert-quantity">Qty: {item.quantity}</span>}
                        </button>
                    ))}
                    {items.length > 5 && (
                        <button type="button" className="btn-link alert-more" onClick={() => goToReport(reportTab)}>
                            +{items.length - 5} more →
                        </button>
                    )}
                </div>
            )}
        </div>
    );

    if (loading && !lastUpdated) {
        return (
            <div className="dashboard-container">
                <div className="loading-spinner">Loading dashboard...</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="dashboard-container">
                <div className="error-message">{error}</div>
                <button onClick={fetchDashboardData} className="btn btn-primary">
                    Retry
                </button>
            </div>
        );
    }

    return (
        <div className="dashboard-container">
            <div className="dashboard-header">
                <div>
                    <h1>Dashboard</h1>
                    <p className="welcome-text">Welcome back, {user?.name}</p>
                </div>
                <div className="dashboard-header-actions">
                    {lastUpdated && (
                        <span className="last-updated">Updated {lastUpdated.toLocaleTimeString()}</span>
                    )}
                    <button
                        type="button"
                        onClick={fetchDashboardData}
                        className="btn btn-secondary btn-refresh"
                        disabled={loading}
                    >
                        <span className={`refresh-icon ${loading ? 'spinning' : ''}`}>🔄</span> Refresh
                    </button>
                </div>
            </div>

            <div className="overview-cards">
                <div className="overview-card">
                    <span className="overview-icon">📋</span>
                    <div>
                        <p className="overview-count">{totalMedicines}</p>
                        <span className="overview-label">Total Medicines</span>
                    </div>
                </div>
                <div className="overview-card">
                    <span className="overview-icon">🔢</span>
                    <div>
                        <p className="overview-count">{totalUnits.toLocaleString(undefined, { maximumFractionDigits: 2 })}</p>
                        <span className="overview-label">Units in Stock</span>
                    </div>
                </div>
                <div className="overview-card">
                    <span className="overview-icon">💰</span>
                    <div>
                        <p className="overview-count">{totalValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</p>
                        <span className="overview-label">Total Inventory Value</span>
                    </div>
                </div>
            </div>

            <div className="stock-health-panel">
                <h2>Stock Health</h2>
                <div className="stock-health-body">
                    <div className="donut-wrapper">
                        <div className="donut-chart" style={{ background: donutGradient }}>
                            <div className="donut-hole">
                                <span className="donut-total">{totalActive}</span>
                                <span className="donut-total-label">Medicines</span>
                            </div>
                        </div>
                        {totalActive > 0 && (
                            <div className="donut-legend">
                                <span className="legend-item"><i style={{ background: STATUS_COLORS.red }} />Expired {expiredPct.toFixed(0)}%</span>
                                <span className="legend-item"><i style={{ background: STATUS_COLORS.amber }} />Expiring {expiringPct.toFixed(0)}%</span>
                                <span className="legend-item"><i style={{ background: STATUS_COLORS.green }} />Safe {safePct.toFixed(0)}%</span>
                            </div>
                        )}
                    </div>

                    <div className="stock-health-cards">
                        <button
                            type="button"
                            className="dashboard-card"
                            style={{ borderLeftColor: STATUS_COLORS.red }}
                            onClick={() => goToReport('expiry')}
                        >
                            <div className="card-icon expired">⚠️</div>
                            <div className="card-content">
                                <h3>Expired</h3>
                                <p className="card-count">{expiredCount}</p>
                                <span className="card-label">Requires immediate attention</span>
                            </div>
                        </button>

                        <button
                            type="button"
                            className="dashboard-card"
                            style={{ borderLeftColor: STATUS_COLORS.amber }}
                            onClick={() => goToReport('expiry')}
                        >
                            <div className="card-icon expiring">⏰</div>
                            <div className="card-content">
                                <h3>Expiring Soon</h3>
                                <p className="card-count">{expiringCount}</p>
                                <span className="card-label">Within 30 days</span>
                            </div>
                        </button>

                        <button
                            type="button"
                            className="dashboard-card"
                            style={{ borderLeftColor: STATUS_COLORS.green }}
                            onClick={() => goToReport('expiry')}
                        >
                            <div className="card-icon safe">✓</div>
                            <div className="card-content">
                                <h3>Safe Stock</h3>
                                <p className="card-count">{safeCount}</p>
                                <span className="card-label">Good condition</span>
                            </div>
                        </button>

                        <button
                            type="button"
                            className="dashboard-card"
                            style={{ borderLeftColor: STATUS_COLORS.amber }}
                            onClick={() => goToReport('low-stock')}
                        >
                            <div className="card-icon low-stock">📦</div>
                            <div className="card-content">
                                <h3>Low Stock</h3>
                                <p className="card-count">{lowStockCount}</p>
                                <span className="card-label">Needs restocking</span>
                            </div>
                        </button>
                    </div>
                </div>
            </div>

            {renderAlertSection({
                icon: '⚠️',
                title: 'Expired Medicines - Immediate Action Required',
                items: expiryData.expired,
                variant: 'expired-alert',
                emptyMessage: 'No expired medicines. Great job staying on top of stock! 🎉',
                reportTab: 'expiry',
                renderDetail: (item) => `Expired: ${new Date(item.expiryDate).toLocaleDateString()}`
            })}

            {renderAlertSection({
                icon: '⏰',
                title: 'Expiring Soon',
                items: expiryData.expiring,
                variant: 'expiring-alert',
                emptyMessage: 'Nothing expiring in the near term. ✅',
                reportTab: 'expiry',
                renderDetail: (item) => `Expires: ${new Date(item.expiryDate).toLocaleDateString()}`
            })}

            {renderAlertSection({
                icon: '📦',
                title: 'Low Stock Items',
                items: lowStockData,
                variant: 'low-stock-alert',
                emptyMessage: 'All stock levels are healthy. ✅',
                reportTab: 'low-stock',
                showQuantity: false,
                renderDetail: (item) => `Current: ${item.quantity} / Threshold: ${item.lowStockThreshold}`
            })}
        </div>
    );
};

export default DashboardPage;
