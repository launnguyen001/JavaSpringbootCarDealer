import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Proxy/rewrite: gọi backend qua cùng origin để miễn CORS
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: "http://localhost:8080/api/:path*",
      },
    ];
  },
  // Cho phép next/image load ảnh từ Unsplash và media backend (dev local)
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "images.unsplash.com",
        port: "",
        pathname: "/**",
      },
      {
        protocol: "http",
        hostname: "localhost",
        port: "8080",
        pathname: "/**",
      },
      {
        protocol: "http",
        hostname: "127.0.0.1",
        port: "8080",
        pathname: "/**",
      },
    ],
  },
};

export default nextConfig;
