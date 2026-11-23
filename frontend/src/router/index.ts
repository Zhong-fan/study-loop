import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router';
import Home from '../views/home.vue';
import NProgress from 'nprogress';
import 'nprogress/nprogress.css';

/**
 * 路由表。
 *
 * 原模板用 permiss 字段做按钮级权限，我们这个项目只有一个角色，
 * 所以只保留"有没有登录"这一层判断。
 */
const routes: RouteRecordRaw[] = [
    {
        path: '/',
        redirect: '/dashboard'
    },
    {
        path: '/',
        name: 'Home',
        component: Home,
        children: [
            {
                path: '/dashboard',
                name: 'dashboard',
                meta: { title: '今日复习' },
                component: () => import('../views/dashboard.vue')
            },
            {
                path: '/courses',
                name: 'courses',
                meta: { title: '课程与资料' },
                component: () => import('../views/courses.vue')
            },
            {
                path: '/questions',
                name: 'questions',
                meta: { title: '题库' },
                component: () => import('../views/questions.vue')
            },
            {
                path: '/quiz',
                name: 'quiz',
                meta: { title: '自测' },
                component: () => import('../views/quiz.vue')
            },
            {
                path: '/ai',
                name: 'ai',
                meta: { title: 'AI 出题' },
                component: () => import('../views/ai.vue')
            },
            {
                path: '/stats',
                name: 'stats',
                meta: { title: '统计' },
                component: () => import('../views/stats.vue')
            },
            {
                path: '/predict',
                name: 'predict',
                meta: { title: '预测卷' },
                component: () => import('../views/predict.vue')
            },
            {
                path: '/exams',
                name: 'exams',
                meta: { title: '真题' },
                component: () => import('../views/exams.vue')
            },
            {
                path: '/admin',
                name: 'admin',
                meta: { title: '用户管理' },
                component: () => import('../views/admin.vue')
            }
        ]
    },
    {
        path: '/login',
        meta: { title: '登录', noAuth: true },
        component: () => import('../views/pages/login.vue')
    },
    {
        path: '/register',
        meta: { title: '注册', noAuth: true },
        component: () => import('../views/pages/login.vue')
    },
    { path: '/:path(.*)', redirect: '/dashboard' }
];

const router = createRouter({
    history: createWebHashHistory(),
    routes
});

router.beforeEach((to, _from, next) => {
    NProgress.start();
    const token = localStorage.getItem('token');

    if (!token && to.meta.noAuth !== true) {
        next('/login');
    } else if (token && (to.path === '/login' || to.path === '/register')) {
        next('/dashboard');
    } else {
        next();
    }
});

router.afterEach(() => {
    NProgress.done();
});

export default router;
