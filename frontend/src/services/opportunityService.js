import api from './api';

export const opportunityService = {
  async getOpportunities(params = {}) {
    const response = await api.get('/opportunities', { params });
    return response.data.data;
  },

  async getOpportunityById(id) {
    const response = await api.get(`/opportunities/${id}`);
    return response.data.data;
  },

  async getOpportunitySkillMatch(id) {
    const response = await api.get(`/opportunities/${id}/skill-match`);
    return response.data.data;
  },

  async createOpportunity(payload) {
    const response = await api.post('/opportunities', payload);
    return response.data.data;
  },

  async updateOpportunity(id, payload) {
    const response = await api.put(`/opportunities/${id}`, payload);
    return response.data.data;
  },

  async deleteOpportunity(id) {
    await api.delete(`/opportunities/${id}`);
  },

  async getCompanies(search) {
    const params = search ? { search } : {};
    const response = await api.get('/companies', { params });
    return response.data.data;
  },

  async createCompany(payload) {
    const response = await api.post('/companies', payload);
    return response.data.data;
  },
};
