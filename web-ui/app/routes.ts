import { type RouteConfig, index, route } from "@react-router/dev/routes";

export default [
  index("routes/home.tsx"),
  route("c/:id", "routes/c.$id.tsx"),
  route("agent", "routes/agent.tsx"),
  route("agent/c/:id", "routes/agent.c.$id.tsx"),
  route("roleplay", "routes/roleplay.tsx"),
] satisfies RouteConfig;
