import withSerwist from "@serwist/next";

const withSW = withSerwist({
  swSrc: "app/sw.ts",
  swDest: "public/sw.js",
  disable: process.env.NODE_ENV === "development",
});

/** @type {import('next').NextConfig} */
const nextConfig = {
  webpack: (config, { nextRuntime }) => {
    if (nextRuntime === "edge") {
      // @serwist/next pulls in Node.js path utilities that reference __dirname,
      // which does not exist in Vercel's Edge Runtime. Mock it so the middleware
      // bundle doesn't throw ReferenceError: __dirname is not defined.
      config.node = { ...config.node, __dirname: "mock", __filename: "mock" };
    }
    return config;
  },
};

export default withSW(nextConfig);
