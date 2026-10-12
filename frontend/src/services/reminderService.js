import api from './api';

export const reminderService = {
  async getReminders(params = {}) {
    const response = await api.get('/reminders', { params });
    return response.data.data;
  },

  async getReminderById(id) {
    const response = await api.get(`/reminders/${id}`);
    return response.data.data;
  },

  async createReminder(payload) {
    const response = await api.post('/reminders', payload);
    return response.data.data;
  },

  async updateReminder(id, payload) {
    const response = await api.put(`/reminders/${id}`, payload);
    return response.data.data;
  },

  async completeReminder(id) {
    const response = await api.patch(`/reminders/${id}/complete`);
    return response.data.data;
  },

  async deleteReminder(id) {
    await api.delete(`/reminders/${id}`);
  },
};

export default reminderService;
