import api from './api';

export const applicationService = {
  async getApplications(status) {
    const params = {};
    if (status && status !== 'ALL') {
      params.status = status;
    }
    const response = await api.get('/applications', { params });
    return response.data.data;
  },

  async getApplicationById(id) {
    const response = await api.get(`/applications/${id}`);
    return response.data.data;
  },

  async createApplication(payload) {
    const response = await api.post('/applications', payload);
    return response.data.data;
  },

  async updateStatus(id, status) {
    const response = await api.patch(`/applications/${id}/status`, { status });
    return response.data.data;
  },

  async batchUpdateStatus(applicationIds, status) {
    const response = await api.patch('/applications/batch-status', {
      applicationIds,
      status,
    });
    return response.data.data;
  },

  async updateApplication(id, payload) {
    const response = await api.put(`/applications/${id}`, payload);
    return response.data.data;
  },

  async deleteApplication(id) {
    await api.delete(`/applications/${id}`);
  },
};