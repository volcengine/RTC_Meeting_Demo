export * from './constant';

export const DEMO_VERSION = '1.0.0';

export const HOST = process.env.HOST as string;

export const BOEHOST = process.env.BOEHOST as string;

export const isProd = window.location.hostname === 'demo.volcvideo.com';
