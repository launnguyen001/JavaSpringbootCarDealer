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
  // Cho phép next/image load ảnh từ Unsplash
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "images.unsplash.com",
        port: "",
        pathname: "/**",
      },
    ],
  },
};

export default nextConfig;
