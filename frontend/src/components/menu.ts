import { Menus } from '@/types/menu';

/** 左侧菜单 */
export const menuData: Menus[] = [
    {
        id: '0',
        title: '今日复习',
        index: '/dashboard',
        icon: 'Odometer'
    },
    {
        id: '1',
        title: '课程与资料',
        index: '/courses',
        icon: 'Notebook'
    },
    {
        id: '2',
        title: '题库',
        index: '/questions',
        icon: 'EditPen'
    },
    {
        id: '3',
        title: '自测',
        index: '/quiz',
        icon: 'Tickets'
    },
    {
        id: '4',
        title: 'AI 出题',
        index: '/ai',
        icon: 'MagicStick'
    },
    {
        id: '5',
        title: '统计',
        index: '/stats',
        icon: 'PieChart'
    },
    {
        id: '7',
        title: '预测卷',
        index: '/predict',
        icon: 'Aim'
    },
    {
        id: '8',
        title: '真题',
        index: '/exams',
        icon: 'Files'
    },
    {
        id: '6',
        title: '用户管理',
        index: '/admin',
        icon: 'User'
    }
];
