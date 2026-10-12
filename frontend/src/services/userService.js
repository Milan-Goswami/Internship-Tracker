import api from './api';

export const userService = {
  /**
   * Fetches current authenticated candidate profile and search preferences.
   */
  async getUserProfile() {
    const response = await api.get('/users/me');
    return response.data.data;
  },

  /**
   * Updates candidate profile preferences (target role, location, salary, work mode).
   */
  async updatePreferences(payload) {
    const response = await api.put('/users/me', payload);
    return response.data.data;
  },

  /**
   * Updates candidate full name.
   */
  async updateProfile(fullName) {
    const response = await api.patch('/users/me', { fullName });
    return response.data.data;
  },
};

export default userService;
