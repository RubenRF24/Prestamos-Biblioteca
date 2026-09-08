/**
 * Base de la API. Se usa una ruta relativa para que el mismo build funcione en dev
 * (proxy de ng serve) y en prod (Nginx proxya /api al backend). Mismo origen => la
 * cookie httpOnly y el token XSRF viajan sin CORS.
 */
export const API_BASE = '/api';
