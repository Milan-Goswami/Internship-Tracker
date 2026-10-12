import api from './api';

export const analyticsService = {
  async getDashboardSummary() {
    const response = await api.get('/analytics/summary');
    return response.data.data;
  },

  async getFunnelAnalytics() {
    const response = await api.get('/analytics/funnel');
    return response.data.data;
  },

  async getSkillDeficits() {
    const response = await api.get('/analytics/skill-deficits');
    return response.data.data;
  },
};

export default analyticsService;
