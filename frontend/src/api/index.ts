import { http } from '@/utils/request';

/** 账号 */
export const login = (data: { username: string; password: string }) =>
    http.post('/auth/login', data);

export const register = (data: { username: string; password: string; nickname?: string }) =>
    http.post('/auth/register', data);

/** 课程 */
export const getCourses = (params: { page: number; size: number; keyword?: string }) =>
    http.get('/courses', params);

export const createCourse = (data: object) => http.post('/courses', data);

export const updateCourse = (id: number, data: object) => http.put(`/courses/${id}`, data);

export const deleteCourse = (id: number) => http.delete(`/courses/${id}`);

/** 章节 */
export const getChapters = (courseId: number) => http.get(`/courses/${courseId}/chapters`);

export const createChapter = (courseId: number, data: object) =>
    http.post(`/courses/${courseId}/chapters`, data);

export const deleteChapter = (id: number) => http.delete(`/chapters/${id}`);

/** 资料 */
export const getMaterials = (courseId: number) => http.get('/materials', { courseId });

export const uploadMaterial = (form: FormData) =>
    http.post('/materials', form, { headers: { 'Content-Type': 'multipart/form-data' } });

export const deleteMaterial = (id: number) => http.delete(`/materials/${id}`);

export const getMaterialPages = (id: number) => http.get(`/materials/${id}/pages`);

export const searchMaterial = (id: number, keyword: string) =>
    http.get(`/materials/${id}/search`, { keyword });

/** 题目 */
export const getQuestions = (params: object) => http.get('/questions', params);

export const createQuestion = (data: object) => http.post('/questions', data);

export const deleteQuestion = (id: number) => http.delete(`/questions/${id}`);

/** 复习 */
export const getTodayCards = (limit = 50) => http.get('/review/today', { limit });

export const submitAnswer = (cardId: number, answer: string[], costMs: number) =>
    http.post(`/review/cards/${cardId}/answer`, { answer, costMs });

export const rateCard = (cardId: number, rating: number, costMs: number) =>
    http.post(`/review/cards/${cardId}/rate`, { rating, costMs });

/** 统计 */
export const getOverview = () => http.get('/review/stats/overview');

export const getWeakChapters = () => http.get('/review/stats/weak-chapters');

export const getTrend = (days = 30) => http.get('/review/stats/trend', { days });
