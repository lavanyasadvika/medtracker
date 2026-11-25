import { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/api';
import { downloadBlob } from '../../utils/downloadFile';
import '../inventory/InventoryPage.css';
import './ReportsPage.css';

const STATUS_LABELS = {
    red: 'Expired',
    amber: 'Expiring Soon',
    green: 'Safe'
};

/** GET /api/reports/expiry - expired/expiring/safe buckets, colour-coded per FR-23. */
const ExpiryReport = () => {
    const { user } = useAuth();
    const canExport = user?.role === 'ADMIN' || user?.role === 'PHARMACIST';

    const [report, setReport] = useState({ expired: [], expiring: [], safe: [] });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [exporting, setExporting] = useState(false);

    useEffect(() => {
        fetchReport();
    }, []);

    const fetchReport = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await api.get('/reports/expiry');
            setReport(response.data);
        } catch (err) {
            console.error('Error fetching expiry report:', err);
            setError('Failed to load expiry report. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const handleExport = async (format) => {
        setExporting(true);
        try {
            const response = await api.get('/reports/expiry/export', {
                params: { format },
                responseType: 'blob'
            });
            const today = new Date().toISOString().slice(0, 10);
            downloadBlob(response.data, `medtracker-expiry-report-${today}.${format}`);
        } catch (err) {
            console.error('Error exporting expiry report:', err);
            alert('Failed to export expiry report. Please try again.');
        } finally {
            setExporting(false);
        }
    };

    const rows = [
        ...report.expired.map(item => ({ ...item, status: item.status || 'red' })),
        ...report.expiring.map(item => ({ ...item, status: item.status || 'amber' })),
        ...report.safe.map(item => ({ ...item, status: item.status || 'green' }))
    ];

    return (
        <div className="report-section">
            <div className="report-toolbar">
                <h2>Expiry Report</h2>
                {canExport && (
                    <div className="report-export-actions">
                        <button className="btn btn-secondary" onClick={() => handleExport('csv')} disabled={exporting || loading}>
                            Export CSV
                        </button>
                        <button className="btn btn-secondary" onClick={() => handleExport('pdf')} disabled={exporting || loading}>
                            Export PDF
                        </button>
                    </div>
                )}
            </div>

            {loading ? (
                <div className="loading-spinner">Loading expiry report...</div>
            ) : error ? (
                <div className="error-message">{error}</div>
            ) : rows.length === 0 ? (
                <div className="empty-state">
                    <p>No active medicines found.</p>
                </div>
            ) : (
                <>
                    <div className="report-summary">
                        <span className="status-badge status-red">Expired: {report.expired.length}</span>
                        <span className="status-badge status-amber">Expiring Soon: {report.expiring.length}</span>
                        <span className="status-badge status-green">Safe: {report.safe.length}</span>
                    </div>

                    <div className="table-container">
                        <table className="report-table">
                            <thead>
                            <tr>
                                <th>Name</th>
                                <th>Category</th>
                                <th>Quantity</th>
                                <th>Expiry</th>
                                <th>Manufacturer</th>
                                <th>Status</th>
                            </tr>
                            </thead>
                            <tbody>
                            {rows.map((item, index) => (
                                <tr key={`${item.name}-${index}`}>
                                    <td className="drug-name">{item.name}</td>
                                    <td>{item.category || '-'}</td>
                                    <td>{item.quantity}</td>
                                    <td>{item.expiry ? new Date(item.expiry).toLocaleDateString() : '-'}</td>
                                    <td>{item.manufacturer || '-'}</td>
                                    <td>
                                        <span className={`status-badge status-${item.status}`}>
                                            {STATUS_LABELS[item.status] || item.status}
                                        </span>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                </>
            )}
        </div>
    );
};

export default ExpiryReport;
