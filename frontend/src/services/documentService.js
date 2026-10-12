import api from './api';

export const documentService = {
  async getDocuments(documentType) {
    const params = documentType ? { documentType } : {};
    const response = await api.get('/documents', { params });
    return response.data.data;
  },

  async getDocumentById(id) {
    const response = await api.get(`/documents/${id}`);
    return response.data.data;
  },

  async createDocument(payload) {
    const response = await api.post('/documents', payload);
    return response.data.data;
  },

  async updateDocument(id, payload) {
    const response = await api.put(`/documents/${id}`, payload);
    return response.data.data;
  },

  async deleteDocument(id) {
    await api.delete(`/documents/${id}`);
  },
};

export default documentService;
