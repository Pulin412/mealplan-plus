import withSerwist from "@serwist/next";

const withSW = withSerwist({
  swSrc: "app/sw.ts",
  swDest: "public/sw.js",
  disable: process.env.NODE_ENV === "development",
});

/** @type {import('next').NextConfig} */
const nextConfig = {
  webpack: (config, { nextRuntime, webpack }) => {
    if (nextRuntime === "edge") {
      // config.node doesn't apply to the Edge (webworker) target in webpack 5.
      // DefinePlugin does a compile-time text substitution that works on all
      // targets — it replaces every __dirname/__filename reference in the
      // middleware bundle (including those from @serwist/next internals) with
      // a safe string before the bundle is sealed, preventing the ReferenceError
      // at Vercel Edge Runtime.
      config.plugins.push(
        new webpack.DefinePlugin({
          __dirname: JSON.stringify("/"),
          __filename: JSON.stringify("/"),
        })
      );
    }
    return config;
  },
};

export default withSW(nextConfig);
