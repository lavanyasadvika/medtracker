import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/api';
import AddMedicine from './AddMedicine';
import EditMedicine from './EditMedicine';
import DispenseModal from './DispenseModal';
import RestockModal from './RestockModal';
import './InventoryPage.css';

const InventoryPage = () => {
    const { user } = useAuth();
    const [medicines, setMedicines] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // Filters (initial search term may arrive via ?q= deep link, e.g. from the Dashboard)
    const [searchParams] = useSearchParams();
    const [searchQuery, setSearchQuery] = useState(searchParams.get('q') || '');
    const [expiringInDays, setExpiringInDays] = useState('');
    const [lowStockFilter, setLowStockFilter] = useState(false);

    // Modals
    const [showAddModal, setShowAddModal] = useState(false);
    const [editingMedicine, setEditingMedicine] = useState(null);
    const [dispensingMedicine, setDispensingMedicine] = useState(null);
    const [restockingMedicine, setRestockingMedicine] = useState(null);

    const canEdit = user?.role === 'ADMIN' || user?.role === 'PHARMACIST';
    const canDelete = user?.role === 'ADMIN';
    const canDispense = user?.role === 'ADMIN' || user?.role === 'PHARMACIST' || user?.role === 'VIEWER';
    const canRestock = user?.role === 'ADMIN' || user?.role === 'PHARMACIST';

    useEffect(() => {
        fetchMedicines();
    }, [searchQuery, expiringInDays, lowStockFilter]);

    const fetchMedicines = async () => {
        try {
            setLoading(true);
            setError(null);

            const params = {};
            if (searchQuery) params.q = searchQuery;
            if (expiringInDays) params.expiringInDays = expiringInDays;
            if (lowStockFilter) params.lowStock = true;

            const response = await api.get('/medicines', { params });
            setMedicines(response.data);
        } catch (err) {
            console.error('Error fetching medicines:', err);
            setError('Failed to load medicines. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (id) => {
        if (!window.confirm('Are you sure you want to delete this medicine?')) {
            return;
        }

        try {
            await api.delete(`/medicines/${id}`);
            setMedicines(medicines.filter(m => m.id !== id));
        } catch (err) {
            console.error('Error deleting medicine:', err);
            alert('Failed to delete medicine. Please try again.');
        }
    };

    const handleAddSuccess = (newMedicine) => {
        setMedicines([newMedicine, ...medicines]);
        setShowAddModal(false);
    };

    const handleEditSuccess = (updatedMedicine) => {
        setMedicines(medicines.map(m => m.id === updatedMedicine.id ? updatedMedicine : m));
        setEditingMedicine(null);
    };

    const handleDispenseSuccess = (updatedMedicine) => {
        setMedicines(medicines.map(m => m.id === updatedMedicine.id ? updatedMedicine : m));
        setDispensingMedicine(null);
    };

    const handleRestockSuccess = (updatedMedicine) => {
        setMedicines(medicines.map(m => m.id === updatedMedicine.id ? updatedMedicine : m));
        setRestockingMedicine(null);
    };

    const getExpiryStatus = (expiryDate) => {
        const today = new Date();
        const expiry = new Date(expiryDate);
        const daysUntilExpiry = Math.ceil((expiry - today) / (1000 * 60 * 60 * 24));

        if (daysUntilExpiry < 0) return { status: 'Expired', color: 'red' };
        if (daysUntilExpiry <= 30) return { status: 'Expiring Soon', color: 'amber' };
        return { status: 'Safe', color: 'green' };
    };

    return (
        <div className="inventory-container">
            <div className="inventory-header">
                <h1>Medicine Inventory</h1>
                {canEdit && (
                    <button onClick={() => setShowAddModal(true)} className="btn btn-primary">
                        + Add Medicine
                    </button>
                )}
            </div>

            <div className="inventory-filters">
                <input
                    type="text"
                    placeholder="Search by drug name..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="search-input"
                />

                <input
                    type="number"
                    placeholder="Expiring in days..."
                    value={expiringInDays}
                    onChange={(e) => setExpiringInDays(e.target.value)}
                    className="filter-input"
                    min="0"
                />

                <label className="checkbox-label">
                    <input
                        type="checkbox"
                        checked={lowStockFilter}
                        onChange={(e) => setLowStockFilter(e.target.checked)}
                    />
                    Low Stock Only
                </label>

                <button onClick={fetchMedicines} className="btn btn-secondary">
                    Refresh
                </button>
            </div>

            {loading ? (
                <div className="loading-spinner">Loading medicines...</div>
            ) : error ? (
                <div className="error-message">{error}</div>
            ) : medicines.length === 0 ? (
                <div className="empty-state">
                    <p>No medicines found.</p>
                    {canEdit && (
                        <button onClick={() => setShowAddModal(true)} className="btn btn-primary">
                            Add First Medicine
                        </button>
                    )}
                </div>
            ) : (
                <div className="table-container">
                    <table className="inventory-table">
                        <thead>
                        <tr>
                            <th>Drug Name</th>
                            <th>Category</th>
                            <th>Manufacturer</th>
                            <th>Batch Number</th>
                            <th>Expiry Date</th>
                            <th>Status</th>
                            <th>Quantity</th>
                            <th>Low Stock Threshold</th>
                            <th>Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        {medicines.map((medicine) => {
                            const expiryStatus = getExpiryStatus(medicine.expiryDate);
                            const isLowStock = medicine.lowStockThreshold != null
                                && Number(medicine.quantity) <= Number(medicine.lowStockThreshold);

                            return (
                                <tr key={medicine.id}>
                                    <td className="drug-name">{medicine.drugName}</td>
                                    <td>{medicine.category || '-'}</td>
                                    <td>{medicine.manufacturer || '-'}</td>
                                    <td>{medicine.batchNumber || '-'}</td>
                                    <td>{new Date(medicine.expiryDate).toLocaleDateString()}</td>
                                    <td>
                      <span className={`status-badge status-${expiryStatus.color}`}>
                        {expiryStatus.status}
                      </span>
                                    </td>
                                    <td>
                      <span className={isLowStock ? 'low-stock-qty' : ''}>
                        {medicine.quantity}
                          {isLowStock && ' ⚠️'}
                      </span>
                                    </td>
                                    <td>{medicine.lowStockThreshold ?? '-'}</td>
                                    <td className="actions-cell">
                                        {canEdit && (
                                            <button
                                                onClick={() => setEditingMedicine(medicine)}
                                                className="btn-icon btn-edit"
                                                title="Edit"
                                            >
                                                ✏️
                                            </button>
                                        )}
                                        {canDelete && (
                                            <button
                                                onClick={() => handleDelete(medicine.id)}
                                                className="btn-icon btn-delete"
                                                title="Delete"
                                            >
                                                🗑️
                                            </button>
                                        )}
                                        {canDispense && (
                                            <button
                                                onClick={() => setDispensingMedicine(medicine)}
                                                className="btn-icon btn-dispense"
                                                title="Dispense"
                                            >
                                                📤
                                            </button>
                                        )}
                                        {canRestock && (
                                            <button
                                                onClick={() => setRestockingMedicine(medicine)}
                                                className="btn-icon btn-restock"
                                                title="Restock"
                                            >
                                                📥
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            );
                        })}
                        </tbody>
                    </table>
                </div>
            )}

            {showAddModal && (
                <AddMedicine
                    onClose={() => setShowAddModal(false)}
                    onSuccess={handleAddSuccess}
                />
            )}

            {editingMedicine && (
                <EditMedicine
                    medicine={editingMedicine}
                    onClose={() => setEditingMedicine(null)}
                    onSuccess={handleEditSuccess}
                />
            )}

            {dispensingMedicine && (
                <DispenseModal
                    medicine={dispensingMedicine}
                    onClose={() => setDispensingMedicine(null)}
                    onSuccess={handleDispenseSuccess}
                />
            )}

            {restockingMedicine && (
                <RestockModal
                    medicine={restockingMedicine}
                    onClose={() => setRestockingMedicine(null)}
                    onSuccess={handleRestockSuccess}
                />
            )}
        </div>
    );
};

export default InventoryPage;
