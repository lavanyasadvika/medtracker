import { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/api';
import { downloadBlob } from '../../utils/downloadFile';
import '../inventory/InventoryPage.css';
import './ReportsPage.css';

/** GET /api/reports/inventory - name/category/quantity/expiry/manufacturer per FR-25..27. */
const InventoryReport = () => {
    const { user } = useAuth();
    const canExport = user?.role === 'ADMIN' || user?.role === 'PHARMACIST';

    const [items, setItems] = useState([]);
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
            const response = await api.get('/reports/inventory');
            setItems(response.data);
        } catch (err) {
            console.error('Error fetching inventory report:', err);
            setError('Failed to load inventory report. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const handleExport = async (format) => {
        setExporting(true);
        try {
            const response = await api.get('/reports/inventory/export', {
                params: { format },
                responseType: 'blob'
            });
            const today = new Date().toISOString().slice(0, 10);
            downloadBlob(response.data, `medtracker-inventory-report-${today}.${format}`);
        } catch (err) {
            console.error('Error exporting inventory report:', err);
            alert('Failed to export inventory report. Please try again.');
        } finally {
            setExporting(false);
        }
    };

    return (
        <div className="report-section">
            <div className="report-toolbar">
                <h2>Inventory Report</h2>
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
                <div className="loading-spinner">Loading inventory report...</div>
            ) : error ? (
                <div className="error-message">{error}</div>
            ) : items.length === 0 ? (
                <div className="empty-state">
                    <p>No active medicines found.</p>
                </div>
            ) : (
                <div className="table-container">
                    <table className="report-table">
                        <thead>
                        <tr>
                            <th>Name</th>
                            <th>Category</th>
                            <th>Quantity</th>
                            <th>Expiry</th>
                            <th>Manufacturer</th>
                        </tr>
                        </thead>
                        <tbody>
                        {items.map((item, index) => (
                            <tr key={`${item.name}-${index}`}>
                                <td className="drug-name">{item.name}</td>
                                <td>{item.category || '-'}</td>
                                <td>{item.quantity}</td>
                                <td>{item.expiry ? new Date(item.expiry).toLocaleDateString() : '-'}</td>
                                <td>{item.manufacturer || '-'}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

export default InventoryReport;
