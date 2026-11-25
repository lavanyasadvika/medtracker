import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import ExpiryReport from './ExpiryReport';
import InventoryReport from './InventoryReport';
import LowStockReport from './LowStockReport';
import './ReportsPage.css';

const TABS = [
    { id: 'expiry', label: 'Expiry Report' },
    { id: 'inventory', label: 'Inventory Report' },
    { id: 'low-stock', label: 'Low Stock Report' }
];

const ReportsPage = () => {
    const [searchParams, setSearchParams] = useSearchParams();
    const requestedTab = searchParams.get('tab');
    const initialTab = TABS.some((tab) => tab.id === requestedTab) ? requestedTab : 'expiry';
    const [activeTab, setActiveTab] = useState(initialTab);

    const handleTabChange = (tabId) => {
        setActiveTab(tabId);
        setSearchParams({ tab: tabId });
    };

    return (
        <div className="reports-container">
            <div className="reports-header">
                <h1>Reports</h1>
            </div>

            <div className="reports-tabs" role="tablist" aria-label="Report type">
                {TABS.map((tab) => (
                    <button
                        key={tab.id}
                        role="tab"
                        aria-selected={activeTab === tab.id}
                        className={`reports-tab ${activeTab === tab.id ? 'reports-tab-active' : ''}`}
                        onClick={() => handleTabChange(tab.id)}
                    >
                        {tab.label}
                    </button>
                ))}
            </div>

            <div className="reports-tab-panel" role="tabpanel">
                {activeTab === 'expiry' && <ExpiryReport />}
                {activeTab === 'inventory' && <InventoryReport />}
                {activeTab === 'low-stock' && <LowStockReport />}
            </div>
        </div>
    );
};

export default ReportsPage;
