import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/auth/ProtectedRoute';
import RoleRoute from './components/auth/RoleRoute';
import Layout from './components/layout/Layout';
import LoginPage from './pages/auth/LoginPage';
import SignupPage from './pages/auth/SignupPage';
import DashboardPage from './pages/dashboard/DashboardPage';
import InventoryPage from './pages/inventory/InventoryPage';
import ScanPage from './pages/scan/ScanPage';
import ReportsPage from './pages/reports/ReportsPage';
import AdminPage from './pages/admin/AdminPage';
import { ROLES } from './utils/rolePermissions';

function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    {/* Public Routes */}
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/signup" element={<SignupPage />} />

                    {/* Protected Routes with Layout */}
                    <Route
                        element={
                            <ProtectedRoute>
                                <Layout />
                            </ProtectedRoute>
                        }
                    >
                        {/* Dashboard - All authenticated users */}
                        <Route path="/dashboard" element={<DashboardPage />} />

                        {/* Inventory - All authenticated server roles */}
                        <Route path="/inventory" element={<InventoryPage />} />

                        {/* Scan - Admin and Pharmacist only */}
                        <Route
                            path="/scan"
                            element={
                                <RoleRoute allow={[ROLES.ADMIN, ROLES.PHARMACIST]}>
                                    <ScanPage />
                                </RoleRoute>
                            }
                        />

                        {/* Reports - All authenticated server roles */}
                        <Route path="/reports" element={<ReportsPage />} />

                        {/* Admin - Admin only */}
                        <Route
                            path="/admin"
                            element={
                                <RoleRoute allow={[ROLES.ADMIN]}>
                                    <AdminPage />
                                </RoleRoute>
                            }
                        />
                    </Route>

                    {/* Default redirect */}
                    <Route path="/" element={<Navigate to="/dashboard" replace />} />

                    {/* Catch-all redirect */}
                    <Route path="*" element={<Navigate to="/dashboard" replace />} />
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    );
}

export default App;
