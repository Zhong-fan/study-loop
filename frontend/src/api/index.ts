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

export const getQuestion = (id: number) => http.get(`/questions/${id}`);

export const updateQuestion = (id: number, data: object) => http.put(`/questions/${id}`, data);

export const deleteQuestion = (id: number) => http.delete(`/questions/${id}`);

/** 复习 */
export const getDueQuestions = (limit = 20) => http.get('/review/due-questions', { limit });

export const submitQuestionAnswer = (questionId: number, answer: string[], costMs: number) =>
    http.post(`/review/questions/${questionId}/answer`, { answer, costMs });

export const rateQuestion = (questionId: number, rating: number, costMs: number) =>
    http.post(`/review/questions/${questionId}/rate`, { rating, costMs });

/** 统计 */
export const getOverview = () => http.get('/review/stats/overview');
export const getWeakChapters = () => http.get('/review/stats/weak-chapters');

export const getTrend = (days = 30) => http.get('/review/stats/trend', { days });

/** 自测组卷 */
export const createQuiz = (data: { courseId: number; count: number; title?: string }) =>
    http.post('/quizzes', data);

export const getQuiz = (id: number) => http.get(`/quizzes/${id}`);

export const submitQuiz = (id: number, answers: object[]) =>
    http.post(`/quizzes/${id}/submit`, { answers });

/** 知识点（节） */
export const getKnowledgeByChapter = (chapterId: number) => http.get('/knowledge', { chapterId });

export const getKnowledgeByCourse = (courseId: number) => http.get('/knowledge/by-course', { courseId });

export const createKnowledge = (courseId: number, data: object) =>
    http.post(`/knowledge/${courseId}`, data);

export const deleteKnowledge = (id: number) => http.delete(`/knowledge/${id}`);

/** 覆盖与调度 */
export const getCoverage = (courseId: number) => http.get('/insight/coverage', { courseId });

export const getDueMastery = () => http.get('/insight/due');

export const getWeakKnowledge = () => http.get('/insight/weak');
export const getTransfer = () => http.get('/insight/transfer');

/** 知识点证据：出处页码与原文 */
export const getKnowledgeEvidence = (courseId: number) =>
    http.get('/knowledge/evidence', { courseId });

/** 按知识点生成题目草稿（多知识点、带评分点） */
export const aiGenerateDrafts = (data: object) => http.post('/ai/generate-drafts', data);

/** 草稿存入题库（含知识点关联与评分点） */
export const saveDrafts = (courseId: number, chapterId: number | null, drafts: any[]) =>
    http.post('/questions/from-drafts', drafts, { params: { courseId, chapterId } });

export const getNotifications = () => http.get('/insight/notifications');

export const markNotificationsRead = () => http.post('/insight/notifications/read-all');

/** 教育 AI */
export const getAiStatus = () => http.get('/ai/status');

export const aiGenerateQuestions = (data: object) => http.post('/ai/questions', data);

export const aiGrade = (data: object) => http.post('/ai/grade', data);

/** 管理员 */
export const getUsers = () => http.get('/admin/users');

export const setUserStatus = (id: number, deleted: 0 | 1) =>
    http.put(`/admin/users/${id}/status?deleted=${deleted}`);

/** 页面解析：逐页交给大模型，产出结构化内容与强调信号 */
export const parseMaterial = (id: number) => http.post(`/materials/${id}/parse`);

/** 从资料里自动抽取知识点（全自动入库，重复运行不重复） */
export const extractKnowledge = (courseId: number, chapterId: number, materialId: number) =>
    http.post('/knowledge/extract', {}, { params: { courseId, chapterId, materialId } });