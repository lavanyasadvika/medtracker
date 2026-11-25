/**
 * Role-based permission matrix matching the Spring controllers and OpenAPI contract.
 * Defines which routes each role can access.
 */

export const ROLES = {
    ADMIN: 'ADMIN',
    USER: 'USER',
    PHARMACIST: 'PHARMACIST',
    VIEWER: 'VIEWER'
};

const ALL_AUTHENTICATED_ROLES = [ROLES.ADMIN, ROLES.PHARMACIST, ROLES.VIEWER, ROLES.USER];

export const ROUTE_PERMISSIONS = {
    '/dashboard': ALL_AUTHENTICATED_ROLES,
    '/inventory': ALL_AUTHENTICATED_ROLES,
    '/scan': [ROLES.ADMIN, ROLES.PHARMACIST],
    '/reports': ALL_AUTHENTICATED_ROLES,
    '/admin': [ROLES.ADMIN]
};

/**
 * Check if a role has permission to access a route
 * @param {string} role - User role
 * @param {string} route - Route path
 * @returns {boolean}
 */
export const hasRoutePermission = (role, route) => {
    const allowedRoles = ROUTE_PERMISSIONS[route];
    return allowedRoles ? allowedRoles.includes(role) : false;
};

/**
 * Get all accessible routes for a given role
 * @param {string} role - User role
 * @returns {string[]} Array of accessible route paths
 */
export const getAccessibleRoutes = (role) => {
    return Object.keys(ROUTE_PERMISSIONS).filter((route) =>
        ROUTE_PERMISSIONS[route].includes(role)
    );
};
