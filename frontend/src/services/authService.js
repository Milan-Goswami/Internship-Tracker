import api, { setAuthToken } from './api';
import axios from 'axios';

let activeRefreshPromise = null;

export const authService = {
  async register({ email, password, fullName }) {
    const response = await api.post('/auth/register', {
      email,
      password,
      fullName,
    });
    return response.data;
  },

  async login({ email, password }) {
    const response = await api.post('/auth/login', {
      email,
      password,
    });
    const { user, accessToken } = response.data.data;
    setAuthToken(accessToken);
    return { user, accessToken };
  },

  async refresh() {
    if (activeRefreshPromise) {
      return activeRefreshPromise;
    }

    activeRefreshPromise = (async () => {
      try {
        const response = await axios.post(
          '/api/v1/auth/refresh',
          {},
          {
            headers: {
              'X-InternTrack-Client': 'web',
            },
            withCredentials: true,
          }
        );
        const { user, accessToken } = response.data.data;
        setAuthToken(accessToken);
        return { user, accessToken };
      } finally {
        activeRefreshPromise = null;
      }
    })();

    return activeRefreshPromise;
  },

  async logout() {
    try {
      await api.post('/auth/logout');
    } finally {
      setAuthToken(null);
    }
  },

  async getMe() {
    const response = await api.get('/users/me');
    return response.data.data;
  },

  async updateProfileAndPreferences(preferencesData) {
    const response = await api.put('/users/me', preferencesData);
    return response.data.data;
  },

  async updateFullName(fullName) {
    const response = await api.patch('/users/me', { fullName });
    return response.data.data;
  },
};

export default authService;
