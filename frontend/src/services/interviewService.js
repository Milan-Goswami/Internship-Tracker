import api from './api';

export const interviewService = {
  async getInterviewsByApplication(appId) {
    const response = await api.get(`/applications/${appId}/interviews`);
    return response.data.data;
  },

  async getInterviewById(interviewId) {
    const response = await api.get(`/interviews/${interviewId}`);
    return response.data.data;
  },

  async createInterview(appId, payload) {
    const response = await api.post(`/applications/${appId}/interviews`, payload);
    return response.data.data;
  },

  async updateInterview(interviewId, payload) {
    const response = await api.put(`/interviews/${interviewId}`, payload);
    return response.data.data;
  },

  async recordOutcome(interviewId, payload) {
    const response = await api.patch(`/interviews/${interviewId}/outcome`, payload);
    return response.data.data;
  },

  async deleteInterview(interviewId) {
    await api.delete(`/interviews/${interviewId}`);
  },
};
