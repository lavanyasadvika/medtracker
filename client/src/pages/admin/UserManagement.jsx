import { useState, useEffect } from 'react';
import api from '../../api/api';
import '../inventory/MedicineForm.css';
import '../inventory/InventoryPage.css';
import './AdminPage.css';

const ROLES = ['ADMIN', 'PHARMACIST', 'VIEWER', 'USER'];

/**
 * GET/POST /api/admin/users, PUT /api/admin/users/{id}, PATCH /api/admin/users/{id}/deactivate.
 * There is no reactivate endpoint on the backend, so deactivated users have no action available.
 */
const UserManagement = () => {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const [showCreateModal, setShowCreateModal] = useState(false);
    const [editingUser, setEditingUser] = useState(null);

    useEffect(() => {
        fetchUsers();
    }, []);

    const fetchUsers = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await api.get('/admin/users');
            setUsers(response.data);
        } catch (err) {
            console.error('Error fetching users:', err);
            setError('Failed to load users. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const handleDeactivate = async (id) => {
        if (!window.confirm('Are you sure you want to deactivate this user?')) {
            return;
        }

        try {
            const response = await api.patch(`/admin/users/${id}/deactivate`);
            setUsers(users.map(u => (u.id === id ? response.data : u)));
        } catch (err) {
            console.error('Error deactivating user:', err);
            alert(err.response?.data?.message || 'Failed to deactivate user. Please try again.');
        }
    };

    const handleCreateSuccess = () => {
        setShowCreateModal(false);
        fetchUsers();
    };

    const handleEditSuccess = (updatedUser) => {
        setUsers(users.map(u => (u.id === updatedUser.id ? updatedUser : u)));
        setEditingUser(null);
    };

    return (
        <div className="admin-section">
            <div className="admin-section-toolbar">
                <h2>Users</h2>
                <button className="btn btn-primary" onClick={() => setShowCreateModal(true)}>
                    + Add User
                </button>
            </div>

            {loading ? (
                <div className="loading-spinner">Loading users...</div>
            ) : error ? (
                <div className="error-message">{error}</div>
            ) : users.length === 0 ? (
                <div className="empty-state">
                    <p>No users found.</p>
                </div>
            ) : (
                <div className="table-container">
                    <table className="report-table">
                        <thead>
                        <tr>
                            <th>Name</th>
                            <th>Email</th>
                            <th>Role</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        {users.map((u) => (
                            <tr key={u.id}>
                                <td className="drug-name">{u.name}</td>
                                <td>{u.email}</td>
                                <td>{u.role}</td>
                                <td>
                                    <span className={`status-badge ${u.isActive ? 'status-green' : 'status-red'}`}>
                                        {u.isActive ? 'Active' : 'Inactive'}
                                    </span>
                                </td>
                                <td className="actions-cell">
                                    <button
                                        className="btn-icon btn-edit"
                                        title="Edit"
                                        onClick={() => setEditingUser(u)}
                                    >
                                        ✏️
                                    </button>
                                    {u.isActive && (
                                        <button
                                            className="btn-icon btn-delete"
                                            title="Deactivate"
                                            onClick={() => handleDeactivate(u.id)}
                                        >
                                            🚫
                                        </button>
                                    )}
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}

            {showCreateModal && (
                <CreateUserModal
                    onClose={() => setShowCreateModal(false)}
                    onSuccess={handleCreateSuccess}
                />
            )}

            {editingUser && (
                <EditUserModal
                    user={editingUser}
                    onClose={() => setEditingUser(null)}
                    onSuccess={handleEditSuccess}
                />
            )}
        </div>
    );
};

const CreateUserModal = ({ onClose, onSuccess }) => {
    const [formData, setFormData] = useState({ name: '', email: '', password: '', role: 'USER' });
    const [errors, setErrors] = useState({});
    const [submitting, setSubmitting] = useState(false);
    const [apiError, setApiError] = useState('');

    const validate = () => {
        const newErrors = {};

        if (!formData.name.trim() || formData.name.trim().length < 2) {
            newErrors.name = 'Name must be at least 2 characters';
        }
        if (!formData.email.trim()) {
            newErrors.email = 'Email is required';
        }
        if (!formData.password || formData.password.length < 8) {
            newErrors.password = 'Password must be at least 8 characters';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        if (errors[name]) {
            setErrors(prev => ({ ...prev, [name]: '' }));
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setApiError('');

        if (!validate()) {
            return;
        }

        setSubmitting(true);
        try {
            await api.post('/admin/users', {
                name: formData.name.trim(),
                email: formData.email.trim(),
                password: formData.password,
                role: formData.role
            });
            onSuccess();
        } catch (err) {
            console.error('Error creating user:', err);
            setApiError(err.response?.data?.message || 'Failed to create user. Please try again.');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content modal-small" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>Add User</h2>
                    <button onClick={onClose} className="close-btn">×</button>
                </div>

                <form onSubmit={handleSubmit} className="medicine-form">
                    {apiError && <span className="error-text admin-form-error">{apiError}</span>}

                    <div className="form-group">
                        <label htmlFor="createName">Name *</label>
                        <input
                            type="text"
                            id="createName"
                            name="name"
                            value={formData.name}
                            onChange={handleChange}
                            className={errors.name ? 'error' : ''}
                        />
                        {errors.name && <span className="error-text">{errors.name}</span>}
                    </div>

                    <div className="form-group">
                        <label htmlFor="createEmail">Email *</label>
                        <input
                            type="email"
                            id="createEmail"
                            name="email"
                            value={formData.email}
                            onChange={handleChange}
                            className={errors.email ? 'error' : ''}
                        />
                        {errors.email && <span className="error-text">{errors.email}</span>}
                    </div>

                    <div className="form-group">
                        <label htmlFor="createPassword">Password *</label>
                        <input
                            type="password"
                            id="createPassword"
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            className={errors.password ? 'error' : ''}
                        />
                        {errors.password && <span className="error-text">{errors.password}</span>}
                    </div>

                    <div className="form-group">
                        <label htmlFor="createRole">Role *</label>
                        <select id="createRole" name="role" value={formData.role} onChange={handleChange}>
                            {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                        </select>
                    </div>

                    <div className="form-actions">
                        <button type="button" onClick={onClose} className="btn btn-secondary" disabled={submitting}>
                            Cancel
                        </button>
                        <button type="submit" className="btn btn-primary" disabled={submitting}>
                            {submitting ? 'Creating...' : 'Create User'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

const EditUserModal = ({ user, onClose, onSuccess }) => {
    const [formData, setFormData] = useState({ name: user.name || '', role: user.role || 'USER' });
    const [errors, setErrors] = useState({});
    const [submitting, setSubmitting] = useState(false);
    const [apiError, setApiError] = useState('');

    const validate = () => {
        const newErrors = {};
        if (!formData.name.trim() || formData.name.trim().length < 2) {
            newErrors.name = 'Name must be at least 2 characters';
        }
        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        if (errors[name]) {
            setErrors(prev => ({ ...prev, [name]: '' }));
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setApiError('');

        if (!validate()) {
            return;
        }

        setSubmitting(true);
        try {
            const response = await api.put(`/admin/users/${user.id}`, {
                name: formData.name.trim(),
                role: formData.role
            });
            onSuccess(response.data);
        } catch (err) {
            console.error('Error updating user:', err);
            setApiError(err.response?.data?.message || 'Failed to update user. Please try again.');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content modal-small" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>Edit User</h2>
                    <button onClick={onClose} className="close-btn">×</button>
                </div>

                <form onSubmit={handleSubmit} className="medicine-form">
                    {apiError && <span className="error-text admin-form-error">{apiError}</span>}

                    <div className="form-group">
                        <label htmlFor="editEmail">Email</label>
                        <input type="email" id="editEmail" value={user.email} disabled />
                    </div>

                    <div className="form-group">
                        <label htmlFor="editName">Name *</label>
                        <input
                            type="text"
                            id="editName"
                            name="name"
                            value={formData.name}
                            onChange={handleChange}
                            className={errors.name ? 'error' : ''}
                        />
                        {errors.name && <span className="error-text">{errors.name}</span>}
                    </div>

                    <div className="form-group">
                        <label htmlFor="editRole">Role *</label>
                        <select id="editRole" name="role" value={formData.role} onChange={handleChange}>
                            {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                        </select>
                    </div>

                    <div className="form-actions">
                        <button type="button" onClick={onClose} className="btn btn-secondary" disabled={submitting}>
                            Cancel
                        </button>
                        <button type="submit" className="btn btn-primary" disabled={submitting}>
                            {submitting ? 'Saving...' : 'Save Changes'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default UserManagement;
