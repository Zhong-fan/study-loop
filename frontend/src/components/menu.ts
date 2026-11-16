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
        title: '统计',
        index: '/stats',
        icon: 'PieChart'
    }
];
