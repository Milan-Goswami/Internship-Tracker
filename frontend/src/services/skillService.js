import api from './api';

export const skillService = {
  async getUserSkills() {
    const response = await api.get('/skills/user');
    return response.data.data;
  },

  async searchSkills(query) {
    const params = query ? { q: query } : {};
    const response = await api.get('/skills/search', { params });
    return response.data.data;
  },

  async addUserSkill(payload) {
    const response = await api.post('/skills/user', payload);
    return response.data.data;
  },

  async removeUserSkill(skillId) {
    await api.delete(`/skills/user/${skillId}`);
  },
};
