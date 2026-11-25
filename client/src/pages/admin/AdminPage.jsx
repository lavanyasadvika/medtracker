import { useState } from 'react';
import UserManagement from './UserManagement';
import AlertSettings from './AlertSettings';
import './AdminPage.css';

const TABS = [
    { id: 'users', label: 'User Management' },
    { id: 'settings', label: 'Alert Settings' }
];

/** Route-guarded to ADMIN only via RoleRoute in App.jsx. */
const AdminPage = () => {
    const [activeTab, setActiveTab] = useState('users');

    return (
        <div className="admin-container">
            <div className="admin-header">
                <h1>Admin Panel</h1>
            </div>

            <div className="admin-tabs" role="tablist" aria-label="Admin section">
                {TABS.map((tab) => (
                    <button
                        key={tab.id}
                        role="tab"
                        aria-selected={activeTab === tab.id}
                        className={`admin-tab ${activeTab === tab.id ? 'admin-tab-active' : ''}`}
                        onClick={() => setActiveTab(tab.id)}
                    >
                        {tab.label}
                    </button>
                ))}
            </div>

            <div className="admin-tab-panel" role="tabpanel">
                {activeTab === 'users' && <UserManagement />}
                {activeTab === 'settings' && <AlertSettings />}
            </div>
        </div>
    );
};

export default AdminPage;
