export default {
  hash: true,
  outputPath: 'output',
  publicPath: `//lf-cdn-tos.bytescm.com/obj/static/rtc_meeting/`,
  define: {
    'process.env': {
      ENV: 'PROD',
      HOST: 'https://rtc.bytedance.com/vertc_demo',
      BOEHOST: 'https://rtc.bytedance.com/vertc_demo',
    },
  },
};
